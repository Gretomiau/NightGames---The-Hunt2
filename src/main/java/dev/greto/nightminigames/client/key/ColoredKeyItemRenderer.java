package dev.greto.nightminigames.client.key;

import dev.greto.nightminigames.key.ColoredKeyItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class ColoredKeyItemRenderer extends GeoItemRenderer<ColoredKeyItem> {
    public ColoredKeyItemRenderer() {
        super(new ColoredKeyModel());
    }
}
