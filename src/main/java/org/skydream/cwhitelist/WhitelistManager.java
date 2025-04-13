package org.skydream.cwhitelist;

import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import net.minecraft.server.level.ServerPlayer;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import static org.skydream.cwhitelist.Config.*;
import static org.skydream.cwhitelist.LogHandler.cleanOldLogs;

public class WhitelistManager {
    private static final Path WHITELIST_PATH = Paths.get("config/cwhitelist_entries.json");
    private static final List<WhitelistEntry> entries = new ArrayList<>();

    private static boolean isLoaded = false;

    public static void load() {
        if (isLoaded) return; // 如果已经加载过，则跳过
        cleanOldLogs();
        try {
            if (!Files.exists(WHITELIST_PATH)) {
                Files.createFile(WHITELIST_PATH);
                save();
            }
            String json = Files.readString(WHITELIST_PATH);
            entries.clear();
            entries.addAll(new Gson().fromJson(json, new TypeToken<List<WhitelistEntry>>(){}.getType()));
            isLoaded = true; // 标记为已加载
        } catch (IOException e) {
            Cwhitelist.LOGGER.error("Failed to load whitelist", e);
        }
    }

    public static void loadAsync() {
        CompletableFuture.runAsync(WhitelistManager::load);
    }

    public static boolean isAllowed(ServerPlayer player) {
        String name = player.getGameProfile().getName();
        UUID uuid = player.getGameProfile().getId();
        String ip = ((InetSocketAddress) player.connection.getConnection().getRemoteAddress()).getAddress().getHostAddress();

        boolean ENABLE_NAME_CHECK = Config.ENABLE_NAME_CHECK.get();
        boolean ENABLE_UUID_CHECK = Config.ENABLE_UUID_CHECK.get();
        boolean ENABLE_IP_CHECK = Config.ENABLE_IP_CHECK.get();

        for (WhitelistEntry entry : entries) {
            if (ENABLE_NAME_CHECK && entry.type.equals("name") && entry.value.equalsIgnoreCase(name)) {
                LogHandler.log(player, true);
                return true;
            }
            if (ENABLE_UUID_CHECK && entry.type.equals("uuid") && entry.value.equalsIgnoreCase(uuid.toString())) {
                LogHandler.log(player, true);
                return true;
            }
            if (ENABLE_IP_CHECK && entry.type.equals("ip") && matchIP(entry.value, ip)) {
                LogHandler.log(player, true);
                return true;
            }
        }

        LogHandler.log(player, false);
        return false;
    }

    private static final Map<String, Pattern> regexCache = new HashMap<>();

    private static boolean matchIP(String pattern, String ip) {
        try {
            // 检查缓存中是否已有编译好的正则表达式
            Pattern compiledPattern = regexCache.computeIfAbsent(pattern, p -> {
                String regex = p.replace(".", "\\.").replace("*", ".*");
                if (ip.contains(":")) {
                    regex = regex.replaceAll("::", "(::|:([0-9a-fA-F]{0,4}:){0,7})");
                }
                return Pattern.compile(regex);
            });

            // 使用预编译的正则表达式进行匹配
            return compiledPattern.matcher(ip).matches();
        } catch (Exception e) {
            Cwhitelist.LOGGER.error("Invalid regex pattern: " + pattern, e);
            return false;
        }
    }

    public static void save() {
        try {
            Files.writeString(WHITELIST_PATH, new Gson().toJson(entries));
        } catch (IOException e) {
            Cwhitelist.LOGGER.error("Failed to save whitelist", e);
        }
    }

    public static void addEntry(WhitelistEntry entry) {
        entries.add(entry);
        save();
    }

    public static boolean removeEntry(String type, String value) {
        boolean removed = entries.removeIf(e -> e.type.equals(type) && e.value.equalsIgnoreCase(value));
        if (removed) save();
        return removed;
    }

    public static List<WhitelistEntry> getEntries() {
        return new ArrayList<>(entries);
    }

    public static void reload() {
        isLoaded = false; // 标记为已加载
        loadAsync();
    }

    public static boolean containsEntry(WhitelistEntry entry) {
        for (WhitelistEntry existingEntry : entries) {
            if (existingEntry.getType().equalsIgnoreCase(entry.getType()) &&
                    existingEntry.getValue().equalsIgnoreCase(entry.getValue())) {
                return true; // 条目已存在
            }
        }
        return false; // 条目不存在
    }

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
    }
}