package dev.greto.nightminigames.region;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.greto.nightminigames.module.ModuleSystem;
import dev.greto.nightminigames.registry.ModItems;
import dev.greto.nightminigames.team.TeamBinding;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class RegionSelectionCommands {
    private RegionSelectionCommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        var root = Commands.literal("regiones");
        var wand = Commands.literal("wand");
        var clear = Commands.literal("limpiar");
        for (RegionKind kind : RegionKind.values()) {
            wand.then(Commands.literal(kind.id)
                .then(Commands.argument("equipo", StringArgumentType.word())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(ModuleSystem.TEAMS, builder))
                    .executes(context -> give(context.getSource(), context.getSource().getPlayerOrException(), kind, StringArgumentType.getString(context, "equipo")))
                    .then(Commands.argument("jugador", EntityArgument.player())
                        .executes(context -> give(context.getSource(), EntityArgument.getPlayer(context, "jugador"), kind, StringArgumentType.getString(context, "equipo"))))));
            clear.then(Commands.literal(kind.id)
                .then(Commands.argument("equipo", StringArgumentType.word())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(ModuleSystem.TEAMS, builder))
                    .executes(context -> clear(context.getSource(), kind, StringArgumentType.getString(context, "equipo")))));
        }
        root.then(wand);
        root.then(clear);
        root.then(Commands.literal("estado").executes(context -> status(context.getSource())));
        return root;
    }

    private static int give(CommandSourceStack source, ServerPlayer player, RegionKind kind, String team) {
        if (!TeamBinding.valid(team)) {
            source.sendFailure(Component.literal("Equipo no válido").withStyle(ChatFormatting.RED));
            return 0;
        }
        ItemStack stack = TeamBinding.label(new ItemStack(ModItems.regionWand(kind)), team);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        source.sendSuccess(() -> Component.literal("Varita " + kind.label + " de " + team + " entregada").withStyle(TeamBinding.color(team)), false);
        return 1;
    }

    private static int clear(CommandSourceStack source, RegionKind kind, String team) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        if (!TeamBinding.valid(team)) {
            return 0;
        }
        RegionSelectionSystem.clear(source.getPlayerOrException(), team, kind);
        return 1;
    }

    private static int status(CommandSourceStack source) {
        for (String team : ModuleSystem.TEAMS) {
            for (RegionKind kind : RegionKind.values()) {
                source.sendSuccess(() -> Component.literal(team.toUpperCase() + " · " + kind.label + ": " + RegionSelectionSystem.status(kind, team))
                    .withStyle(TeamBinding.color(team)), false);
            }
        }
        return 1;
    }
}
