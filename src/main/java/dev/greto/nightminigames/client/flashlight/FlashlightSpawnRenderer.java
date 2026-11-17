package dev.greto.nightminigames.client.flashlight;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.greto.nightminigames.team.TeamBinding;
import dev.greto.nightminigames.client.admin.SpawnerAdminMarker;
import dev.greto.nightminigames.flashlight.FlashlightSpawnBlock;
import dev.greto.nightminigames.flashlight.FlashlightSpawnBlockEntity;
import dev.greto.nightminigames.registry.ModItems;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;

public final class FlashlightSpawnRenderer implements BlockEntityRenderer<FlashlightSpawnBlockEntity> {
    private static final float HOLOGRAM_SCALE = 2.60F;
    private final net.minecraft.client.renderer.entity.ItemRenderer itemRenderer;

    public FlashlightSpawnRenderer(BlockEntityRendererProvider.Context context) {
        itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(FlashlightSpawnBlockEntity entity, float tickDelta, PoseStack pose, MultiBufferSource buffers, int packedLight, int overlay) {
        boolean collected = entity.getBlockState().getValue(FlashlightSpawnBlock.COLLECTED);
        if (!collected) {
            float time = (System.nanoTime() % 8_000_000_000L) / 1_000_000_000.0F;
            pose.pushPose();
            pose.translate(0.5D, 0.72D + Math.sin(time * 2.0F) * 0.06D, 0.5D);
            pose.mulPose(new Quaternionf().rotationY(time * 0.55F));
            pose.scale(HOLOGRAM_SCALE, HOLOGRAM_SCALE, HOLOGRAM_SCALE);
            itemRenderer.renderStatic(new ItemStack(ModItems.FLASHLIGHT), ItemDisplayContext.GROUND, 0x00F000F0, OverlayTexture.NO_OVERLAY,
                pose, buffers, entity.getLevel(), 0);
            pose.popPose();
        }
        SpawnerAdminMarker.renderSpawner(pose, buffers, 2.20F, TeamBinding.rgb(entity.ownerTeam()), ("SPAWNER LINTERNA" + " · " + entity.ownerTeam().toUpperCase(java.util.Locale.ROOT)), collected, entity.ownerTeam());
    }

    @Override
    public int getViewDistance() {
        return 48;
    }
}
