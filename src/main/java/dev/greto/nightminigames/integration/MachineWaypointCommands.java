package dev.greto.nightminigames.integration;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.greto.nightminigames.key.KeyMessages;
import dev.greto.nightminigames.module.ModuleSystem;
import dev.greto.nightminigames.persistence.WorldStructureJson;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.*;

/** Operator-owned global switch, while each machine keeps its team audience. */
public final class MachineWaypointCommands {
    private MachineWaypointCommands() {}
    public static LiteralArgumentBuilder<CommandSourceStack> commands() {
        return Commands.literal("waypoints").requires(source -> source.hasPermission(2))
            .then(Commands.literal("maquina")
                .then(Commands.literal("activar").executes(context -> change(context.getSource(), true)))
                .then(Commands.literal("desactivar").executes(context -> change(context.getSource(), false)))
                .then(Commands.literal("estado").executes(context -> report(context.getSource()))));
    }
    private static int change(CommandSourceStack source, boolean enabled) {
        if (!source.hasPermission(2)) return 0;
        if (!MapWaypointBridge.available()) {
            source.sendFailure(KeyMessages.message("Necesitas MapaMystrix para los waypoints de máquinas.", ChatFormatting.RED)); return 0;
        }
        MapWaypointBridge.setMachineWaypoints(source.getServer(), enabled);
        if (!MapWaypointBridge.synchronize(source.getServer())) {
            source.sendFailure(KeyMessages.message("La opción se guardó, pero falló la sincronización con MapaMystrix. Consulta el log.", ChatFormatting.RED)); return 0;
        }
        source.sendSuccess(() -> KeyMessages.message("Waypoints de máquinas " + (enabled ? "HABILITADOS" : "DESACTIVADOS")
            + " para todos los equipos. Cada equipo ve solo sus máquinas durante su partida, según las luces. La opción queda guardada hasta que un OP la cambie.", ChatFormatting.GREEN), false);
        return report(source);
    }
    private static int report(CommandSourceStack source) {
        int total = 0;
        for (String team : ModuleSystem.TEAMS) {
            int machines = WorldStructureJson.knownMachines(source.getServer(), team).size(); total += machines;
            boolean enabled = MapWaypointBridge.machineWaypointsEnabled(team);
            String line = team.toUpperCase() + " · máquina: " + (enabled ? "HABILITADA" : "DESACTIVADA") + " · " + machines + " registrada(s) · "
                + (machines == 0 ? "FALTA COLOCAR O VINCULAR LA MÁQUINA" : MapWaypointBridge.status(team));
            source.sendSuccess(() -> KeyMessages.message(line, machines == 0 ? ChatFormatting.YELLOW : ChatFormatting.AQUA), false);
        }
        return total;
    }
}
