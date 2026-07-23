package org.skydream.cwhitelist;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import org.slf4j.Logger;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.skydream.cwhitelist.LogHandler.cleanOldLogs;

@Mod(Cwhitelist.MODID)
public class Cwhitelist {
    public static final String MODID = "cwhitelist";
    static final Logger LOGGER = LogUtils.getLogger();

    public Cwhitelist(IEventBus modEventBus, ModContainer modContainer) {
        // 注册配置
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        // 注册事件监听
        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.register(this);

        // 注册命令
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);

        // 注册权限节点
        NeoForge.EVENT_BUS.addListener(this::onPermissionGather);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // 初始化API客户端
            ApiClient.initialize();

            // 预加载翻译文件（用于服务端翻译，兼容未安装模组的客户端）
            loadTranslations();

            // 定期清理过期日志
            cleanOldLogs();

            // 加载白名单
            WhitelistManager.loadAsync();

            LOGGER.info("CWhitelist mod initialized");
            LOGGER.info("API Integration: {}", Config.ENABLE_API.get() ? "ENABLED" : "DISABLED");

            if (Config.ENABLE_API.get()) {
                LOGGER.info("API Base URL: {}", Config.API_BASE_URL.get());

                // 检查Token状态
                CompletableFuture.runAsync(() -> {
                    try {
                        Thread.sleep(2000);
                        if (ApiClient.isEnabled()) {
                            LOGGER.info("Token Status: {}", ApiClient.getTokenStatus());
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
        });
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        LOGGER.info("Registering CWhitelist commands...");
        WhitelistCommand.register(event.getDispatcher());
        LOGGER.info("CWhitelist commands registered successfully");
    }

    /**
     * 注册所有权限节点
     */
    private void onPermissionGather(PermissionGatherEvent.Nodes event) {
        event.addNodes(
                WhitelistCommand.PERMISSION_ADMIN,
                WhitelistCommand.PERMISSION_USE,
                WhitelistCommand.PERMISSION_ADD,
                WhitelistCommand.PERMISSION_REMOVE,
                WhitelistCommand.PERMISSION_LIST,
                WhitelistCommand.PERMISSION_RELOAD,
                WhitelistCommand.PERMISSION_API,
                WhitelistCommand.PERMISSION_API_STATUS,
                WhitelistCommand.PERMISSION_API_HEALTH,
                WhitelistCommand.PERMISSION_API_SYNC,
                WhitelistCommand.PERMISSION_API_VERIFY,
                WhitelistCommand.PERMISSION_API_CLEARCACHE
        );

        LOGGER.info("Registered {} permission nodes for CWhitelist", 13);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("CWhitelist mod is ready on server!");

        // 记录当前使用的白名单源
        if (Config.ENABLE_API.get()) {
            String source = WhitelistManager.isUsingApi() ?
                    "API (Primary)" : "Local File (Fallback)";
            LOGGER.info("Current whitelist source: {}", source);
            LOGGER.info("Loaded entries: {}", WhitelistManager.getEntryCount());

            if (!WhitelistManager.isApiAvailable()) {
                LOGGER.warn("API is configured but not available. Using local file as fallback.");
            }
        } else {
            LOGGER.info("Using local whitelist file with {} entries",
                    WhitelistManager.getEntryCount());
        }
    }

    /** 预加载的翻译表: languageCode -> (key -> value) */
    private static final Map<String, Map<String, String>> translations = new HashMap<>();
    private static final Gson GSON = new Gson();
    private static final String[] SUPPORTED_LANGUAGES = {"en_us", "zh_cn"};
    private static final String DEFAULT_LANGUAGE = "en_us";

    private static void loadTranslations() {
        for (String lang : SUPPORTED_LANGUAGES) {
            String path = "/assets/cwhitelist/lang/" + lang + ".json";
            try (InputStream is = Cwhitelist.class.getResourceAsStream(path)) {
                if (is == null) {
                    LOGGER.warn("Translation file not found: {}", path);
                    continue;
                }
                String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                Map<String, String> map = GSON.fromJson(json,
                        new TypeToken<Map<String, String>>() {}.getType());
                if (map != null) {
                    translations.put(lang, map);
                    LOGGER.debug("Loaded {} translations for language: {}", map.size(), lang);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load translation file: {}", path, e);
            }
        }
        LOGGER.info("Loaded translations for {} language(s)", translations.size());
    }

    /**
     * 根据玩家的客户端语言获取翻译后的踢出消息组件。
     * 在服务端完成翻译，发送 Component.literal 给客户端，
     * 确保客户端即使未安装本模组也能看到正确的翻译文本。
     */
    private static Component getKickMessageForPlayer(ServerPlayer player, String key) {
        String langCode = player.clientInformation().language().toLowerCase();
        Map<String, String> langMap = translations.get(langCode);

        // 语言代码可能带有地区变体（如 zh_cn, zh-cn），尝试拆分匹配
        if (langMap == null) {
            String normalized = langCode.replace('-', '_');
            langMap = translations.get(normalized);
            if (langMap == null && normalized.contains("_")) {
                langMap = translations.get(normalized.split("_")[0]);
            }
        }

        // 回退到默认语言（en_us）
        if (langMap == null) {
            langMap = translations.get(DEFAULT_LANGUAGE);
        }

        // 最终回退：直接使用翻译键本身
        String text;
        if (langMap != null) {
            text = langMap.getOrDefault(key, key);
        } else {
            text = key;
        }

        return Component.literal(text).withStyle(net.minecraft.ChatFormatting.RED);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // 检查白名单
            if (!WhitelistManager.isAllowed(player)) {
                // 在服务端根据客户端语言完成翻译，客户端无需安装本模组即可正常显示
                Component kickMessage = getKickMessageForPlayer(player,
                        "cwhitelist.player.kick.not_whitelisted");
                player.connection.disconnect(kickMessage);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ApiClient.logLogoutEvent(player);
        }
    }
}