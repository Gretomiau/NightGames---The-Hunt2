package dev.greto.nightminigames.key;

import dev.greto.nightminigames.registry.ModBlocks;
import dev.greto.nightminigames.team.TeamBinding;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class KeySpawnerWandItem extends Item {
    private final KeyColor color;

    public KeySpawnerWandItem(KeyColor color) {
        super(new Item.Properties().stacksTo(1));
        this.color = color;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.SUCCESS;
        }
        if (!player.hasPermissions(2)) {
            dev.greto.nightminigames.admin.AdminMessages.send(player, KeyMessages.message("Herramienta reservada a operadores.", net.minecraft.ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        if (!TeamBinding.requireKitTeam(player, context.getItemInHand())) {
            return InteractionResult.FAIL;
        }
        Level world = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        BlockState state = world.getBlockState(clicked);
        if (state.getBlock() instanceof KeySpawnBlock existing) {
            if (!(world.getBlockEntity(clicked) instanceof KeySpawnBlockEntity entity)
                || !entity.ownerTeam().equals(TeamBinding.of(context.getItemInHand()))) {
                dev.greto.nightminigames.admin.AdminMessages.send(player, KeyMessages.message("Este spawner pertenece a otro equipo.", net.minecraft.ChatFormatting.RED));
                return InteractionResult.FAIL;
            }
            if (player.isShiftKeyDown()) {
                world.removeBlock(clicked, false);
                dev.greto.nightminigames.admin.AdminMessages.send(player, KeyMessages.key("Spawner eliminado: ", existing.color));
            } else if (state.getValue(KeySpawnBlock.COLLECTED)) {
                world.setBlock(clicked, state.setValue(KeySpawnBlock.COLLECTED, false), 3);
                dev.greto.nightminigames.persistence.WorldStructureJson.markBlockStateChanged(player.serverLevel(), clicked);
                dev.greto.nightminigames.admin.AdminMessages.send(player, KeyMessages.key("Spawner reactivado: ", existing.color));
            } else {
                dev.greto.nightminigames.admin.AdminMessages.send(player, KeyMessages.message("El spawner ya está activo. Agáchate y haz clic derecho para eliminarlo.", net.minecraft.ChatFormatting.YELLOW));
            }
            return InteractionResult.SUCCESS;
        }
        BlockPos target = clicked.relative(context.getClickedFace());
        if (!world.getWorldBorder().isWithinBounds(target) || !world.mayInteract(player, target) || !world.getBlockState(target).canBeReplaced()) {
            dev.greto.nightminigames.admin.AdminMessages.send(player, KeyMessages.message("No se puede colocar el spawner en esa posición.", net.minecraft.ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        if (!TeamBinding.requireKitTeam(player, context.getItemInHand())) {
            return InteractionResult.FAIL;
        }
        if (!world.setBlock(target, ModBlocks.byColor(color).defaultBlockState(), 3)) {
            return InteractionResult.FAIL;
        }
        if (world.getBlockEntity(target) instanceof KeySpawnBlockEntity blockEntity) {
            blockEntity.setOwnerTeam(TeamBinding.of(context.getItemInHand()));
            dev.greto.nightminigames.round.RoundResetSystem.markNew(blockEntity);
        }
        KeySpawnerPlacementPayload notice = new KeySpawnerPlacementPayload(target, color.id);
        Vec3 center = Vec3.atCenterOf(target);
        for (ServerPlayer recipient : player.server.getPlayerList().getPlayers()) {
            if (recipient.hasPermissions(2) && recipient.serverLevel() == player.serverLevel()
                && recipient.distanceToSqr(center) <= 2304.0D) {
                ServerPlayNetworking.send(recipient, notice);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
