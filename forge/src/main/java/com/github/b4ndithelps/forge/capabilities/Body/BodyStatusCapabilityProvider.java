package com.github.b4ndithelps.forge.capabilities.Body;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;

public class BodyStatusCapabilityProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    public static final Capability<IBodyStatusCapability> BODY_STATUS_CAPABILITY =
            CapabilityManager.get(new CapabilityToken<IBodyStatusCapability>(){});

    private final IBodyStatusCapability capability;
    private final LazyOptional<IBodyStatusCapability> lazyCapability;

    public BodyStatusCapabilityProvider() {
        this.capability = new BodyStatusCapability(100.0f);
        this.lazyCapability = LazyOptional.of(() -> capability);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == BODY_STATUS_CAPABILITY) {
            return lazyCapability.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return capability.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        capability.deserializeNBT(nbt);
    }
}
