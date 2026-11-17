package dev.greto.nightminigames.flashlight;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.greto.nightminigames.registry.ModBlocks;
import dev.greto.nightminigames.team.TeamBinding;
import dev.greto.nightminigames.module.ModuleSystem;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.SharedSuggestionProvider;
import dev.greto.nightminigames.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;

public final class FlashlightSpawnerCommands {
    private FlashlightSpawnerCommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        var root = Commands.literal("spawner");
        root.then(Commands.literal("wand")
            .then(Commands.argument("equipo", StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(ModuleSystem.TEAMS, builder))
                .executes(context -> give(context.getSource(), context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "equipo")))));
        root.then(Commands.literal("reactivar").executes(context -> edit(context.getSource(), false)));
        root.then(Commands.literal("eliminar").executes(context -> edit(context.getSource(), true)));
        return root;
    }

    private static int give(CommandSourceStack source, ServerPlayer target, String team) {
        if (!TeamBinding.valid(team)) {
            return 0;
        }
        ItemStack wand = TeamBinding.label(new ItemStack(ModItems.WAND_FLASHLIGHT), team);
        if (!target.getInventory().add(wand)) {
            target.drop(wand, false);
        }
        source.sendSuccess(() -> Component.literal("[Nightminigames] ").withStyle(ChatFormatting.DARK_AQUA)
            .append(Component.literal("Varita de linterna entregada a " + target.getGameProfile().getName()).withStyle(ChatFormatting.AQUA)), false);
        return 1;
    }

    private static int edit(CommandSourceStack source, boolean remove) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        var eye = player.getEyePosition();
        var hit = player.serverLevel().clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(6.0D)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        BlockPos pos = hit.getBlockPos();
        if (!player.serverLevel().hasChunkAt(pos)) {
            return 0;
        }
        var state = player.serverLevel().getBlockState(pos);
        if (state.getBlock() != ModBlocks.FLASHLIGHT_SPAWN) {
            source.sendFailure(Component.literal("Apunta al spawner de linterna.").withStyle(ChatFormatting.RED));
            return 0;
        }
        if (remove) {
            player.serverLevel().removeBlock(pos, false);
        } else {
            player.serverLevel().setBlock(pos, state.setValue(FlashlightSpawnBlock.COLLECTED, false), 3);
            dev.greto.nightminigames.persistence.WorldStructureJson.markBlockStateChanged(player.serverLevel(), pos);
        }
        source.sendSuccess(() -> Component.literal(remove ? "Spawner de linterna eliminado" : "Spawner de linterna reactivado").withStyle(ChatFormatting.GREEN), false);
        return 1;
    }
}
