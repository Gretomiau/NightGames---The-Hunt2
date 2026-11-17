package dev.greto.nightminigames.client.key;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.greto.nightminigames.team.TeamBinding;
import dev.greto.nightminigames.client.admin.SpawnerAdminMarker;
import dev.greto.nightminigames.key.KeySpawnBlock;
import dev.greto.nightminigames.key.KeySpawnBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;

public final class KeyHologramRenderer implements BlockEntityRenderer<KeySpawnBlockEntity> {
    private final net.minecraft.client.renderer.entity.ItemRenderer itemRenderer;

    public KeyHologramRenderer(BlockEntityRendererProvider.Context context) {
        itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(KeySpawnBlockEntity entity, float tickDelta, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!(entity.getBlockState().getBlock() instanceof KeySpawnBlock block)) {
            return;
        }
        boolean collected = entity.getBlockState().getValue(KeySpawnBlock.COLLECTED);
        if (collected) {
            SpawnerAdminMarker.renderSpawner(pose, buffers, 1.55F, TeamBinding.rgb(entity.ownerTeam()), ("SPAWNER LLAVE " + block.color.display.toUpperCase(java.util.Locale.ROOT) + " · " + entity.ownerTeam().toUpperCase(java.util.Locale.ROOT)), true, entity.ownerTeam());
            return;
        }
        float time = (System.nanoTime() % 8_000_000_000L) / 1_000_000_000.0F;
        float bob = (float) Math.sin(time * 2.4F) * 0.08F;
        pose.pushPose();
        pose.translate(0.5D, 0.76F + bob, 0.5D);
        pose.mulPose(new Quaternionf().rotationY(time * 0.78F));
        pose.scale(0.80F, 0.80F, 0.80F);
        itemRenderer.renderStatic(new ItemStack(block.color.keyItem()), ItemDisplayContext.GROUND, 0x00F000F0, OverlayTexture.NO_OVERLAY, pose, buffers, entity.getLevel(), 0);
        pose.popPose();
        SpawnerAdminMarker.renderSpawner(pose, buffers, 1.55F, TeamBinding.rgb(entity.ownerTeam()), ("SPAWNER LLAVE " + block.color.display.toUpperCase(java.util.Locale.ROOT) + " · " + entity.ownerTeam().toUpperCase(java.util.Locale.ROOT)), false, entity.ownerTeam());
    }

    @Override
    public int getViewDistance() {
        return 48;
    }
}
