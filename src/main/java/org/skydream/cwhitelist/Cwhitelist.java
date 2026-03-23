package org.skydream.cwhitelist;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
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

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // 检查白名单
            if (!WhitelistManager.isAllowed(player)) {
                net.minecraft.network.chat.Component kickMessage = net.minecraft.network.chat.Component.translatable(
                        "cwhitelist.player.kick.not_whitelisted"
                ).withStyle(net.minecraft.ChatFormatting.RED);

                player.connection.disconnect(kickMessage);
            }
        }
    }
}