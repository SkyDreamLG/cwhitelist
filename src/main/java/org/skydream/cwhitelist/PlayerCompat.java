package org.skydream.cwhitelist;

import net.minecraft.server.level.ServerPlayer;
import com.mojang.authlib.GameProfile;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.lang.reflect.Method;
import java.util.UUID;

public class PlayerCompat {
    private static final Logger LOGGER = LogUtils.getLogger();

    // 使用反射缓存方法，避免每次调用都查找
    private static Method getNameMethod = null;
    private static Method getUuidMethod = null;
    private static boolean methodsInitialized = false;

    /**
     * 初始化反射方法
     */
    private static synchronized void initializeMethods() {
        if (methodsInitialized) {
            return;
        }

        try {
            // 尝试获取旧版本的 GameProfile 方法
            Class<?> gameProfileClass = GameProfile.class;

            // 方法1: getName() - 旧版本
            try {
                getNameMethod = gameProfileClass.getMethod("getName");
                LOGGER.debug("Found GameProfile.getName() method (old API)");
            } catch (NoSuchMethodException e) {
                LOGGER.debug("GameProfile.getName() not found, using new API");
            }

            // 方法2: getId() - 旧版本
            try {
                getUuidMethod = gameProfileClass.getMethod("getId");
                LOGGER.debug("Found GameProfile.getId() method (old API)");
            } catch (NoSuchMethodException e) {
                LOGGER.debug("GameProfile.getId() not found, using new API");
            }

            methodsInitialized = true;
        } catch (Exception e) {
            LOGGER.error("Failed to initialize reflection methods", e);
        }
    }

    /**
     * 获取玩家名称（兼容性方法）
     */
    public static String getPlayerName(ServerPlayer player) {
        initializeMethods();

        // 首先尝试新版本的 API
        try {
            // 新版本: player.getName().getString()
            return player.getName().getString();
        } catch (Exception e1) {
            // 新版本失败，尝试使用反射调用旧版本方法
            if (getNameMethod != null) {
                try {
                    GameProfile profile = player.getGameProfile();
                    Object result = getNameMethod.invoke(profile);
                    if (result instanceof String) {
                        return (String) result;
                    }
                } catch (Exception e2) {
                    LOGGER.warn("Reflection call to GameProfile.getName() failed", e2);
                }
            }

            // 所有方法都失败
            LOGGER.error("Failed to get player name using all methods", e1);
            return "unknown";
        }
    }

    /**
     * 获取玩家UUID（兼容性方法）
     */
    public static String getPlayerUuid(ServerPlayer player) {
        initializeMethods();

        // 首先尝试新版本的 API
        try {
            // 新版本: player.getUUID()
            return player.getUUID().toString();
        } catch (Exception e1) {
            // 新版本失败，尝试使用反射调用旧版本方法
            if (getUuidMethod != null) {
                try {
                    GameProfile profile = player.getGameProfile();
                    Object result = getUuidMethod.invoke(profile);
                    if (result instanceof UUID) {
                        return ((UUID) result).toString();
                    }
                } catch (Exception e2) {
                    LOGGER.warn("Reflection call to GameProfile.getId() failed", e2);
                }
            }

            // 所有方法都失败
            LOGGER.error("Failed to get player UUID using all methods", e1);
            return UUID.randomUUID().toString();
        }
    }

    /**
     * 获取玩家名称（无反射，用于编译检查）
     */
    public static String getPlayerNameSafe(ServerPlayer player) {
        try {
            return getPlayerName(player);
        } catch (Exception e) {
            LOGGER.error("Error in getPlayerNameSafe", e);
            return "unknown";
        }
    }

    /**
     * 获取玩家UUID（无反射，用于编译检查）
     */
    public static String getPlayerUuidSafe(ServerPlayer player) {
        try {
            return getPlayerUuid(player);
        } catch (Exception e) {
            LOGGER.error("Error in getPlayerUuidSafe", e);
            return UUID.randomUUID().toString();
        }
    }
}