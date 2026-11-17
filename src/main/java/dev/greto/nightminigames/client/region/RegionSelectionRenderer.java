package dev.greto.nightminigames.client.region;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.greto.nightminigames.client.admin.AdminShowClient;
import dev.greto.nightminigames.module.ModuleSystem;
import dev.greto.nightminigames.region.RegionKind;
import dev.greto.nightminigames.region.RegionSelectionWandItem;
import dev.greto.nightminigames.region.RegionSyncPayload;
import dev.greto.nightminigames.team.TeamBinding;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

public final class RegionSelectionRenderer {
    private static final Map<String, RegionSyncPayload> REGIONS = new HashMap<>();
    private static boolean initialized;

    private RegionSelectionRenderer() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        ClientPlayNetworking.registerGlobalReceiver(RegionSyncPayload.TYPE, (payload, context) ->
            context.client().execute(() -> REGIONS.put(payload.team() + ":" + payload.region(), payload)));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> REGIONS.clear());
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null || client.level == null || !client.player.hasPermissions(2)
                || (!AdminShowClient.isEnabled() && !(client.player.getMainHandItem().getItem() instanceof RegionSelectionWandItem))
                || context.matrixStack() == null || context.consumers() == null) {
                return;
            }
            String selected = client.player.getMainHandItem().getItem() instanceof RegionSelectionWandItem
                ? TeamBinding.of(client.player.getMainHandItem()) : AdminShowClient.teamFilter();
            PoseStack poseStack = context.matrixStack();
            Vec3 camera = context.camera().getPosition();
            poseStack.pushPose();
            poseStack.translate(-camera.x, -camera.y, -camera.z);
            var vertex = context.consumers().getBuffer(RenderType.lines());
            for (String team : ModuleSystem.TEAMS) {
                if (!selected.isEmpty() && !selected.equals(team)) {
                    continue;
                }
                int rgb = TeamBinding.rgb(team);
                float r = ((rgb >>> 16) & 255) / 255.0F;
                float g = ((rgb >>> 8) & 255) / 255.0F;
                float b = (rgb & 255) / 255.0F;
                for (RegionKind kind : RegionKind.values()) {
                    RegionSyncPayload data = REGIONS.get(team + ":" + kind.id);
                    if (data == null || !data.dimension().equals(client.level.dimension().location().toString())) {
                        continue;
                    }
                    if (data.hasFirst() && data.hasSecond()) {
                        renderBox(poseStack, vertex, data.first(), data.second(), r, g, b, kind == RegionKind.ENTRADA ? 0.98F : 0.60F);
                    }
                    if (data.hasFirst()) {
                        renderMarker(poseStack, vertex, data.first(), r, g, b);
                    }
                    if (data.hasSecond()) {
                        renderMarker(poseStack, vertex, data.second(), r, g, b);
                    }
                }
            }
            poseStack.popPose();
        });
    }

    private static void renderBox(PoseStack stack, com.mojang.blaze3d.vertex.VertexConsumer vertex, BlockPos first, BlockPos second, float r, float g, float b, float alpha) {
        double x1 = Math.min(first.getX(), second.getX());
        double y1 = Math.min(first.getY(), second.getY());
        double z1 = Math.min(first.getZ(), second.getZ());
        double x2 = Math.max(first.getX(), second.getX()) + 1.0D;
        double y2 = Math.max(first.getY(), second.getY()) + 1.0D;
        double z2 = Math.max(first.getZ(), second.getZ()) + 1.0D;
        LevelRenderer.renderLineBox(stack, vertex, new AABB(x1, y1, z1, x2, y2, z2), r, g, b, alpha);
    }

    private static void renderMarker(PoseStack stack, com.mojang.blaze3d.vertex.VertexConsumer vertex, BlockPos pos, float r, float g, float b) {
        LevelRenderer.renderLineBox(stack, vertex, new AABB(pos).inflate(0.08D), r, g, b, 1.0F);
    }
}
