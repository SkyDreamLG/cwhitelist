package org.skydream.cwhitelist;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.Arrays;

public class WhitelistCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("cwhitelist")
                .requires(source -> source.hasPermission(4))
                .then(Commands.literal("add")
                        .then(Commands.literal("name") // type 为 "name"
                                .then(Commands.argument("value", StringArgumentType.string())
                                        .executes(ctx -> addEntry(ctx.getSource(), "name", StringArgumentType.getString(ctx, "value")))
                                )
                        )
                        .then(Commands.literal("uuid") // type 为 "uuid"
                                .then(Commands.argument("value", StringArgumentType.string())
                                        .executes(ctx -> addEntry(ctx.getSource(), "uuid", StringArgumentType.getString(ctx, "value")))
                                )
                        )
                        .then(Commands.literal("ip") // type 为 "ip"
                                .then(Commands.argument("value", StringArgumentType.string())
                                        .executes(ctx -> addEntry(ctx.getSource(), "ip", StringArgumentType.getString(ctx, "value")))
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
                        .then(Commands.literal("name") // type 为 "name"
                                .then(Commands.argument("value", StringArgumentType.string())
                                        .executes(ctx -> removeEntry(ctx.getSource(), "name", StringArgumentType.getString(ctx, "value")))
                                )
                        )
                        .then(Commands.literal("uuid") // type 为 "uuid"
                                .then(Commands.argument("value", StringArgumentType.string())
                                        .executes(ctx -> removeEntry(ctx.getSource(), "uuid", StringArgumentType.getString(ctx, "value")))
                                )
                        )
                        .then(Commands.literal("ip") // type 为 "ip"
                                .then(Commands.argument("value", StringArgumentType.string())
                                        .executes(ctx -> removeEntry(ctx.getSource(), "ip", StringArgumentType.getString(ctx, "value")))
                                )
                        )
                )
        );
    }

    private static int addEntry(CommandSourceStack source, String type, String value) {
        // 校验 type 是否合法
        if (!Arrays.asList("name", "uuid", "ip").contains(type.toLowerCase())) {
            source.sendFailure(Component.literal("输入有误! 请输入 name/uuid/ip"));
            return 0; // 返回 0 表示类型无效
        }

        // 创建条目对象
        WhitelistManager.WhitelistEntry entry = new WhitelistManager.WhitelistEntry(type, value);

        // 检查条目是否已存在
        if (WhitelistManager.containsEntry(entry)) {
            source.sendFailure(Component.literal("条目已存在: " + type + "=" + value));
            return 2; // 返回 2 表示条目已存在
        }

        try {
            // 添加条目到白名单
            WhitelistManager.addEntry(entry);
            source.sendSuccess(() -> Component.literal("条目已添加: " + type + "=" + value), true);
            return 1; // 返回 1 表示成功
        } catch (Exception e) {
            // 如果添加失败，发送错误消息
            source.sendFailure(Component.literal("条目添加失败: " + e.getMessage()));
            return 3; // 返回 3 表示添加失败
        }
    }

    private static int removeEntry(CommandSourceStack source, String type, String value) {
        boolean removed = WhitelistManager.removeEntry(type, value);
        if (removed) {
            source.sendSuccess(() -> Component.literal("条目已移除: " + type + "=" + value), true);
        } else {
            source.sendFailure(Component.literal("条目不存在!"));
        }
        return removed ? 1 : 0;
    }

    private static int listEntries(CommandSourceStack source) {
        StringBuilder sb = new StringBuilder("白名单列表:\n");
        WhitelistManager.getEntries().forEach(e -> sb.append(e.getType()).append(": ").append(e.getValue()).append("\n"));
        source.sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    private static int reload(CommandSourceStack source) {
        WhitelistManager.reload();
        source.sendSuccess(() -> Component.literal("白名单配置已重载!"), true);
        return 1;
    }
}