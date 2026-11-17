package dev.greto.nightminigames.client.key;

import dev.greto.nightminigames.NightminigamesMod;
import dev.greto.nightminigames.key.ColoredKeyItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class ColoredKeyModel extends GeoModel<ColoredKeyItem> {
    @Override
    public ResourceLocation getModelResource(ColoredKeyItem animatable) {
        return NightminigamesMod.id("geo/colored_key.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ColoredKeyItem animatable) {
        return NightminigamesMod.id("textures/item/key_" + animatable.getColor() + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(ColoredKeyItem animatable) {
        return NightminigamesMod.id("animations/empty.animation.json");
    }
}
