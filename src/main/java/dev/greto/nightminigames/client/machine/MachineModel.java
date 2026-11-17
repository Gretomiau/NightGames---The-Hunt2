package dev.greto.nightminigames.client.machine;

import dev.greto.nightminigames.NightminigamesMod;
import dev.greto.nightminigames.machine.MachineBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class MachineModel extends GeoModel<MachineBlockEntity> {
    private static final ResourceLocation MODEL = NightminigamesMod.id("geo/machine.geo.json");
    private static final ResourceLocation TEXTURE = NightminigamesMod.id("textures/block/machine.png");
    private static final ResourceLocation ANIMATION = NightminigamesMod.id("animations/empty.animation.json");

    @Override
    public ResourceLocation getModelResource(MachineBlockEntity entity) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(MachineBlockEntity entity) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(MachineBlockEntity entity) {
        return ANIMATION;
    }
}
