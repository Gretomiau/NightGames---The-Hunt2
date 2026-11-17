package dev.greto.nightminigames.key;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.greto.nightminigames.integration.MapWaypointBridge;
import dev.greto.nightminigames.module.ModuleSystem;
import dev.greto.nightminigames.persistence.WorldStructureJson;
import dev.greto.nightminigames.team.TeamBinding;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.*;

/** Checks placed spawners, not keys currently held by players. Never creates markers. */
public final class KeyConfigurationCommands {
    private KeyConfigurationCommands() {}
    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("comprobar")
            .executes(context -> all(context.getSource()))
            .then(Commands.literal("todos").executes(context -> all(context.getSource())))
            .then(Commands.argument("equipo", StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(ModuleSystem.TEAMS, builder))
                .executes(context -> report(context.getSource(), StringArgumentType.getString(context, "equipo"))));
    }
    public static LiteralArgumentBuilder<CommandSourceStack> previewCommands() {
        return Commands.literal("vista")
            .then(Commands.literal("todos").executes(context -> preview(context.getSource(), "todos")))
            .then(Commands.literal("ocultar").executes(context -> preview(context.getSource(), null)))
            .then(Commands.argument("equipo", StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(ModuleSystem.TEAMS, builder))
                .executes(context -> preview(context.getSource(), StringArgumentType.getString(context, "equipo"))));
    }

    private static int preview(CommandSourceStack source, String team) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = source.getPlayerOrException();
        if (team != null && !team.equals("todos") && !TeamBinding.valid(team)) {
            source.sendFailure(KeyMessages.message("Equipo inválido.", ChatFormatting.RED)); return 0;
        }
        if (!MapWaypointBridge.available()) {
            source.sendFailure(KeyMessages.message("Necesitas MapaMystrix para la vista de evaluación.", ChatFormatting.RED)); return 0;
        }
        if (team != null && !MapWaypointBridge.canPreview()) {
            source.sendFailure(KeyMessages.message("La evaluación se usa antes de iniciar. Detén la partida o reinicia la ronda finalizada para evaluar.", ChatFormatting.YELLOW)); return 0;
        }
        if (!MapWaypointBridge.preview(player, team)) {
            source.sendFailure(KeyMessages.message("No se pudo sincronizar la vista. Consulta el log del servidor.", ChatFormatting.RED)); return 0;
        }
        source.sendSuccess(() -> KeyMessages.message(team == null ? "Vista de evaluación ocultada." : "Vista de evaluación: " + team.toUpperCase()
            + ". Abre el mapa: solo tú ves todas las llaves colocadas y las máquinas habilitadas por OP, incluso con luces apagadas. Se retira al iniciar.", ChatFormatting.AQUA), false);
        return 1;
    }

    private static int all(CommandSourceStack source) {
        int complete = 0;
        for (String team : ModuleSystem.TEAMS) complete += report(source, team);
        int total = complete;
        source.sendSuccess(() -> KeyMessages.message("Equipos con los cuatro colores colocados: " + total + "/7. Los equipos vacíos no bloquean el inicio.", ChatFormatting.AQUA), false);
        return complete;
    }
    private static int report(CommandSourceStack source, String team) {
        if (!TeamBinding.valid(team)) {
            source.sendFailure(KeyMessages.message("Equipo inválido.", ChatFormatting.RED)); return 0;
        }
        var keys = WorldStructureJson.knownKeys(source.getServer(), team);
        int colors = 0;
        for (KeyColor color : KeyColor.values()) if (keys.stream().anyMatch(k -> k.color().equals(color.id))) colors++;
        String summary = team.toUpperCase() + " · " + colors + "/4 COLORES COLOCADOS · " + (colors == 4 ? "COMPLETO" : "FALTAN LLAVES");
        source.sendSuccess(() -> KeyMessages.message(summary, summary.contains("COMPLETO") ? ChatFormatting.GREEN : ChatFormatting.YELLOW), false);
        for (KeyColor color : KeyColor.values()) {
            var same = keys.stream().filter(k -> k.color().equals(color.id)).toList();
            String text = color.display.toUpperCase() + ": " + (same.isEmpty() ? "FALTA COLOCAR" : same.size() + " spawner(s), " + same.stream().filter(k -> !k.collected()).count() + " disponibles");
            source.sendSuccess(() -> KeyMessages.message(text, same.isEmpty() ? ChatFormatting.RED : color.formatting), false);
        }
        String state = MapWaypointBridge.status(team);
        source.sendSuccess(() -> KeyMessages.message("Waypoints: " + state + ". Comprobación de ubicaciones registradas; no confundir con llaves en inventario.", ChatFormatting.GRAY), false);
        return colors == 4 ? 1 : 0;
    }
}
