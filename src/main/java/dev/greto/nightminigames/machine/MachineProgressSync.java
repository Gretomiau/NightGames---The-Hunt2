package dev.greto.nightminigames.machine;

import dev.greto.nightminigames.key.KeyColor;
import dev.greto.nightminigames.feed.HudFeedSystem;
import dev.greto.nightminigames.registry.ModBlocks;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

public final class MachineProgressSync {
    private static boolean initialized;

    private MachineProgressSync() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        PayloadTypeRegistry.playS2C().register(MachineProgressPayload.TYPE, MachineProgressPayload.CODEC);
    }

    public static void update(ServerLevel level, BlockPos pos, String actor, int eventColor) {
        if (eventColor == -1) {
            return;
        }
        MachineProgressPayload payload = snapshot(level, pos, actor, eventColor);
        if (payload == null) {
            return;
        }
        String team = "";
        if (level.getBlockEntity(pos) instanceof MachineBlockEntity entity) {
            team = entity.getOwnerTeam();
        }
        if (team.isBlank()) {
            var actorPlayer = level.getServer().getPlayerList().getPlayerByName(actor);
            if (actorPlayer != null) {
                var membership = level.getServer().getScoreboard().getPlayersTeam(actorPlayer.getScoreboardName());
                team = membership == null ? "" : membership.getName();
            }
        }
        HudFeedSystem.machineUpdated(level.getServer(), payload, team);
        for (ServerPlayer receiver : level.getServer().getPlayerList().getPlayers()) {
            if (HudFeedSystem.canView(receiver, team)) {
                ServerPlayNetworking.send(receiver, payload);
            }
        }
    }

    public static void sendTo(ServerPlayer player, BlockPos pos) {
        sendTo(player, pos, -1);
    }

    public static void sendTo(ServerPlayer player, BlockPos pos, int noticeType) {
        MachineProgressPayload payload = snapshot(player.serverLevel(), pos, "", noticeType);
        if (payload != null) {
            ServerPlayNetworking.send(player, payload);
        }
    }



    private static MachineProgressPayload snapshot(ServerLevel level, BlockPos pos, String actor, int eventColor) {
        if (!level.hasChunkAt(pos)) {
            return null;
        }
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() != ModBlocks.MACHINE) {
            return null;
        }
        if (!(level.getBlockEntity(pos) instanceof MachineBlockEntity entity)) {
            return null;
        }
        return new MachineProgressPayload(
            level.dimension().location().toString(), pos.immutable(), state.getValue(MachineBlock.MASK),
            entity.getInserter(KeyColor.BLUE), entity.getInserter(KeyColor.YELLOW),
            entity.getInserter(KeyColor.RED), entity.getInserter(KeyColor.GREEN), actor, eventColor, false
        );
    }
}
