package org.skydream.cwhitelist;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class WhitelistCommand {
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
            source.sendFailure(Component.translatable(
                    "cwhitelist.error.invalid_type", type));
            return 0;
        }

        // 创建条目对象
        WhitelistManager.WhitelistEntry entry =
                new WhitelistManager.WhitelistEntry(type, value);

        // 检查条目是否已存在
        if (WhitelistManager.containsEntry(entry)) {
            source.sendFailure(Component.translatable(
                    "cwhitelist.error.entry_exists", type, value));
            return 2;
        }

        try {
            // 添加条目到白名单
            WhitelistManager.addEntry(entry);
            source.sendSuccess(() -> Component.translatable(
                    "cwhitelist.success.entry_added", type, value), true);

            // 显示当前使用的源
            if (WhitelistManager.isUsingApi()) {
                source.sendSuccess(() -> Component.translatable(
                        "cwhitelist.info.added_to_api"), false);
            } else {
                source.sendSuccess(() -> Component.translatable(
                        "cwhitelist.info.added_to_local"), false);
            }
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable(
                    "cwhitelist.error.add_failed", e.getMessage()));
            return 3;
        }
    }

    private static int removeEntry(CommandSourceStack source, String type, String value) {
        boolean removed = WhitelistManager.removeEntry(type, value);
        if (removed) {
            source.sendSuccess(() -> Component.translatable(
                    "cwhitelist.success.entry_removed", type, value), true);

            // 显示当前使用的源
            if (WhitelistManager.isUsingApi()) {
                source.sendSuccess(() -> Component.translatable(
                        "cwhitelist.info.removed_from_api"), false);
            } else {
                source.sendSuccess(() -> Component.translatable(
                        "cwhitelist.info.removed_from_local"), false);
            }
        } else {
            source.sendFailure(Component.translatable(
                    "cwhitelist.error.entry_not_found", type, value));
        }
        return removed ? 1 : 0;
    }

    private static int listEntries(CommandSourceStack source) {
        Component message;

        if (Config.ENABLE_API.get()) {
            if (WhitelistManager.isUsingApi()) {
                ApiClient.TokenInfo tokenInfo = ApiClient.getTokenInfo();
                if (tokenInfo != null) {
                    message = Component.translatable(
                            "cwhitelist.list.api_with_token",
                            tokenInfo.name,
                            tokenInfo.canRead ? "✓" : "✗",
                            tokenInfo.canWrite ? "✓" : "✗",
                            tokenInfo.canDelete ? "✓" : "✗",
                            WhitelistManager.getEntryCount()
                    );
                } else {
                    message = Component.translatable(
                            "cwhitelist.list.api_mode",
                            WhitelistManager.getEntryCount()
                    );
                }
            } else {
                message = Component.translatable(
                        "cwhitelist.list.api_unavailable",
                        WhitelistManager.getEntryCount()
                );
            }
        } else {
            message = Component.translatable(
                    "cwhitelist.list.local_mode",
                    WhitelistManager.getEntryCount()
            );
        }

        // 添加条目列表
        if (WhitelistManager.getEntryCount() > 0) {
            MutableComponent entriesList = Component.literal("\n");
            WhitelistManager.getEntries().forEach(e ->
                    entriesList.append(Component.translatable(
                            "cwhitelist.list.entry_item",
                            e.getType(),
                            e.getValue()
                    )).append("\n")
            );
            message = message.copy().append(entriesList);
        } else {
            message = message.copy().append("\n")
                    .append(Component.translatable("cwhitelist.list.no_entries"));
        }

        Component finalMessage = message;
        source.sendSuccess(() -> finalMessage, false);
        return 1;
    }

    private static int reload(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("cwhitelist.reload.starting"), false);

        WhitelistManager.reload();

        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(1000);
                source.getServer().execute(() -> {
                    source.sendSuccess(() -> Component.translatable(
                            "cwhitelist.reload.success"), false);

                    if (Config.ENABLE_API.get()) {
                        if (WhitelistManager.isUsingApi()) {
                            source.sendSuccess(() -> Component.translatable(
                                    "cwhitelist.reload.source_api"), false);
                        } else {
                            source.sendSuccess(() -> Component.translatable(
                                    "cwhitelist.reload.source_local"), false);
                        }
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        return 1;
    }

    private static int apiStatus(CommandSourceStack source) {
        if (!Config.ENABLE_API.get()) {
            source.sendFailure(Component.translatable("cwhitelist.api.disabled"));
            return 0;
        }

        Config.API_TOKEN.get();
        Component status = Component.translatable(
                "cwhitelist.api.status.title",
                Config.API_BASE_URL.get(),
                Config.ENABLE_API.get() ? "§aYes" : "§cNo",
                !Config.API_TOKEN.get().isEmpty() ? "§aYes" : "§cNo",
                Config.API_USE_HEADER_AUTH.get() ? "Header" : "Query Param",
                WhitelistManager.isUsingApi() ? "§aAPI" : "§6Local (Fallback)"
        );

        if (ApiClient.isEnabled()) {
            status = status.copy().append("\n")
                    .append(Component.translatable("cwhitelist.api.status.token_status",
                            ApiClient.getTokenStatus()));
        }

        Component finalStatus = status;
        source.sendSuccess(() -> finalStatus, false);
        return 1;
    }

    private static int apiHealthCheck(CommandSourceStack source) {
        if (!Config.ENABLE_API.get()) {
            source.sendFailure(Component.translatable("cwhitelist.api.disabled"));
            return 0;
        }

        source.sendSuccess(() -> Component.translatable("cwhitelist.api.health.checking"), false);

        CompletableFuture.runAsync(() -> {
            try {
                boolean healthy = ApiClient.healthCheck().get(30, TimeUnit.SECONDS);
                source.getServer().execute(() -> {
                    if (healthy) {
                        source.sendSuccess(() -> Component.translatable(
                                "cwhitelist.api.health.success"), false);
                    } else {
                        source.sendFailure(Component.translatable(
                                "cwhitelist.api.health.failed"));
                    }
                });
            } catch (Exception e) {
                source.getServer().execute(() -> {
                    source.sendFailure(Component.translatable(
                            "cwhitelist.api.health.error",
                            e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
                });
            }
        });

        return 1;
    }

    private static int apiVerify(CommandSourceStack source) {
        if (!Config.ENABLE_API.get()) {
            source.sendFailure(Component.translatable("cwhitelist.api.disabled"));
            return 0;
        }

        source.sendSuccess(() -> Component.translatable("cwhitelist.api.verify.checking"), false);

        CompletableFuture.runAsync(() -> {
            try {
                boolean verified = ApiClient.verifyToken().get(30, TimeUnit.SECONDS);
                source.getServer().execute(() -> {
                    if (verified) {
                        ApiClient.TokenInfo tokenInfo = ApiClient.getTokenInfo();
                        if (tokenInfo != null) {
                            source.sendSuccess(() -> Component.translatable(
                                    "cwhitelist.api.verify.success"), false);
                            source.sendSuccess(() -> Component.translatable(
                                    "cwhitelist.api.verify.token_info",
                                    tokenInfo.name,
                                    tokenInfo.canRead ? "✓" : "✗",
                                    tokenInfo.canWrite ? "✓" : "✗",
                                    tokenInfo.canDelete ? "✓" : "✗"), false);
                        }
                    } else {
                        source.sendFailure(Component.translatable(
                                "cwhitelist.api.verify.failed"));
                    }
                });
            } catch (Exception e) {
                source.getServer().execute(() -> {
                    source.sendFailure(Component.translatable(
                            "cwhitelist.api.verify.error",
                            e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
                });
            }
        });

        return 1;
    }

    private static int apiSync(CommandSourceStack source) {
        if (!Config.ENABLE_API.get()) {
            source.sendFailure(Component.translatable("cwhitelist.api.disabled"));
            return 0;
        }

        source.sendSuccess(() -> Component.translatable("cwhitelist.api.sync.starting"), false);

        CompletableFuture.runAsync(() -> {
            try {
                // 使用强制同步方法，忽略缓存
                java.util.List<WhitelistManager.WhitelistEntry> entries =
                        (java.util.List<WhitelistManager.WhitelistEntry>) ApiClient.forceSyncWhitelist().get(30, TimeUnit.SECONDS);

                source.getServer().execute(() -> {
                    if (entries != null && !entries.isEmpty()) {
                        // 强制重新加载白名单管理器以应用新的数据
                        WhitelistManager.reload();

                        source.sendSuccess(() -> Component.translatable(
                                "cwhitelist.api.sync.success", entries.size()), false);

                        // 显示当前模式
                        if (WhitelistManager.isUsingApi()) {
                            source.sendSuccess(() -> Component.translatable(
                                    "cwhitelist.reload.source_api"), false);
                        } else {
                            source.sendSuccess(() -> Component.translatable(
                                    "cwhitelist.reload.source_local"), false);
                        }
                    } else {
                        source.sendFailure(Component.translatable(
                                "cwhitelist.api.sync.no_entries"));
                    }
                });
            } catch (java.util.concurrent.TimeoutException e) {
                source.getServer().execute(() -> {
                    source.sendFailure(Component.translatable(
                            "cwhitelist.api.sync.timeout"));
                });
            } catch (Exception e) {
                source.getServer().execute(() -> {
                    source.sendFailure(Component.translatable(
                            "cwhitelist.api.sync.error",
                            e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
                });
            }
        });

        return 1;
    }

    private static int clearCache(CommandSourceStack source) {
        if (!Config.ENABLE_API.get()) {
            source.sendFailure(Component.translatable("cwhitelist.api.disabled"));
            return 0;
        }

        ApiClient.clearCache();
        source.sendSuccess(() -> Component.translatable("cwhitelist.api.cache.cleared"), true);
        return 1;
    }

    public static String getLocaleString(String langKey) {
        // 直接返回翻译组件
        return Component.translatable(langKey).getString();
    }
}