package com.github.b4ndithelps.forge.capabilities.genome;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class GenomeCapabilityProvider implements ICapabilitySerializable<CompoundTag> {
    public static final Capability<IGenomeCapability> GENOME_CAPABILITY = CapabilityManager.get(new CapabilityToken<IGenomeCapability>(){});

    private IGenomeCapability capability = null;
    private final LazyOptional<IGenomeCapability> lazyCapability = LazyOptional.of(this::createCapability);
    private final Supplier<Player> playerSupplier;
    private final boolean enableAutoSync;

    public GenomeCapabilityProvider(Supplier<Player> playerSupplier) {
        this.playerSupplier = playerSupplier;
        this.enableAutoSync = true;
    }

    public GenomeCapabilityProvider() {
        this.playerSupplier = null;
        this.enableAutoSync = false;
    }

    private IGenomeCapability createCapability() {
        if (this.capability == null) {
            if (enableAutoSync && playerSupplier != null) {
                this.capability = new SyncedGenomeCapability(playerSupplier);
            } else {
                this.capability = new GenomeCapability();
            }
        }
        return this.capability;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == GENOME_CAPABILITY) return lazyCapability.cast();
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() { return createCapability().serializeNBT(); }

    @Override
    public void deserializeNBT(CompoundTag nbt) { createCapability().deserializeNBT(nbt); }

    public void invalidate() { lazyCapability.invalidate(); }
}