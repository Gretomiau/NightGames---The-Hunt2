package dev.greto.nightminigames.client.door;

import dev.greto.nightminigames.NightminigamesMod;
import dev.greto.nightminigames.door.DoorBlock;
import dev.greto.nightminigames.door.DoorBlockEntity;
import dev.greto.nightminigames.region.RegionKind;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class DoorModel extends GeoModel<DoorBlockEntity> {
    private static final ResourceLocation ENTRANCE_GEO = NightminigamesMod.id("geo/door_entrance.geo.json");
    private static final ResourceLocation EXIT_GEO = NightminigamesMod.id("geo/door_exit.geo.json");
    private static final ResourceLocation ENTRANCE_TEXTURE = NightminigamesMod.id("textures/block/door_entrance.png");
    private static final ResourceLocation EXIT_TEXTURE = NightminigamesMod.id("textures/block/door_exit.png");
    private static final ResourceLocation ENTRANCE_ANIM = NightminigamesMod.id("animations/door_entrance.animation.json");
    private static final ResourceLocation EXIT_ANIM = NightminigamesMod.id("animations/door_exit.animation.json");

    @Override
    public ResourceLocation getModelResource(DoorBlockEntity entity) {
        return isEntrance(entity) ? ENTRANCE_GEO : EXIT_GEO;
    }

    @Override
    public ResourceLocation getTextureResource(DoorBlockEntity entity) {
        return isEntrance(entity) ? ENTRANCE_TEXTURE : EXIT_TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(DoorBlockEntity entity) {
        return isEntrance(entity) ? ENTRANCE_ANIM : EXIT_ANIM;
    }

    private static boolean isEntrance(DoorBlockEntity entity) {
        return entity.getBlockState().getBlock() instanceof DoorBlock door && door.kind() == RegionKind.ENTRADA;
    }
}
