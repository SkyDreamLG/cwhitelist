package org.skydream.cwhitelist;

import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    // 日志配置
    public static final ModConfigSpec.BooleanValue ENABLE_LOGGING;
    public static final ModConfigSpec.IntValue LOG_RETENTION_DAYS;
    public static final ModConfigSpec.IntValue LOG_CUT_SIZE_MB;
    public static final boolean ENABLE_NAME_CHECK = true;
    public static final boolean ENABLE_UUID_CHECK = true;
    public static boolean ENABLE_IP_CHECK = true;

    static {
        ENABLE_LOGGING = BUILDER
                .comment("Enable logging for whitelist events")
                .define("enableLogging", true);

        LOG_RETENTION_DAYS = BUILDER
                .comment("Number of days to retain logs")
                .defineInRange("logRetentionDays", 7, 1, 365);

        LOG_CUT_SIZE_MB = BUILDER
                .comment("Maximum size of a single log file (in MB)")
                .defineInRange("logCutSizeMB", 10, 1, 100);

        SPEC = BUILDER.build();
    }
}