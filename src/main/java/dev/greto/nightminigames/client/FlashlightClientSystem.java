package dev.greto.nightminigames.client;

import dev.greto.nightminigames.flashlight.FlashlightLightingProfile;
import dev.greto.nightminigames.flashlight.FlashlightData;
import dev.greto.nightminigames.registry.ModItems;
import dev.greto.nightminigames.flashlight.FlashlightStatePayload;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.light.data.AreaLightData;
import foundry.veil.api.client.render.light.renderer.LightRenderHandle;
import foundry.veil.api.event.VeilRenderLevelStageEvent;
import foundry.veil.platform.VeilEventPlatform;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.ClipContext;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class FlashlightClientSystem {
    private static final Map<UUID, String> ACTIVE_PLAYERS = new HashMap<>();
    private static net.minecraft.client.multiplayer.ClientLevel lastLevel;
    private static final int MAX_VISIBLE_LIGHTS = 16;
    private static final Map<UUID, PlayerLights> LIGHTS = new HashMap<>();
    private static FlashlightLightingProfile profile = FlashlightLightingProfile.DEFAULT;
    private static int profileRevision;
    private static boolean initialized;

    private FlashlightClientSystem() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        ClientPlayNetworking.registerGlobalReceiver(FlashlightStatePayload.TYPE, (payload, context) -> context.client().execute(() -> setState(payload.playerId(), payload.dimension(), payload.active())));
        ClientPlayNetworking.registerGlobalReceiver(dev.greto.nightminigames.flashlight.FlashlightProfilePayload.TYPE, (payload, context) -> context.client().execute(() -> {
            profile = payload.profile();
            profileRevision++;
        }));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());
        VeilEventPlatform.INSTANCE.onVeilRenderLevelStage((stage, levelRenderer, bufferSource, poseStack, frustum, camera, cullingFrustum, deltaTracker, renderTickCounter, source) -> {
            if (stage == VeilRenderLevelStageEvent.Stage.AFTER_LEVEL) {
                renderFrame(Minecraft.getInstance(), deltaTracker.getGameTimeDeltaPartialTick(true));
            }
        });
    }

    public static FlashlightLightingProfile currentProfile() { return profile; }

    private static void setState(UUID playerId, String dimension, boolean active) {
        Minecraft client = Minecraft.getInstance();
        if (active && client.player != null && playerId.equals(client.player.getUUID())) {
            FlashlightChargeHud.onActivated();
        }
        if (active) {
            ACTIVE_PLAYERS.put(playerId, dimension);
        } else {
            ACTIVE_PLAYERS.remove(playerId);
            removeLights(playerId);
        }
    }

    private static void renderFrame(Minecraft client, float partialTick) {
        if (client.level != lastLevel) {
            clearLights();
            lastLevel = client.level;
        }
        if (client.level == null || client.player == null || ACTIVE_PLAYERS.isEmpty()) {
            clearLights();
            return;
        }
        String dimension = client.level.dimension().location().toString();
        java.util.ArrayList<Player> candidates = new java.util.ArrayList<>();
        for (Player player : client.level.players()) {
            if (!dimension.equals(ACTIVE_PLAYERS.get(player.getUUID()))) {
                continue;
            }
            if (player.isAlive() && (player == client.player || player.distanceToSqr(client.player) < 64.0D * 64.0D)) {
                candidates.add(player);
            }
        }
        candidates.sort(java.util.Comparator.comparingDouble(p -> p.distanceToSqr(client.player)));
        Set<UUID> visible = new HashSet<>();
        int count = 0;
        for (Player player : candidates) {
            if (count++ >= MAX_VISIBLE_LIGHTS) {
                break;
            }
            UUID playerId = player.getUUID();
            visible.add(playerId);
            PlayerLights playerLights = LIGHTS.computeIfAbsent(playerId, ignored -> createLights());
            ensureCurrentProfile(playerLights);
            updateLights(client, player, playerLights, partialTick);
        }
        ArrayList<UUID> stale = new ArrayList<>();
        for (UUID playerId : LIGHTS.keySet()) {
            if (!visible.contains(playerId)) {
                stale.add(playerId);
            }
        }
        for (UUID playerId : stale) {
            removeLights(playerId);
        }
    }

    private static PlayerLights createLights() {
        var renderer = VeilRenderSystem.renderer().getLightRenderer();
        LightRenderHandle<AreaLightData> core = renderer.addLight(new AreaLightData());
        LightRenderHandle<AreaLightData> spill = renderer.addLight(new AreaLightData());
        PlayerLights result = new PlayerLights(core, spill, profileRevision);
        applyProfile(result, profile);
        return result;
    }

    private static void ensureCurrentProfile(PlayerLights playerLights) {
        if (playerLights.profileRevision == profileRevision) {
            return;
        }
        applyProfile(playerLights, profile);
        playerLights.profileRevision = profileRevision;
    }

    private static void applyProfile(PlayerLights playerLights, FlashlightLightingProfile profile) {
        playerLights.core.getLightData()
            .setColor(1.0F, 0.945F, 0.824F)
            .setBrightness(profile.coreBrightness())
            .setSize(profile.coreSize(), profile.coreSize())
            .setAngle((float) Math.toRadians(profile.coreAngleDegrees()))
            .setDistance(profile.rangeBlocks())
            .setOcclusionEnabled(true);

        playerLights.spill.getLightData()
            .setColor(1.0F, 0.945F, 0.824F)
            .setBrightness(profile.spillBrightness())
            .setSize(profile.spillSize(), profile.spillSize())
            .setAngle((float) Math.toRadians(profile.spillAngleDegrees()))
            .setDistance(profile.rangeBlocks() * profile.spillRangeMultiplier())
            .setOcclusionEnabled(true);
    }

    private static void updateLights(Minecraft client, Player player, PlayerLights playerLights, float partialTick) {
        if (client.player != null && client.player.getUUID().equals(player.getUUID())
            && !client.gameRenderer.getMainCamera().isDetached()) {
            var camera = client.gameRenderer.getMainCamera();
            Vec3 cameraPosition = camera.getPosition();
            Vector3f cameraLook = camera.getLookVector();
            Vector3f cameraLeft = camera.getLeftVector();
            Vector3f cameraUp = camera.getUpVector();
            Vec3 forward = new Vec3(cameraLook.x(), cameraLook.y(), cameraLook.z()).normalize();
            boolean mainHandOn = player.getMainHandItem().is(ModItems.FLASHLIGHT)
                && FlashlightData.isEnabled(player.getMainHandItem());
            double handSide = mainHandOn ? -0.12D : 0.12D;
            Vec3 origin = cameraPosition
                .add(forward.scale(0.08D))
                .add(new Vec3(cameraLeft.x(), cameraLeft.y(), cameraLeft.z()).scale(handSide))
                .add(new Vec3(cameraUp.x(), cameraUp.y(), cameraUp.z()).scale(-0.12D));

            Vec3 rayEnd = cameraPosition.add(forward.scale(profile.rangeBlocks()));
            var hit = client.level.clip(new ClipContext(
                cameraPosition,
                rayEnd,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
            ));
            double hitDistance = hit.getType() == HitResult.Type.MISS
                ? profile.rangeBlocks()
                : cameraPosition.distanceTo(hit.getLocation());
            Vec3 aimPoint = hitDistance < 1.0D
                ? cameraPosition.add(forward.scale(1.0D))
                : hit.getType() == HitResult.Type.MISS ? rayEnd : hit.getLocation();
            Vec3 beamDirection = aimPoint.subtract(origin).normalize();
            Quaternionf orientation = new Quaternionf().lookAlong(
                new Vector3f((float) -beamDirection.x, (float) -beamDirection.y, (float) -beamDirection.z),
                new Vector3f(cameraUp)
            );

            float nearWallAttenuation = 1.0F;
            if (hit.getType() != HitResult.Type.MISS) {
                float proximity = (float) Math.clamp((hitDistance - 0.75D) / 5.0D, 0.0D, 1.0D);
                float smoothed = proximity * proximity * (3.0F - 2.0F * proximity);
                nearWallAttenuation = 0.26F + 0.74F * smoothed;
            }
            updateRemoteLight(playerLights.core.getLightData(), origin.x, origin.y, origin.z, orientation);
            updateRemoteLight(playerLights.spill.getLightData(), origin.x, origin.y, origin.z, orientation);
            playerLights.core.getLightData().setBrightness(profile.coreBrightness() * nearWallAttenuation);
            playerLights.spill.getLightData().setBrightness(profile.spillBrightness() * nearWallAttenuation);
            return;
        }
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 forward = player.getViewVector(partialTick).normalize();
        Vec3 worldUp = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 right = forward.cross(worldUp);
        if (right.lengthSqr() < 1.0E-5D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        }
        right = right.normalize();
        boolean mainHand = player.getMainHandItem().is(ModItems.FLASHLIGHT);
        Vec3 origin = eye.add(forward.scale(0.20D))
            .add(right.scale(mainHand ? 0.23D : -0.23D))
            .add(0.0D, -0.24D, 0.0D);
        Vec3 up = Math.abs(forward.y) > 0.98D ? right : worldUp;
        Quaternionf orientation = new Quaternionf().lookAlong(
            new Vector3f((float) -forward.x, (float) -forward.y, (float) -forward.z),
            new Vector3f((float) up.x, (float) up.y, (float) up.z)
        );
        updateRemoteLight(playerLights.core.getLightData(), origin.x, origin.y, origin.z, orientation);
        updateRemoteLight(playerLights.spill.getLightData(), origin.x, origin.y, origin.z, orientation);
        playerLights.core.getLightData().setBrightness(profile.coreBrightness());
        playerLights.spill.getLightData().setBrightness(profile.spillBrightness());
    }

    private static void updateRemoteLight(AreaLightData light, double x, double y, double z, Quaternionf orientation) {
        light.getPositionMutable().set(x, y, z);
        light.getOrientationMutable().set(orientation);
    }

    private static void clear() {
        ACTIVE_PLAYERS.clear();
        lastLevel = null;
        profile = FlashlightLightingProfile.DEFAULT;
        profileRevision++;
        clearLights();
    }

    private static void clearLights() {
        ArrayList<PlayerLights> existing = new ArrayList<>(LIGHTS.values());
        LIGHTS.clear();
        for (PlayerLights playerLights : existing) {
            playerLights.free();
        }
    }

    private static void removeLights(UUID playerId) {
        PlayerLights removed = LIGHTS.remove(playerId);
        if (removed != null) {
            removed.free();
        }
    }

    private static final class PlayerLights {
        private final LightRenderHandle<AreaLightData> core;
        private final LightRenderHandle<AreaLightData> spill;
        private int profileRevision;

        private PlayerLights(LightRenderHandle<AreaLightData> core, LightRenderHandle<AreaLightData> spill, int profileRevision) {
            this.core = core;
            this.spill = spill;
            this.profileRevision = profileRevision;
        }

        private void free() {
            core.free();
            spill.free();
        }
    }
}
