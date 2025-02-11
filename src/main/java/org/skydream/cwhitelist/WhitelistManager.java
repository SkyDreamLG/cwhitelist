package org.skydream.cwhitelist;

import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import net.minecraft.server.level.ServerPlayer;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;

public class WhitelistManager {
    private static final Path WHITELIST_PATH = Paths.get("config/cwhitelist_entries.json");
    private static final List<WhitelistEntry> entries = new ArrayList<>();

    public static void load() {
        try {
            if (!Files.exists(WHITELIST_PATH)) {
                Files.createFile(WHITELIST_PATH);
                save();
            }
            String json = Files.readString(WHITELIST_PATH);
            entries.clear();
            entries.addAll(new Gson().fromJson(json, new TypeToken<List<WhitelistEntry>>(){}.getType()));
        } catch (IOException e) {
            Cwhitelist.LOGGER.error("Failed to load whitelist", e);
        }
    }

    public static boolean isAllowed(ServerPlayer player) {
        String name = player.getGameProfile().getName();
        UUID uuid = player.getGameProfile().getId();
        String ip = player.connection.getConnection().getRemoteAddress().toString().split(":")[0];

        // 检查玩家是否在白名单中
        boolean allowed = entries.stream().anyMatch(entry -> {
            if (Config.ENABLE_NAME_CHECK && entry.type.equals("name") && entry.value.equalsIgnoreCase(name)) return true;
            if (Config.ENABLE_UUID_CHECK && entry.type.equals("uuid") && entry.value.equalsIgnoreCase(uuid.toString())) return true;
            return Config.ENABLE_IP_CHECK && entry.type.equals("ip") && matchIP(entry.value, ip);
        });

        // 记录日志
        LogHandler.log(player, allowed);

        // 返回结果
        return allowed;
    }

    private static boolean matchIP(String pattern, String ip) {
        String regex = pattern.replace(".", "\\.").replace("*", ".*");
        return Pattern.matches(regex, ip);
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
        load();
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