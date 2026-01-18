package org.skydream.cwhitelist;

import com.google.gson.*;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.io.IOException;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class ApiClient {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().create();

    private static HttpClient httpClient;
    private static String baseUrl;
    private static String apiToken;
    private static boolean useHeaderAuth = true;
    private static String serverId = "";
    private static boolean sendServerId = false;
    private static boolean includeExpired = false;
    private static int timeoutSeconds;

    // 缓存相关
    private static final Map<String, CacheEntry> whitelistCache = new ConcurrentHashMap<>();
    private static int cacheDurationSeconds = 0;
    private static Instant lastSyncTime = Instant.MIN;

    // Token权限信息
    private static TokenInfo tokenInfo = null;
    private static final AtomicBoolean isTokenVerified = new AtomicBoolean(false);

    // 请求队列
    private static final Queue<Runnable> requestQueue = new ArrayDeque<>();
    private static boolean isProcessingQueue = false;

    // 强制刷新标记
    private static final boolean forceRefresh = false;

    public static boolean isForceRefresh() {
        return forceRefresh;
    }

    static class CacheEntry {
        final Object data;
        final Instant expiryTime;

        CacheEntry(Object data, int durationSeconds) {
            this.data = data;
            this.expiryTime = Instant.now().plusSeconds(durationSeconds);
        }

        boolean isExpired() {
            return Instant.now().isAfter(expiryTime);
        }
    }

    public static class TokenInfo {
        public final String id;
        public final String name;
        public final Instant expiresAt;
        public final boolean canRead;
        public final boolean canWrite;
        public final boolean canDelete;
        public final boolean canManage;
        public final boolean isActive;

        public TokenInfo(JsonObject json) {
            Instant expiresAt1;
            this.id = json.get("id").getAsString();
            this.name = json.get("name").getAsString();
            this.isActive = json.get("is_active").getAsBoolean();

            JsonObject permissions = json.getAsJsonObject("permissions");
            this.canRead = permissions.get("can_read").getAsBoolean();
            this.canWrite = permissions.get("can_write").getAsBoolean();
            this.canDelete = permissions.get("can_delete").getAsBoolean();
            this.canManage = permissions.get("can_manage").getAsBoolean();

            JsonElement expiresElement = json.get("expires_at");
            if (expiresElement != null && !expiresElement.isJsonNull()) {
                String expiresStr = expiresElement.getAsString();
                try {
                    // 修复日期时间解析问题
                    expiresAt1 = parseDateTime(expiresStr);
                } catch (Exception e) {
                    LOGGER.warn("Failed to parse expires_at: {}, using max value", expiresStr);
                    expiresAt1 = Instant.MAX;
                }
            } else {
                expiresAt1 = Instant.MAX;
            }
            this.expiresAt = expiresAt1;
        }

        /**
         * 安全地解析日期时间字符串
         */
        private static Instant parseDateTime(String dateTimeStr) {
            try {
                // 先尝试标准ISO格式
                return Instant.parse(dateTimeStr);
            } catch (DateTimeParseException e1) {
                try {
                    // 如果失败，尝试添加时区信息
                    if (!dateTimeStr.endsWith("Z") && !dateTimeStr.contains("+")) {
                        // 添加UTC时区
                        dateTimeStr += "Z";
                    }
                    return Instant.parse(dateTimeStr);
                } catch (DateTimeParseException e2) {
                    // 如果还是失败，使用本地解析
                    try {
                        // 移除微秒部分
                        if (dateTimeStr.contains(".")) {
                            dateTimeStr = dateTimeStr.split("\\.")[0] + "Z";
                        }
                        return Instant.parse(dateTimeStr);
                    } catch (DateTimeParseException e3) {
                        LOGGER.error("Failed to parse date time: {}", dateTimeStr);
                        throw e3;
                    }
                }
            }
        }

        public boolean isValidForReading() {
            return isActive && canRead && !isExpired();
        }

        public boolean isValidForWriting() {
            return !isActive || !canWrite || isExpired();
        }

        public boolean isValidForDeleting() {
            return isActive && canDelete && !isExpired();
        }

        private boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }

        @Override
        public String toString() {
            return String.format("TokenInfo{id='%s', name='%s', read=%s, write=%s, delete=%s, expires=%s}",
                    id, name, canRead, canWrite, canDelete,
                    expiresAt.equals(Instant.MAX) ? "Never" : expiresAt.toString());
        }
    }

    public static void initialize() {
        boolean enableApi = Config.ENABLE_API.get();
        LOGGER.info("Initializing API client. API enabled: {}", enableApi);

        if (!enableApi) {
            LOGGER.info("API integration is disabled in config");
            return;
        }

        baseUrl = Config.API_BASE_URL.get();
        apiToken = Config.API_TOKEN.get();
        useHeaderAuth = Config.API_USE_HEADER_AUTH.get();
        timeoutSeconds = Config.API_TIMEOUT_SECONDS.get();
        cacheDurationSeconds = Config.API_CACHE_DURATION_SECONDS.get();
        serverId = Config.SERVER_ID.get();
        sendServerId = Config.API_SEND_SERVER_ID.get();
        includeExpired = Config.API_INCLUDE_EXPIRED.get();

        LOGGER.info("API Configuration:");
        LOGGER.info("  Base URL: {}", baseUrl);
        LOGGER.info("  Use Header Auth: {}", useHeaderAuth);
        LOGGER.info("  Timeout: {} seconds", timeoutSeconds);
        LOGGER.info("  Cache Duration: {} seconds", cacheDurationSeconds);
        LOGGER.info("  Token configured: {}",
                apiToken != null && !apiToken.trim().isEmpty() ? "YES" : "NO");

        if (apiToken == null || apiToken.trim().isEmpty()) {
            LOGGER.error("API token is not set. Please configure token in cwhitelist-common.toml");
            LOGGER.error("Example: token = \"your-actual-token-here\"");
            return;
        }

        // 创建HTTP客户端
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .version(HttpClient.Version.HTTP_2)
                .build();

        LOGGER.info("API client initialized successfully");

        // 首先测试健康检查（不需要Token）
        LOGGER.info("Testing API health check...");
        healthCheck().thenAccept(healthy -> {
            if (healthy) {
                LOGGER.info("✅ API health check PASSED");

                // 如果健康检查通过，验证Token
                LOGGER.info("Verifying API token...");
                verifyToken().thenAccept(tokenVerified -> {
                    if (tokenVerified) {
                        LOGGER.info("✅ Token verification SUCCESSFUL");
                        LOGGER.info("Token info: {}", tokenInfo);
                    } else {
                        LOGGER.error("❌ Token verification FAILED");
                        LOGGER.error("Please check if token is valid and has required permissions");
                    }
                });
            } else {
                LOGGER.error("❌ API health check FAILED");
                LOGGER.error("Please check if API server is running at: {}", baseUrl);
            }
        }).exceptionally(e -> {
            LOGGER.error("❌ API health check ERROR: {}", e.getMessage());
            return null;
        });
    }

    public static boolean isEnabled() {
        return Config.ENABLE_API.get() && httpClient != null && apiToken != null && !apiToken.trim().isEmpty();
    }

    public static boolean hasValidToken() {
        return !isTokenVerified.get() || tokenInfo == null;
    }

    public static CompletableFuture<Boolean> verifyToken() {
        if (!isEnabled()) {
            return CompletableFuture.completedFuture(false);
        }

        return sendRequest("/tokens/verify", "GET", null, false)
                .thenApply(response -> {
                    try {
                        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                        if (json.get("success").getAsBoolean()) {
                            tokenInfo = new TokenInfo(json.getAsJsonObject("token"));
                            isTokenVerified.set(true);
                            LOGGER.info("Token verified: {}", tokenInfo.name);
                            return true;
                        } else {
                            LOGGER.error("Token verification failed: {}", json.get("message").getAsString());
                            return false;
                        }
                    } catch (Exception e) {
                        LOGGER.error("Failed to parse token verification response", e);
                        return false;
                    }
                })
                .exceptionally(e -> {
                    LOGGER.error("Token verification request failed", e);
                    return false;
                });
    }

    public static CompletableFuture<Boolean> healthCheck() {
        // 健康检查不需要Token
        return sendRequest("/health", "GET", null, true)
                .thenApply(response -> {
                    try {
                        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                        return json.get("success").getAsBoolean() &&
                                json.get("status").getAsString().equals("ok");
                    } catch (Exception e) {
                        LOGGER.error("Failed to parse health check response", e);
                        return false;
                    }
                })
                .exceptionally(e -> {
                    LOGGER.error("Health check failed", e);
                    return false;
                });
    }

    public static CompletableFuture<List<?>> syncWhitelist() {
        return syncWhitelist(false);
    }

    /**
     * 同步白名单，可指定是否强制刷新
     */
    public static CompletableFuture<List<?>> syncWhitelist(boolean force) {
        if (!isEnabled()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        // 检查Token权限
        if (hasValidToken() || !tokenInfo.isValidForReading()) {
            LOGGER.error("Token does not have read permission or is invalid");
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        // 检查缓存，除非强制刷新
        if (cacheDurationSeconds > 0 && !force) {
            CacheEntry cached = whitelistCache.get("whitelist");
            if (cached != null && !cached.isExpired()) {
                LOGGER.debug("Returning cached whitelist");
                return CompletableFuture.completedFuture((List<WhitelistManager.WhitelistEntry>) cached.data);
            }
        }

        // 构建查询参数
        StringBuilder urlBuilder = new StringBuilder("/whitelist/sync");
        urlBuilder.append("?only_active=true");

        // 添加强制刷新参数
        if (force) {
            urlBuilder.append("&force_refresh=true");
            LOGGER.debug("Forcing refresh from API (bypassing cache)");
        }

        if (sendServerId && !serverId.isEmpty()) {
            urlBuilder.append("&server_id=").append(URLEncoder.encode(serverId, java.nio.charset.StandardCharsets.UTF_8));
        }

        if (includeExpired) {
            urlBuilder.append("&include_expired=true");
        }

        return sendRequest(urlBuilder.toString(), "GET", null, false)
                .thenApply(response -> {
                    try {
                        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                        if (json.get("success").getAsBoolean()) {
                            JsonArray entriesJson = json.get("entries").getAsJsonArray();
                            List<WhitelistManager.WhitelistEntry> entries = new ArrayList<>();

                            for (JsonElement elem : entriesJson) {
                                JsonObject entryJson = elem.getAsJsonObject();
                                String type = entryJson.get("type").getAsString();
                                String value = entryJson.get("value").getAsString();

                                // 验证类型
                                if (Arrays.asList("name", "uuid", "ip").contains(type)) {
                                    entries.add(new WhitelistManager.WhitelistEntry(type, value));
                                } else {
                                    LOGGER.warn("Ignoring entry with invalid type: {}", type);
                                }
                            }

                            // 更新缓存
                            if (cacheDurationSeconds > 0) {
                                whitelistCache.put("whitelist", new CacheEntry(entries, cacheDurationSeconds));
                                lastSyncTime = Instant.now();
                                LOGGER.debug("Updated cache with {} entries, expires at {}",
                                        entries.size(),
                                        Instant.now().plusSeconds(cacheDurationSeconds));
                            }

                            LOGGER.info("Successfully synced {} whitelist entries from API", entries.size());
                            return entries;
                        } else {
                            LOGGER.error("API sync failed: {}", json.get("message").getAsString());
                            return Collections.emptyList();
                        }
                    } catch (Exception e) {
                        LOGGER.error("Failed to parse whitelist sync response", e);
                        return Collections.emptyList();
                    }
                })
                .exceptionally(e -> {
                    LOGGER.error("Failed to sync whitelist from API", e);
                    return Collections.emptyList();
                });
    }

    /**
     * 强制从API刷新白名单（忽略缓存）
     */
    public static CompletableFuture<List<?>> forceSyncWhitelist() {
        // 清除缓存
        clearCache();
        return syncWhitelist(true);
    }

    public static CompletableFuture<Boolean> addEntry(WhitelistManager.WhitelistEntry entry) {
        if (!isEnabled()) {
            return CompletableFuture.completedFuture(false);
        }

        // 检查Token权限
        if (hasValidToken() || tokenInfo.isValidForWriting()) {
            LOGGER.error("Token does not have write permission or is invalid");
            return CompletableFuture.completedFuture(false);
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("type", entry.getType());
        requestBody.put("value", entry.getValue());
        requestBody.put("is_active", true);

        String bodyJson = GSON.toJson(requestBody);

        return sendRequest("/whitelist/entries", "POST", bodyJson, false)
                .thenApply(response -> {
                    try {
                        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                        boolean success = json.get("success").getAsBoolean();

                        if (success) {
                            // 清除缓存
                            whitelistCache.remove("whitelist");
                            LOGGER.info("Successfully added entry via API: {}={}",
                                    entry.getType(), entry.getValue());
                        } else {
                            String message = json.get("message").getAsString();
                            LOGGER.error("Failed to add entry via API: {}", message);

                            // 检查是否条目已存在
                            if (message.contains("already exists")) {
                                LOGGER.warn("Entry already exists in API");
                            }
                        }

                        return success;
                    } catch (Exception e) {
                        LOGGER.error("Failed to parse add entry response", e);
                        return false;
                    }
                })
                .exceptionally(e -> {
                    LOGGER.error("Failed to add entry via API", e);
                    return false;
                });
    }

    public static CompletableFuture<Boolean> removeEntry(String type, String value) {
        if (!isEnabled()) {
            return CompletableFuture.completedFuture(false);
        }

        // 检查Token权限
        if (hasValidToken() || !tokenInfo.isValidForDeleting()) {
            LOGGER.error("Token does not have delete permission or is invalid");
            return CompletableFuture.completedFuture(false);
        }

        String encodedType = URLEncoder.encode(type, java.nio.charset.StandardCharsets.UTF_8);
        String encodedValue = URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
        String url = String.format("/whitelist/entries/%s/%s", encodedType, encodedValue);

        return sendRequest(url, "DELETE", null, false)
                .thenApply(response -> {
                    try {
                        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                        boolean success = json.get("success").getAsBoolean();

                        if (success) {
                            // 清除缓存
                            whitelistCache.remove("whitelist");
                            LOGGER.info("Successfully removed entry via API: {}={}", type, value);
                        } else {
                            LOGGER.error("Failed to remove entry via API: {}",
                                    json.get("message").getAsString());
                        }

                        return success;
                    } catch (Exception e) {
                        LOGGER.error("Failed to parse remove entry response", e);
                        return false;
                    }
                })
                .exceptionally(e -> {
                    LOGGER.error("Failed to remove entry via API", e);
                    return false;
                });
    }

    public static void logLoginEvent(ServerPlayer player, boolean allowed, String checkType) {
        if (!isEnabled() || !Config.API_LOG_LOGIN_EVENTS.get()) {
            return;
        }

        // 检查Token权限
        if (hasValidToken() || tokenInfo.isValidForWriting()) {
            LOGGER.warn("Token does not have write permission, skipping login event logging");
            return;
        }

        queueRequest(() -> {
            try {
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("player_name", PlayerCompat.getPlayerNameSafe(player));
                requestBody.put("player_uuid", PlayerCompat.getPlayerUuidSafe(player));
                requestBody.put("player_ip", getPlayerIP(player));
                requestBody.put("allowed", allowed);
                requestBody.put("check_type", checkType != null ? checkType : "none");

                String bodyJson = GSON.toJson(requestBody);

                // 异步发送，不等待响应
                sendRequest("/login/log", "POST", bodyJson, false)
                        .exceptionally(e -> {
                            LOGGER.warn("Failed to log login event to API", e);
                            return null;
                        });
            } catch (Exception e) {
                LOGGER.error("Error preparing login log request", e);
            }
        });
    }

    private static String getPlayerIP(ServerPlayer player) {
        try {
            return ((java.net.InetSocketAddress) player.connection.getConnection()
                    .getRemoteAddress()).getAddress().getHostAddress();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private static void queueRequest(Runnable request) {
        synchronized (requestQueue) {
            requestQueue.add(request);
        }

        if (!isProcessingQueue) {
            processQueue();
        }
    }

    private static void processQueue() {
        synchronized (requestQueue) {
            if (requestQueue.isEmpty()) {
                isProcessingQueue = false;
                return;
            }

            isProcessingQueue = true;
            Runnable request = requestQueue.poll();

            CompletableFuture.runAsync(() -> {
                try {
                    request.run();
                } finally {
                    processQueue();
                }
            });
        }
    }

    private static CompletableFuture<String> sendRequest(String endpoint, String method, String body, boolean skipAuth) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = baseUrl + endpoint;
                LOGGER.debug("Preparing API request to: {}", url);
                LOGGER.debug("Method: {}, SkipAuth: {}", method, skipAuth);

                // 构建请求URL（如果使用查询参数认证）
                if (!skipAuth && !useHeaderAuth) {
                    String separator = url.contains("?") ? "&" : "?";
                    url = url + separator + "token=" + URLEncoder.encode(apiToken, java.nio.charset.StandardCharsets.UTF_8);
                    LOGGER.debug("Using query parameter authentication");
                }

                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(timeoutSeconds))
                        .header("Content-Type", "application/json")
                        .header("User-Agent", "CWhitelist-Mod/1.0");

                // 添加认证头（如果使用头部认证）
                if (!skipAuth && useHeaderAuth) {
                    String authHeader = "Bearer " + apiToken;
                    requestBuilder.header("Authorization", authHeader);
                    LOGGER.debug("Adding Authorization header: Bearer {}",
                            apiToken.substring(0, Math.min(8, apiToken.length())) + "...");
                } else if (skipAuth) {
                    LOGGER.debug("Skipping authentication (health check)");
                }

                // 设置方法和请求体
                switch (method.toUpperCase()) {
                    case "GET":
                        requestBuilder.GET();
                        break;
                    case "POST":
                        requestBuilder.POST(body == null ?
                                HttpRequest.BodyPublishers.noBody() :
                                HttpRequest.BodyPublishers.ofString(body));
                        LOGGER.debug("Request body: {}", body);
                        break;
                    case "DELETE":
                        requestBuilder.DELETE();
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported method: " + method);
                }

                HttpRequest request = requestBuilder.build();
                LOGGER.debug("Sending HTTP request...");

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                LOGGER.debug("Response status: {}", response.statusCode());
                LOGGER.debug("Response body: {}", response.body());

                // 处理响应
                if (response.statusCode() == 401 || response.statusCode() == 403) {
                    // Token认证失败，标记为未验证
                    isTokenVerified.set(false);
                    LOGGER.error("Authentication failed: HTTP {} - {}",
                            response.statusCode(), response.body());

                    // 尝试解析错误信息
                    try {
                        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                        String message = json.get("message").getAsString();
                        LOGGER.error("API Error: {}", message);
                    } catch (Exception e) {
                        LOGGER.error("Could not parse error response");
                    }

                    throw new IOException("Authentication failed: " + response.statusCode());
                }

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return response.body();
                } else {
                    LOGGER.error("API request failed with status: {}", response.statusCode());
                    throw new IOException(String.format("API request failed: %d %s",
                            response.statusCode(), response.body()));
                }
            } catch (Exception e) {
                LOGGER.error("API request failed with exception: {}", e.getMessage());
                throw new RuntimeException("API request failed: " + e.getMessage(), e);
            }
        });
    }

    public static void clearCache() {
        whitelistCache.clear();
        lastSyncTime = Instant.MIN;
        LOGGER.info("API cache cleared");
    }

    public static Instant getLastSyncTime() {
        return lastSyncTime;
    }

    public static TokenInfo getTokenInfo() {
        return tokenInfo;
    }

    public static String getTokenStatus() {
        if (!isEnabled()) {
            return "API disabled";
        }

        if (hasValidToken()) {
            return "Token not verified";
        }

        return String.format("Token: %s (R:%s, W:%s, D:%s, Expires: %s)",
                tokenInfo.name,
                tokenInfo.canRead ? "✓" : "✗",
                tokenInfo.canWrite ? "✓" : "✗",
                tokenInfo.canDelete ? "✓" : "✗",
                tokenInfo.expiresAt.equals(Instant.MAX) ? "Never" : tokenInfo.expiresAt.toString()
        );
    }
}