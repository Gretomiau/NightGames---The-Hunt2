package dev.greto.nightminigames.registry;

import dev.greto.nightminigames.NightminigamesMod;
import dev.greto.nightminigames.flashlight.FlashlightSpawnBlockEntity;
import dev.greto.nightminigames.key.KeySpawnBlockEntity;
import dev.greto.nightminigames.machine.MachineBlockEntity;
import dev.greto.nightminigames.door.DoorBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
    public static final BlockEntityType<KeySpawnBlockEntity> KEY_SPAWNER = Registry.register(
        BuiltInRegistries.BLOCK_ENTITY_TYPE,
        NightminigamesMod.id("key_spawner"),
        FabricBlockEntityTypeBuilder.create(KeySpawnBlockEntity::new,
            ModBlocks.SPAWN_BLUE, ModBlocks.SPAWN_YELLOW, ModBlocks.SPAWN_RED, ModBlocks.SPAWN_GREEN).build()
    );

    public static final BlockEntityType<FlashlightSpawnBlockEntity> FLASHLIGHT_SPAWN = Registry.register(
        BuiltInRegistries.BLOCK_ENTITY_TYPE,
        NightminigamesMod.id("flashlight_spawn"),
        FabricBlockEntityTypeBuilder.create(FlashlightSpawnBlockEntity::new, ModBlocks.FLASHLIGHT_SPAWN).build()
    );

    public static final BlockEntityType<MachineBlockEntity> MACHINE = Registry.register(
        BuiltInRegistries.BLOCK_ENTITY_TYPE,
        NightminigamesMod.id("machine"),
        FabricBlockEntityTypeBuilder.create(MachineBlockEntity::new, ModBlocks.MACHINE).build()
    );

    public static final BlockEntityType<DoorBlockEntity> DOOR = Registry.register(
        BuiltInRegistries.BLOCK_ENTITY_TYPE,
        NightminigamesMod.id("door"),
        FabricBlockEntityTypeBuilder.create(DoorBlockEntity::new, ModBlocks.DOOR_ENTRANCE, ModBlocks.DOOR_EXIT).build()
    );

    private ModBlockEntities() {
    }

    public static void initialize() {
    }
}
