package dev.greto.nightminigames.team;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.greto.nightminigames.NightminigamesMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.scores.PlayerTeam;

import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public final class TeamCapacitySystem {
    public static final int DEFAULT_CAPACITY = 20;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, Integer> CAPACITIES = new HashMap<>();
    private static MinecraftServer active;
    private static boolean initialized;

    private TeamCapacitySystem() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            active = server;
            load(server);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            active = null;
            CAPACITIES.clear();
        });
    }

    public static int capacity(String name) {
        return CAPACITIES.getOrDefault(name, DEFAULT_CAPACITY);
    }

    public static boolean hasSpace(PlayerTeam team) {
        return team != null && team.getPlayers().size() < capacity(team.getName());
    }

    public static void set(MinecraftServer server, String name, int amount) {
        CAPACITIES.put(name, amount);
        save(server);
    }

    public static void setAll(MinecraftServer server, Iterable<String> names, int amount) {
        for (String name : names) {
            CAPACITIES.put(name, amount);
        }
        save(server);
    }

    private static Path file(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("data/nightminigames_team_cupos.json");
    }

    private static void load(MinecraftServer server) {
        CAPACITIES.clear();
        Path file = file(server);
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var entry : root.entrySet()) {
                int amount = entry.getValue().getAsInt();
                if (amount >= 1 && amount <= 1000) {
                    CAPACITIES.put(entry.getKey(), amount);
                }
            }
        } catch (Exception exception) {
            NightminigamesMod.LOGGER.error("[Nightminigames] Error leyendo cupos", exception);
        }
    }

    private static void save(MinecraftServer server) {
        Path file = file(server);
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(temp, GSON.toJson(CAPACITIES), StandardCharsets.UTF_8);
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception exception) {
            NightminigamesMod.LOGGER.error("[Nightminigames] Error guardando cupos", exception);
        }
    }
}
