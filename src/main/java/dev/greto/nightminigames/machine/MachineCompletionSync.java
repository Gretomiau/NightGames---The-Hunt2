package dev.greto.nightminigames.machine;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;

public final class MachineCompletionSync {
    private static boolean initialized;

    private MachineCompletionSync() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        PayloadTypeRegistry.playS2C().register(MachineCompletionPayload.TYPE, MachineCompletionPayload.CODEC);
    }

    public static void announce(ServerLevel level, BlockPos pos, ServerPlayer player) {
        String name = "";
        if (level.getBlockEntity(pos) instanceof MachineBlockEntity entity) {
            name = entity.getOwnerTeam();
        }
        if (name.isBlank()) {
            PlayerTeam team = level.getServer().getScoreboard().getPlayersTeam(player.getScoreboardName());
            name = team == null ? "" : team.getName();
        }
        if (!name.isBlank()) {
            TeamExitCompletionSystem.begin(level, pos, name);
        } else {
            ServerPlayNetworking.send(player, new MachineCompletionPayload("", 0x57FFB3, true));
        }
    }

    public static void send(ServerPlayer player, String teamName, boolean visible) {
        PlayerTeam team = player.server.getScoreboard().getPlayerTeam(teamName);
        int color = team == null || team.getColor().getColor() == null ? 0x57FFB3 : team.getColor().getColor();
        ServerPlayNetworking.send(player, new MachineCompletionPayload(teamName, color, visible));
    }

    public static void reset(ServerLevel level, BlockPos pos) {
        TeamExitCompletionSystem.reset(level, pos);
    }
}
