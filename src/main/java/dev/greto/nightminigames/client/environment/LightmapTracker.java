package dev.greto.nightminigames.client.environment;

import net.minecraft.client.renderer.texture.DynamicTexture;

public final class LightmapTracker {
    private static DynamicTexture lightmap;

    private LightmapTracker() {
    }

    public static void mark(DynamicTexture texture) {
        lightmap = texture;
    }

    public static boolean isLightmap(DynamicTexture texture) {
        return lightmap == texture;
    }
}
