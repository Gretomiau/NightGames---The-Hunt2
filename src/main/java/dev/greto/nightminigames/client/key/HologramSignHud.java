package dev.greto.nightminigames.client.key;

import dev.greto.nightminigames.key.KeyColor;
import dev.greto.nightminigames.client.HudScale;
import dev.greto.nightminigames.key.KeySpawnerPlacementPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class HologramSignHud {
    private static final long DISPLAY_NANOS = 5_000_000_000L;
    private static final int MAX_VISIBLE = 6;
    private static final Map<Long, Notice> NOTICES = new HashMap<>();
    private static boolean initialized;

    private HologramSignHud() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        ClientPlayNetworking.registerGlobalReceiver(KeySpawnerPlacementPayload.TYPE, (payload, context) ->
            context.client().execute(() -> {
                KeyColor color = colorOf(payload.color());
                NOTICES.put(payload.pos().asLong(), new Notice(payload.pos().immutable(), color, System.nanoTime()));
            })
        );
        HudRenderCallback.EVENT.register(HologramSignHud::render);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> NOTICES.clear());
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) {
            NOTICES.clear();
            return;
        }
        long now = System.nanoTime();
        NOTICES.values().removeIf(notice -> now - notice.createdAt() >= DISPLAY_NANOS);
        if (NOTICES.isEmpty() || !client.player.isCreative() || client.options.hideGui || client.screen != null) {
            return;
        }
        Vec3 cameraPos = client.gameRenderer.getMainCamera().getPosition();
        Quaternionf cameraInverse = new Quaternionf(client.gameRenderer.getMainCamera().rotation()).conjugate();
        float fov = client.options.fov().get();
        float focalLength = (float) (graphics.guiHeight() * 0.5D / Math.tan(Math.toRadians(fov) * 0.5D));
        ArrayList<Notice> nearby = new ArrayList<>();
        for (Notice notice : NOTICES.values()) {
            if (notice.pos().distToCenterSqr(cameraPos) <= 2304.0D) {
                nearby.add(notice);
            }
        }
        nearby.sort(Comparator.comparingDouble(entry -> entry.pos().distToCenterSqr(cameraPos)));
        int drawn = 0;
        for (Notice notice : nearby) {
            if (drawn >= MAX_VISIBLE) {
                break;
            }
            Vec3 target = Vec3.atCenterOf(notice.pos()).add(0.0D, 1.65D, 0.0D);
            Vec3 delta = target.subtract(cameraPos);
            Vector3f local = new Vector3f((float) delta.x, (float) delta.y, (float) delta.z).rotate(cameraInverse);
            float forward = -local.z;
            if (forward <= 0.4F) {
                continue;
            }
            float screenX = graphics.guiWidth() * 0.5F + local.x * focalLength / forward;
            float screenY = graphics.guiHeight() * 0.5F - local.y * focalLength / forward;
            if (screenX < -80.0F || screenX > graphics.guiWidth() + 80.0F
                || screenY < -30.0F || screenY > graphics.guiHeight() + 30.0F) {
                continue;
            }
            var hit = client.level.clip(new ClipContext(cameraPos, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, client.player));
            if (hit.getType() != HitResult.Type.MISS && !hit.getBlockPos().equals(notice.pos())
                && hit.getLocation().distanceToSqr(cameraPos) + 0.09D < target.distanceToSqr(cameraPos)) {
                continue;
            }
            double age = (now - notice.createdAt()) * 1.0E-9D;
            float fadeIn = smooth((float) (age / 0.22D));
            float fadeOut = 1.0F - smooth((float) ((age - 4.35D) / 0.65D));
            float opacity = Mth.clamp(Math.min(fadeIn, fadeOut), 0.0F, 1.0F);
            float distance = (float) delta.length();
            float scale = HudScale.reference(Mth.clamp(6.0F / Math.max(3.0F, distance), 0.7F, 1.5F));
            float motion = HudScale.reference((1.0F - fadeIn) * 9.0F + (float) Math.sin(age * 4.1D) * 2.0F);
            graphics.pose().pushPose();
            graphics.pose().translate(screenX, screenY + motion, 0.0F);
            graphics.pose().scale(scale, scale, 1.0F);
            int strong = rgba(notice.color().rgb, Math.round(255.0F * opacity));
            int glow = rgba(notice.color().rgb, Math.round(85.0F * opacity));
            String label = "◆ LLAVE " + notice.color().display.toUpperCase(Locale.ROOT) + " ◆";
            graphics.drawCenteredString(client.font, label, 1, -1, glow);
            graphics.drawCenteredString(client.font, label, -1, 1, glow);
            graphics.drawCenteredString(client.font, label, 0, 0, strong);
            graphics.drawCenteredString(client.font, "SPAWNER COLOCADO", 0, 14, rgba(0xE0F6FF, Math.round(240.0F * opacity)));
            graphics.pose().popPose();
            drawn++;
        }
    }

    private static int rgba(int rgb, int alpha) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (rgb & 0xFFFFFF);
    }

    private static float smooth(float value) {
        float t = Mth.clamp(value, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private static KeyColor colorOf(String value) {
        for (KeyColor color : KeyColor.values()) {
            if (color.id.equals(value)) {
                return color;
            }
        }
        return KeyColor.BLUE;
    }

    private record Notice(BlockPos pos, KeyColor color, long createdAt) {
    }
}
