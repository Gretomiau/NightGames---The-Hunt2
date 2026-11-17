package dev.greto.nightminigames.client.environment;

import dev.greto.nightminigames.registry.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public final class AmbientLoopSound extends AbstractTickableSoundInstance {
    public AmbientLoopSound(float volume) {
        super(ModSounds.AMBIENT_LOOP, SoundSource.MASTER, RandomSource.create());
        this.looping = true;
        this.delay = 0;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.relative = true;
        this.pitch = 1.0F;
        setRuntimeVolume(volume);
    }

    @Override
    public void tick() {
    }

    public void setRuntimeVolume(float value) {
        this.volume = Math.clamp(value, 0.0F, 1.0F);
    }
}
