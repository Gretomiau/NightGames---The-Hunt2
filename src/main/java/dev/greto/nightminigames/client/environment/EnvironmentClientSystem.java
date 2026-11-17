package dev.greto.nightminigames.client.environment;

import dev.greto.nightminigames.environment.EnvironmentStatePayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

public final class EnvironmentClientSystem {
    private static boolean darkness;
    private static boolean music;
    private static float volume = 0.70F;
    private static boolean refresh;
    private static AmbientLoopSound current;
    private static int musicMaintenanceTicks;
    private static boolean initialized;

    private EnvironmentClientSystem() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        ClientPlayNetworking.registerGlobalReceiver(EnvironmentStatePayload.TYPE, (payload, context) -> context.client().execute(() -> apply(context.client(), payload)));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset(client));
        ClientTickEvents.END_CLIENT_TICK.register(EnvironmentClientSystem::tick);
    }

    public static boolean isDark() {
        return darkness;
    }

    public static boolean consumeRefresh() {
        boolean pending = refresh;
        refresh = false;
        return pending;
    }

    private static void apply(Minecraft client, EnvironmentStatePayload payload) {
        if (darkness != payload.darkness()) {
            darkness = payload.darkness();
            refresh = true;
        }
        music = payload.music();
        volume = Math.clamp(payload.volume(), 0.0F, 1.0F);
        if (!music) {
            stopMusic(client);
        } else if (current != null) {
            current.setRuntimeVolume(volume);
        }
    }

    private static void tick(Minecraft client) {
        if (client.level == null) {
            stopMusic(client);
            return;
        }
        if (!music) {
            stopMusic(client);
            return;
        }
        if (current != null) {
            current.setRuntimeVolume(volume);
        }
        if (current == null) {
            ensureMusic(client);
        }
        if (++musicMaintenanceTicks >= 200) {
            musicMaintenanceTicks = 0;
            client.getMusicManager().stopPlaying();
            if (current != null && !client.getSoundManager().isActive(current)) {
                client.getSoundManager().stop(current);
                current = null;
            }
        }
    }

    private static void ensureMusic(Minecraft client) {
        if (client.level == null || !music) {
            return;
        }
        if (current != null) {
            current.setRuntimeVolume(volume);
            return;
        }
        client.getMusicManager().stopPlaying();
        AmbientLoopSound next = new AmbientLoopSound(volume);
        current = next;
        client.getSoundManager().play(next);
    }

    private static void stopMusic(Minecraft client) {
        if (current != null) {
            client.getSoundManager().stop(current);
            current = null;
        }
        musicMaintenanceTicks = 0;
    }

    private static void reset(Minecraft client) {
        stopMusic(client);
        music = false;
        volume = 0.70F;
        if (darkness) {
            darkness = false;
            refresh = true;
        }
    }
}
