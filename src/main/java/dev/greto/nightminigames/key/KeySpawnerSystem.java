package dev.greto.nightminigames.key;

import dev.greto.nightminigames.feed.HudFeedSystem;
import dev.greto.nightminigames.team.TeamBinding;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import dev.greto.nightminigames.registry.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class KeySpawnerSystem {
    public static final int HOLD_TICKS = 120;
    private static final Map<UUID, Hold> HOLDS = new HashMap<>();
    private static boolean initialized;

    private KeySpawnerSystem() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        PayloadTypeRegistry.playC2S().register(KeyHoldPayload.TYPE, KeyHoldPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(KeyPickupPayload.TYPE, KeyPickupPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(KeyAnnouncementPayload.TYPE, KeyAnnouncementPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(KeySpawnerPlacementPayload.TYPE, KeySpawnerPlacementPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(KeyHoldPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> handle(player, payload));
        });
        ServerTickEvents.END_SERVER_TICK.register(KeySpawnerSystem::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> HOLDS.remove(handler.player.getUUID()));
    }

    private static void handle(ServerPlayer player, KeyHoldPayload packet) {
        if (dev.greto.nightminigames.machine.TeamExitCompletionSystem.isFinished()) {
            HOLDS.remove(player.getUUID());
            return;
        }
        UUID id = player.getUUID();
        if (!packet.holding()) {
            Hold previous = HOLDS.get(id);
            if (previous != null && previous.pos.equals(packet.pos())) {
                HOLDS.remove(id);
            }
            return;
        }
        if (!valid(player, packet.pos())) {
            HOLDS.remove(id);
            return;
        }
        long tick = player.server.getTickCount();
        Hold previous = HOLDS.get(id);
        if (previous == null || !previous.pos.equals(packet.pos()) || previous.dimension != player.serverLevel()) {
            HOLDS.put(id, new Hold(packet.pos(), player.serverLevel(), tick, tick));
        } else {
            previous.heartbeat = tick;
        }
    }

    private static void tick(MinecraftServer server) {
        if (dev.greto.nightminigames.machine.TeamExitCompletionSystem.isFinished()) {
            HOLDS.clear();
            return;
        }
        long now = server.getTickCount();
        var iterator = HOLDS.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            Hold hold = entry.getValue();
            if (player == null || player.serverLevel() != hold.dimension || now - hold.heartbeat > 12 || !valid(player, hold.pos)) {
                iterator.remove();
                continue;
            }
            if (now - hold.start < HOLD_TICKS) {
                continue;
            }
            iterator.remove();
            BlockState state = player.level().getBlockState(hold.pos);
            KeySpawnBlock block = (KeySpawnBlock) state.getBlock();
            player.level().setBlock(hold.pos, state.setValue(KeySpawnBlock.COLLECTED, true), 3);
            dev.greto.nightminigames.persistence.WorldStructureJson.markBlockStateChanged(player.serverLevel(), hold.pos);
            ItemStack item = new ItemStack(block.color.keyItem());
            String team = "";
            if (player.serverLevel().getBlockEntity(hold.pos) instanceof KeySpawnBlockEntity spawnEntity) {
                team = spawnEntity.ownerTeam();
                TeamBinding.set(item, team);
                KeyReturnSystem.bind(item, player.serverLevel(), hold.pos, spawnEntity);
            }
            if (team.isBlank()) {
                var membership = server.getScoreboard().getPlayersTeam(player.getScoreboardName());
                team = membership == null ? "" : membership.getName();
            }
            if (!player.getInventory().add(item)) {
                player.drop(item, false);
            }
            player.playNotifySound(ModSounds.KEY_COLLECT, SoundSource.MASTER, 0.9F, 1.0F);
            player.serverLevel().playSound(null, hold.pos, ModSounds.KEY_HOLOGRAM_WHOOSH, SoundSource.BLOCKS, 0.55F, 1.0F);
            HudFeedSystem.keyCollected(server, player.getGameProfile().getName(), block.color, team);
            var visualAnnouncement = new KeyAnnouncementPayload(player.getUUID(), player.getGameProfile().getName(), block.color.id);
            for (ServerPlayer recipient : server.getPlayerList().getPlayers()) {
                if (HudFeedSystem.canView(recipient, team)) {
                    ServerPlayNetworking.send(recipient, visualAnnouncement);
                    recipient.playNotifySound(ModSounds.KEY_GLOBAL_CONFIRMATION, SoundSource.MASTER, 0.6F, 1.0F);
                }
            }
            ServerPlayNetworking.send(player, new KeyPickupPayload(block.color.id));
        }
    }

    private static boolean valid(ServerPlayer player, BlockPos pos) {
        if (player.isSpectator() || !player.isAlive() || !player.serverLevel().hasChunkAt(pos)) {
            return false;
        }
        BlockState state = player.serverLevel().getBlockState(pos);
        if (!(state.getBlock() instanceof KeySpawnBlock) || state.getValue(KeySpawnBlock.COLLECTED)) {
            return false;
        }
        if (player.serverLevel().getBlockEntity(pos) instanceof KeySpawnBlockEntity spawner
            && !TeamBinding.canInteract(player, spawner.ownerTeam())) {
            return false;
        }
        Vec3 eye = player.getEyePosition();
        Vec3 center = Vec3.atCenterOf(pos);
        if (eye.distanceToSqr(center) > 25.0D) {
            return false;
        }
        Vec3 direction = player.getLookAngle();
        BlockHitResult hit = player.serverLevel().clip(new ClipContext(eye, eye.add(direction.scale(5.5D)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        return hit.getBlockPos().equals(pos);
    }

    private static final class Hold {
        private final BlockPos pos;
        private final ServerLevel dimension;
        private final long start;
        private long heartbeat;

        private Hold(BlockPos pos, ServerLevel dimension, long start, long heartbeat) {
            this.pos = pos;
            this.dimension = dimension;
            this.start = start;
            this.heartbeat = heartbeat;
        }
    }
}
