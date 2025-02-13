package org.skydream.cwhitelist;

import net.minecraft.server.level.ServerPlayer;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

public class LogHandler {
    private static final Path LOG_DIR = Paths.get("logs/cwhitelist");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    // 配置变量
    private static final int LOG_RETENTION_DAYS = Config.LOG_RETENTION_DAYS.get(); // 日志保留天数
    private static final long LOG_CUT_SIZE_MB = Config.LOG_CUT_SIZE_MB.get() * 1024 * 1024; // 最大日志文件大小（单位：字节）

    static {
        try {
            Files.createDirectories(LOG_DIR);
        } catch (IOException e) {
            Cwhitelist.LOGGER.error("Failed to create log directory", e);
        }
    }

    public static void log(ServerPlayer player, boolean allowed) {
        boolean ENABLE_LOGGING = Config.ENABLE_LOGGING.get();
        if (ENABLE_LOGGING) {
            String time = LocalDateTime.now().format(TIME_FORMAT);
            String name = player.getGameProfile().getName();
            String uuid = player.getGameProfile().getId().toString();

            // 提取 IP 地址
            String ip = ((InetSocketAddress) player.connection.getConnection().getRemoteAddress()).getAddress().getHostAddress();

            String result = allowed ? "ALLOW" : "DENY";

            String logLine = String.format("[%s] [%s] %s %s %s\n", time, result, name, uuid, ip);

            try {
                Path logFile = LOG_DIR.resolve(LocalDate.now().format(DATE_FORMAT) + ".log");
                if (Files.exists(logFile) && Files.size(logFile) >= LOG_CUT_SIZE_MB) {
                    rotateLogFile(logFile); // 如果文件过大，进行日志轮转
                }
                Files.writeString(logFile, logLine, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                Cwhitelist.LOGGER.error("Failed to write log", e);
            }
        }
    }

    /**
     * 清理超过保留天数的日志文件。
     */
    static void cleanOldLogs() {
        try (Stream<Path> paths = Files.list(LOG_DIR)) {
            LocalDate retentionDate = LocalDate.now().minusDays(LOG_RETENTION_DAYS);

            paths.filter(Files::isRegularFile)
                    .filter(LogHandler::isLogFilePath)
                    .forEach(path -> {
                        String fileName = path.getFileName().toString();
                        LocalDate fileDate = LocalDate.parse(fileName.substring(0, fileName.length() - 4), DATE_FORMAT);
                        if (fileDate.isBefore(retentionDate)) {
                            try {
                                Files.delete(path);
                                Cwhitelist.LOGGER.info("Deleted old log file: {}", path);
                            } catch (IOException e) {
                                Cwhitelist.LOGGER.error("Failed to delete old log file: {}", path, e);
                            }
                        }
                    });
        } catch (IOException e) {
            Cwhitelist.LOGGER.error("Failed to clean old logs", e);
        }
    }

    /**
     * 检查路径是否为日志文件路径。
     *
     * @param path 文件路径
     * @return 是否为日志文件路径
     */
    private static boolean isLogFilePath(Path path) {
        String fileName = path.getFileName().toString();
        return fileName.endsWith(".log") && fileName.matches("\\d{4}-\\d{2}-\\d{2}\\.log");
    }

    /**
     * 当日志文件达到最大大小时，进行日志轮转。
     *
     * @param logFile 当前日志文件
     * @throws IOException 如果发生 I/O 错误
     */
    private static void rotateLogFile(Path logFile) throws IOException {
        String baseName = logFile.getFileName().toString();
        String rotatedName = baseName + "." + System.currentTimeMillis(); // 添加时间戳作为后缀

        Path rotatedPath = LOG_DIR.resolve(rotatedName);
        Files.move(logFile, rotatedPath);

        Cwhitelist.LOGGER.info("Rotated log file: {} -> {}", logFile, rotatedPath);
    }
}