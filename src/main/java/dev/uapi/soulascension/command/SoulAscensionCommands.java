package dev.uapi.soulascension.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.uapi.soulascension.data.Stat;
import dev.uapi.soulascension.data.PlayerProgress;
import dev.uapi.soulascension.progression.SoulAscensionService;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import dev.uapi.soulascension.title.TitleService;

public final class SoulAscensionCommands {
    private SoulAscensionCommands() {}

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("soulascension")
            .then(Commands.literal("view").executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                PlayerProgress data = SoulAscensionService.get(player);
                context.getSource().sendSuccess(() -> Component.literal("Level " + data.level() +
                    ", progress " + String.format("%.1f", data.damageProgress()) + ", points " + data.unspentPoints()), false);
                return data.level();
            }))
            .then(Commands.literal("add_xp").requires(source -> source.hasPermission(2))
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                        .executes(context -> SoulAscensionService.addExperience(EntityArgument.getPlayer(context, "player"),
                            DoubleArgumentType.getDouble(context, "amount"))))))
            .then(Commands.literal("add_points").requires(source -> source.hasPermission(2))
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("amount", IntegerArgumentType.integer())
                        .executes(context -> {
                            SoulAscensionService.addPoints(EntityArgument.getPlayer(context, "player"),
                                IntegerArgumentType.getInteger(context, "amount"));
                            return 1;
                        }))))
            .then(Commands.literal("stat").requires(source -> source.hasPermission(2))
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("stat", StringArgumentType.word())
                        .then(Commands.argument("delta", IntegerArgumentType.integer(-1, 1)).executes(context -> {
                            int delta = IntegerArgumentType.getInteger(context, "delta");
                            if (delta == 0) return 0;
                            try {
                                Stat stat = Stat.valueOf(StringArgumentType.getString(context, "stat").toUpperCase(java.util.Locale.ROOT));
                                return SoulAscensionService.changeStat(EntityArgument.getPlayer(context, "player"), stat, delta) ? 1 : 0;
                            } catch (IllegalArgumentException exception) { return 0; }
                        })))))
            .then(Commands.literal("reset_stats").requires(source -> source.hasPermission(2))
                .then(Commands.argument("player", EntityArgument.player()).executes(context ->
                    SoulAscensionService.reset(EntityArgument.getPlayer(context, "player")) ? 1 : 0)))
            .then(Commands.literal("title").requires(source -> source.hasPermission(2))
                .then(Commands.literal("unlock").then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("title", ResourceLocationArgument.id()).executes(context ->
                        TitleService.forceUnlock(EntityArgument.getPlayer(context, "player"),
                            ResourceLocationArgument.getId(context, "title")) ? 1 : 0))))
                .then(Commands.literal("select").then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("title", ResourceLocationArgument.id()).executes(context ->
                        TitleService.select(EntityArgument.getPlayer(context, "player"),
                            ResourceLocationArgument.getId(context, "title")) ? 1 : 0))))
                .then(Commands.literal("reset").then(Commands.argument("player", EntityArgument.player()).executes(context -> {
                    TitleService.resetTitles(EntityArgument.getPlayer(context, "player")); return 1;
                }))))
            .then(Commands.literal("reset").requires(source -> source.hasPermission(2))
                .then(Commands.argument("player", EntityArgument.player()).executes(context -> {
                    SoulAscensionService.resetAll(EntityArgument.getPlayer(context, "player"));
                    return 1;
                })));
    }
}
