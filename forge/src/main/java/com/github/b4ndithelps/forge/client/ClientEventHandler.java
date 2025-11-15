package com.github.b4ndithelps.forge.client;

import com.github.b4ndithelps.BanditsQuirkLib;
import com.github.b4ndithelps.forge.client.blackwhip.BlackwhipStruggleClient;
import com.github.b4ndithelps.forge.client.renderer.entity.BetterWallProjectileRenderer;
import com.github.b4ndithelps.forge.client.renderer.entity.BlockStackEntityRenderer;
import com.github.b4ndithelps.forge.client.renderer.entity.ThrownHeldItemRenderer;
import com.github.b4ndithelps.forge.entities.ModEntities;
import com.github.b4ndithelps.forge.entities.WindProjectileEntity;
import com.github.b4ndithelps.forge.network.BQLNetwork;
import com.github.b4ndithelps.forge.network.DoubleJumpC2SPacket;
import com.github.b4ndithelps.forge.network.ZoomStatePacket;
import com.github.b4ndithelps.util.FileManager;
import com.github.b4ndithelps.forge.blocks.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import com.github.b4ndithelps.forge.blocks.ModBlocks;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@SuppressWarnings("removal")
public class ClientEventHandler {

    @Mod.EventBusSubscriber(modid = BanditsQuirkLib.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModEvents {
        @SubscribeEvent
        public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
            // Register the wind projectile renderer - invisible renderer since particles handle visuals
            event.registerEntityRenderer(ModEntities.WIND_PROJECTILE.get(), WindProjectileRenderer::new);
            event.registerEntityRenderer(ModEntities.BETTER_WALL_PROJECTILE.get(), BetterWallProjectileRenderer::new);
            event.registerEntityRenderer(ModEntities.BLOCK_STACK.get(), BlockStackEntityRenderer::new);
            event.registerEntityRenderer(ModEntities.THROWN_HELD_ITEM.get(), ThrownHeldItemRenderer::new);
        }

        @SubscribeEvent
        public static void addLayers(EntityRenderersEvent.AddLayers event) {
            PlayerRenderer defaultRenderer = event.getSkin("default");
            if (defaultRenderer != null) {
                defaultRenderer.addLayer(new LongArmsLayer(defaultRenderer));
                defaultRenderer.addLayer(new LongLegsLayer(defaultRenderer));
            }
            PlayerRenderer slimRenderer = event.getSkin("slim");
            if (slimRenderer != null) {
                slimRenderer.addLayer(new LongArmsLayer(slimRenderer));
                slimRenderer.addLayer(new LongLegsLayer(slimRenderer));
            }
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                MenuScreens.register(ModMenus.BIO_TERMINAL.get(), BioTerminalScreen::new);
                MenuScreens.register(ModMenus.GENE_SEQUENCER.get(), GeneSequencerScreen::new);
                MenuScreens.register(ModMenus.GENE_SLICER.get(), GeneSlicerScreen::new);
                MenuScreens.register(ModMenus.BIO_PRINTER.get(), BioPrinterScreen::new);
                MenuScreens.register(ModMenus.GENE_COMBINER.get(), GeneCombinerScreen::new);
				// Ensure blocks with transparency render correctly
                ItemBlockRenderTypes.setRenderLayer(ModBlocks.BIO_TERMINAL.get(), RenderType.cutout());
				ItemBlockRenderTypes.setRenderLayer(ModBlocks.BIO_PRINTER.get(), RenderType.cutout());
            });
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

		@SubscribeEvent
		public static void onComputeFov(ViewportEvent.ComputeFov event) {
			// Apply zoom FOV scaling if enabled
			if (ZoomStatePacket.ENABLED) {
				double base = event.getFOV();
				event.setFOV(base * ZoomStatePacket.FOV_SCALE);
			}
		}

        // Client-side: detect jump key while airborne to trigger double jump request once per press
        private static boolean lastJumpDown = false;

        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;

            boolean jumpDown = mc.options.keyJump.isDown();
            if (jumpDown && !lastJumpDown) {
                // Rising edge of jump key
                if (!mc.player.onGround() && !mc.player.isInWater() && !mc.player.isInLava()) {
                    BQLNetwork.CHANNEL.sendToServer(new DoubleJumpC2SPacket());
                }
				// Struggle tap (if tagged)
				BlackwhipStruggleClient.sendTapIfActiveOnJumpEdge(jumpDown, lastJumpDown);
            }
            lastJumpDown = jumpDown;
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;
            if (mc.screen != null) return; // ignore while in GUIs

            boolean jumpDown = mc.options.keyJump.isDown();
            if (jumpDown && !lastJumpDown) {
                if (!mc.player.onGround() && !mc.player.isInWater() && !mc.player.isInLava()) {
                    BQLNetwork.CHANNEL.sendToServer(new DoubleJumpC2SPacket());
                }
				// Struggle tap (if tagged)
				BlackwhipStruggleClient.sendTapIfActiveOnJumpEdge(jumpDown, lastJumpDown);
            }
            lastJumpDown = jumpDown;
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