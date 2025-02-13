package org.skydream.cwhitelist;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLE_LOGGING;
    public static final ModConfigSpec.IntValue LOG_RETENTION_DAYS;
    public static final ModConfigSpec.IntValue LOG_CUT_SIZE_MB;
    public static final ModConfigSpec.BooleanValue ENABLE_NAME_CHECK;
    public static final ModConfigSpec.BooleanValue ENABLE_UUID_CHECK;
    public static final ModConfigSpec.BooleanValue ENABLE_IP_CHECK;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        ENABLE_LOGGING = builder
                .comment("Enable logging").define("enableLogging", true);

        ENABLE_NAME_CHECK = builder
                .comment("Enable name check").define("enableNameCheck", true);

        ENABLE_UUID_CHECK = builder
                .comment("Enable UUID check").define("enableUuidCheck", true);

        ENABLE_IP_CHECK = builder
                .comment("Enable IP check").define("enableIpCheck", true);

        LOG_RETENTION_DAYS = builder
                .comment("Log retention days").defineInRange("logRetentionDays", 7, 1, 365);

        LOG_CUT_SIZE_MB = builder
                .comment("Max log size (MB)").defineInRange("logCutSizeMB", 10, 1, 100);

        SPEC = builder.build();
    }
}