package dev.greto.nightminigames.environment;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.greto.nightminigames.NightminigamesMod;
import dev.greto.nightminigames.key.KeyMessages;
import net.minecraft.ChatFormatting;
import dev.greto.nightminigames.registry.ModSounds;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class EnvironmentSystem {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path LEGACY_FILE = FabricLoader.getInstance().getConfigDir().resolve("nightminigames/environment.json");

    private static Path file(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("data/nightminigames_environment.json");
    }
    private static boolean darkness = true;
    private static boolean music = true;
    private static float volume = 0.70F;
    private static boolean initialized;

    private EnvironmentSystem() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        PayloadTypeRegistry.playS2C().register(EnvironmentStatePayload.TYPE, EnvironmentStatePayload.CODEC);
        ServerLifecycleEvents.SERVER_STARTING.register(EnvironmentSystem::load);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sendState(handler.player));
    }

    public static boolean isDarkness() { return darkness; }

    public static int setDarkness(MinecraftServer server, boolean active) {
        if (darkness == active) {
            return 0;
        }
        darkness = active;
        dev.greto.nightminigames.integration.MapWaypointBridge.requestRefresh(server);
        save(server);
        broadcast(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.playNotifySound(active ? ModSounds.POWER_OFF : ModSounds.POWER_ON, SoundSource.MASTER, 0.85F, 1.0F);
        }
        return 1;
    }

    public static int setMusic(MinecraftServer server, boolean active) {
        if (music == active) {
            broadcast(server);
            return 0;
        }
        music = active;
        save(server);
        broadcast(server);
        return 1;
    }

    public static int setVolume(MinecraftServer server, int percent) {
        float next = Math.clamp(percent, 0, 100) / 100.0F;
        if (Float.compare(next, volume) == 0) {
            return 0;
        }
        volume = next;
        save(server);
        broadcast(server);
        return 1;
    }

    public static int report(CommandSourceStack source) {
        source.sendSuccess(() -> KeyMessages.prefix()
            .append(Component.literal("Oscuridad: ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(darkness ? "ON" : "OFF").withStyle(darkness ? ChatFormatting.DARK_PURPLE : ChatFormatting.GREEN))
            .append(Component.literal("  ◆  Música: ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(music ? "ON" : "OFF").withStyle(music ? ChatFormatting.AQUA : ChatFormatting.RED))
            .append(Component.literal("  ◆  Volumen: ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(Math.round(volume * 100.0F) + "%").withStyle(ChatFormatting.GOLD)), false);
        return 1;
    }

    private static void broadcast(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendState(player);
        }
    }

    private static void sendState(ServerPlayer player) {
        ServerPlayNetworking.send(player, new EnvironmentStatePayload(darkness, music, volume));
    }

    private static void load(MinecraftServer server) {
        Path file = file(server);
        darkness = true;
        music = true;
        volume = 0.70F;
        if (!Files.isRegularFile(file)) {
            if (Files.isRegularFile(LEGACY_FILE)) {
                try {
                    Files.createDirectories(file.getParent());
                    Files.copy(LEGACY_FILE, file);
                } catch (IOException error) {
                    NightminigamesMod.LOGGER.error("[Nightminigames] No se pudo migrar la configuración de entorno", error);
                }
            } else {
                save(server);
                return;
            }
        }
        try {
            JsonObject json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            if (json.has("darkness") && json.get("darkness").isJsonPrimitive()) {
                darkness = json.get("darkness").getAsBoolean();
            }
            if (json.has("music") && json.get("music").isJsonPrimitive()) {
                music = json.get("music").getAsBoolean();
            }
            if (json.has("volume") && json.get("volume").isJsonPrimitive()) {
                volume = Math.clamp(json.get("volume").getAsFloat(), 0.0F, 1.0F);
            }
        } catch (Exception error) {
            NightminigamesMod.LOGGER.error("[Nightminigames] No se pudo leer {}. Se conservará el archivo y se usarán los valores predeterminados.", file, error);
        }
    }

    private static void save(MinecraftServer server) {
        Path file = file(server);
        try {
            Files.createDirectories(file.getParent());
            JsonObject json = new JsonObject();
            json.addProperty("darkness", darkness);
            json.addProperty("music", music);
            json.addProperty("volume", volume);
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(json));
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException error) {
            NightminigamesMod.LOGGER.error("[Nightminigames] Error guardando el entorno: {}", file, error);
        }
    }
}
