package com.github.b4ndithelps.forge.capabilities;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This is a forge Capability, used to help manage and store data for the mod.
 * I guess this is also supposed to stay synced between client and server
 */
public class StaminaDataProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    public static Capability<IStaminaData> STAMINA_DATA = CapabilityManager.get(new CapabilityToken<IStaminaData>() {});

    private IStaminaData staminaData = null;
    private final LazyOptional<IStaminaData> optional = LazyOptional.of(this::createStaminaData);

    private IStaminaData createStaminaData() {
        if (this.staminaData == null) {
            this.staminaData = new StaminaData();
        }
        return this.staminaData;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction arg) {
        if (capability == STAMINA_DATA) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        createStaminaData().saveNBTData(nbt);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        createStaminaData().loadNBTData(nbt);
    }
}
