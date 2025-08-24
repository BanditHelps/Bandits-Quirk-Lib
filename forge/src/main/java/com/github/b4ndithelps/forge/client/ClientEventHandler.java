package com.github.b4ndithelps.forge.client;

import com.github.b4ndithelps.BanditsQuirkLib;
import com.github.b4ndithelps.forge.client.renderer.entity.BetterWallProjectileRenderer;
import com.github.b4ndithelps.forge.entities.ModEntities;
import com.github.b4ndithelps.forge.entities.WindProjectileEntity;
import com.github.b4ndithelps.util.FileManager;
import com.github.b4ndithelps.forge.blocks.ModMenus;
import com.github.b4ndithelps.forge.client.DNASequencerScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class ClientEventHandler {

    @Mod.EventBusSubscriber(modid = BanditsQuirkLib.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModEvents {
        @SubscribeEvent
        public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
            // Register the wind projectile renderer - invisible renderer since particles handle visuals
            event.registerEntityRenderer(ModEntities.WIND_PROJECTILE.get(), WindProjectileRenderer::new);
            event.registerEntityRenderer(ModEntities.BETTER_WALL_PROJECTILE.get(), BetterWallProjectileRenderer::new);
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> MenuScreens.register(ModMenus.DNA_SEQUENCER.get(), DNASequencerScreen::new));
        }
    }

    @Mod.EventBusSubscriber(modid = BanditsQuirkLib.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ForgeEvents {
        @SubscribeEvent
        public static void onScreenInit(ScreenEvent.Init.Post event) {
            // Check if the main menu (TitleScreen) was just initialized
            if (event.getScreen() instanceof TitleScreen) {
                // Check if the options.txt file was replaced
                if (FileManager.wasOptionsFileReplaced()) {
                    // Show the info screen over the title screen
                    Minecraft minecraft = Minecraft.getInstance();
                    minecraft.setScreen(new OptionsReplacedInfoScreen(event.getScreen()));
                }
            }
        }
    }

    // MenuScreens are registered in onClientSetup above

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