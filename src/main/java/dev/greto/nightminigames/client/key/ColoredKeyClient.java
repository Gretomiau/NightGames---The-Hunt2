package dev.greto.nightminigames.client.key;

import dev.greto.nightminigames.key.ColoredKeyItem;
import dev.greto.nightminigames.registry.ModItems;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;

public final class ColoredKeyClient {
    private ColoredKeyClient() {
    }

    public static void initialize() {
        register(ModItems.KEY_BLUE);
        register(ModItems.KEY_YELLOW);
        register(ModItems.KEY_RED);
        register(ModItems.KEY_GREEN);
    }

    private static void register(ColoredKeyItem item) {
        item.setRenderProvider(new GeoRenderProvider() {
            private ColoredKeyItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (renderer == null) {
                    renderer = new ColoredKeyItemRenderer();
                }
                return renderer;
            }
        });
    }
}
