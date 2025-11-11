package com.github.b4ndithelps.forge.network;

import com.github.b4ndithelps.BanditsQuirkLib;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonConfiguration;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

import static dev.kosmx.playerAnim.core.util.Ease.INOUTSINE;


// USE THIS TO PLAY PLAYER !MAIN! ANIMATION
public class PlayerAnimationPacket {
    private final int entityId;
    private final String animation;

    // Track camera override for restrain animation to restore user's previous preference
    private static CameraType PREV_CAMERA_TYPE = null;
    private static boolean CAMERA_FORCED_FOR_RESTRAIN = false;

    public PlayerAnimationPacket(int entityId, String anim) {
        this.entityId = entityId;
        this.animation = anim;
    }

    public static void encode(PlayerAnimationPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeUtf(msg.animation);
    }

    public static PlayerAnimationPacket decode(FriendlyByteBuf buf) {
        int id = buf.readVarInt();
        String anim = buf.readUtf();
        return new PlayerAnimationPacket(id, anim);
    }

    public static void handle(PlayerAnimationPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;
            var entity = mc.level.getEntity(msg.entityId);
            if (entity instanceof AbstractClientPlayer player) {
                boolean isLocalPlayer = (mc.player != null && mc.player.getId() == msg.entityId);
                var animation = (ModifierLayer<IAnimation>) PlayerAnimationAccess.getPlayerAssociatedData(player).get(ResourceLocation.fromNamespaceAndPath(BanditsQuirkLib.MOD_ID, "animation"));
                if (animation != null) {

                    if (msg.animation.isEmpty()) {
                        animation.replaceAnimationWithFade(
                                AbstractFadeModifier.standardFadeIn(10, INOUTSINE),
                                null
                        );
                        // If we previously forced third person for restrain, restore the original camera type
                        if (isLocalPlayer && CAMERA_FORCED_FOR_RESTRAIN && PREV_CAMERA_TYPE != null) {
                            mc.options.setCameraType(PREV_CAMERA_TYPE);
                            CAMERA_FORCED_FOR_RESTRAIN = false;
                            PREV_CAMERA_TYPE = null;
                        }
                    } else if (msg.animation.equals("x")) {
                        animation.setAnimation(null);
                    } else {
                        KeyframeAnimationPlayer freshAnim = new KeyframeAnimationPlayer(
                                PlayerAnimationRegistry.getAnimation(
                                        ResourceLocation.fromNamespaceAndPath(BanditsQuirkLib.MOD_ID, msg.animation)
                                )
                        ).setFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL)
                                .setFirstPersonConfiguration(new FirstPersonConfiguration(true, false, true, false));

                        animation.replaceAnimationWithFade(
                                AbstractFadeModifier.standardFadeIn(10, INOUTSINE),
                                freshAnim
                        );

                        // Force local player's camera into third person while restrain animation plays
                        if (isLocalPlayer && "restrain_animation".equals(msg.animation)) {
                            if (!CAMERA_FORCED_FOR_RESTRAIN) {
                                PREV_CAMERA_TYPE = mc.options.getCameraType();
                            }
                            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                            CAMERA_FORCED_FOR_RESTRAIN = true;
                        }
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

}