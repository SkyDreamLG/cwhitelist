package org.skydream.cwhitelist;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class WhitelistCommand {
    public static String getLocaleString(String langKey) {
        MutableComponent component = Component.translatable(langKey);
        return component.getString();
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("cwhitelist")
                .requires(source -> source.hasPermission(4))
                .then(Commands.literal("add")
                        .then(Commands.literal("name")
                                .then(Commands.argument("value", StringArgumentType.string())
                                        .executes(ctx -> addEntry(ctx.getSource(), "name",
                                                StringArgumentType.getString(ctx, "value")))
                                )
                        )
                        .then(Commands.literal("uuid")
                                .then(Commands.argument("value", StringArgumentType.string())
                                        .executes(ctx -> addEntry(ctx.getSource(), "uuid",
                                                StringArgumentType.getString(ctx, "value")))
                                )
                        )
                        .then(Commands.literal("ip")
                                .then(Commands.argument("value", StringArgumentType.string())
                                        .executes(ctx -> addEntry(ctx.getSource(), "ip",
                                                StringArgumentType.getString(ctx, "value")))
                                )
                        )
                )
                .then(Commands.literal("list")
                        .executes(ctx -> listEntries(ctx.getSource()))
                )
                .then(Commands.literal("reload")
                        .executes(ctx -> reload(ctx.getSource()))
                )
                .then(Commands.literal("remove")
                        .then(Commands.literal("name")
                                .then(Commands.argument("value", StringArgumentType.string())
                                        .executes(ctx -> removeEntry(ctx.getSource(), "name",
                                                StringArgumentType.getString(ctx, "value")))
                                )
                        )
                        .then(Commands.literal("uuid")
                                .then(Commands.argument("value", StringArgumentType.string())
                                        .executes(ctx -> removeEntry(ctx.getSource(), "uuid",
                                                StringArgumentType.getString(ctx, "value")))
                                )
                        )
                        .then(Commands.literal("ip")
                                .then(Commands.argument("value", StringArgumentType.string())
                                        .executes(ctx -> removeEntry(ctx.getSource(), "ip",
                                                StringArgumentType.getString(ctx, "value")))
                                )
                        )
                )
                .then(Commands.literal("api")
                        .requires(source -> source.hasPermission(4))
                        .then(Commands.literal("status")
                                .executes(ctx -> apiStatus(ctx.getSource()))
                        )
                        .then(Commands.literal("health")
                                .executes(ctx -> apiHealthCheck(ctx.getSource()))
                        )
                        .then(Commands.literal("sync")
                                .executes(ctx -> apiSync(ctx.getSource()))
                        )
                        .then(Commands.literal("verify")
                                .executes(ctx -> apiVerify(ctx.getSource()))
                        )
                        .then(Commands.literal("clearcache")
                                .executes(ctx -> clearCache(ctx.getSource()))
                        )
                )
        );
    }

    private static int addEntry(CommandSourceStack source, String type, String value) {
        // 校验 type 是否合法
        if (!Arrays.asList("name", "uuid", "ip").contains(type.toLowerCase())) {
            source.sendFailure(Component.literal(
                    "§cInvalid entry type. Must be: name, uuid, or ip"));
            return 0;
        }

        // 创建条目对象
        WhitelistManager.WhitelistEntry entry =
                new WhitelistManager.WhitelistEntry(type, value);

        // 检查条目是否已存在
        if (WhitelistManager.containsEntry(entry)) {
            source.sendFailure(Component.literal(
                    "§cEntry already exists: " + type + "=" + value));
            return 2;
        }

        try {
            // 添加条目到白名单
            WhitelistManager.addEntry(entry);
            source.sendSuccess(() -> Component.literal(
                    "§aSuccessfully added entry: " + type + "=" + value), true);

            // 显示当前使用的源
            if (WhitelistManager.isUsingApi()) {
                source.sendSuccess(() -> Component.literal(
                        "§7Added to API and synced locally"), false);
            } else {
                source.sendSuccess(() -> Component.literal(
                        "§7Added to local whitelist file"), false);
            }
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal(
                    "§cFailed to add entry: " + e.getMessage()));
            return 3;
        }
    }

    private static int removeEntry(CommandSourceStack source, String type, String value) {
        boolean removed = WhitelistManager.removeEntry(type, value);
        if (removed) {
            source.sendSuccess(() -> Component.literal(
                    "§aSuccessfully removed entry: " + type + "=" + value), true);

            // 显示当前使用的源
            if (WhitelistManager.isUsingApi()) {
                source.sendSuccess(() -> Component.literal(
                        "§7Removed from API and synced locally"), false);
            } else {
                source.sendSuccess(() -> Component.literal(
                        "§7Removed from local whitelist file"), false);
            }
        } else {
            source.sendFailure(Component.literal(
                    "§cEntry not found: " + type + "=" + value));
        }
        return removed ? 1 : 0;
    }

    private static int listEntries(CommandSourceStack source) {
        StringBuilder sb = new StringBuilder();

        // 显示当前模式
        if (Config.ENABLE_API.get()) {
            if (WhitelistManager.isUsingApi()) {
                sb.append("§e=== CWhitelist §a(API Mode) §e===\n");
                ApiClient.TokenInfo tokenInfo = ApiClient.getTokenInfo();
                if (tokenInfo != null) {
                    sb.append("§7Token: §f").append(tokenInfo.name).append("\n");
                    sb.append("§7Permissions: §fR:");
                    sb.append(tokenInfo.canRead ? "§a✓" : "§c✗").append("§f W:");
                    sb.append(tokenInfo.canWrite ? "§a✓" : "§c✗").append("§f D:");
                    sb.append(tokenInfo.canDelete ? "§a✓" : "§c✗").append("\n");
                }
            } else {
                sb.append("§e=== CWhitelist §6(Local Mode - API Unavailable) §e===\n");
            }
        } else {
            sb.append("§e=== CWhitelist (Local Mode) ===\n");
        }

        // 显示条目
        sb.append("§aTotal entries: §f")
                .append(WhitelistManager.getEntryCount())
                .append("\n");

        if (WhitelistManager.getEntryCount() > 0) {
            sb.append("§7Entries:\n");
            WhitelistManager.getEntries().forEach(e ->
                    sb.append("  §7- §f")
                            .append(e.getType())
                            .append(": §e")
                            .append(e.getValue())
                            .append("\n"));
        } else {
            sb.append("§7No entries found.\n");
        }

        source.sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    private static int reload(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("§aReloading whitelist..."), false);

        WhitelistManager.reload();

        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(1000); // 等待加载完成
                source.sendSuccess(() -> Component.literal(
                        "§aWhitelist reloaded successfully"), false);

                if (Config.ENABLE_API.get()) {
                    if (WhitelistManager.isUsingApi()) {
                        source.sendSuccess(() -> Component.literal(
                                "§7Source: API (Synced)"), false);
                    } else {
                        source.sendSuccess(() -> Component.literal(
                                "§7Source: Local File (API unavailable)"), false);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        return 1;
    }

    private static int apiStatus(CommandSourceStack source) {
        if (!Config.ENABLE_API.get()) {
            source.sendFailure(Component.literal("§cAPI integration is disabled in config"));
            return 0;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("§e=== API Status ===\n");
        sb.append("§7Base URL: §f").append(Config.API_BASE_URL.get()).append("\n");
        sb.append("§7Enabled: §f").append(Config.ENABLE_API.get() ? "§aYes" : "§cNo").append("\n");
        sb.append("§7Token Configured: §f")
                .append(Config.API_TOKEN.get() != null && !Config.API_TOKEN.get().isEmpty() ?
                        "§aYes" : "§cNo").append("\n");
        sb.append("§7Auth Method: §f")
                .append(Config.API_USE_HEADER_AUTH.get() ? "Header" : "Query Param").append("\n");
        sb.append("§7Current Mode: §f")
                .append(WhitelistManager.isUsingApi() ? "§aAPI" : "§6Local (Fallback)").append("\n");

        if (ApiClient.isEnabled()) {
            sb.append("\n§7Token Status:\n");
            sb.append("  §f").append(ApiClient.getTokenStatus()).append("\n");
        }

        source.sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    private static int apiHealthCheck(CommandSourceStack source) {
        if (!Config.ENABLE_API.get()) {
            source.sendFailure(Component.literal("§cAPI integration is disabled"));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("§aChecking API health..."), false);

        ApiClient.healthCheck().thenAccept(healthy -> {
            if (healthy) {
                source.sendSuccess(() -> Component.literal("§aAPI health check passed"), false);
            } else {
                source.sendFailure(Component.literal("§cAPI health check failed"));
            }
        }).exceptionally(e -> {
            source.sendFailure(Component.literal("§cAPI health check error: " + e.getMessage()));
            return null;
        });

        return 1;
    }

    private static int apiVerify(CommandSourceStack source) {
        if (!Config.ENABLE_API.get()) {
            source.sendFailure(Component.literal("§cAPI integration is disabled"));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("§aVerifying API token..."), false);

        ApiClient.verifyToken().thenAccept(verified -> {
            if (verified) {
                ApiClient.TokenInfo tokenInfo = ApiClient.getTokenInfo();
                if (tokenInfo != null) {
                    source.sendSuccess(() -> Component.literal(
                            "§aToken verified successfully!"), false);
                    source.sendSuccess(() -> Component.literal(
                            "§7Name: §f" + tokenInfo.name), false);
                    source.sendSuccess(() -> Component.literal(
                            "§7Permissions: R:" + (tokenInfo.canRead ? "✓" : "✗") +
                                    " W:" + (tokenInfo.canWrite ? "✓" : "✗") +
                                    " D:" + (tokenInfo.canDelete ? "✓" : "✗")), false);
                }
            } else {
                source.sendFailure(Component.literal("§cToken verification failed"));
            }
        }).exceptionally(e -> {
            source.sendFailure(Component.literal("§cToken verification error: " + e.getMessage()));
            return null;
        });

        return 1;
    }

    private static int apiSync(CommandSourceStack source) {
        if (!Config.ENABLE_API.get()) {
            source.sendFailure(Component.literal("§cAPI integration is disabled"));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("§aSyncing whitelist from API..."), false);

        ApiClient.syncWhitelist().thenAccept(entries -> {
            if (entries != null) {
                source.sendSuccess(() -> Component.literal(
                        String.format("§aSuccessfully synced §f%d§a entries from API",
                                entries.size())), false);
            } else {
                source.sendFailure(Component.literal("§cFailed to sync from API"));
            }
        }).exceptionally(e -> {
            source.sendFailure(Component.literal("§cSync error: " + e.getMessage()));
            return null;
        });

        return 1;
    }

    private static int clearCache(CommandSourceStack source) {
        if (!Config.ENABLE_API.get()) {
            source.sendFailure(Component.literal("§cAPI integration is disabled"));
            return 0;
        }

        ApiClient.clearCache();
        source.sendSuccess(() -> Component.literal("§aAPI cache cleared"), true);
        return 1;
    }
}