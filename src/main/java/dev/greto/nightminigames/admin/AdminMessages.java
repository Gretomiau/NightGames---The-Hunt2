package dev.greto.nightminigames.admin;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class AdminMessages {
    private AdminMessages() {
    }

    public static void broadcast(MinecraftServer server, Component text) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            send(player, text);
        }
    }

    public static void send(Player target, Component text) {
        if (target instanceof ServerPlayer operator && operator.hasPermissions(2)) {
            operator.sendSystemMessage(text);
        }
    }
}
