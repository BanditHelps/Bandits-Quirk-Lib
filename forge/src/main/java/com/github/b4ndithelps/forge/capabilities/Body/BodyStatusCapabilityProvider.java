package com.github.b4ndithelps.forge.capabilities.Body;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.github.b4ndithelps.values.BodyConstants.MAX_DAMAGE;

/**
 * Provider for the Body Status capability. Follows the same pattern as StaminaDataProvider
 * for consistency across the mod's capability systems.
 */
public class BodyStatusCapabilityProvider implements ICapabilitySerializable<CompoundTag> {
    public static final Capability<IBodyStatusCapability> BODY_STATUS_CAPABILITY =
            CapabilityManager.get(new CapabilityToken<IBodyStatusCapability>(){});

    private IBodyStatusCapability capability = null;
    private final LazyOptional<IBodyStatusCapability> lazyCapability = LazyOptional.of(this::createCapability);

    private IBodyStatusCapability createCapability() {
        if (this.capability == null) {
            this.capability = new BodyStatusCapability(MAX_DAMAGE);
        }
        return this.capability;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == BODY_STATUS_CAPABILITY) {
            return lazyCapability.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return createCapability().serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        createCapability().deserializeNBT(nbt);
    }

    /**
     * Invalidates the capability. Should be called when the capability is no longer needed.
     */
    public void invalidate() {
        lazyCapability.invalidate();
    }
}
