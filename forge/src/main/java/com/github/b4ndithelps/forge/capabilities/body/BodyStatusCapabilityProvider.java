package com.github.b4ndithelps.forge.capabilities.body;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Provider for the Body Status capability. Follows the same pattern as StaminaDataProvider
 * for consistency across the mod's capability systems.
 */
public class BodyStatusCapabilityProvider implements ICapabilitySerializable<CompoundTag> {
    public static final Capability<IBodyStatusCapability> BODY_STATUS_CAPABILITY =
            CapabilityManager.get(new CapabilityToken<IBodyStatusCapability>(){});

    private IBodyStatusCapability capability = null;
    private final LazyOptional<IBodyStatusCapability> lazyCapability = LazyOptional.of(this::createCapability);
    private final Supplier<Player> playerSupplier;
    private final boolean enableAutoSync;

    /**
     * Creates a provider with auto-sync enabled for server players.
     * @param playerSupplier Supplier to get the player for sync operations
     */
    public BodyStatusCapabilityProvider(Supplier<Player> playerSupplier) {
        this.playerSupplier = playerSupplier;
        this.enableAutoSync = true;
    }

    private IBodyStatusCapability createCapability() {
        if (this.capability == null) {
            if (enableAutoSync && playerSupplier != null) {
                this.capability = new SyncedBodyStatusCapability(playerSupplier);
            } else {
                this.capability = new BodyStatusCapability();
            }
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