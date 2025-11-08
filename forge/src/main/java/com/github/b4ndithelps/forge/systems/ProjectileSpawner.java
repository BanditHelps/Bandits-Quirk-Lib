package com.github.b4ndithelps.forge.systems;

import com.github.b4ndithelps.forge.entities.BetterWallProjectileEntity;
import com.github.b4ndithelps.forge.entities.ModEntities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class ProjectileSpawner {
    public static BetterWallProjectileEntity spawnWallProjectile(Level level, Player shooter, float width, float height, int lifetime, float knockback) {
        if (level.isClientSide) return null;

        BetterWallProjectileEntity projectile = new BetterWallProjectileEntity(ModEntities.BETTER_WALL_PROJECTILE.get(), level, shooter, width, height, knockback);
        projectile.setLifetime(lifetime);

        level.addFreshEntity(projectile);

        return projectile;
    }

    // Summon wall projectile with default lifetime (100 ticks)
    public static BetterWallProjectileEntity spawnWallProjectile(Level level, Player shooter, float width, float height, float knockback) {
        return spawnWallProjectile(level, shooter, width, height, 100);
    }
}