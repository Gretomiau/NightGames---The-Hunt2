package dev.greto.nightminigames.machine;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.greto.nightminigames.key.KeyMessages;
import dev.greto.nightminigames.module.ModuleSystem;
import dev.greto.nightminigames.team.TeamBinding;
import net.minecraft.commands.SharedSuggestionProvider;
import dev.greto.nightminigames.door.DoorSystem;
import dev.greto.nightminigames.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;

public final class MachineCommands {
    private MachineCommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("maquina")
            .then(Commands.literal("dar")
                .then(Commands.argument("equipo", StringArgumentType.word())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(ModuleSystem.TEAMS, builder))
                    .executes(context -> give(context.getSource(), context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "equipo")))))
            .then(Commands.literal("estado").executes(context -> status(context.getSource())))
            .then(Commands.literal("resetear").executes(context -> reset(context.getSource())))
            .then(Commands.literal("eliminar").executes(context -> remove(context.getSource())))
            .then(Commands.literal("vincular")
                .then(Commands.argument("equipo", StringArgumentType.word())
                    .executes(context -> bindTeam(context.getSource(), StringArgumentType.getString(context, "equipo")))))
            .then(Commands.literal("desvincular").executes(context -> bindTeam(context.getSource(), "")))
            .then(Commands.literal("ayuda").executes(context -> help(context.getSource())));
    }

    private static int give(CommandSourceStack source, ServerPlayer player, String team) {
        if (!TeamBinding.valid(team)) {
            return 0;
        }
        ItemStack stack = TeamBinding.label(new ItemStack(ModBlocks.MACHINE), team);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        source.sendSuccess(() -> KeyMessages.message("Máquina entregada a " + player.getGameProfile().getName() + ".", ChatFormatting.GREEN), false);
        return 1;
    }

    private static BlockPos targetedMachine(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        BlockHitResult hit = player.serverLevel().clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(7.0D)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        return MachineBlock.resolveMaster(player.serverLevel(), hit.getBlockPos());
    }

    private static int status(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        BlockPos master = targetedMachine(player);
        if (master == null) {
            source.sendFailure(KeyMessages.message("Apunta a una máquina para consultar su estado.", ChatFormatting.RED));
            return 0;
        }
        MachineProgressSync.sendTo(player, master);
        return 1;
    }

    private static int reset(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        BlockPos master = targetedMachine(player);
        if (master == null) {
            source.sendFailure(KeyMessages.message("Apunta a una máquina para reiniciar sus cerraduras.", ChatFormatting.RED));
            return 0;
        }
        BlockState state = player.serverLevel().getBlockState(master);
        player.serverLevel().setBlock(master, state.setValue(MachineBlock.MASK, 0), 3);
        if (player.serverLevel().getBlockEntity(master) instanceof MachineBlockEntity entity) {
            entity.resetInserters();
        }
        MachineProgressSync.update(player.serverLevel(), master, "", -2);
        DoorSystem.onMachineChanged(player.serverLevel(), master, 0);
        MachineCompletionSync.reset(player.serverLevel(), master);
        return 1;
    }

    private static int remove(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        BlockPos master = targetedMachine(player);
        if (master == null) {
            source.sendFailure(KeyMessages.message("Apunta a una máquina para eliminarla.", ChatFormatting.RED));
            return 0;
        }
        player.serverLevel().removeBlock(master, false);
        source.sendSuccess(() -> KeyMessages.message("Máquina y sus piezas eliminadas.", ChatFormatting.GREEN), false);
        return 1;
    }

    private static int bindTeam(CommandSourceStack source, String teamName) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        BlockPos master = targetedMachine(player);
        if (master == null || !(player.serverLevel().getBlockEntity(master) instanceof MachineBlockEntity entity)) {
            source.sendFailure(KeyMessages.message("Apunta a una máquina para vincularla a un equipo.", ChatFormatting.RED));
            return 0;
        }
        if (!teamName.isBlank()) {
            PlayerTeam team = source.getServer().getScoreboard().getPlayerTeam(teamName);
            if (team == null) {
                source.sendFailure(KeyMessages.message("El equipo " + teamName + " no existe.", ChatFormatting.RED));
                return 0;
            }
        }
        entity.setOwnerTeam(teamName);
        source.sendSuccess(() -> KeyMessages.message(teamName.isBlank() ? "Máquina desvinculada: acepta llaves de cualquier jugador." : "Máquina vinculada al equipo " + teamName + ".", ChatFormatting.GREEN), false);
        return 1;
    }

    private static int help(CommandSourceStack source) {
        source.sendSuccess(() -> KeyMessages.message("/nightminigames maquina dar [jugador]", ChatFormatting.AQUA), false);
        source.sendSuccess(() -> KeyMessages.message("/nightminigames maquina estado", ChatFormatting.AQUA), false);
        source.sendSuccess(() -> KeyMessages.message("/nightminigames maquina resetear", ChatFormatting.AQUA), false);
        source.sendSuccess(() -> KeyMessages.message("/nightminigames maquina eliminar", ChatFormatting.AQUA), false);
        source.sendSuccess(() -> KeyMessages.message("/nightminigames maquina vincular <equipo> | desvincular", ChatFormatting.AQUA), false);
        return 1;
    }
}
