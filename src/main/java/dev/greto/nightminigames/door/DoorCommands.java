package dev.greto.nightminigames.door;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.greto.nightminigames.region.RegionKind;
import dev.greto.nightminigames.module.ModuleSystem;
import dev.greto.nightminigames.team.TeamBinding;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.SharedSuggestionProvider;
import dev.greto.nightminigames.region.RegionSelectionSystem;
import dev.greto.nightminigames.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class DoorCommands {
    private DoorCommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("puertas");
        LiteralArgumentBuilder<CommandSourceStack> give = Commands.literal("dar");
        for (RegionKind kind : RegionKind.values()) {
            give.then(Commands.literal(kind.id)
                .then(Commands.argument("equipo", StringArgumentType.word())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(ModuleSystem.TEAMS, builder))
                    .executes(context -> give(context.getSource(), context.getSource().getPlayerOrException(), kind, StringArgumentType.getString(context, "equipo")))));
        }
        root.then(give);
        root.then(Commands.literal("estado").executes(context -> status(context.getSource())));
        root.then(Commands.literal("ayuda").executes(context -> help(context.getSource())));
        return root;
    }

    private static int give(CommandSourceStack source, ServerPlayer player, RegionKind kind, String team) {
        if (!TeamBinding.valid(team)) {
            return 0;
        }
        ItemStack stack = TeamBinding.label(new ItemStack(kind == RegionKind.ENTRADA ? ModItems.DOOR_ENTRANCE : ModItems.DOOR_EXIT), team);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        source.sendSuccess(() -> Component.literal("[Puertas] ").withStyle(ChatFormatting.DARK_GRAY)
            .append(Component.literal("Puerta " + kind.label).withStyle(kind.format, ChatFormatting.BOLD))
            .append(Component.literal(" entregada a " + player.getGameProfile().getName()).withStyle(ChatFormatting.WHITE)), false);
        return 1;
    }

    private static int status(CommandSourceStack source) {
        String dimension = source.getLevel().dimension().location().toString();
        for (String team : ModuleSystem.TEAMS) {
            for (RegionKind kind : RegionKind.values()) {
                int count = DoorSystem.loadedDoors(dimension, kind, team);
                String detail = kind == RegionKind.ENTRADA
                    ? " · Región: " + RegionSelectionSystem.status(kind, team)
                    : " · Máquinas completadas: " + DoorSystem.completedMachines(dimension, team);
                source.sendSuccess(() -> Component.literal("[Puertas] " + team.toUpperCase() + " · " + kind.label + ": " + count + detail)
                    .withStyle(TeamBinding.color(team)), false);
            }
        }
        return 1;
    }

    private static int help(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("/nightminigames puertas dar entrada [jugador]").withStyle(ChatFormatting.AQUA), false);
        source.sendSuccess(() -> Component.literal("/nightminigames puertas dar salida [jugador]").withStyle(ChatFormatting.GOLD), false);
        source.sendSuccess(() -> Component.literal("/nightminigames puertas estado").withStyle(ChatFormatting.GRAY), false);
        return 1;
    }
}
