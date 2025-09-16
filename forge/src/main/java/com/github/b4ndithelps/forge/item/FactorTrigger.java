package com.github.b4ndithelps.forge.item;

import com.github.b4ndithelps.forge.network.BQLNetwork;
import com.github.b4ndithelps.forge.network.PlayerAnimationPacket;
import com.github.b4ndithelps.forge.network.UsingAmpulePacket;
import com.github.b4ndithelps.forge.systems.BodyStatusHelper;
import com.github.b4ndithelps.forge.systems.StaminaHelper;
import com.github.b4ndithelps.forge.utils.DelayTaskHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.network.PacketDistributor;
import net.threetag.palladium.power.SuperpowerUtil;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class FactorTrigger extends Item {

    private final int tier;
    private int[] color = {231, 231, 1};
    private int duration = 1800;
    private int addiction = 10;
    private int factor = 1;

    public FactorTrigger(Properties properties, int tier) {
        super(properties);
        this.tier = tier;

        if (tier == 2) {
            color = new int[]{229, 152, 0};
            duration = 3000;
            addiction = 10;
        } else if (tier == 3) {
            color = new int[]{202, 1, 1};
            duration = 1200;
            addiction = 10;
            factor = 2;
        }
    }

    public int getTier() { return tier; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Scoreboard scoreboard = player.getScoreboard();

        if (world.isClientSide) {
            return InteractionResultHolder.pass(stack);
        }

        if (StaminaHelper.getCurrentStamina(player) <= 0) {
            return InteractionResultHolder.pass(stack);
        }

        // 1. debounce
        if (player.getPersistentData().getBoolean("usingampule")) {
            return InteractionResultHolder.fail(stack);
        }

        // 3. Get custom float (pseudo-code)
        float usage = BodyStatusHelper.getCustomFloat(player, "chest", "usedAmpules");

        // Prevent reuse checks
        if ((tier == 1 && (usage % 10 == 1)) ||
                (tier == 2 && ((usage / 10) % 10 >= 1)) ||
                (tier == 3 && ((usage / 100) % 10 >= 1))) {
            if (!world.isClientSide) player.sendSystemMessage(Component.literal("You already used this ampule"));
            return InteractionResultHolder.fail(stack);
        }


        BQLNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> (ServerPlayer) player),
                new PlayerAnimationPacket("ampuleuse")
        );

        player.getPersistentData().putBoolean("usingampule", true);
        UUID playerId = player.getUUID();


        DelayTaskHandler.schedule(30, () -> {
            ItemStack currentstack = player.getMainHandItem();

            DelayTaskHandler.schedule(20, () -> {
                ServerPlayer current = world.getServer().getPlayerList().getPlayer(playerId);
                current.getPersistentData().putBoolean("usingampule", false);

                BQLNetwork.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> current),
                        new UsingAmpulePacket(false)
                );
            });

            if (currentstack != stack) {
                return;
            }

            player.setItemInHand(hand, ItemStack.EMPTY);
            float oldAddiction = BodyStatusHelper.getCustomFloat(player, "head", "ampuleAddiction");


            if (tier == 1) {
                BodyStatusHelper.setCustomFloat(player, "chest", "usedAmpules", usage+1);
            } else if (tier == 2) {
                BodyStatusHelper.setCustomFloat(player, "chest", "usedAmpules", usage+10);
            } else {
                BodyStatusHelper.setCustomFloat(player, "chest", "usedAmpules", usage+100);
            }
            ServerLevel serverLevel = (ServerLevel) world;
            double x = player.getX();
            double y = player.getY() + 1.0; // a bit above head
            double z = player.getZ();

            DustParticleOptions dust = new DustParticleOptions(new Vector3f(color[0] / 255f, color[1] / 255f, color[2] / 255f), 1.0f);

            serverLevel.sendParticles(
                    dust,
                    x, y, z,
                    35,   // count
                    0.5, 0.5, 0.5,
                    0.01
            );

            float cd = BodyStatusHelper.getCustomFloat(player, "chest", "ampuleCd");

            SuperpowerUtil.addSuperpower(player, ResourceLocation.read("banditsquirklib:ampule_use").get().orThrow());
            Objective objective = scoreboard.getObjective("MineHa.QuirkFactor");
            Score quirkFactor = scoreboard.getOrCreatePlayerScore(player.getScoreboardName(), objective);
            quirkFactor.setScore(quirkFactor.getScore() + factor);

            BodyStatusHelper.setCustomFloat(player, "head", "ampuleAddiction", oldAddiction+addiction);
            BodyStatusHelper.setCustomFloat(player, "chest", "ampuleCd", cd+duration);

        });


        return InteractionResultHolder.fail(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Illegal stuff").withStyle(ChatFormatting.DARK_GREEN));

        String stats = switch (tier) {
            case 1 -> "+ 1 Quirk Factor || 1:30";
            case 2 -> "+ 1 Quirk Factor || 2:30";
            case 3 -> "+ 2 Quirk Factor || 1:00";
            default -> "Unknown?";
        };

        tooltip.add(Component.translatable(stats).withStyle(ChatFormatting.GRAY));

    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 0;
    }
}


