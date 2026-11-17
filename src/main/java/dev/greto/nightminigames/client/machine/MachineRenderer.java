package dev.greto.nightminigames.client.machine;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.greto.nightminigames.team.TeamBinding;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.greto.nightminigames.NightminigamesMod;
import dev.greto.nightminigames.client.admin.SpawnerAdminMarker;
import dev.greto.nightminigames.machine.MachineBlock;
import dev.greto.nightminigames.machine.MachineBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public final class MachineRenderer extends GeoBlockRenderer<MachineBlockEntity> {
    private static final int[] POINT_X = {-17, -5, 6, 18};
    private static final int[] POINT_Y = {19, 19, 19, 19};
    private static final int[] POINT_Z = {-1, -1, -1, 0};
    private static final double PIXELS_PER_BLOCK = 16.0D;
    private static final double HOVER_HEIGHT = 0.23D;
    private static final double PANEL_FRONT = 0.58D;
    private static final float HALF_SIZE = 0.145F;
    private static final int FULL_BRIGHT = 0x00F000F0;
    private static final ResourceLocation MISSING_TEXTURE = NightminigamesMod.id("textures/hologram/lock_missing.png");
    private static final ResourceLocation COMPLETE_TEXTURE = NightminigamesMod.id("textures/hologram/lock_complete.png");
    private static final RenderType MISSING_LAYER = RenderType.entityTranslucentEmissive(MISSING_TEXTURE);
    private static final RenderType COMPLETE_LAYER = RenderType.entityTranslucentEmissive(COMPLETE_TEXTURE);

    public MachineRenderer(net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context context) {
        super(new MachineModel());
    }

    @Override
    public void render(MachineBlockEntity entity, float partialTick, PoseStack pose, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        super.render(entity, partialTick, pose, buffers, packedLight, packedOverlay);
        SpawnerAdminMarker.renderWhenShown(pose, buffers, 2.12F, TeamBinding.rgb(entity.getOwnerTeam()), "MAQUINA " + entity.getOwnerTeam().toUpperCase(java.util.Locale.ROOT), entity.getOwnerTeam());
        BlockState state = entity.getBlockState();
        if (!state.hasProperty(MachineBlock.MASK) || !state.hasProperty(MachineBlock.FACING)) {
            return;
        }
        int mask = state.getValue(MachineBlock.MASK);
        Direction facing = state.getValue(MachineBlock.FACING);
        Direction right = facing.getClockWise();
        var camera = Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation();
        for (int index = 0; index < 4; index++) {
            double rightOffset = -POINT_X[index] / PIXELS_PER_BLOCK;
            double frontOffset = PANEL_FRONT - POINT_Z[index] / PIXELS_PER_BLOCK;
            double x = 0.5D + right.getStepX() * rightOffset + facing.getStepX() * frontOffset;
            double y = POINT_Y[index] / PIXELS_PER_BLOCK + HOVER_HEIGHT;
            double z = 0.5D + right.getStepZ() * rightOffset + facing.getStepZ() * frontOffset;
            pose.pushPose();
            pose.translate(x, y, z);
            pose.mulPose(camera);
            drawQuad(pose, buffers.getBuffer((mask & (1 << index)) != 0 ? COMPLETE_LAYER : MISSING_LAYER));
            pose.popPose();
        }
    }

    private static void drawQuad(PoseStack pose, VertexConsumer consumer) {
        PoseStack.Pose current = pose.last();
        consumer.addVertex(current, -HALF_SIZE, -HALF_SIZE, 0F).setColor(255, 255, 255, 255).setUv(0F, 1F)
            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(current, 0F, 0F, 1F);
        consumer.addVertex(current, HALF_SIZE, -HALF_SIZE, 0F).setColor(255, 255, 255, 255).setUv(1F, 1F)
            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(current, 0F, 0F, 1F);
        consumer.addVertex(current, HALF_SIZE, HALF_SIZE, 0F).setColor(255, 255, 255, 255).setUv(1F, 0F)
            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(current, 0F, 0F, 1F);
        consumer.addVertex(current, -HALF_SIZE, HALF_SIZE, 0F).setColor(255, 255, 255, 255).setUv(0F, 0F)
            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(current, 0F, 0F, 1F);
    }

    @Override
    public boolean shouldRenderOffScreen(MachineBlockEntity entity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
