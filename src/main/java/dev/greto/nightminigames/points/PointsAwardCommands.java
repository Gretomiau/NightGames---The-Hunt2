package dev.greto.nightminigames.points;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.greto.nightminigames.key.KeyMessages;
import dev.greto.nightminigames.machine.TeamExitCompletionSystem;
import dev.greto.nightminigames.module.ModuleSystem;
import dev.greto.nightminigames.team.TeamBinding;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.PlayerTeam;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionException;

public final class PointsAwardCommands {
    private static final Set<String> PENDING = new java.util.HashSet<>();
    private static MinecraftServer currentServer;

    private PointsAwardCommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> commands() {
        return Commands.literal("puntos")
            .then(Commands.literal("estado").executes(context -> overview(context.getSource())))
            .then(Commands.literal("lista").executes(context -> overview(context.getSource())))
            .then(Commands.literal("diagnostico").executes(context -> diagnostic(context.getSource())))
            .then(Commands.literal("equipo")
                .then(Commands.argument("equipo", StringArgumentType.word())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(ModuleSystem.TEAMS, builder))
                    .then(Commands.argument("cantidad", IntegerArgumentType.integer(1, 10000))
                        .executes(context -> grant(context.getSource(), StringArgumentType.getString(context, "equipo"), IntegerArgumentType.getInteger(context, "cantidad"))))));
    }

    public static boolean available() {
        return FabricLoader.getInstance().isModLoaded("mystrixpoints");
    }

    private static void info(CommandSourceStack source, String message, ChatFormatting color) {
        source.sendSuccess(() -> KeyMessages.message(message, color), false);
    }

    private static int overview(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        info(source, "PUNTOS MANUALES | MystrixPoints: " + (available() ? "CONECTADO" : "NO INSTALADO")
            + " | Ronda: " + TeamExitCompletionSystem.roundId(), ChatFormatting.AQUA);
        info(source, "Otorgar con: /nightminigames puntos equipo <equipo> <cantidad>", ChatFormatting.GRAY);
        for (String name : ModuleSystem.TEAMS) {
            Map<UUID, String> recipients = resolvedRecipients(server, name);
            Integer awarded = TeamExitCompletionSystem.awardedAmount(name);
            String state = awarded != null ? awarded < 0 ? "ENTREGA PREVIA, IMPORTE DESCONOCIDO" : "ENTREGADOS: " + awarded + " POR JUGADOR"
                : "PENDIENTE DE PAGO";
            info(source, name.toUpperCase() + " | " + state + " | " + recipients.size() + " destinatarios", TeamBinding.color(name));
        }
        return 1;
    }

    private static int diagnostic(CommandSourceStack source) {
        overview(source);
        for (String team : ModuleSystem.TEAMS) {
            detail(source, team);
        }
        return 1;
    }

    private static int detail(CommandSourceStack source, String team) {
        if (!TeamBinding.valid(team)) {
            source.sendFailure(KeyMessages.message("Equipo inválido.", ChatFormatting.RED));
            return 0;
        }
        PlayerTeam scoreboardTeam = source.getServer().getScoreboard().getPlayerTeam(team);
        int total = scoreboardTeam == null ? 0 : scoreboardTeam.getPlayers().size();
        int eligible = 0;
        int spectators = 0;
        int offline = 0;
        int creative = 0;
        if (scoreboardTeam != null) {
            for (String name : scoreboardTeam.getPlayers()) {
                ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(name);
                if (player == null) {
                    offline++;
                } else if (player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
                    spectators++;
                } else if (player.gameMode.getGameModeForPlayer() == GameType.CREATIVE) {
                    creative++;
                } else if (TeamExitCompletionSystem.eligible(player)) {
                    eligible++;
                }
            }
        }
        Map<UUID, String> recipients = resolvedRecipients(source.getServer(), team);
        Integer awarded = TeamExitCompletionSystem.awardedAmount(team);
        info(source, team.toUpperCase() + " | total: " + total + " | activos: " + eligible
            + " | desconectados: " + offline + " | espectadores: " + spectators + " | creativos: " + creative,
            TeamBinding.color(team));
        info(source, "Destinatarios actuales: " + recipients.size() + " | puntos: " + (awarded == null ? "SIN ENTREGAR" : awarded < 0 ? "ENTREGA PREVIA, IMPORTE NO CONFIRMADO" : awarded + " c/u"),
            awarded != null ? ChatFormatting.GREEN : ChatFormatting.YELLOW);
        if (!recipients.isEmpty()) {
            info(source, "Integrantes del equipo: " + String.join(", ", recipients.values()), ChatFormatting.GRAY);
        }
        return 1;
    }



    private static boolean ready(CommandSourceStack source, String team) {
        if (!TeamBinding.valid(team)) {
            source.sendFailure(KeyMessages.message("Equipo inválido.", ChatFormatting.RED));
            return false;
        }
        if (resolvedRecipients(source.getServer(), team).isEmpty()) {
            source.sendFailure(KeyMessages.message("No hay UUID identificables para este equipo. Conecta a los participantes o comprueba la plantilla antes de entregar.", ChatFormatting.RED));
            return false;
        }
        if (TeamExitCompletionSystem.awardedAmount(team) != null) {
            source.sendFailure(KeyMessages.message("El equipo ya recibió puntos en esta ronda. No se repetirá el pago.", ChatFormatting.YELLOW));
            return false;
        }
        return true;
    }

    private static int grant(CommandSourceStack source, String team, int amount) {
        if (!ready(source, team)) {
            return 0;
        }
        if (!available()) {
            source.sendFailure(KeyMessages.message("MystrixPoints no está instalado. Compila su proyecto y coloca el JAR en el servidor.", ChatFormatting.RED));
            return 0;
        }
        MinecraftServer server = source.getServer();
        if (server != currentServer) {
            currentServer = server;
            PENDING.clear();
        }
        UUID requestRoundId = TeamExitCompletionSystem.roundId();
        String eventId = "team-award:" + requestRoundId + ":" + team;
        if (!PENDING.add(eventId)) {
            source.sendFailure(KeyMessages.message("La entrega de puntos de este equipo sigue en proceso.", ChatFormatting.YELLOW));
            return 0;
        }
        Map<UUID, Long> awards = new LinkedHashMap<>();
        resolvedRecipients(server, team).keySet().forEach(id -> awards.put(id, (long) amount));
        try {
            MystrixPointsBridge.grant(eventId, awards).whenComplete((result, error) -> server.execute(() -> {
                PENDING.remove(eventId);
                if (error != null) {
                    Throwable actual = error;
                    while (actual instanceof CompletionException && actual.getCause() != null) {
                        actual = actual.getCause();
                    }
                    notifyOperators(server, "ERROR en MystrixPoints para " + team + ": " + actual.getMessage(), ChatFormatting.RED);
                    return;
                }
                if (!TeamExitCompletionSystem.roundId().equals(requestRoundId)) {
                    notifyOperators(server, "La entrega de " + team + " se procesó para una ronda anterior.", ChatFormatting.YELLOW);
                    return;
                }
                TeamExitCompletionSystem.markAwarded(server, team, result.applied() ? amount : -1);
                if (result.applied()) {
                    notifyOperators(server, "PUNTOS ENTREGADOS · " + team.toUpperCase() + " · " + result.recipients()
                        + " jugadores × " + amount + " = " + (long) result.recipients() * amount + " puntos.", ChatFormatting.GREEN);
                } else {
                    notifyOperators(server, "MystrixPoints confirmó que la entrega de " + team + " ya se procesó. No se duplicaron puntos. El importe anterior no puede verificarse desde este evento.", ChatFormatting.YELLOW);
                }
            }));
            info(source, "Solicitud de " + team + " enviada a MystrixPoints. Esperando confirmación de la base de datos.", ChatFormatting.AQUA);
            return awards.size();
        } catch (RuntimeException exception) {
            PENDING.remove(eventId);
            source.sendFailure(KeyMessages.message("No se pudo enviar el pago: " + exception.getMessage(), ChatFormatting.RED));
            return 0;
        }
    }

    private static Map<UUID, String> resolvedRecipients(MinecraftServer server, String team) {
        Map<UUID, String> players = new LinkedHashMap<>();
        PlayerTeam scoreboardTeam = server.getScoreboard().getPlayerTeam(team);
        if (scoreboardTeam == null) {
            return players;
        }
        for (String name : scoreboardTeam.getPlayers()) {
            ServerPlayer online = server.getPlayerList().getPlayerByName(name);
            if (online != null) {
                players.put(online.getUUID(), name);
            } else {
                server.getProfileCache().get(name).ifPresent(profile -> players.put(profile.getId(), name));
            }
        }
        return players;
    }

    private static void notifyOperators(MinecraftServer server, String message, ChatFormatting color) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.hasPermissions(2)) {
                player.sendSystemMessage(KeyMessages.message(message, color));
            }
        }
    }
}
