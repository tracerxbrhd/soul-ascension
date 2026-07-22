package dev.uapi.soulascension.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.uapi.soulascension.data.Stat;
import dev.uapi.soulascension.data.PlayerProgress;
import dev.uapi.soulascension.data.SoulAscensionAttachments;
import dev.uapi.soulascension.progression.SoulAscensionService;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
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
            .then(Commands.literal("profile")
                .then(Commands.literal("public")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        boolean enabled = player.getData(SoulAscensionAttachments.PROFILE_SETTINGS).publicProfile();
                        context.getSource().sendSuccess(() -> Component.translatable(
                            "command.soul_ascension.profile.public.status", enabled), false);
                        return enabled ? 1 : 0;
                    })
                    .then(Commands.argument("enabled", BoolArgumentType.bool()).executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        boolean enabled = BoolArgumentType.getBool(context, "enabled");
                        player.setData(SoulAscensionAttachments.PROFILE_SETTINGS,
                            player.getData(SoulAscensionAttachments.PROFILE_SETTINGS).withPublicProfile(enabled));
                        context.getSource().sendSuccess(() -> Component.translatable(
                            "command.soul_ascension.profile.public.updated", enabled), false);
                        return 1;
                    }))))
            .then(Commands.literal("add_xp").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                        .executes(context -> SoulAscensionService.addExperience(EntityArgument.getPlayer(context, "player"),
                            DoubleArgumentType.getDouble(context, "amount"))))))
            .then(Commands.literal("add_points").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("amount", IntegerArgumentType.integer())
                        .executes(context -> {
                            return SoulAscensionService.addPointsIfWritable(
                                EntityArgument.getPlayer(context, "player"),
                                IntegerArgumentType.getInteger(context, "amount")) ? 1 : 0;
                        }))))
            .then(Commands.literal("stat").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
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
            .then(Commands.literal("reset_stats").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("player", EntityArgument.player()).executes(context ->
                    SoulAscensionService.reset(EntityArgument.getPlayer(context, "player")) ? 1 : 0)))
            .then(Commands.literal("title").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("unlock").then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("title", IdentifierArgument.id()).executes(context ->
                        TitleService.forceUnlock(EntityArgument.getPlayer(context, "player"),
                            IdentifierArgument.getId(context, "title")) ? 1 : 0))))
                .then(Commands.literal("select").then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("title", IdentifierArgument.id()).executes(context ->
                        TitleService.select(EntityArgument.getPlayer(context, "player"),
                            IdentifierArgument.getId(context, "title")) ? 1 : 0))))
                .then(Commands.literal("reset").then(Commands.argument("player", EntityArgument.player()).executes(context -> {
                    TitleService.resetTitles(EntityArgument.getPlayer(context, "player")); return 1;
                }))))
            .then(Commands.literal("reset").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("player", EntityArgument.player()).executes(context ->
                    SoulAscensionService.resetAllIfWritable(EntityArgument.getPlayer(context, "player"))
                        ? 1 : 0)));
    }
}
