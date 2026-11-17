package dev.greto.nightminigames.client.key;

import dev.greto.nightminigames.client.VeilHudPainter;
import dev.greto.nightminigames.client.HudScale;
import dev.greto.nightminigames.client.VeilHudShader;
import dev.greto.nightminigames.key.KeyColor;
import dev.greto.nightminigames.key.KeyHoldPayload;
import dev.greto.nightminigames.key.KeyPickupPayload;
import dev.greto.nightminigames.key.KeySpawnBlock;
import dev.greto.nightminigames.key.KeySpawnerWandItem;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Locale;

public final class KeyHoldClient {
    private static BlockPos active;
    private static KeyColor activeColor;
    private static int startTick;
    private static int ticks;
    private static long completedAt;
    private static String completedColor;
    private static boolean initialized;

    private KeyHoldClient() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (!level.isClientSide() || hand != InteractionHand.MAIN_HAND || player.getMainHandItem().getItem() instanceof KeySpawnerWandItem) {
                return InteractionResult.PASS;
            }
            if (level.getBlockState(hit.getBlockPos()).getBlock() instanceof KeySpawnBlock) {
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });
        ClientTickEvents.END_CLIENT_TICK.register(KeyHoldClient::tick);
        HudRenderCallback.EVENT.register(KeyHoldClient::render);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
        ClientPlayNetworking.registerGlobalReceiver(KeyPickupPayload.TYPE, (payload, context) -> context.client().execute(() -> {
            completedAt = System.nanoTime();
            completedColor = payload.color();
            active = null;
            activeColor = null;
        }));
    }

    private static void tick(Minecraft client) {
        if (client.level == null || client.player == null || client.screen != null) {
            stop();
            return;
        }
        BlockPos target = null;
        KeyColor color = null;
        if (client.options.keyUse.isDown() && !(client.player.getMainHandItem().getItem() instanceof KeySpawnerWandItem)
            && client.hitResult instanceof BlockHitResult hit && client.player.getEyePosition().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(hit.getBlockPos())) <= 25.0D) {
            var state = client.level.getBlockState(hit.getBlockPos());
            if (state.getBlock() instanceof KeySpawnBlock spawn && !state.getValue(KeySpawnBlock.COLLECTED)) {
                target = hit.getBlockPos();
                color = spawn.color;
            }
        }
        if (target == null) {
            stop();
            return;
        }
        if (!target.equals(active)) {
            stop();
            active = target.immutable();
            activeColor = color;
            startTick = client.player.tickCount;
            ticks = 0;
            ClientPlayNetworking.send(new KeyHoldPayload(active, true));
        } else {
            ticks = client.player.tickCount - startTick;
            if (ticks % 5 == 0) {
                ClientPlayNetworking.send(new KeyHoldPayload(active, true));
            }
        }
    }

    private static void stop() {
        if (active != null && ClientPlayNetworking.canSend(KeyHoldPayload.TYPE)) {
            ClientPlayNetworking.send(new KeyHoldPayload(active, false));
        }
        active = null;
        activeColor = null;
        ticks = 0;
    }

    private static void reset() {
        active = null;
        activeColor = null;
        ticks = 0;
        completedAt = 0L;
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.screen != null || client.options.hideGui) {
            return;
        }
        boolean success = System.nanoTime() - completedAt < 650_000_000L;
        if (active == null && !success) {
            return;
        }
        int rgb = success ? colorOf(completedColor) : activeColor.rgb;
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        int panelWidth = 258;
        int panelHeight = 63;
        float scale = HudScale.fit(graphics, panelWidth, panelHeight, 18.0F, 190.0F);
        float px = (w - panelWidth * scale) / 2.0F;
        float py = Math.max(HudScale.pixels(8.0F), h - HudScale.reference(84.0F) - panelHeight * scale);
        double time = System.nanoTime() * 1.0E-9D;
        float progress = success ? 1.0F : Mth.clamp((ticks + deltaTracker.getGameTimeDeltaPartialTick(true)) / 120.0F, 0.0F, 1.0F);
        boolean veilBackground = VeilHudShader.draw(graphics, px, py, panelWidth * scale, panelHeight * scale, rgb, 1.0F, progress, 1, time);
        graphics.pose().pushPose();
        graphics.pose().translate(px, py, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        if (!veilBackground) {
            VeilHudPainter.drawPanel(graphics, 0, 0, panelWidth, panelHeight, rgb, 1.0F, time);
        }
        if (!veilBackground) {
            VeilHudPainter.drawEnergyOrb(graphics, 30, 31, 11, rgb, 1.0F, time);
            VeilHudPainter.drawDropletBurst(graphics, 30, 31, 13, rgb, 0.75F, time);
        }
        String label = success ? "LLAVE RECUPERADA" : "EXTRAYENDO LLAVE " + activeColor.display.toUpperCase(Locale.ROOT);
        graphics.drawString(client.font, label, 52, 10, VeilHudPainter.rgba(0xF0FBFF, 255), false);
        graphics.drawString(client.font, success ? "TRANSFERENCIA COMPLETA" : "MANTEN CLIC DERECHO PARA EXTRAER", 52, 22, VeilHudPainter.rgba(0xB8DEE8, 178), false);
        VeilHudPainter.drawSegmentBar(graphics, 18, 37, panelWidth - 36, 11, progress, rgb, 1.0F, time);
        graphics.drawCenteredString(client.font, success ? "100%  COMPLETADO" : Math.round(progress * 100.0F) + "%", panelWidth / 2, 50, VeilHudPainter.rgba(0xD8FBFF, 255));
        graphics.pose().popPose();
    }

    private static int colorOf(String id) {
        for (KeyColor color : KeyColor.values()) {
            if (color.id.equals(id)) {
                return color.rgb;
            }
        }
        return 0x55EBFF;
    }
}
