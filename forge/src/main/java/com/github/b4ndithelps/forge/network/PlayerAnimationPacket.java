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
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import net.threetag.palladium.util.Easing;
import org.checkerframework.checker.units.qual.C;


// USE THIS TO PLAY PLAYER !MAIN! ANIMATION
public class PlayerAnimationPacket {
    private final String animation;

    public PlayerAnimationPacket(String anim) {
        this.animation = anim;
    }

    public static void encode(PlayerAnimationPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.animation);
    }

    public static PlayerAnimationPacket decode(FriendlyByteBuf buf) {
        return new PlayerAnimationPacket(buf.readUtf());
    }

    public static void handle(PlayerAnimationPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;

            if (player != null) {
                var animation = (ModifierLayer<IAnimation>)PlayerAnimationAccess.getPlayerAssociatedData(player).get(ResourceLocation.fromNamespaceAndPath(BanditsQuirkLib.MOD_ID, "animation"));
                if (animation != null) {

                    if (msg.animation == "") {
                        animation.replaceAnimationWithFade(
                                AbstractFadeModifier.standardFadeIn(10, INOUTSINE),
                                null
                        );
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
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

}