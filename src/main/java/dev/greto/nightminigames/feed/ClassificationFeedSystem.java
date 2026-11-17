package dev.greto.nightminigames.feed;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.greto.nightminigames.NightminigamesMod;
import dev.greto.nightminigames.key.KeyMessages;
import dev.greto.nightminigames.team.TeamBinding;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ClassificationFeedSystem {
    private static final int MAX_HISTORY = 20;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<HudFeedPayload.Entry> HISTORY = new ArrayList<>();
    private static int visibleLimit = 5;
    private static MinecraftServer activeServer;
    private static boolean initialized;

    private ClassificationFeedSystem() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        PayloadTypeRegistry.playS2C().register(ClassificationFeedPayload.TYPE, ClassificationFeedPayload.CODEC);
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            activeServer = server;
            load(server);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            HISTORY.clear();
            visibleLimit = 5;
            activeServer = null;
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ensureServer(server);
            ServerPlayNetworking.send(handler.player, new ClassificationFeedPayload(true, visibleLimit, List.copyOf(HISTORY)));
        });
    }

    public static LiteralArgumentBuilder<CommandSourceStack> commands() {
        return Commands.literal("clasificacion")
            .then(Commands.literal("limite")
                .then(Commands.argument("lineas", IntegerArgumentType.integer(1, 8))
                    .executes(context -> limitCommand(context.getSource(), IntegerArgumentType.getInteger(context, "lineas")))));
    }

    public static void clearForRound(MinecraftServer server) {
        ensureServer(server);
        HISTORY.clear();
        save(server);
        broadcast(server, true, List.of());
    }

    public static void teamQualified(MinecraftServer server, String teamName, int count, int slots) {
        append(server, new HudFeedPayload.Entry(
            "EQUIPO " + teamName.toUpperCase(Locale.ROOT),
            " CLASIFICÓ · ",
            count + "/" + slots + " · FALTAN " + Math.max(0, slots - count),
            "",
            TeamBinding.rgb(teamName)
        ));
    }

    public static void teamWithoutSlot(MinecraftServer server, String teamName, int count, int slots) {
        append(server, new HudFeedPayload.Entry(
            "EQUIPO " + teamName.toUpperCase(Locale.ROOT),
            " LLEGÓ · ",
            "SIN CUPO · " + count + "/" + slots,
            "",
            0xFFAA66
        ));
    }

    public static void classificationStatus(MinecraftServer server, int count, int slots) {
        append(server, new HudFeedPayload.Entry(
            "CLASIFICACIÓN",
            " ",
            count + "/" + slots + " · FALTAN " + Math.max(0, slots - count),
            "",
            0x55DCEF
        ));
    }

    public static boolean isClassificationEntry(HudFeedPayload.Entry entry) {
        return entry.subject().startsWith("EQUIPO ") || entry.subject().equals("CLASIFICACIÓN");
    }

    private static int clearCommand(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        ensureServer(server);
        int removed = HISTORY.size();
        HISTORY.clear();
        save(server);
        broadcast(server, true, List.of());
        source.sendSuccess(() -> KeyMessages.message("Historial de clasificación limpiado: " + removed + " notificación(es).", ChatFormatting.GREEN), false);
        return 1;
    }

    public static int report(CommandSourceStack source) {
        ensureServer(source.getServer());
        source.sendSuccess(() -> KeyMessages.message("HUD clasificación: " + HISTORY.size() + " eventos guardados · máximo " + visibleLimit + " líneas visibles.", ChatFormatting.AQUA), false);
        return 1;
    }

    private static int limitCommand(CommandSourceStack source, int limit) {
        MinecraftServer server = source.getServer();
        ensureServer(server);
        visibleLimit = limit;
        save(server);
        broadcast(server, true, List.copyOf(HISTORY));
        source.sendSuccess(() -> KeyMessages.message("HUD clasificación: límite de " + visibleLimit + " líneas visibles.", ChatFormatting.GREEN), false);
        return 1;
    }

    private static void append(MinecraftServer server, HudFeedPayload.Entry entry) {
        ensureServer(server);
        if (HISTORY.size() >= MAX_HISTORY) {
            HISTORY.remove(0);
        }
        HISTORY.add(entry);
        save(server);
        broadcast(server, false, List.of(entry));
    }

    private static void broadcast(MinecraftServer server, boolean replace, List<HudFeedPayload.Entry> entries) {
        ClassificationFeedPayload packet = new ClassificationFeedPayload(replace, visibleLimit, entries);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(player, packet);
        }
    }

    private static void ensureServer(MinecraftServer server) {
        if (activeServer != server) {
            activeServer = server;
            load(server);
        }
    }

    private static Path path(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("data").resolve("nightminigames_classification_hud.json");
    }

    private static Path oldPath(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("data").resolve("nightminigames_hud_history.json");
    }

    private static void load(MinecraftServer server) {
        HISTORY.clear();
        visibleLimit = 5;
        Path file = path(server);
        boolean legacy = !Files.isRegularFile(file);
        if (legacy) {
            file = oldPath(server);
        }
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            JsonObject object = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!legacy && object.has("visibleLimit")) {
                visibleLimit = Math.clamp(object.get("visibleLimit").getAsInt(), 1, 8);
            }
            JsonArray array = object.getAsJsonArray("entries");
            if (array != null) {
                for (JsonElement value : array) {
                    if (!value.isJsonObject()) {
                        continue;
                    }
                    JsonObject item = value.getAsJsonObject();
                    HudFeedPayload.Entry entry = new HudFeedPayload.Entry(
                        clean(item.get("subject").getAsString(), 48),
                        clean(item.get("verb").getAsString(), 48),
                        clean(item.get("highlight").getAsString(), 80),
                        "",
                        item.get("rgb").getAsInt() & 0xFFFFFF
                    );
                    if (legacy && !isClassificationEntry(entry)) {
                        continue;
                    }
                    if (HISTORY.size() >= MAX_HISTORY) {
                        HISTORY.remove(0);
                    }
                    HISTORY.add(entry);
                }
            }
            if (legacy && !HISTORY.isEmpty()) {
                save(server);
            }
        } catch (Exception exception) {
            NightminigamesMod.LOGGER.error("[Nightminigames] No se pudo cargar el historial de clasificación", exception);
            HISTORY.clear();
        }
    }

    private static void save(MinecraftServer server) {
        Path file = path(server);
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        JsonObject object = new JsonObject();
        object.addProperty("visibleLimit", visibleLimit);
        JsonArray entries = new JsonArray();
        for (HudFeedPayload.Entry entry : HISTORY) {
            JsonObject element = new JsonObject();
            element.addProperty("subject", entry.subject());
            element.addProperty("verb", entry.verb());
            element.addProperty("highlight", entry.highlight());
            element.addProperty("rgb", entry.rgb());
            entries.add(element);
        }
        object.add("entries", entries);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(temp, GSON.toJson(object), StandardCharsets.UTF_8);
            try {
                Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception exception) {
            NightminigamesMod.LOGGER.error("[Nightminigames] No se pudo guardar el historial de clasificación", exception);
        }
    }

    private static String clean(String value, int max) {
        String cleaned = value.replace('\n', ' ').replace('\r', ' ');
        return cleaned.length() > max ? cleaned.substring(0, max) : cleaned;
    }
}
