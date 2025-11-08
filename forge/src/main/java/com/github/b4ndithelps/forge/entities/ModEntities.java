package com.github.b4ndithelps.forge.entities;

import com.github.b4ndithelps.BanditsQuirkLib;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, BanditsQuirkLib.MOD_ID);

    public static final RegistryObject<EntityType<WindProjectileEntity>> WIND_PROJECTILE =
            ENTITY_TYPES.register("wind_projectile", () -> EntityType.Builder.<WindProjectileEntity>of(WindProjectileEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F) // Small projectile size
                    .clientTrackingRange(64) // How far away clients can see it
                    .updateInterval(1) // Update every tick for smooth movement
                    .build("wind_projectile"));

    public static final RegistryObject<EntityType<BetterWallProjectileEntity>> BETTER_WALL_PROJECTILE =
            ENTITY_TYPES.register("better_wall_projectile", () ->
                    EntityType.Builder.<BetterWallProjectileEntity>of(BetterWallProjectileEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F) // Default size
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("better_wall_projectile"));

    public static final RegistryObject<EntityType<BlockStackEntity>> BLOCK_STACK =
            ENTITY_TYPES.register("block_stack", () ->
                    EntityType.Builder.<BlockStackEntity>of(BlockStackEntity::new, MobCategory.MISC)
                            .sized(1.0F, 3.0F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("block_stack"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}