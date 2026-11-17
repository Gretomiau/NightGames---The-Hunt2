package dev.greto.nightminigames.points;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.greto.nightminigames.key.KeyMessages;
import dev.greto.nightminigames.machine.TeamExitCompletionSystem;
import dev.greto.nightminigames.module.ModuleSystem;
import dev.greto.nightminigames.team.TeamBinding;
import dev.greto.nightminigames.team.TeamCapacitySystem;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.PlayerTeam;

public final class AdminReportCommands {
    private AdminReportCommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> commands() {
        return Commands.literal("op")
            .then(Commands.literal("informe").executes(context -> report(context.getSource())))
            .then(Commands.literal("estado").executes(context -> report(context.getSource())));
    }

    private static int report(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        int total = 0;
        int eligible = 0;
        int spectators = 0;
        int creative = 0;
        int offline = 0;
        int granted = 0;
        source.sendSuccess(() -> KeyMessages.message("INFORME OPERADOR · NIGHTMINIGAMES · RONDA " + TeamExitCompletionSystem.roundId(), ChatFormatting.AQUA), false);
        for (String id : ModuleSystem.TEAMS) {
            PlayerTeam team = server.getScoreboard().getPlayerTeam(id);
            int count = team == null ? 0 : team.getPlayers().size();
            int live = 0;
            int dead = 0;
            int builders = 0;
            int absent = 0;
            if (team != null) {
                for (String name : team.getPlayers()) {
                    ServerPlayer player = server.getPlayerList().getPlayerByName(name);
                    if (player == null) {
                        absent++;
                    } else if (player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
                        dead++;
                    } else if (player.gameMode.getGameModeForPlayer() == GameType.CREATIVE) {
                        builders++;
                    } else if (TeamExitCompletionSystem.eligible(player)) {
                        live++;
                    }
                }
            }
            total += count;
            eligible += live;
            offline += absent;
            spectators += dead;
            creative += builders;
            boolean qualified = TeamExitCompletionSystem.isQualified(id);
            Integer paid = TeamExitCompletionSystem.awardedAmount(id);
            if (paid != null) {
                granted++;
            }
            String line = id.toUpperCase() + " | " + count + "/" + TeamCapacitySystem.capacity(id)
                + " | activos " + live + " | fuera " + absent + " | espect. " + dead
                + " | clasif. " + (qualified ? "SÍ" : "NO")
                + " | puntos " + (paid == null ? "PENDIENTES" : paid < 0 ? "PAGO ANTERIOR" : paid + " c/u");
            source.sendSuccess(() -> KeyMessages.message(line, TeamBinding.color(id)), false);
        }
        int all = total;
        int ok = eligible;
        int lost = offline;
        int spec = spectators;
        int builder = creative;
        int paidTeams = granted;
        source.sendSuccess(() -> KeyMessages.message("PARTICIPANTES " + all + " · activos " + ok + " · desconectados " + lost
            + " · espectadores " + spec + " · creativos " + builder, ChatFormatting.GRAY), false);
        source.sendSuccess(() -> KeyMessages.message("CLASIFICACIÓN " + TeamExitCompletionSystem.qualifiedCount() + "/"
            + TeamExitCompletionSystem.qualifyingSlots() + " equipos · pagos manuales confirmados " + paidTeams
            + " · MystrixPoints " + (PointsAwardCommands.available() ? "CONECTADO" : "SIN INSTALAR"), ChatFormatting.AQUA), false);
        source.sendSuccess(() -> KeyMessages.message("Más información: /nightminigames inicio diagnostico | clasificacion estado | puntos diagnostico", ChatFormatting.YELLOW), false);
        return 1;
    }
}
