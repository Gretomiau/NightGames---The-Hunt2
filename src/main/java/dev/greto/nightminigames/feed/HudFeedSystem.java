package dev.greto.nightminigames.feed;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.greto.nightminigames.NightminigamesMod;
import dev.greto.nightminigames.key.KeyColor;
import dev.greto.nightminigames.machine.MachineProgressPayload;
import dev.greto.nightminigames.module.ModuleSystem;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.scores.PlayerTeam;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class HudFeedSystem {
    private static final int MAX_PER_TEAM = 120;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<TeamEntry> HISTORY = new ArrayList<>();
    private static MinecraftServer currentServer;
    private static boolean initialized;

    private HudFeedSystem() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        PayloadTypeRegistry.playS2C().register(HudFeedPayload.TYPE, HudFeedPayload.CODEC);
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            currentServer = server;
            load(server);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            HISTORY.clear();
            currentServer = null;
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ensureServer(server);
            syncPlayer(handler.player);
        });
    }

    public static void syncPlayer(ServerPlayer player) {
        List<HudFeedPayload.Entry> visible = new ArrayList<>();
        for (TeamEntry event : HISTORY) {
            if (canView(player, event.team())) {
                visible.add(event.entry());
            }
        }
        ServerPlayNetworking.send(player, new HudFeedPayload(true, visible));
    }

    public static void syncOnline(MinecraftServer server) {
        ensureServer(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncPlayer(player);
        }
    }

    public static boolean canView(ServerPlayer player, String team) {
        if (player.hasPermissions(2)) {
            return true;
        }
        if (!ModuleSystem.TEAMS.contains(team)) {
            return false;
        }
        PlayerTeam membership = player.server.getScoreboard().getPlayersTeam(player.getScoreboardName());
        return membership != null && team.equals(membership.getName());
    }

    public static void keyCollected(MinecraftServer server, String playerName, KeyColor color, String team) {
        append(server, team, new HudFeedPayload.Entry(
            sanitize(playerName, 48),
            " ha recogido ",
            "LLAVE " + color.display.toUpperCase(Locale.ROOT),
            "",
            color.rgb
        ));
    }

    public static void machineUpdated(MinecraftServer server, MachineProgressPayload payload, String team) {
        int event = payload.eventColor();
        int count = Integer.bitCount(payload.mask() & 15);
        int remaining = 4 - count;
        if (event >= 0 && event < KeyColor.values().length) {
            append(server, team, new HudFeedPayload.Entry("MÁQUINA", " ", count + "/4 · FALTAN " + remaining, "", count == 4 ? 0x61E8A0 : 0x5BDEEF));
        } else if (event == -2) {
            append(server, team, new HudFeedPayload.Entry("MÁQUINA", " ", "0/4 · FALTAN 4", "", 0x5BDEEF));
        }
    }

    public static int clear(MinecraftServer server) {
        ensureServer(server);
        int count = HISTORY.size();
        HISTORY.clear();
        save(server);
        syncOnline(server);
        return count;
    }

    public static int count(MinecraftServer server) {
        ensureServer(server);
        return HISTORY.size();
    }

    private static void append(MinecraftServer server, String team, HudFeedPayload.Entry entry) {
        ensureServer(server);
        String target = ModuleSystem.TEAMS.contains(team) ? team : "op";
        HISTORY.add(new TeamEntry(target, entry));
        int count = 0;
        boolean trimmed = false;
        for (int i = HISTORY.size() - 1; i >= 0; i--) {
            if (target.equals(HISTORY.get(i).team())) {
                count++;
                if (count > MAX_PER_TEAM) {
                    HISTORY.remove(i);
                    trimmed = true;
                }
            }
        }
        if (trimmed) {
            syncOnline(server);
        } else {
            HudFeedPayload payload = new HudFeedPayload(false, List.of(entry));
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (canView(player, target)) {
                    ServerPlayNetworking.send(player, payload);
                }
            }
        }
        save(server);
    }

    private static void ensureServer(MinecraftServer server) {
        if (currentServer != server) {
            currentServer = server;
            load(server);
        }
    }

    private static Path path(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("data/nightminigames_hud_history.json");
    }

    private static void load(MinecraftServer server) {
        HISTORY.clear();
        Path file = path(server);
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            JsonObject object = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray events = object.getAsJsonArray("teamEvents");
            if (events == null) {
                Path backup = file.resolveSibling(file.getFileName() + ".legacy.bak");
                if (!Files.isRegularFile(backup)) {
                    Files.copy(file, backup);
                }
                NightminigamesMod.LOGGER.info("[Nightminigames] Historial HUD previo sin identificación de equipos: no se importa para evitar exponer mensajes ajenos.");
                return;
            }
            for (JsonElement value : events) {
                if (!value.isJsonObject()) {
                    continue;
                }
                JsonObject item = value.getAsJsonObject();
                String team = sanitize(item.get("team").getAsString(), 16);
                if (!ModuleSystem.TEAMS.contains(team) && !"op".equals(team)) {
                    continue;
                }
                HudFeedPayload.Entry entry = new HudFeedPayload.Entry(
                    sanitize(item.get("subject").getAsString(), 48),
                    sanitize(item.get("verb").getAsString(), 48),
                    sanitize(item.get("highlight").getAsString(), 80),
                    "",
                    item.get("rgb").getAsInt() & 0xFFFFFF
                );
                if (!ClassificationFeedSystem.isClassificationEntry(entry)) {
                    HISTORY.add(new TeamEntry(team, entry));
                }
            }
        } catch (Exception exception) {
            NightminigamesMod.LOGGER.error("[Nightminigames] No se pudo cargar historial HUD por equipos", exception);
            HISTORY.clear();
        }
    }

    private static void save(MinecraftServer server) {
        Path file = path(server);
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        JsonObject object = new JsonObject();
        object.addProperty("schemaVersion", 2);
        JsonArray array = new JsonArray();
        for (TeamEntry event : HISTORY) {
            JsonObject item = new JsonObject();
            item.addProperty("team", event.team());
            item.addProperty("subject", event.entry().subject());
            item.addProperty("verb", event.entry().verb());
            item.addProperty("highlight", event.entry().highlight());
            item.addProperty("rgb", event.entry().rgb());
            array.add(item);
        }
        object.add("teamEvents", array);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(temp, GSON.toJson(object), StandardCharsets.UTF_8);
            try {
                Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            NightminigamesMod.LOGGER.error("[Nightminigames] No se pudo guardar historial HUD por equipos", exception);
        }
    }

    private static String sanitize(String value, int limit) {
        if (value == null) {
            return "";
        }
        String clean = value.replace('\n', ' ').replace('\r', ' ');
        return clean.length() > limit ? clean.substring(0, limit) : clean;
    }

    private record TeamEntry(String team, HudFeedPayload.Entry entry) {
    }
}
