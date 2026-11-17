package dev.greto.nightminigames.client.kill;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.greto.nightminigames.client.admin.AdminShowClient;
import dev.greto.nightminigames.registry.ModBlocks;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class KillBlockOverlayRenderer {
    private static final int HORIZONTAL_RADIUS = 11;
    private static final int VERTICAL_RADIUS = 5;
    private static final int MAX_MARKERS = 72;
    private static final List<BlockPos> VISIBLE = new ArrayList<>();
    private static int tickCounter;
    private static boolean initialized;

    private KillBlockOverlayRenderer() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        ClientTickEvents.END_CLIENT_TICK.register(KillBlockOverlayRenderer::update);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> VISIBLE.clear());
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            Minecraft client = Minecraft.getInstance();
            if (!AdminShowClient.creative() || context.matrixStack() == null || context.consumers() == null) {
                return;
            }
            var camera = context.camera().getPosition();
            PoseStack pose = context.matrixStack();
            Font font = client.font;
            String icon = "X";
            for (BlockPos pos : VISIBLE) {
                pose.pushPose();
                pose.translate(pos.getX() + 0.5D - camera.x, pos.getY() + 0.56D - camera.y, pos.getZ() + 0.5D - camera.z);
                pose.mulPose(client.getEntityRenderDispatcher().cameraOrientation());
                pose.scale(0.040F, -0.040F, 0.040F);
                font.drawInBatch(icon, -font.width(icon) * 0.5F, -4.0F, 0xFFFF5555, true,
                    pose.last().pose(), context.consumers(), Font.DisplayMode.SEE_THROUGH, 0, 0x00F000F0);
                if (AdminShowClient.isEnabled()) {
                    String label = "KILL";
                    font.drawInBatch(label, -font.width(label) * 0.5F, 9.0F, 0xFFFF9999, true,
                        pose.last().pose(), context.consumers(), Font.DisplayMode.SEE_THROUGH, 0, 0x00F000F0);
                }
                pose.popPose();
            }
        });
    }

    private static void update(Minecraft client) {
        tickCounter++;
        if (client.player == null || client.level == null || !client.player.isCreative()) {
            VISIBLE.clear();
            return;
        }
        if (tickCounter % 10 != 0) {
            return;
        }
        BlockPos center = client.player.blockPosition();
        List<BlockPos> found = new ArrayList<>();
        int px = center.getX();
        int py = center.getY();
        int pz = center.getZ();
        for (int x = px - HORIZONTAL_RADIUS; x <= px + HORIZONTAL_RADIUS; x++) {
            for (int z = pz - HORIZONTAL_RADIUS; z <= pz + HORIZONTAL_RADIUS; z++) {
                if (!client.level.hasChunk(x >> 4, z >> 4)) {
                    continue;
                }
                for (int y = Math.max(client.level.getMinBuildHeight(), py - VERTICAL_RADIUS); y <= Math.min(client.level.getMaxBuildHeight() - 1, py + VERTICAL_RADIUS); y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (client.level.getBlockState(pos).is(ModBlocks.KILL)) {
                        found.add(pos);
                    }
                }
            }
        }
        found.sort(Comparator.comparingDouble(pos -> pos.distSqr(center)));
        VISIBLE.clear();
        VISIBLE.addAll(found.subList(0, Math.min(MAX_MARKERS, found.size())));
    }
}
