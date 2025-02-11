package org.skydream.cwhitelist;

import net.minecraft.server.level.ServerPlayer;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;

public class LogHandler {
    private static final Path LOG_DIR = Paths.get("logs/cwhitelist");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    static {
        try {
            Files.createDirectories(LOG_DIR);
        } catch (IOException e) {
            Cwhitelist.LOGGER.error("Failed to create log directory", e);
        }
    }

    public static void log(ServerPlayer player, boolean allowed) {
        String time = LocalDateTime.now().format(TIME_FORMAT);
        String name = player.getGameProfile().getName();
        String uuid = player.getGameProfile().getId().toString();
        String ip = player.connection.getConnection().getRemoteAddress().toString().split(":")[0];
        String result = allowed ? "ALLOW" : "DENY";

        String logLine = String.format("[%s] [%s] %s %s %s\n", time, result, name, uuid, ip);

        try {
            Path logFile = LOG_DIR.resolve(LocalDate.now().format(DATE_FORMAT) + ".log");
            Files.writeString(logFile, logLine, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            Cwhitelist.LOGGER.error("Failed to write log", e);
        }
    }
}