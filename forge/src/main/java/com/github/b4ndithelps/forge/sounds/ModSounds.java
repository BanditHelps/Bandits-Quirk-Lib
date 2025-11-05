package com.github.b4ndithelps.forge.sounds;

import com.github.b4ndithelps.BanditsQuirkLib;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, BanditsQuirkLib.MOD_ID);
    public static final RegistryObject<SoundEvent> HEARTBEAT =
            SOUND_EVENTS.register("heartbeat",
                    () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(BanditsQuirkLib.MOD_ID, "heartbeat")));
    public static final RegistryObject<SoundEvent> EXPLOSION_CHARGE =
            SOUND_EVENTS.register("explosion_charge",
                    () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(BanditsQuirkLib.MOD_ID, "explosion_charge")));
}
