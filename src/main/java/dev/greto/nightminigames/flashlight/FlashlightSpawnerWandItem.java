package dev.greto.nightminigames.flashlight;

import dev.greto.nightminigames.registry.ModBlocks;
import dev.greto.nightminigames.team.TeamBinding;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class FlashlightSpawnerWandItem extends Item {
    public FlashlightSpawnerWandItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.SUCCESS;
        }
        if (!player.hasPermissions(2)) {
            return InteractionResult.FAIL;
        }
        if (!TeamBinding.requireKitTeam(player, context.getItemInHand())) {
            return InteractionResult.FAIL;
        }
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        BlockState state = level.getBlockState(clicked);
        if (state.getBlock() == ModBlocks.FLASHLIGHT_SPAWN) {
            if (!(level.getBlockEntity(clicked) instanceof FlashlightSpawnBlockEntity entity)
                || !entity.ownerTeam().equals(TeamBinding.of(context.getItemInHand()))) {
                message(player, "El spawner pertenece a otro equipo", ChatFormatting.RED);
                return InteractionResult.FAIL;
            }
            if (player.isShiftKeyDown()) {
                level.removeBlock(clicked, false);
                message(player, "Spawner de linterna eliminado", ChatFormatting.RED);
            } else if (state.getValue(FlashlightSpawnBlock.COLLECTED)) {
                level.setBlock(clicked, state.setValue(FlashlightSpawnBlock.COLLECTED, false), 3);
                dev.greto.nightminigames.persistence.WorldStructureJson.markBlockStateChanged(player.serverLevel(), clicked);
                message(player, "Spawner de linterna reactivado", ChatFormatting.GREEN);
            } else {
                message(player, "Agáchate y haz clic derecho para eliminarlo", ChatFormatting.YELLOW);
            }
            return InteractionResult.SUCCESS;
        }
        BlockPos target = clicked.relative(context.getClickedFace());
        if (!level.hasChunkAt(target) || !level.getWorldBorder().isWithinBounds(target)
            || !level.mayInteract(player, target) || !level.getBlockState(target).canBeReplaced()) {
            message(player, "Posición no válida para el spawner", ChatFormatting.RED);
            return InteractionResult.FAIL;
        }
        if (!TeamBinding.requireKitTeam(player, context.getItemInHand())) {
            return InteractionResult.FAIL;
        }
        if (level.setBlock(target, ModBlocks.FLASHLIGHT_SPAWN.defaultBlockState(), 3)) {
            if (level.getBlockEntity(target) instanceof FlashlightSpawnBlockEntity entity) {
                entity.setOwnerTeam(TeamBinding.of(context.getItemInHand()));
            dev.greto.nightminigames.round.RoundResetSystem.markNew(entity);
            }
            message(player, "Spawner de linterna colocado", ChatFormatting.AQUA);
        }
        return InteractionResult.SUCCESS;
    }

    private static void message(ServerPlayer player, String text, ChatFormatting color) {
        dev.greto.nightminigames.admin.AdminMessages.send(player,
            dev.greto.nightminigames.key.KeyMessages.message(text, color));
    }
}
