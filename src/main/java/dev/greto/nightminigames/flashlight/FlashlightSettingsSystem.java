package dev.greto.nightminigames.flashlight;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.greto.nightminigames.NightminigamesMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class FlashlightSettingsSystem {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static FlashlightLightingProfile profile = FlashlightLightingProfile.DEFAULT;
    private static boolean initialized;

    private FlashlightSettingsSystem() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        PayloadTypeRegistry.playS2C().register(FlashlightProfilePayload.TYPE, FlashlightProfilePayload.CODEC);
        ServerLifecycleEvents.SERVER_STARTING.register(server -> profile = load(server));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> profile = FlashlightLightingProfile.DEFAULT);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sync(handler.player));
    }

    public static FlashlightLightingProfile profile() {
        return profile;
    }

    public static void sync(ServerPlayer player) {
        ServerPlayNetworking.send(player, new FlashlightProfilePayload(profile));
    }

    public static boolean reload(MinecraftServer server) {
        Path file = file(server);
        if (!Files.isRegularFile(file)) {
            save(server, FlashlightLightingProfile.DEFAULT);
        }
        FlashlightLightingProfile loaded = load(server);
        if (loaded == null) {
            return false;
        }
        profile = loaded;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sync(player);
        }
        return true;
    }

    private static Path file(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("data/nightminigames_flashlight.json");
    }

    private static FlashlightLightingProfile load(MinecraftServer server) {
        Path file = file(server);
        if (!Files.isRegularFile(file)) {
            save(server, FlashlightLightingProfile.DEFAULT);
            return FlashlightLightingProfile.DEFAULT;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            FlashlightLightingProfile defaults = FlashlightLightingProfile.DEFAULT;
            FlashlightLightingProfile result = new FlashlightLightingProfile(
                readFloat(root, "rangeBlocks", defaults.rangeBlocks(), 6.0F, 64.0F),
                readFloat(root, "coreBrightness", defaults.coreBrightness(), 0.0F, 16.0F),
                readFloat(root, "spillBrightness", defaults.spillBrightness(), 0.0F, 16.0F),
                readFloat(root, "coreAngleDegrees", defaults.coreAngleDegrees(), 5.0F, 85.0F),
                readFloat(root, "spillAngleDegrees", defaults.spillAngleDegrees(), 5.0F, 120.0F),
                readFloat(root, "coreSize", (float) defaults.coreSize(), 0.01F, 5.0F),
                readFloat(root, "spillSize", (float) defaults.spillSize(), 0.01F, 5.0F),
                readFloat(root, "spillRangeMultiplier", defaults.spillRangeMultiplier(), 0.1F, 1.0F)
            );
            return result;
        } catch (Exception exception) {
            NightminigamesMod.LOGGER.error("[Nightminigames] No se pudo cargar nightminigames_flashlight.json. Se mantienen los valores seguros.", exception);
            return FlashlightLightingProfile.DEFAULT;
        }
    }

    private static float readFloat(JsonObject object, String key, float fallback, float min, float max) {
        if (!object.has(key)) {
            return fallback;
        }
        float value = object.get(key).getAsFloat();
        if (!Float.isFinite(value) || value < min || value > max) {
            throw new IllegalArgumentException("Valor fuera de rango: " + key);
        }
        return value;
    }

    private static void save(MinecraftServer server, FlashlightLightingProfile p) {
        Path file = file(server);
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", 1);
        root.addProperty("rangeBlocks", p.rangeBlocks());
        root.addProperty("coreBrightness", p.coreBrightness());
        root.addProperty("spillBrightness", p.spillBrightness());
        root.addProperty("coreAngleDegrees", p.coreAngleDegrees());
        root.addProperty("spillAngleDegrees", p.spillAngleDegrees());
        root.addProperty("coreSize", p.coreSize());
        root.addProperty("spillSize", p.spillSize());
        root.addProperty("spillRangeMultiplier", p.spillRangeMultiplier());
        try {
            Files.createDirectories(file.getParent());
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(temporary, GSON.toJson(root), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception exception) {
            NightminigamesMod.LOGGER.error("[Nightminigames] No se pudo guardar la configuración de linterna", exception);
        }
    }
}
