package com.github.b4ndithelps.forge.item;

import com.github.b4ndithelps.forge.config.BQLConfig;
import com.github.b4ndithelps.forge.network.BQLNetwork;
import com.github.b4ndithelps.forge.network.PlayerAnimationPacket;
import com.github.b4ndithelps.forge.systems.BodyStatusHelper;
import com.github.b4ndithelps.forge.systems.QuirkFactorHelper;
import com.github.b4ndithelps.forge.systems.StaminaHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;
import net.threetag.palladium.power.SuperpowerUtil;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.List;

public class FactorTrigger extends Item {

    private final int tier;
    private boolean isUsing = false;
    private int usedSlot = -1;
    private int[] color = {231, 231, 1};
    private int factor = 1;

    public FactorTrigger(Properties properties, int tier) {
        super(properties);
        this.tier = tier;

        if (tier == 2) {
            color = new int[]{229, 152, 0};
        } else if (tier == 3) {
            color = new int[]{202, 1, 1};
            factor = 2;
        }
    }

    public int getTier() { return tier; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (world.isClientSide) {
            return InteractionResultHolder.pass(stack);
        }

        if (StaminaHelper.getCurrentStamina(player) <= 0) {
            return InteractionResultHolder.pass(stack);
        }

        if (isUsing) {
            return InteractionResultHolder.pass(stack);
        }

        // Used ampules is stored in binary, each digit is a tier (001 - tier 1, 010 - tier 2, 100 - tier 3, 110 - tier 2 and 3, etc)
        float usage = BodyStatusHelper.getCustomFloat(player, "chest", "usedAmpules");

        // Prevent reuse checks
        // By dividing binary 110 by ten we get 11 ( removing digit from right ). By using % 10 we get that digit ( 11 - 1, 10 - 0, 110 - 0)
        if ((tier == 1 && (usage % 10 == 1)) ||
                (tier == 2 && ((usage / 10) % 10 >= 1)) ||
                (tier == 3 && ((usage / 100) % 10 >= 1))) {
            if (!world.isClientSide) player.sendSystemMessage(Component.literal("You already used this ampule"));
            return InteractionResultHolder.pass(stack);
        }

        // Start Animation
        BQLNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> (ServerPlayer) player),
                new PlayerAnimationPacket("ampuleuse")
        );
        isUsing = true;
        usedSlot = player.getInventory().selected;
        player.startUsingItem(hand);


        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);

        // should improve performance a bit by skipping checks if not using
        if (!isUsing) return;


        if (world.isClientSide) {
            return;
        }
        if (entity instanceof Player player) {
            if (slot == usedSlot && !selected) {

               BQLNetwork.CHANNEL.send(
                     PacketDistributor.PLAYER.with(() -> (ServerPlayer) player),
                     new PlayerAnimationPacket("")
               );

               isUsing = false;
            }
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level world, LivingEntity entity, int timeLeft) {
        if (world.isClientSide) {
            return;
        }
        if (entity instanceof Player player) {
            // Stop Animation
            BQLNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> (ServerPlayer) player),
                    new PlayerAnimationPacket("")
            );
            isUsing = false;
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity entity) {

        if (!world.isClientSide && entity instanceof Player player) {

            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            float usage = BodyStatusHelper.getCustomFloat(player, "chest", "usedAmpules");
            float oldAddiction = BodyStatusHelper.getCustomFloat(player, "head", "ampuleAddiction");
            int duration, addiction;

            if (tier == 1) {
                BodyStatusHelper.setCustomFloat(player, "chest", "usedAmpules", usage+1);
                duration = BQLConfig.INSTANCE.tier1Duration.get();
                addiction = BQLConfig.INSTANCE.tier1Addiction.get();
            } else if (tier == 2) {
                BodyStatusHelper.setCustomFloat(player, "chest", "usedAmpules", usage+10);
                duration = BQLConfig.INSTANCE.tier2Duration.get();
                addiction = BQLConfig.INSTANCE.tier2Addiction.get();
            } else {
                BodyStatusHelper.setCustomFloat(player, "chest", "usedAmpules", usage+100);
                duration = BQLConfig.INSTANCE.tier3Duration.get();
                addiction = BQLConfig.INSTANCE.tier3Addiction.get();
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

            SuperpowerUtil.addSuperpower(player, ResourceLocation.read("bql:ampule_use").get().orThrow());

            double quirkFactor = QuirkFactorHelper.getQuirkFactor((ServerPlayer) player);
            QuirkFactorHelper.setQuirkFactor((ServerPlayer) player,  quirkFactor + factor);

            BodyStatusHelper.setCustomFloat(player, "head", "ampuleAddiction", oldAddiction+addiction);
            BodyStatusHelper.setCustomFloat(player, "chest", "ampuleCd", cd+duration);
            isUsing = false;
        }

        return super.finishUsingItem(stack, world, entity);
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
        return 27;
    }

    public void setUsing(boolean using) {
        isUsing = using;
    }

    public boolean isUsing() {
        return isUsing;
    }
}


