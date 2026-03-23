package org.skydream.cwhitelist;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class WhitelistCommand {

    // 权限节点常量（保持不变）
    private static final String MOD_ID = "cwhitelist";

    public static final PermissionNode<Boolean> PERMISSION_USE =
            new PermissionNode<>(MOD_ID, "use", PermissionTypes.BOOLEAN,
                    (player, playerUUID, context) -> false);

    public static final PermissionNode<Boolean> PERMISSION_ADD =
            new PermissionNode<>(MOD_ID, "add", PermissionTypes.BOOLEAN,
                    (player, playerUUID, context) -> false);

    public static final PermissionNode<Boolean> PERMISSION_REMOVE =
            new PermissionNode<>(MOD_ID, "remove", PermissionTypes.BOOLEAN,
                    (player, playerUUID, context) -> false);

    public static final PermissionNode<Boolean> PERMISSION_LIST =
            new PermissionNode<>(MOD_ID, "list", PermissionTypes.BOOLEAN,
                    (player, playerUUID, context) -> false);

    public static final PermissionNode<Boolean> PERMISSION_RELOAD =
            new PermissionNode<>(MOD_ID, "reload", PermissionTypes.BOOLEAN,
                    (player, playerUUID, context) -> false);

    public static final PermissionNode<Boolean> PERMISSION_API =
            new PermissionNode<>(MOD_ID, "api", PermissionTypes.BOOLEAN,
                    (player, playerUUID, context) -> false);

    public static final PermissionNode<Boolean> PERMISSION_API_STATUS =
            new PermissionNode<>(MOD_ID, "api.status", PermissionTypes.BOOLEAN,
                    (player, playerUUID, context) -> false);

    public static final PermissionNode<Boolean> PERMISSION_API_HEALTH =
            new PermissionNode<>(MOD_ID, "api.health", PermissionTypes.BOOLEAN,
                    (player, playerUUID, context) -> false);

    public static final PermissionNode<Boolean> PERMISSION_API_SYNC =
            new PermissionNode<>(MOD_ID, "api.sync", PermissionTypes.BOOLEAN,
                    (player, playerUUID, context) -> false);

    public static final PermissionNode<Boolean> PERMISSION_API_VERIFY =
            new PermissionNode<>(MOD_ID, "api.verify", PermissionTypes.BOOLEAN,
                    (player, playerUUID, context) -> false);

    public static final PermissionNode<Boolean> PERMISSION_API_CLEARCACHE =
            new PermissionNode<>(MOD_ID, "api.clearcache", PermissionTypes.BOOLEAN,
                    (player, playerUUID, context) -> false);

    public static final PermissionNode<Boolean> PERMISSION_ADMIN =
            new PermissionNode<>(MOD_ID, "admin", PermissionTypes.BOOLEAN,
                    (player, playerUUID, context) -> false);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // 创建一个总是返回 true 的权限检查（用于客户端显示）
        dispatcher.register(Commands.literal("cwhitelist")
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.translatable("cwhitelist.help"), false);
                    return 1;
                })
                // add 子命令 - 移除 requires 或使用宽松的权限检查
                .then(Commands.literal("add")
                        .then(Commands.literal("name")
                                .then(Commands.argument("value", StringArgumentType.word())
                                        .executes(ctx -> {
                                            // 在命令执行时检查权限
                                            if (!hasPermission(ctx.getSource(), PERMISSION_ADD)) {
                                                ctx.getSource().sendFailure(Component.translatable(
                                                        "cwhitelist.error.no_permission", "add entries"));
                                                return 0;
                                            }
                                            return addEntry(ctx.getSource(), "name",
                                                    StringArgumentType.getString(ctx, "value"));
                                        }))
                        )
                        .then(Commands.literal("uuid")
                                .then(Commands.argument("value", StringArgumentType.word())
                                        .executes(ctx -> {
                                            if (!hasPermission(ctx.getSource(), PERMISSION_ADD)) {
                                                ctx.getSource().sendFailure(Component.translatable(
                                                        "cwhitelist.error.no_permission", "add entries"));
                                                return 0;
                                            }
                                            return addEntry(ctx.getSource(), "uuid",
                                                    StringArgumentType.getString(ctx, "value"));
                                        }))
                        )
                        .then(Commands.literal("ip")
                                .then(Commands.argument("value", StringArgumentType.word())
                                        .executes(ctx -> {
                                            if (!hasPermission(ctx.getSource(), PERMISSION_ADD)) {
                                                ctx.getSource().sendFailure(Component.translatable(
                                                        "cwhitelist.error.no_permission", "add entries"));
                                                return 0;
                                            }
                                            return addEntry(ctx.getSource(), "ip",
                                                    StringArgumentType.getString(ctx, "value"));
                                        }))
                        )
                )
                .then(Commands.literal("list")
                        .executes(ctx -> {
                            if (!hasPermission(ctx.getSource(), PERMISSION_LIST)) {
                                ctx.getSource().sendFailure(Component.translatable(
                                        "cwhitelist.error.no_permission", "list entries"));
                                return 0;
                            }
                            return listEntries(ctx.getSource());
                        })
                )
                .then(Commands.literal("reload")
                        .executes(ctx -> {
                            if (!hasPermission(ctx.getSource(), PERMISSION_RELOAD)) {
                                ctx.getSource().sendFailure(Component.translatable(
                                        "cwhitelist.error.no_permission", "reload configuration"));
                                return 0;
                            }
                            return reload(ctx.getSource());
                        })
                )
                .then(Commands.literal("remove")
                        .then(Commands.literal("name")
                                .then(Commands.argument("value", StringArgumentType.word())
                                        .executes(ctx -> {
                                            if (!hasPermission(ctx.getSource(), PERMISSION_REMOVE)) {
                                                ctx.getSource().sendFailure(Component.translatable(
                                                        "cwhitelist.error.no_permission", "remove entries"));
                                                return 0;
                                            }
                                            return removeEntry(ctx.getSource(), "name",
                                                    StringArgumentType.getString(ctx, "value"));
                                        }))
                        )
                        .then(Commands.literal("uuid")
                                .then(Commands.argument("value", StringArgumentType.word())
                                        .executes(ctx -> {
                                            if (!hasPermission(ctx.getSource(), PERMISSION_REMOVE)) {
                                                ctx.getSource().sendFailure(Component.translatable(
                                                        "cwhitelist.error.no_permission", "remove entries"));
                                                return 0;
                                            }
                                            return removeEntry(ctx.getSource(), "uuid",
                                                    StringArgumentType.getString(ctx, "value"));
                                        }))
                        )
                        .then(Commands.literal("ip")
                                .then(Commands.argument("value", StringArgumentType.word())
                                        .executes(ctx -> {
                                            if (!hasPermission(ctx.getSource(), PERMISSION_REMOVE)) {
                                                ctx.getSource().sendFailure(Component.translatable(
                                                        "cwhitelist.error.no_permission", "remove entries"));
                                                return 0;
                                            }
                                            return removeEntry(ctx.getSource(), "ip",
                                                    StringArgumentType.getString(ctx, "value"));
                                        }))
                        )
                )
                .then(Commands.literal("api")
                        .then(Commands.literal("status")
                                .executes(ctx -> {
                                    if (!hasPermission(ctx.getSource(), PERMISSION_API_STATUS)) {
                                        ctx.getSource().sendFailure(Component.translatable(
                                                "cwhitelist.error.no_permission", "check API status"));
                                        return 0;
                                    }
                                    return apiStatus(ctx.getSource());
                                })
                        )
                        .then(Commands.literal("health")
                                .executes(ctx -> {
                                    if (!hasPermission(ctx.getSource(), PERMISSION_API_HEALTH)) {
                                        ctx.getSource().sendFailure(Component.translatable(
                                                "cwhitelist.error.no_permission", "check API health"));
                                        return 0;
                                    }
                                    return apiHealthCheck(ctx.getSource());
                                })
                        )
                        .then(Commands.literal("sync")
                                .executes(ctx -> {
                                    if (!hasPermission(ctx.getSource(), PERMISSION_API_SYNC)) {
                                        ctx.getSource().sendFailure(Component.translatable(
                                                "cwhitelist.error.no_permission", "sync with API"));
                                        return 0;
                                    }
                                    return apiSync(ctx.getSource());
                                })
                        )
                        .then(Commands.literal("verify")
                                .executes(ctx -> {
                                    if (!hasPermission(ctx.getSource(), PERMISSION_API_VERIFY)) {
                                        ctx.getSource().sendFailure(Component.translatable(
                                                "cwhitelist.error.no_permission", "verify API token"));
                                        return 0;
                                    }
                                    return apiVerify(ctx.getSource());
                                })
                        )
                        .then(Commands.literal("clearcache")
                                .executes(ctx -> {
                                    if (!hasPermission(ctx.getSource(), PERMISSION_API_CLEARCACHE)) {
                                        ctx.getSource().sendFailure(Component.translatable(
                                                "cwhitelist.error.no_permission", "clear API cache"));
                                        return 0;
                                    }
                                    return clearCache(ctx.getSource());
                                })
                        )
                )
        );
    }

    /**
     * 检查命令源是否有指定权限
     */
    private static boolean hasPermission(CommandSourceStack source, PermissionNode<Boolean> permission) {
        // 如果是控制台或命令同步时的空源，默认有权限
        if (!source.isPlayer()) {
            return true;
        }

        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return true; // 如果玩家为空（可能发生在命令同步时），返回 true
        }

        // 检查管理员权限
        if (PermissionAPI.getPermission(player, PERMISSION_ADMIN)) {
            return true;
        }

        // 检查具体权限
        return PermissionAPI.getPermission(player, permission);
    }

    private static boolean checkPermission(CommandSourceStack source, PermissionNode<Boolean> permission, String action) {
        if (hasPermission(source, permission)) {
            return true;
        }
        source.sendFailure(Component.translatable("cwhitelist.error.no_permission", action));
        return false;
    }

    private static int addEntry(CommandSourceStack source, String type, String value) {
        if (!checkPermission(source, PERMISSION_ADD, "add entries")) {
            return 0;
        }
        if (!Arrays.asList("name", "uuid", "ip").contains(type.toLowerCase())) {
            source.sendFailure(Component.translatable("cwhitelist.error.invalid_type", type));
            return 0;
        }
        WhitelistManager.WhitelistEntry entry = new WhitelistManager.WhitelistEntry(type, value);
        if (WhitelistManager.containsEntry(entry)) {
            source.sendFailure(Component.translatable("cwhitelist.error.entry_exists", type, value));
            return 2;
        }
        try {
            WhitelistManager.addEntry(entry);
            source.sendSuccess(() -> Component.translatable("cwhitelist.success.entry_added", type, value), true);
            if (WhitelistManager.isUsingApi()) {
                source.sendSuccess(() -> Component.translatable("cwhitelist.info.added_to_api"), false);
            } else {
                source.sendSuccess(() -> Component.translatable("cwhitelist.info.added_to_local"), false);
            }
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("cwhitelist.error.add_failed", e.getMessage()));
            return 3;
        }
    }

    private static int removeEntry(CommandSourceStack source, String type, String value) {
        if (!checkPermission(source, PERMISSION_REMOVE, "remove entries")) {
            return 0;
        }
        boolean removed = WhitelistManager.removeEntry(type, value);
        if (removed) {
            source.sendSuccess(() -> Component.translatable("cwhitelist.success.entry_removed", type, value), true);
            if (WhitelistManager.isUsingApi()) {
                source.sendSuccess(() -> Component.translatable("cwhitelist.info.removed_from_api"), false);
            } else {
                source.sendSuccess(() -> Component.translatable("cwhitelist.info.removed_from_local"), false);
            }
        } else {
            source.sendFailure(Component.translatable("cwhitelist.error.entry_not_found", type, value));
        }
        return removed ? 1 : 0;
    }

    private static int listEntries(CommandSourceStack source) {
        if (!checkPermission(source, PERMISSION_LIST, "list entries")) {
            return 0;
        }
        Component message;
        if (Config.ENABLE_API.get()) {
            if (WhitelistManager.isUsingApi()) {
                ApiClient.TokenInfo tokenInfo = ApiClient.getTokenInfo();
                if (tokenInfo != null) {
                    message = Component.translatable("cwhitelist.list.api_with_token",
                            tokenInfo.name,
                            tokenInfo.canRead ? "✓" : "✗",
                            tokenInfo.canWrite ? "✓" : "✗",
                            tokenInfo.canDelete ? "✓" : "✗",
                            WhitelistManager.getEntryCount());
                } else {
                    message = Component.translatable("cwhitelist.list.api_mode",
                            WhitelistManager.getEntryCount());
                }
            } else {
                message = Component.translatable("cwhitelist.list.api_unavailable",
                        WhitelistManager.getEntryCount());
            }
        } else {
            message = Component.translatable("cwhitelist.list.local_mode",
                    WhitelistManager.getEntryCount());
        }
        if (WhitelistManager.getEntryCount() > 0) {
            MutableComponent entriesList = Component.literal("\n");
            WhitelistManager.getEntries().forEach(e ->
                    entriesList.append(Component.translatable("cwhitelist.list.entry_item",
                            e.getType(), e.getValue())).append("\n"));
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
        if (!checkPermission(source, PERMISSION_RELOAD, "reload configuration")) {
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("cwhitelist.reload.starting"), false);
        WhitelistManager.reload();
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(1000);
                source.getServer().execute(() -> {
                    source.sendSuccess(() -> Component.translatable("cwhitelist.reload.success"), false);
                    if (Config.ENABLE_API.get()) {
                        if (WhitelistManager.isUsingApi()) {
                            source.sendSuccess(() -> Component.translatable("cwhitelist.reload.source_api"), false);
                        } else {
                            source.sendSuccess(() -> Component.translatable("cwhitelist.reload.source_local"), false);
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
        if (!checkPermission(source, PERMISSION_API_STATUS, "check API status")) {
            return 0;
        }
        if (!Config.ENABLE_API.get()) {
            source.sendFailure(Component.translatable("cwhitelist.api.disabled"));
            return 0;
        }
        Config.API_TOKEN.get();
        Component status = Component.translatable("cwhitelist.api.status.title",
                Config.API_BASE_URL.get(),
                Config.ENABLE_API.get() ? "§aYes" : "§cNo",
                !Config.API_TOKEN.get().isEmpty() ? "§aYes" : "§cNo",
                Config.API_USE_HEADER_AUTH.get() ? "Header" : "Query Param",
                WhitelistManager.isUsingApi() ? "§aAPI" : "§6Local (Fallback)");
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
        if (!checkPermission(source, PERMISSION_API_HEALTH, "check API health")) {
            return 0;
        }
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
                        source.sendSuccess(() -> Component.translatable("cwhitelist.api.health.success"), false);
                    } else {
                        source.sendFailure(Component.translatable("cwhitelist.api.health.failed"));
                    }
                });
            } catch (Exception e) {
                source.getServer().execute(() -> {
                    source.sendFailure(Component.translatable("cwhitelist.api.health.error",
                            e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
                });
            }
        });
        return 1;
    }

    private static int apiVerify(CommandSourceStack source) {
        if (!checkPermission(source, PERMISSION_API_VERIFY, "verify API token")) {
            return 0;
        }
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
                            source.sendSuccess(() -> Component.translatable("cwhitelist.api.verify.success"), false);
                            source.sendSuccess(() -> Component.translatable("cwhitelist.api.verify.token_info",
                                    tokenInfo.name,
                                    tokenInfo.canRead ? "✓" : "✗",
                                    tokenInfo.canWrite ? "✓" : "✗",
                                    tokenInfo.canDelete ? "✓" : "✗"), false);
                        }
                    } else {
                        source.sendFailure(Component.translatable("cwhitelist.api.verify.failed"));
                    }
                });
            } catch (Exception e) {
                source.getServer().execute(() -> {
                    source.sendFailure(Component.translatable("cwhitelist.api.verify.error",
                            e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
                });
            }
        });
        return 1;
    }

    private static int apiSync(CommandSourceStack source) {
        if (!checkPermission(source, PERMISSION_API_SYNC, "sync with API")) {
            return 0;
        }
        if (!Config.ENABLE_API.get()) {
            source.sendFailure(Component.translatable("cwhitelist.api.disabled"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("cwhitelist.api.sync.starting"), false);
        CompletableFuture.runAsync(() -> {
            try {
                java.util.List<WhitelistManager.WhitelistEntry> entries =
                        (java.util.List<WhitelistManager.WhitelistEntry>) ApiClient.forceSyncWhitelist().get(30, TimeUnit.SECONDS);
                source.getServer().execute(() -> {
                    if (entries != null && !entries.isEmpty()) {
                        WhitelistManager.reload();
                        source.sendSuccess(() -> Component.translatable("cwhitelist.api.sync.success", entries.size()), false);
                        if (WhitelistManager.isUsingApi()) {
                            source.sendSuccess(() -> Component.translatable("cwhitelist.reload.source_api"), false);
                        } else {
                            source.sendSuccess(() -> Component.translatable("cwhitelist.reload.source_local"), false);
                        }
                    } else {
                        source.sendFailure(Component.translatable("cwhitelist.api.sync.no_entries"));
                    }
                });
            } catch (java.util.concurrent.TimeoutException e) {
                source.getServer().execute(() -> {
                    source.sendFailure(Component.translatable("cwhitelist.api.sync.timeout"));
                });
            } catch (Exception e) {
                source.getServer().execute(() -> {
                    source.sendFailure(Component.translatable("cwhitelist.api.sync.error",
                            e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
                });
            }
        });
        return 1;
    }

    private static int clearCache(CommandSourceStack source) {
        if (!checkPermission(source, PERMISSION_API_CLEARCACHE, "clear API cache")) {
            return 0;
        }
        if (!Config.ENABLE_API.get()) {
            source.sendFailure(Component.translatable("cwhitelist.api.disabled"));
            return 0;
        }
        ApiClient.clearCache();
        source.sendSuccess(() -> Component.translatable("cwhitelist.api.cache.cleared"), true);
        return 1;
    }
}