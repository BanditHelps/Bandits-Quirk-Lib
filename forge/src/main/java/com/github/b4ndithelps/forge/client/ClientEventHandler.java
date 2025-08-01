package com.github.b4ndithelps.forge.client;

import com.github.b4ndithelps.BanditsQuirkLib;
import com.github.b4ndithelps.forge.entities.ModEntities;
import com.github.b4ndithelps.forge.entities.WindProjectileEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BanditsQuirkLib.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEventHandler {

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Register the wind projectile renderer - invisible renderer since particles handle visuals
        event.registerEntityRenderer(ModEntities.WIND_PROJECTILE.get(), WindProjectileRenderer::new);
    }

    // Custom renderer that doesn't render anything visible (particles handle the visuals)
    public static class WindProjectileRenderer extends EntityRenderer<WindProjectileEntity> {
        public WindProjectileRenderer(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public ResourceLocation getTextureLocation(WindProjectileEntity entity) {
            // Return a dummy texture location - won't be used since we don't render anything
            return new ResourceLocation(BanditsQuirkLib.MOD_ID, "textures/entity/wind_projectile.png");
        }

        // Don't render anything - particles handle the visuals
    }
}