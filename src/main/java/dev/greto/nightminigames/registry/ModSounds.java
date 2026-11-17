package dev.greto.nightminigames.registry;

import dev.greto.nightminigames.NightminigamesMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
    public static final SoundEvent FLASHLIGHT_TOGGLE = Registry.register(
        BuiltInRegistries.SOUND_EVENT,
        NightminigamesMod.id("flashlight_toggle"),
        SoundEvent.createVariableRangeEvent(NightminigamesMod.id("flashlight_toggle"))
    );

    public static final SoundEvent AMBIENT_LOOP = Registry.register(
        BuiltInRegistries.SOUND_EVENT,
        NightminigamesMod.id("ambient_loop"),
        SoundEvent.createVariableRangeEvent(NightminigamesMod.id("ambient_loop"))
    );

    public static final SoundEvent POWER_OFF = Registry.register(
        BuiltInRegistries.SOUND_EVENT,
        NightminigamesMod.id("power_off"),
        SoundEvent.createVariableRangeEvent(NightminigamesMod.id("power_off"))
    );

    public static final SoundEvent POWER_ON = Registry.register(
        BuiltInRegistries.SOUND_EVENT,
        NightminigamesMod.id("power_on"),
        SoundEvent.createVariableRangeEvent(NightminigamesMod.id("power_on"))
    );

    public static final SoundEvent KEY_COLLECT = Registry.register(
        BuiltInRegistries.SOUND_EVENT,
        NightminigamesMod.id("key_collect"),
        SoundEvent.createVariableRangeEvent(NightminigamesMod.id("key_collect"))
    );

    public static final SoundEvent KEY_GLOBAL_CONFIRMATION = Registry.register(
        BuiltInRegistries.SOUND_EVENT,
        NightminigamesMod.id("key_global_confirmation"),
        SoundEvent.createVariableRangeEvent(NightminigamesMod.id("key_global_confirmation"))
    );

    public static final SoundEvent KEY_HOLOGRAM_WHOOSH = Registry.register(
        BuiltInRegistries.SOUND_EVENT,
        NightminigamesMod.id("key_hologram_whoosh"),
        SoundEvent.createVariableRangeEvent(NightminigamesMod.id("key_hologram_whoosh"))
    );

    public static final SoundEvent MACHINE_INSERT = register("machine_insert");
    public static final SoundEvent MACHINE_LOCK = register("machine_lock");
    public static final SoundEvent MACHINE_CLICK = register("machine_click");
    public static final SoundEvent MACHINE_ALARM = register("machine_alarm");

    private static SoundEvent register(String name) {
        return Registry.register(BuiltInRegistries.SOUND_EVENT, NightminigamesMod.id(name), SoundEvent.createVariableRangeEvent(NightminigamesMod.id(name)));
    }

    public static final SoundEvent DOOR_OPEN = Registry.register(
        BuiltInRegistries.SOUND_EVENT,
        NightminigamesMod.id("door_open"),
        SoundEvent.createVariableRangeEvent(NightminigamesMod.id("door_open"))
    );
    public static final SoundEvent DOOR_CLOSE = Registry.register(
        BuiltInRegistries.SOUND_EVENT,
        NightminigamesMod.id("door_close"),
        SoundEvent.createVariableRangeEvent(NightminigamesMod.id("door_close"))
    );

    private ModSounds() {
    }

    public static void initialize() {
    }
}
