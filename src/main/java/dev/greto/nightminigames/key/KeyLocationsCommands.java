package dev.greto.nightminigames.key;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.greto.nightminigames.integration.MapWaypointBridge;
import dev.greto.nightminigames.module.ModuleSystem;
import dev.greto.nightminigames.persistence.WorldStructureJson;
import dev.greto.nightminigames.team.TeamBinding;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;

import java.util.List;

public final class KeyLocationsCommands {
    private KeyLocationsCommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("ubicaciones")
            .then(Commands.literal("todos").executes(context -> all(context.getSource())))
            .then(Commands.argument("equipo", StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(ModuleSystem.TEAMS, builder))
                .executes(context -> report(context.getSource(), StringArgumentType.getString(context, "equipo"))));
    }

    private static int all(CommandSourceStack source) {
        int total = 0;
        for (String team : ModuleSystem.TEAMS) {
            total += report(source, team);
        }
        return total;
    }

    private static int report(CommandSourceStack source, String team) {
        if (!TeamBinding.valid(team)) {
            source.sendFailure(KeyMessages.message("Equipo inválido.", ChatFormatting.RED));
            return 0;
        }
        List<WorldStructureJson.KnownKey> keys = WorldStructureJson.knownKeys(source.getServer(), team);
        int pending = (int) keys.stream().filter(key -> !key.collected()).count();
        int collected = keys.size() - pending;
        line(source, team.toUpperCase() + " · LLAVES DISPONIBLES: " + pending + " · RECOGIDAS: " + collected + " · SPAWNERS: " + keys.size(), TeamBinding.color(team));
        for (WorldStructureJson.KnownKey key : keys) {
            line(source, "LLAVE " + key.color().toUpperCase() + " · " + (key.collected() ? "RECOGIDA" : "DISPONIBLE")
                + " · " + key.dimension() + " [" + key.pos().getX() + ", " + key.pos().getY() + ", " + key.pos().getZ() + "]",
                key.collected() ? ChatFormatting.GRAY : ChatFormatting.AQUA);
        }
        if (MapWaypointBridge.available()) {
            boolean published = MapWaypointBridge.publish(source.getServer(), team, keys);
            line(source, published ? "MapaMystrix: " + MapWaypointBridge.status(team) + ". Solo el equipo " + team + " recibe sus marcadores."
                    : "MapaMystrix: error al sincronizar. Consulta el log del servidor.",
                published ? ChatFormatting.GREEN : ChatFormatting.RED);
        } else {
            line(source, "MapaMystrix no está instalado. Se muestran las coordenadas conocidas sin modificar ningún mapa.", ChatFormatting.YELLOW);
        }
        return keys.size();
    }

    private static void line(CommandSourceStack source, String message, ChatFormatting color) {
        source.sendSuccess(() -> KeyMessages.message(message, color), false);
    }
}
