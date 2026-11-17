package dev.greto.nightminigames.client.door;

import dev.greto.nightminigames.door.DoorBlockEntity;
import dev.greto.nightminigames.door.DoorBlock;
import dev.greto.nightminigames.client.admin.SpawnerAdminMarker;
import dev.greto.nightminigames.region.RegionKind;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public final class DoorRenderer extends GeoBlockRenderer<DoorBlockEntity> {
    public DoorRenderer(BlockEntityRendererProvider.Context context) {
        super(new DoorModel());
        withScale(1.2857143F, 1.321101F);
    }

    @Override
    public void render(DoorBlockEntity entity, float tickDelta, PoseStack pose, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        super.render(entity, tickDelta, pose, buffers, packedLight, packedOverlay);
        if (entity.getBlockState().getBlock() instanceof DoorBlock block) {
            boolean entrance = block.kind() == RegionKind.ENTRADA;
            SpawnerAdminMarker.renderWhenShown(pose, buffers, 5.0F, entrance ? 0x44DDEC : 0xFFAA55,
                entrance ? "PUERTA ENTRADA · " + entity.ownerTeam().toUpperCase() : "PUERTA SALIDA · " + entity.ownerTeam().toUpperCase(), entity.ownerTeam());
        }
    }

    @Override
    public boolean shouldRenderOffScreen(DoorBlockEntity entity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
