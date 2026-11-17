package dev.greto.nightminigames.machine;

import dev.greto.nightminigames.key.KeyColor;
import dev.greto.nightminigames.door.DoorSystem;
import dev.greto.nightminigames.registry.ModBlocks;
import dev.greto.nightminigames.registry.ModSounds;
import net.minecraft.world.scores.PlayerTeam;
import dev.greto.nightminigames.team.TeamBinding;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class MachineSystem {
    private static final List<DelayedLock> DELAYED_LOCKS = new ArrayList<>();
    private static boolean initialized;

    private MachineSystem() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        MachineProgressSync.initialize();
        MachineCompletionSync.initialize();
        ServerTickEvents.END_SERVER_TICK.register(MachineSystem::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> DELAYED_LOCKS.clear());
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }
            BlockPos master = MachineBlock.resolveMaster(level, hit.getBlockPos());
            if (master == null) {
                return InteractionResult.PASS;
            }
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
                return InteractionResult.PASS;
            }
            if (dev.greto.nightminigames.machine.TeamExitCompletionSystem.isFinished()) {
                return InteractionResult.FAIL;
            }
            if (player.isSpectator() || !player.isAlive() || player.getEyePosition().distanceToSqr(Vec3.atCenterOf(master)) > 49.0D) {
                return InteractionResult.FAIL;
            }
            ItemStack held = player.getItemInHand(hand);
            KeyColor color = keyColor(held.getItem());
            BlockState state = serverLevel.getBlockState(master);
            if (state.getBlock() != ModBlocks.MACHINE) {
                return InteractionResult.PASS;
            }
            int mask = state.getValue(MachineBlock.MASK);
            if (color != null && serverLevel.getBlockEntity(master) instanceof MachineBlockEntity machineEntity) {
                String requiredTeam = machineEntity.getOwnerTeam();
                if (!requiredTeam.isBlank() && !serverPlayer.hasPermissions(2)) {
                    PlayerTeam playerTeam = serverLevel.getServer().getScoreboard().getPlayersTeam(serverPlayer.getScoreboardName());
                    String keyTeam = TeamBinding.of(held);
                    if (playerTeam == null || !requiredTeam.equals(playerTeam.getName()) || !requiredTeam.equals(keyTeam)) {
                        MachineProgressSync.sendTo(serverPlayer, master, -4);
                        return InteractionResult.SUCCESS;
                    }
                }
            }
            if (color == null) {
                MachineProgressSync.sendTo(serverPlayer, master);
                return InteractionResult.SUCCESS;
            }
            int bit = 1 << color.ordinal();
            if ((mask & bit) != 0) {
                MachineProgressSync.sendTo(serverPlayer, master, -3);
                return InteractionResult.SUCCESS;
            }
            int next = mask | bit;
            if (!serverLevel.setBlock(master, state.setValue(MachineBlock.MASK, next), 3)) {
                return InteractionResult.FAIL;
            }
            if (serverLevel.getBlockEntity(master) instanceof MachineBlockEntity entity) {
                entity.setInserter(color, player.getGameProfile().getName());
            }
            if (!serverPlayer.getAbilities().instabuild) {
                held.shrink(1);
            }
            serverLevel.playSound(null, master, ModSounds.MACHINE_INSERT, SoundSource.BLOCKS, 0.85F, 1.0F);
            DELAYED_LOCKS.add(new DelayedLock(serverLevel, master.immutable(), serverLevel.getServer().getTickCount() + 7));
            MachineProgressSync.update(serverLevel, master, player.getGameProfile().getName(), color.ordinal());
            DoorSystem.onMachineChanged(serverLevel, master, next);
            if (next == 15) {
                MachineCompletionSync.announce(serverLevel, master, serverPlayer);
            }
            String teamForSound = "";
            if (serverLevel.getBlockEntity(master) instanceof MachineBlockEntity machine) {
                teamForSound = machine.getOwnerTeam();
            }
            if (teamForSound.isBlank()) {
                var membership = serverLevel.getServer().getScoreboard().getPlayersTeam(serverPlayer.getScoreboardName());
                teamForSound = membership == null ? "" : membership.getName();
            }
            for (ServerPlayer receiver : serverLevel.getServer().getPlayerList().getPlayers()) {
                if (dev.greto.nightminigames.feed.HudFeedSystem.canView(receiver, teamForSound)) {
                    receiver.playNotifySound(ModSounds.MACHINE_CLICK, SoundSource.MASTER, 0.55F, 1.0F);
                    if (next == 15) {
                        receiver.playNotifySound(ModSounds.MACHINE_ALARM, SoundSource.MASTER, 0.52F, 1.0F);
                    }
                }
            }
            return InteractionResult.SUCCESS;
        });
    }

    private static void tick(MinecraftServer server) {
        int currentTick = server.getTickCount();
        var iterator = DELAYED_LOCKS.iterator();
        while (iterator.hasNext()) {
            DelayedLock sound = iterator.next();
            if (sound.dueTick() > currentTick) {
                continue;
            }
            iterator.remove();
            if (sound.level().getServer() == server && sound.level().hasChunkAt(sound.pos())
                && sound.level().getBlockState(sound.pos()).getBlock() == ModBlocks.MACHINE) {
                sound.level().playSound(null, sound.pos(), ModSounds.MACHINE_LOCK, SoundSource.BLOCKS, 0.72F, 1.0F);
            }
        }
    }

    public static KeyColor keyColor(Item item) {
        for (KeyColor color : KeyColor.values()) {
            if (color.keyItem() == item) {
                return color;
            }
        }
        return null;
    }

    private record DelayedLock(ServerLevel level, BlockPos pos, int dueTick) {
    }
}
