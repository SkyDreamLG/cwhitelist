package org.skydream.cwhitelist;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

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
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // 定期清理过期日志
        cleanOldLogs();
        // 加载白名单
        WhitelistManager.loadAsync();
        LOGGER.info("CWhitelist mod initialized and whitelist loaded!");
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        WhitelistCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("CWhitelist mod is ready on server!");
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (!WhitelistManager.isAllowed(player)) {
                player.connection.disconnect(net.minecraft.network.chat.Component.literal(WhitelistCommand.getLocaleString("commands.cwhitelist.banned")));
            }
        }
    }
}