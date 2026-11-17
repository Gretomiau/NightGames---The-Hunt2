package dev.greto.nightminigames.client;

import net.fabricmc.api.ClientModInitializer;
import dev.greto.nightminigames.client.key.KeyHoldClient;
import dev.greto.nightminigames.client.admin.AdminShowClient;
import dev.greto.nightminigames.client.flashlight.FlashlightSpawnRenderer;
import dev.greto.nightminigames.client.machine.MachineRenderer;
import dev.greto.nightminigames.client.key.KeyHologramRenderer;
import dev.greto.nightminigames.registry.ModBlockEntities;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import dev.greto.nightminigames.client.key.ColoredKeyClient;
import dev.greto.nightminigames.client.environment.EnvironmentClientSystem;
import dev.greto.nightminigames.client.region.RegionSelectionRenderer;
import dev.greto.nightminigames.client.door.DoorRenderer;
import dev.greto.nightminigames.client.kill.KillBlockOverlayRenderer;

public final class NightminigamesClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        AdminShowClient.initialize();
        ColoredKeyClient.initialize();
        RegionSelectionRenderer.initialize();
        BlockEntityRendererRegistry.register(ModBlockEntities.KEY_SPAWNER, KeyHologramRenderer::new);
        BlockEntityRendererRegistry.register(ModBlockEntities.FLASHLIGHT_SPAWN, FlashlightSpawnRenderer::new);
        BlockEntityRendererRegistry.register(ModBlockEntities.MACHINE, MachineRenderer::new);
        BlockEntityRendererRegistry.register(ModBlockEntities.DOOR, DoorRenderer::new);
        KillBlockOverlayRenderer.initialize();
        GlobalEventFeedHud.initialize();
        ClassificationFeedHud.initialize();
        dev.greto.nightminigames.client.key.HologramSignHud.initialize();
        KeyHoldClient.initialize();
        dev.greto.nightminigames.client.key.KeyAnnouncementHud.initialize();
        dev.greto.nightminigames.client.machine.MachineProgressHud.initialize();
        dev.greto.nightminigames.client.machine.MachineCompleteVignette.initialize();
        dev.greto.nightminigames.client.machine.MachineCompletionTitleHud.initialize();
        FlashlightClientSystem.initialize();
        FlashlightChargeHud.initialize();
        EnvironmentClientSystem.initialize();
        dev.greto.nightminigames.client.terror.TerrorPreviewRenderer.initialize();
    }
}
