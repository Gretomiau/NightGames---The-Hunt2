package dev.greto.nightminigames.registry;

import dev.greto.nightminigames.NightminigamesMod;
import dev.greto.nightminigames.flashlight.FlashlightSpawnBlock;
import dev.greto.nightminigames.key.KeyColor;
import dev.greto.nightminigames.key.KeySpawnBlock;
import dev.greto.nightminigames.machine.MachineBlock;
import dev.greto.nightminigames.kill.KillBlock;
import dev.greto.nightminigames.door.DoorBlock;
import dev.greto.nightminigames.door.DoorGhostBlock;
import dev.greto.nightminigames.region.RegionKind;
import dev.greto.nightminigames.machine.MachinePartBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public final class ModBlocks {
    public static final KeySpawnBlock SPAWN_BLUE = register(KeyColor.BLUE);
    public static final KeySpawnBlock SPAWN_YELLOW = register(KeyColor.YELLOW);
    public static final KeySpawnBlock SPAWN_RED = register(KeyColor.RED);
    public static final KeySpawnBlock SPAWN_GREEN = register(KeyColor.GREEN);

    public static final FlashlightSpawnBlock FLASHLIGHT_SPAWN = Registry.register(
        BuiltInRegistries.BLOCK, NightminigamesMod.id("flashlight_spawn"),
        new FlashlightSpawnBlock(BlockBehaviour.Properties.of().strength(-1.0F, 3600000.0F).noOcclusion().noLootTable())
    );

    public static final KillBlock KILL = Registry.register(
        BuiltInRegistries.BLOCK, NightminigamesMod.id("kill"),
        new KillBlock(BlockBehaviour.Properties.of().strength(-1.0F, 3600000.0F).noCollission().noOcclusion().noLootTable())
    );

    public static final MachineBlock MACHINE = Registry.register(
        BuiltInRegistries.BLOCK, NightminigamesMod.id("machine"),
        new MachineBlock(BlockBehaviour.Properties.of().strength(4.0F).noOcclusion())
    );
    public static final MachinePartBlock MACHINE_PART = Registry.register(
        BuiltInRegistries.BLOCK, NightminigamesMod.id("machine_part"),
        new MachinePartBlock(BlockBehaviour.Properties.of().strength(4.0F).noOcclusion())
    );

    public static final DoorBlock DOOR_ENTRANCE = Registry.register(
        BuiltInRegistries.BLOCK, NightminigamesMod.id("door_entrance"),
        new DoorBlock(RegionKind.ENTRADA, BlockBehaviour.Properties.of().strength(5.0F).noOcclusion())
    );
    public static final DoorBlock DOOR_EXIT = Registry.register(
        BuiltInRegistries.BLOCK, NightminigamesMod.id("door_exit"),
        new DoorBlock(RegionKind.SALIDA, BlockBehaviour.Properties.of().strength(5.0F).noOcclusion())
    );
    public static final DoorGhostBlock DOOR_GHOST = Registry.register(
        BuiltInRegistries.BLOCK, NightminigamesMod.id("door_ghost"),
        new DoorGhostBlock(BlockBehaviour.Properties.of().strength(-1.0F, 3600000.0F).noOcclusion().noLootTable())
    );

    private ModBlocks() {
    }

    private static KeySpawnBlock register(KeyColor color) {
        return Registry.register(BuiltInRegistries.BLOCK, NightminigamesMod.id("key_spawn_" + color.id), new KeySpawnBlock(color));
    }

    public static KeySpawnBlock byColor(KeyColor color) {
        return switch (color) {
            case BLUE -> SPAWN_BLUE;
            case YELLOW -> SPAWN_YELLOW;
            case RED -> SPAWN_RED;
            case GREEN -> SPAWN_GREEN;
        };
    }

    public static void initialize() {
    }
}
