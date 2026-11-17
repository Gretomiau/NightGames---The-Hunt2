package dev.greto.nightminigames.key;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.greto.nightminigames.registry.ModItems;
import dev.greto.nightminigames.team.TeamBinding;
import dev.greto.nightminigames.module.ModuleSystem;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.ClipContext;

public final class KeySpawnerCommands {
    private KeySpawnerCommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        var root = Commands.literal("llaves");
        var wands = Commands.literal("wand");
        var give = Commands.literal("dar");
        for (KeyColor color : KeyColor.values()) {
            wands.then(Commands.literal(spanish(color))
                .then(Commands.argument("equipo", StringArgumentType.word())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(ModuleSystem.TEAMS, builder))
                    .executes(context -> give(context.getSource(), context.getSource().getPlayerOrException(), color, true, StringArgumentType.getString(context, "equipo")))));
            give.then(Commands.literal(spanish(color))
                .then(Commands.argument("equipo", StringArgumentType.word())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(ModuleSystem.TEAMS, builder))
                    .executes(context -> give(context.getSource(), context.getSource().getPlayerOrException(), color, false, StringArgumentType.getString(context, "equipo")))));
        }
        root.then(wands);
        root.then(give);
        root.then(KeyLocationsCommands.create());
        root.then(KeyConfigurationCommands.create());
        root.then(KeyConfigurationCommands.previewCommands());
        root.then(Commands.literal("spawner")
            .then(Commands.literal("reactivar").executes(context -> edit(context.getSource(), false)))
            .then(Commands.literal("eliminar").executes(context -> edit(context.getSource(), true))));
        root.then(Commands.literal("ayuda").executes(context -> help(context.getSource())));
        return root;
    }

    private static String spanish(KeyColor color) {
        return switch (color) {
            case BLUE -> "azul";
            case YELLOW -> "amarilla";
            case RED -> "roja";
            case GREEN -> "verde";
        };
    }

    private static int give(CommandSourceStack source, ServerPlayer target, KeyColor color, boolean wand, String team) {
        if (!TeamBinding.valid(team)) {
            return 0;
        }
        ItemStack stack = TeamBinding.label(new ItemStack(wand ? ModItems.wand(color) : color.keyItem()), team);
        if (!target.getInventory().add(stack)) {
            target.drop(stack, false);
        }
        source.sendSuccess(() -> KeyMessages.prefix()
            .append(net.minecraft.network.chat.Component.literal(wand ? "Varita " : "Llave ").withStyle(ChatFormatting.GRAY))
            .append(net.minecraft.network.chat.Component.literal(color.display).withStyle(color.formatting, ChatFormatting.BOLD))
            .append(net.minecraft.network.chat.Component.literal(" entregada a " + target.getGameProfile().getName()).withStyle(ChatFormatting.WHITE)), false);
        return 1;
    }

    private static int edit(CommandSourceStack source, boolean remove) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        var eye = player.getEyePosition();
        var hit = player.serverLevel().clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(6.0D)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        BlockPos pos = hit.getBlockPos();
        var state = player.serverLevel().getBlockState(pos);
        if (!(state.getBlock() instanceof KeySpawnBlock block)) {
            source.sendFailure(KeyMessages.message("Apunta a un holograma de llave válido.", ChatFormatting.RED));
            return 0;
        }
        if (remove) {
            player.serverLevel().removeBlock(pos, false);
        } else {
            player.serverLevel().setBlock(pos, state.setValue(KeySpawnBlock.COLLECTED, false), 3);
            dev.greto.nightminigames.persistence.WorldStructureJson.markBlockStateChanged(player.serverLevel(), pos);
        }
        source.sendSuccess(() -> KeyMessages.key(remove ? "Spawner eliminado: " : "Spawner reactivado: ", block.color), false);
        return 1;
    }

    private static int help(CommandSourceStack source) {
        source.sendSuccess(() -> KeyMessages.message("LLAVES • CONFIGURACIÓN", ChatFormatting.GOLD), false);
        source.sendSuccess(() -> KeyMessages.message("/nightminigames llaves wand <azul|amarilla|roja|verde> <equipo>", ChatFormatting.AQUA), false);
        source.sendSuccess(() -> KeyMessages.message("/nightminigames llaves dar <color> <equipo>", ChatFormatting.AQUA), false);
        source.sendSuccess(() -> KeyMessages.message("/nightminigames llaves spawner reactivar|eliminar", ChatFormatting.AQUA), false);
        source.sendSuccess(() -> KeyMessages.message("/nightminigames llaves ubicaciones <equipo|todos> · Coordenadas y waypoints por equipo.", ChatFormatting.AQUA), false);
        source.sendSuccess(() -> KeyMessages.message("/nightminigames llaves comprobar [equipo] · Revisa los cuatro colores colocados.", ChatFormatting.AQUA), false);
        source.sendSuccess(() -> KeyMessages.message("Varita: clic derecho para colocar; agachado para eliminar.", ChatFormatting.GRAY), false);
        source.sendSuccess(() -> KeyMessages.message("Jugador: mantener clic derecho 6 s para obtener la llave.", ChatFormatting.GREEN), false);
        return 1;
    }
}
