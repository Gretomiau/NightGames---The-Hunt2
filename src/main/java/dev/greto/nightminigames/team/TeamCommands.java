package dev.greto.nightminigames.team;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.world.level.GameType;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.greto.nightminigames.key.KeyMessages;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import dev.greto.nightminigames.persistence.WorldStructureJson;

import java.util.Locale;

public final class TeamCommands {
    private static final String[] SEVEN_TEAMS = {"rojo", "azul", "verde", "amarillo", "naranja", "morado", "rosa"};

    private TeamCommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        var team = Commands.argument("equipo", StringArgumentType.word())
            .suggests((context, builder) -> SharedSuggestionProvider.suggest(context.getSource().getServer().getScoreboard().getTeamNames(), builder));
        var create = Commands.literal("crear")
            .then(Commands.argument("nombre", StringArgumentType.word())
                .executes(context -> create(context.getSource(), StringArgumentType.getString(context, "nombre"), ""))
                .then(Commands.argument("color", StringArgumentType.word())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(new String[]{"rojo", "azul", "verde", "amarillo", "naranja", "morado", "rosa", "cian", "blanco", "gris"}, builder))
                    .executes(context -> create(context.getSource(), StringArgumentType.getString(context, "nombre"), StringArgumentType.getString(context, "color")))));
        return Commands.literal("equipos")
            .then(Commands.literal("preparar").executes(context -> prepare(context.getSource())))
            .then(create)
            .then(Commands.literal("asignar")
                .then(Commands.argument("jugador", EntityArgument.player())
                    .then(team.executes(context -> assign(context.getSource(), EntityArgument.getPlayer(context, "jugador"), StringArgumentType.getString(context, "equipo"))))))
            .then(Commands.literal("quitar")
                .then(Commands.argument("jugador", StringArgumentType.word())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(members(context.getSource().getServer().getScoreboard()), builder))
                    .executes(context -> remove(context.getSource(), StringArgumentType.getString(context, "jugador")))))
            .then(Commands.literal("desasignar")
                .then(Commands.argument("jugador", StringArgumentType.word())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(members(context.getSource().getServer().getScoreboard()), builder))
                    .executes(context -> remove(context.getSource(), StringArgumentType.getString(context, "jugador")))))
            .then(Commands.literal("vaciar")
                .then(Commands.literal("todos").executes(context -> clearAll(context.getSource())))
                .then(Commands.argument("equipo", StringArgumentType.word())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(SEVEN_TEAMS, builder))
                    .executes(context -> clearTeam(context.getSource(), StringArgumentType.getString(context, "equipo")))))
            .then(Commands.literal("eliminar")
                .then(Commands.argument("equipo", StringArgumentType.word())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(context.getSource().getServer().getScoreboard().getTeamNames(), builder))
                    .executes(context -> delete(context.getSource(), StringArgumentType.getString(context, "equipo")))))
            .then(Commands.literal("autoasignar").executes(context -> autoAssign(context.getSource())))
            .then(Commands.literal("cupos")
                .then(Commands.literal("estado").executes(context -> capacities(context.getSource())))
                .then(Commands.literal("todos")
                    .then(Commands.argument("cantidad", IntegerArgumentType.integer(1, 1000))
                        .executes(context -> setAllCapacities(context.getSource(), IntegerArgumentType.getInteger(context, "cantidad")))))
                .then(Commands.argument("equipo", StringArgumentType.word())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(SEVEN_TEAMS, builder))
                    .then(Commands.argument("cantidad", IntegerArgumentType.integer(1, 1000))
                        .executes(context -> setCapacity(context.getSource(), StringArgumentType.getString(context, "equipo"), IntegerArgumentType.getInteger(context, "cantidad"))))))
            .then(Commands.literal("lista").executes(context -> list(context.getSource())))
            .then(Commands.literal("estado")
                .executes(context -> capacities(context.getSource()))
                .then(Commands.argument("jugador", StringArgumentType.word())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(members(context.getSource().getServer().getScoreboard()), builder))
                    .executes(context -> status(context.getSource(), StringArgumentType.getString(context, "jugador")))));
    }

    private static int create(CommandSourceStack source, String name, String colorName) {
        if (!name.matches("[a-z0-9_-]{1,16}")) {
            source.sendFailure(KeyMessages.message("Usa un ID de 1 a 16 caracteres: minúsculas, números, _ o -.", ChatFormatting.RED));
            return 0;
        }
        if (!isNightTeam(name)) {
            source.sendFailure(KeyMessages.message("Nightminigames utiliza 7 equipos: rojo, azul, verde, amarillo, naranja, morado y rosa.", ChatFormatting.RED));
            return 0;
        }
        Scoreboard scoreboard = source.getServer().getScoreboard();
        if (scoreboard.getPlayerTeam(name) != null) {
            source.sendFailure(KeyMessages.message("Ya existe el equipo " + name + ".", ChatFormatting.RED));
            return 0;
        }
        ChatFormatting color = colorName.isBlank() ? inferColor(name) : inferColor(colorName);
        PlayerTeam result = scoreboard.addPlayerTeam(name);
        result.setColor(color);
        result.setDisplayName(Component.literal(name.toUpperCase(Locale.ROOT)).withStyle(color, ChatFormatting.BOLD));
        result.setAllowFriendlyFire(false);
        source.sendSuccess(() -> KeyMessages.message("Equipo creado: " + name + " (" + color.getName() + ").", color), false);
        return 1;
    }

    private static ChatFormatting inferColor(String name) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "rojo", "red" -> ChatFormatting.RED;
            case "azul", "blue" -> ChatFormatting.BLUE;
            case "verde", "green" -> ChatFormatting.GREEN;
            case "amarillo", "yellow" -> ChatFormatting.YELLOW;
            case "naranja", "orange", "dorado" -> ChatFormatting.GOLD;
            case "morado", "purple" -> ChatFormatting.DARK_PURPLE;
            case "rosa", "pink" -> ChatFormatting.LIGHT_PURPLE;
            case "cian", "cyan" -> ChatFormatting.AQUA;
            case "blanco", "white" -> ChatFormatting.WHITE;
            case "gris", "gray" -> ChatFormatting.GRAY;
            default -> ChatFormatting.AQUA;
        };
    }

    private static int assign(CommandSourceStack source, ServerPlayer player, String name) {
        if (!isNightTeam(name)) {
            source.sendFailure(KeyMessages.message("Solo los siete equipos de Nightminigames son válidos.", ChatFormatting.RED));
            return 0;
        }
        Scoreboard scoreboard = source.getServer().getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam(name);
        if (team == null) {
            source.sendFailure(KeyMessages.message("No existe el equipo " + name + ".", ChatFormatting.RED));
            return 0;
        }
        if (player.gameMode.getGameModeForPlayer() != GameType.SURVIVAL && player.gameMode.getGameModeForPlayer() != GameType.ADVENTURE) {
            source.sendFailure(KeyMessages.message("Solo se asignan jugadores en Survival o Adventure.", ChatFormatting.YELLOW));
            return 0;
        }
        PlayerTeam existing = scoreboard.getPlayersTeam(player.getScoreboardName());
        if ((existing == null || !existing.getName().equals(name)) && !TeamCapacitySystem.hasSpace(team)) {
            source.sendFailure(KeyMessages.message("Equipo " + name + " completo: " + team.getPlayers().size() + "/" + TeamCapacitySystem.capacity(name), ChatFormatting.RED));
            return 0;
        }
        PlayerTeam previous = scoreboard.getPlayersTeam(player.getScoreboardName());
        if (previous != null && !previous.getName().equals(name)) {
            dev.greto.nightminigames.machine.MachineCompletionSync.send(player, previous.getName(), false);
        }
        scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
        WorldStructureJson.markRosterChanged(source.getServer());
        dev.greto.nightminigames.machine.TeamExitCompletionSystem.syncPlayer(player);
        dev.greto.nightminigames.feed.HudFeedSystem.syncPlayer(player);
        dev.greto.nightminigames.integration.MapWaypointBridge.refreshGroups(source.getServer());
        source.sendSuccess(() -> KeyMessages.message(player.getGameProfile().getName() + " asignado a " + name + ".", team.getColor()), false);
        return 1;
    }

    private static Set<String> members(Scoreboard scoreboard) {
        Set<String> names = new LinkedHashSet<>();
        for (String name : SEVEN_TEAMS) {
            PlayerTeam team = scoreboard.getPlayerTeam(name);
            if (team != null) {
                names.addAll(team.getPlayers());
            }
        }
        return names;
    }

    private static String actualName(Scoreboard scoreboard, String requested) {
        for (String member : members(scoreboard)) {
            if (member.equalsIgnoreCase(requested)) {
                return member;
            }
        }
        return requested;
    }

    private static int remove(CommandSourceStack source, String requested) {
        Scoreboard scoreboard = source.getServer().getScoreboard();
        String name = actualName(scoreboard, requested);
        PlayerTeam previous = scoreboard.getPlayersTeam(name);
        if (previous == null || !isNightTeam(previous.getName())) {
            source.sendFailure(KeyMessages.message("El jugador " + requested + " no pertenece a un equipo de Nightminigames.", ChatFormatting.YELLOW));
            return 0;
        }
        ServerPlayer online = source.getServer().getPlayerList().getPlayerByName(name);
        if (online != null) {
            dev.greto.nightminigames.machine.MachineCompletionSync.send(online, previous.getName(), false);
        }
        scoreboard.removePlayerFromTeam(name);
        if (online != null) {
            dev.greto.nightminigames.feed.HudFeedSystem.syncPlayer(online);
        }
        dev.greto.nightminigames.integration.MapWaypointBridge.refreshGroups(source.getServer());
        WorldStructureJson.markRosterChanged(source.getServer());
        source.sendSuccess(() -> KeyMessages.message("Jugador " + name + " desasignado de " + previous.getName()
            + (online == null ? " (desconectado)." : "."), ChatFormatting.GREEN), false);
        return 1;
    }

    private static int clearTeam(CommandSourceStack source, String name) {
        if (!isNightTeam(name)) {
            source.sendFailure(KeyMessages.message("Equipo inválido: " + name, ChatFormatting.RED));
            return 0;
        }
        Scoreboard scoreboard = source.getServer().getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam(name);
        if (team == null) {
            source.sendFailure(KeyMessages.message("El equipo " + name + " no existe.", ChatFormatting.RED));
            return 0;
        }
        int removed = clearMembers(source, scoreboard, team);
        WorldStructureJson.markRosterChanged(source.getServer());
        dev.greto.nightminigames.feed.HudFeedSystem.syncOnline(source.getServer());
        dev.greto.nightminigames.integration.MapWaypointBridge.refreshGroups(source.getServer());
        source.sendSuccess(() -> KeyMessages.message("Equipo " + name + " vaciado: " + removed
            + " jugadores retirados, incluidos los desconectados.", ChatFormatting.GREEN), false);
        return 1;
    }

    private static int clearAll(CommandSourceStack source) {
        Scoreboard scoreboard = source.getServer().getScoreboard();
        int removed = 0;
        for (String name : SEVEN_TEAMS) {
            PlayerTeam team = scoreboard.getPlayerTeam(name);
            if (team != null) {
                removed += clearMembers(source, scoreboard, team);
            }
        }
        WorldStructureJson.markRosterChanged(source.getServer());
        dev.greto.nightminigames.feed.HudFeedSystem.syncOnline(source.getServer());
        dev.greto.nightminigames.integration.MapWaypointBridge.refreshGroups(source.getServer());
        int total = removed;
        source.sendSuccess(() -> KeyMessages.message("Los 7 equipos han sido vaciados: " + total
            + " jugadores desasignados, incluidos los desconectados. Los equipos y configuraciones se conservan.", ChatFormatting.GREEN), false);
        return 1;
    }

    private static int clearMembers(CommandSourceStack source, Scoreboard scoreboard, PlayerTeam team) {
        int removed = 0;
        for (String member : new ArrayList<>(team.getPlayers())) {
            ServerPlayer online = source.getServer().getPlayerList().getPlayerByName(member);
            if (online != null) {
                dev.greto.nightminigames.machine.MachineCompletionSync.send(online, team.getName(), false);
            }
            scoreboard.removePlayerFromTeam(member);
            removed++;
        }
        return removed;
    }

    private static int delete(CommandSourceStack source, String name) {
        if (!isNightTeam(name)) {
            source.sendFailure(KeyMessages.message("Solo los siete equipos de Nightminigames son válidos.", ChatFormatting.RED));
            return 0;
        }
        Scoreboard scoreboard = source.getServer().getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam(name);
        if (team == null) {
            source.sendFailure(KeyMessages.message("No existe el equipo " + name + ".", ChatFormatting.RED));
            return 0;
        }
        scoreboard.removePlayerTeam(team);
        WorldStructureJson.markRosterChanged(source.getServer());
        dev.greto.nightminigames.feed.HudFeedSystem.syncOnline(source.getServer());
        dev.greto.nightminigames.integration.MapWaypointBridge.refreshGroups(source.getServer());
        source.sendSuccess(() -> KeyMessages.message("Equipo eliminado: " + name + ".", ChatFormatting.GREEN), false);
        return 1;
    }

    private static int prepare(CommandSourceStack source) {
        Scoreboard scoreboard = source.getServer().getScoreboard();
        int created = 0;
        for (String name : SEVEN_TEAMS) {
            if (scoreboard.getPlayerTeam(name) != null) {
                continue;
            }
            PlayerTeam team = scoreboard.addPlayerTeam(name);
            ChatFormatting color = inferColor(name);
            team.setColor(color);
            team.setDisplayName(Component.literal(name.toUpperCase(Locale.ROOT)).withStyle(color, ChatFormatting.BOLD));
            team.setAllowFriendlyFire(false);
            created++;
        }
        int total = created;
        source.sendSuccess(() -> KeyMessages.message("Siete equipos preparados: " + total + " nuevos, " + (7 - total) + " existentes.", ChatFormatting.GREEN), false);
        return 1;
    }

    private static boolean isNightTeam(String name) {
        for (String team : SEVEN_TEAMS) {
            if (team.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static int autoAssign(CommandSourceStack source) {
        prepare(source);
        Scoreboard scoreboard = source.getServer().getScoreboard();
        List<ServerPlayer> candidates = new ArrayList<>();
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            GameType mode = player.gameMode.getGameModeForPlayer();
            if ((mode == GameType.SURVIVAL || mode == GameType.ADVENTURE)
                && scoreboard.getPlayersTeam(player.getScoreboardName()) == null) {
                candidates.add(player);
            }
        }
        Collections.shuffle(candidates);
        int assigned = 0;
        for (ServerPlayer player : candidates) {
            int minimum = Integer.MAX_VALUE;
            List<PlayerTeam> choices = new ArrayList<>();
            for (String name : SEVEN_TEAMS) {
                PlayerTeam team = scoreboard.getPlayerTeam(name);
                if (!TeamCapacitySystem.hasSpace(team)) {
                    continue;
                }
                int size = team.getPlayers().size();
                if (size < minimum) {
                    minimum = size;
                    choices.clear();
                    choices.add(team);
                } else if (size == minimum) {
                    choices.add(team);
                }
            }
            if (choices.isEmpty()) {
                break;
            }
            PlayerTeam chosen = choices.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(choices.size()));
            scoreboard.addPlayerToTeam(player.getScoreboardName(), chosen);
            dev.greto.nightminigames.machine.TeamExitCompletionSystem.syncPlayer(player);
            assigned++;
        }
        if (assigned > 0) {
            WorldStructureJson.markRosterChanged(source.getServer());
            dev.greto.nightminigames.feed.HudFeedSystem.syncOnline(source.getServer());
            dev.greto.nightminigames.integration.MapWaypointBridge.refreshGroups(source.getServer());
        }
        int total = assigned;
        int unassigned = candidates.size() - assigned;
        source.sendSuccess(() -> KeyMessages.message("Asignación aleatoria equilibrada: " + total + " asignados, " + unassigned + " sin cupo.", unassigned == 0 ? ChatFormatting.GREEN : ChatFormatting.YELLOW), false);
        capacities(source);
        return assigned;
    }

    private static int setCapacity(CommandSourceStack source, String name, int amount) {
        if (!isNightTeam(name)) {
            source.sendFailure(KeyMessages.message("Equipo inválido: " + name, ChatFormatting.RED));
            return 0;
        }
        PlayerTeam team = source.getServer().getScoreboard().getPlayerTeam(name);
        int occupancy = team == null ? 0 : team.getPlayers().size();
        if (amount < occupancy) {
            source.sendFailure(KeyMessages.message("El equipo ya tiene " + occupancy + " miembros. No puedes reducir el cupo por debajo.", ChatFormatting.RED));
            return 0;
        }
        TeamCapacitySystem.set(source.getServer(), name, amount);
        source.sendSuccess(() -> KeyMessages.message("Cupo de " + name + " configurado: " + occupancy + "/" + amount, ChatFormatting.GREEN), false);
        return 1;
    }

    private static int setAllCapacities(CommandSourceStack source, int amount) {
        Scoreboard scoreboard = source.getServer().getScoreboard();
        for (String name : SEVEN_TEAMS) {
            PlayerTeam team = scoreboard.getPlayerTeam(name);
            if (team != null && team.getPlayers().size() > amount) {
                source.sendFailure(KeyMessages.message("No puedo bajar el cupo: " + name + " tiene " + team.getPlayers().size() + " miembros.", ChatFormatting.RED));
                return 0;
            }
        }
        TeamCapacitySystem.setAll(source.getServer(), java.util.Arrays.asList(SEVEN_TEAMS), amount);
        source.sendSuccess(() -> KeyMessages.message("Cupo configurado para los 7 equipos: " + amount, ChatFormatting.GREEN), false);
        return 1;
    }

    private static int capacities(CommandSourceStack source) {
        Scoreboard scoreboard = source.getServer().getScoreboard();
        int occupied = 0;
        int capacity = 0;
        for (String name : SEVEN_TEAMS) {
            PlayerTeam team = scoreboard.getPlayerTeam(name);
            int total = team == null ? 0 : team.getPlayers().size();
            int connected = 0;
            if (team != null) {
                for (String playerName : team.getPlayers()) {
                    if (source.getServer().getPlayerList().getPlayerByName(playerName) != null) {
                        connected++;
                    }
                }
            }
            int limit = TeamCapacitySystem.capacity(name);
            occupied += total;
            capacity += limit;
            int online = connected;
            source.sendSuccess(() -> KeyMessages.message(name.toUpperCase(Locale.ROOT) + "  " + total + "/" + limit + "  · disponibles " + Math.max(0, limit - total) + "  · conectados " + online, team == null ? ChatFormatting.GRAY : team.getColor()), false);
        }
        int used = occupied;
        int totalCap = capacity;
        source.sendSuccess(() -> KeyMessages.message("TOTAL  " + used + "/" + totalCap + " · libres " + (totalCap - used), ChatFormatting.AQUA), false);
        return occupied;
    }

    private static int list(CommandSourceStack source) {
        Scoreboard scoreboard = source.getServer().getScoreboard();
        int found = 0;
        for (String name : SEVEN_TEAMS) {
            PlayerTeam team = scoreboard.getPlayerTeam(name);
            if (team == null) {
                continue;
            }
            found++;
            int size = team.getPlayers().size();
            int limit = TeamCapacitySystem.capacity(name);
            source.sendSuccess(() -> KeyMessages.message(name.toUpperCase(Locale.ROOT) + " · " + size + "/" + limit + " jugadores", team.getColor()), false);
        }
        if (found == 0) {
            source.sendSuccess(() -> KeyMessages.message("Todavía no se han preparado los siete equipos.", ChatFormatting.YELLOW), false);
        }
        return found;
    }

    private static int status(CommandSourceStack source, String requested) {
        Scoreboard scoreboard = source.getServer().getScoreboard();
        String name = actualName(scoreboard, requested);
        PlayerTeam team = scoreboard.getPlayersTeam(name);
        if (team == null || !isNightTeam(team.getName())) {
            source.sendSuccess(() -> KeyMessages.message(name + " no tiene equipo de Nightminigames.", ChatFormatting.YELLOW), false);
            return 0;
        }
        boolean connected = source.getServer().getPlayerList().getPlayerByName(name) != null;
        source.sendSuccess(() -> KeyMessages.message(name + " pertenece a " + team.getName()
            + (connected ? " (conectado)." : " (desconectado)."), team.getColor()), false);
        return 1;
    }
}
