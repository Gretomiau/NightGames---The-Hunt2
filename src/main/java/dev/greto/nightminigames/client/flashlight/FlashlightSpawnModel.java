package dev.greto.nightminigames.client.flashlight;

import dev.greto.nightminigames.NightminigamesMod;
import dev.greto.nightminigames.flashlight.FlashlightSpawnBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class FlashlightSpawnModel extends GeoModel<FlashlightSpawnBlockEntity> {
    private static final ResourceLocation MODEL = NightminigamesMod.id("geo/flashlight_spawner.geo.json");
    private static final ResourceLocation TEXTURE = NightminigamesMod.id("textures/block/flashlight_spawner.png");
    private static final ResourceLocation ANIMATION = NightminigamesMod.id("animations/empty.animation.json");

    @Override
    public ResourceLocation getModelResource(FlashlightSpawnBlockEntity entity) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(FlashlightSpawnBlockEntity entity) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(FlashlightSpawnBlockEntity entity) {
        return ANIMATION;
    }
}
