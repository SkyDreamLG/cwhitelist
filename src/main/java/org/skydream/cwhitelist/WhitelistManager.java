package org.skydream.cwhitelist;

import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import static org.skydream.cwhitelist.LogHandler.cleanOldLogs;

public class WhitelistManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Path WHITELIST_PATH = Paths.get("config/cwhitelist_entries.json");
    private static final List<WhitelistEntry> entries = new ArrayList<>();
    private static final Gson GSON = new Gson();

    private static boolean isLoaded = false;
    private static boolean useApi = false;
    private static boolean apiAvailable = false;

    public static class WhitelistEntry {
        private final String type;
        private final String value;

        public WhitelistEntry(String type, String value) {
            this.type = type;
            this.value = value;
        }

        public String getType() {
            return type;
        }

        public String getValue() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            WhitelistEntry that = (WhitelistEntry) o;
            return type.equalsIgnoreCase(that.type) &&
                    value.equalsIgnoreCase(that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type.toLowerCase(), value.toLowerCase());
        }
    }

    public static void load() {
        if (isLoaded) return;

        // 检查是否启用API
        useApi = Config.ENABLE_API.get() && Config.API_SYNC_ON_STARTUP.get();

        cleanOldLogs();

        if (useApi) {
            loadFromApi();
        } else {
            loadFromFile();
        }
    }

    private static void loadFromFile() {
        try {
            if (!Files.exists(WHITELIST_PATH)) {
                Files.createFile(WHITELIST_PATH);
                saveToFile();
            }

            String json = Files.readString(WHITELIST_PATH);
            List<WhitelistEntry> loadedEntries = GSON.fromJson(json,
                    new TypeToken<List<WhitelistEntry>>(){}.getType());

            synchronized (entries) {
                entries.clear();
                if (loadedEntries != null) {
                    entries.addAll(loadedEntries);
                }
            }

            isLoaded = true;
            apiAvailable = false;
            LOGGER.info("Loaded {} whitelist entries from local file", entries.size());
        } catch (IOException e) {
            LOGGER.error("Failed to load whitelist from file", e);
        }
    }

    private static void loadFromApi() {
        LOGGER.info("Attempting to load whitelist from API...");

        // 首先检查API是否可用
        ApiClient.healthCheck().thenAccept(healthy -> {
            if (healthy) {
                LOGGER.info("API health check passed, proceeding with sync...");

                // 验证Token
                ApiClient.verifyToken().thenAccept(tokenVerified -> {
                    if (tokenVerified) {
                        // 从API同步白名单
                        ApiClient.syncWhitelist()
                                .thenAccept(apiEntries -> {
                                    synchronized (entries) {
                                        entries.clear();
                                        entries.addAll((Collection<? extends WhitelistEntry>) apiEntries);
                                    }

                                    isLoaded = true;
                                    apiAvailable = true;
                                    LOGGER.info("Successfully loaded {} whitelist entries from API", entries.size());

                                    // 保存到本地文件作为备份
                                    saveToFile();
                                })
                                .exceptionally(e -> {
                                    LOGGER.error("Failed to sync from API, falling back to local file", e);
                                    loadFromFile();
                                    return null;
                                });
                    } else {
                        LOGGER.error("Token verification failed, falling back to local file");
                        loadFromFile();
                    }
                });
            } else {
                LOGGER.warn("API health check failed, falling back to local file");
                loadFromFile();
            }
        }).exceptionally(e -> {
            LOGGER.error("API health check error, falling back to local file", e);
            loadFromFile();
            return null;
        });
    }

    public static void loadAsync() {
        CompletableFuture.runAsync(WhitelistManager::load);
    }

    public static boolean isAllowed(ServerPlayer player) {
        // 确保已加载
        if (!isLoaded) {
            load();
        }

        String name = PlayerCompat.getPlayerNameSafe(player);
        String uuid = PlayerCompat.getPlayerUuidSafe(player);
        String ip = getPlayerIP(player);

        boolean ENABLE_NAME_CHECK = Config.ENABLE_NAME_CHECK.get();
        boolean ENABLE_UUID_CHECK = Config.ENABLE_UUID_CHECK.get();
        boolean ENABLE_IP_CHECK = Config.ENABLE_IP_CHECK.get();

        String checkType = null;
        boolean allowed = false;

        synchronized (entries) {
            for (WhitelistEntry entry : entries) {
                if (ENABLE_NAME_CHECK && entry.type.equals("name") &&
                        entry.value.equalsIgnoreCase(name)) {
                    checkType = "name";
                    allowed = true;
                    break;
                }
                if (ENABLE_UUID_CHECK && entry.type.equals("uuid") &&
                        entry.value.equalsIgnoreCase(uuid)) {
                    checkType = "uuid";
                    allowed = true;
                    break;
                }
                if (ENABLE_IP_CHECK && entry.type.equals("ip") &&
                        matchIP(entry.value, ip)) {
                    checkType = "ip";
                    allowed = true;
                    break;
                }
            }
        }

        // 记录日志
        LogHandler.log(player, allowed);

        // 发送登录事件到API（如果启用且API可用）
        if (Config.API_LOG_LOGIN_EVENTS.get() && apiAvailable) {
            if (allowed && checkType != null) {
                ApiClient.logLoginEvent(player, true, checkType);
            } else if (!allowed) {
                ApiClient.logLoginEvent(player, false, "none");
            }
        }

        return allowed;
    }

    private static String getPlayerIP(ServerPlayer player) {
        try {
            return ((InetSocketAddress) player.connection.getConnection()
                    .getRemoteAddress()).getAddress().getHostAddress();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private static final Map<String, Pattern> regexCache = new HashMap<>();

    private static boolean matchIP(String pattern, String ip) {
        try {
            Pattern compiledPattern = regexCache.computeIfAbsent(pattern, p -> {
                String regex = p.replace(".", "\\.").replace("*", ".*");
                if (ip.contains(":")) {
                    regex = regex.replaceAll("::", "(::|:([0-9a-fA-F]{0,4}:){0,7})");
                }
                return Pattern.compile(regex);
            });

            return compiledPattern.matcher(ip).matches();
        } catch (Exception e) {
            LOGGER.error("Invalid regex pattern: " + pattern, e);
            return false;
        }
    }

    static void saveToFile() {
        try {
            synchronized (entries) {
                Files.writeString(WHITELIST_PATH, GSON.toJson(entries));
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save whitelist to file", e);
        }
    }

    public static void addEntry(WhitelistEntry entry) {
        // 先添加到内存
        synchronized (entries) {
            if (!entries.contains(entry)) {
                entries.add(entry);
            }
        }

        // 如果API可用，同步到API
        if (useApi && apiAvailable) {
            ApiClient.addEntry(entry)
                    .thenAccept(success -> {
                        if (success) {
                            // API成功，清除缓存并重新同步
                            ApiClient.clearCache();
                            ApiClient.syncWhitelist()
                                    .thenAccept(newEntries -> {
                                        synchronized (entries) {
                                            entries.clear();
                                            entries.addAll((Collection<? extends WhitelistEntry>) newEntries);
                                        }
                                        saveToFile();
                                    });
                        } else {
                            // API失败，只保存到本地
                            LOGGER.warn("Failed to add entry to API, saving locally only");
                            saveToFile();
                        }
                    });
        } else {
            // 直接保存到本地文件
            saveToFile();
        }
    }

    public static boolean removeEntry(String type, String value) {
        WhitelistEntry entry = new WhitelistEntry(type, value);
        boolean removed;

        synchronized (entries) {
            removed = entries.remove(entry);
        }

        if (removed) {
            if (useApi && apiAvailable) {
                ApiClient.removeEntry(type, value)
                        .thenAccept(success -> {
                            if (success) {
                                // API成功，清除缓存并重新同步
                                ApiClient.clearCache();
                                ApiClient.syncWhitelist()
                                        .thenAccept(newEntries -> {
                                            synchronized (entries) {
                                                entries.clear();
                                                entries.addAll((Collection<? extends WhitelistEntry>) newEntries);
                                            }
                                            saveToFile();
                                        });
                            } else {
                                // API失败，重新添加被删除的条目
                                LOGGER.warn("Failed to remove entry from API, restoring locally");
                                synchronized (entries) {
                                    if (!entries.contains(entry)) {
                                        entries.add(entry);
                                    }
                                }
                                saveToFile();
                            }
                        });
            } else {
                saveToFile();
            }
        }

        return removed;
    }

    public static List<WhitelistEntry> getEntries() {
        synchronized (entries) {
            return new ArrayList<>(entries);
        }
    }

    public static void reload() {
        isLoaded = false;
        ApiClient.clearCache();
        loadAsync();
    }

    public static boolean containsEntry(WhitelistEntry entry) {
        synchronized (entries) {
            return entries.contains(entry);
        }
    }

    public static boolean isUsingApi() {
        return useApi && apiAvailable;
    }

    public static boolean isApiAvailable() {
        return apiAvailable;
    }

    public static int getEntryCount() {
        synchronized (entries) {
            return entries.size();
        }
    }
}