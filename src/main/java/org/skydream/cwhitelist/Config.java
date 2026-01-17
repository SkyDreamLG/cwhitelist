package org.skydream.cwhitelist;

import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.List;

public class Config {
    public static final ModConfigSpec SPEC;

    // 基础配置
    public static final ModConfigSpec.BooleanValue ENABLE_LOGGING;
    public static final ModConfigSpec.IntValue LOG_RETENTION_DAYS;
    public static final ModConfigSpec.IntValue LOG_CUT_SIZE_MB;

    // 检查配置
    public static final ModConfigSpec.BooleanValue ENABLE_NAME_CHECK;
    public static final ModConfigSpec.BooleanValue ENABLE_UUID_CHECK;
    public static final ModConfigSpec.BooleanValue ENABLE_IP_CHECK;

    // API配置
    public static final ModConfigSpec.BooleanValue ENABLE_API;
    public static final ModConfigSpec.ConfigValue<String> API_BASE_URL;
    public static final ModConfigSpec.ConfigValue<String> API_TOKEN;
    public static final ModConfigSpec.BooleanValue API_USE_HEADER_AUTH;
    public static final ModConfigSpec.IntValue API_TIMEOUT_SECONDS;
    public static final ModConfigSpec.IntValue API_CACHE_DURATION_SECONDS;
    public static final ModConfigSpec.BooleanValue API_SYNC_ON_STARTUP;
    public static final ModConfigSpec.BooleanValue API_LOG_LOGIN_EVENTS;
    public static final ModConfigSpec.ConfigValue<String> SERVER_ID;
    public static final ModConfigSpec.BooleanValue API_SEND_SERVER_ID;
    public static final ModConfigSpec.BooleanValue API_INCLUDE_EXPIRED;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        // 基础配置
        builder.comment("Basic Settings").push("basic");
        ENABLE_LOGGING = builder
                .comment("Enable local logging")
                .define("enableLogging", true);
        LOG_RETENTION_DAYS = builder
                .comment("Log retention days")
                .defineInRange("logRetentionDays", 7, 1, 365);
        LOG_CUT_SIZE_MB = builder
                .comment("Max log size (MB)")
                .defineInRange("logCutSizeMB", 10, 1, 100);
        builder.pop();

        // 检查配置
        builder.comment("Check Settings").push("checks");
        ENABLE_NAME_CHECK = builder
                .comment("Enable name check")
                .define("enableNameCheck", true);
        ENABLE_UUID_CHECK = builder
                .comment("Enable UUID check")
                .define("enableUuidCheck", true);
        ENABLE_IP_CHECK = builder
                .comment("Enable IP check")
                .define("enableIpCheck", true);
        builder.pop();

        // API配置
        builder.comment("API Settings").push("api");
        ENABLE_API = builder
                .comment("Enable API integration")
                .define("enableApi", false);
        API_BASE_URL = builder
                .comment("API base URL")
                .define("baseUrl", "http://127.0.0.1:5000/api");
        API_TOKEN = builder
                .comment("API authentication token")
                .define("token", "");
        API_USE_HEADER_AUTH = builder
                .comment("Use header authentication (true for header, false for query param)")
                .define("useHeaderAuth", true);
        API_TIMEOUT_SECONDS = builder
                .comment("API timeout in seconds")
                .defineInRange("timeoutSeconds", 10, 1, 60);
        API_CACHE_DURATION_SECONDS = builder
                .comment("Cache duration in seconds (0 to disable)")
                .defineInRange("cacheDurationSeconds", 30, 0, 3600);
        API_SYNC_ON_STARTUP = builder
                .comment("Sync whitelist on mod startup")
                .define("syncOnStartup", true);
        API_LOG_LOGIN_EVENTS = builder
                .comment("Send login events to API")
                .define("logLoginEvents", true);
        SERVER_ID = builder
                .comment("Server identifier (optional)")
                .define("serverId", "");
        API_SEND_SERVER_ID = builder
                .comment("Send server ID in requests")
                .define("sendServerId", false);
        API_INCLUDE_EXPIRED = builder
                .comment("Include expired entries when syncing")
                .define("includeExpired", false);
        builder.pop();

        SPEC = builder.build();
    }
}