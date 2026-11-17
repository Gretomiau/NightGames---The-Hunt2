package dev.greto.nightminigames.client.admin;

import dev.greto.nightminigames.admin.AdminShowPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

public final class AdminShowClient {
    private static boolean enabled;
    private static String team = "";
    private static boolean initialized;

    private AdminShowClient() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        ClientPlayNetworking.registerGlobalReceiver(AdminShowPayload.TYPE, (payload, context) -> context.client().execute(() -> { enabled = payload.enabled(); team = payload.team(); }));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> { enabled = false; team = ""; });
    }

    public static boolean isEnabled() {
        Minecraft client = Minecraft.getInstance();
        return enabled && client.player != null && client.player.hasPermissions(2);
    }

    public static String teamFilter() {
        return enabled ? team : "";
    }

    public static boolean visible(String owner) {
        return !enabled || team.isBlank() || team.equals(owner);
    }

    public static boolean creative() {
        Minecraft client = Minecraft.getInstance();
        return client.player != null && client.player.isCreative();
    }
}
