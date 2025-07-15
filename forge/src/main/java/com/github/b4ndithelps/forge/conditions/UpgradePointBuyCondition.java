package com.github.b4ndithelps.forge.conditions;

import com.github.b4ndithelps.forge.systems.StaminaHelper;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.threetag.palladium.condition.*;
import net.threetag.palladium.power.ability.AbilityConfiguration;
import net.threetag.palladium.util.icon.IIcon;
import net.threetag.palladium.util.icon.ItemIcon;
import net.threetag.palladium.util.property.*;

public class UpgradePointBuyCondition extends BuyableCondition {
    private final int amount;
    private final IIcon icon;
    private final Component description;

    public UpgradePointBuyCondition(int amount, IIcon icon, Component description) {
        this.amount = amount;
        this.icon = icon;
        this.description = description;
    }

    @Override
    public AbilityConfiguration.UnlockData createData() {
        return new AbilityConfiguration.UnlockData(this.icon, this.amount, this.description);
    }

    @Override
    public boolean isAvailable(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return false;
        }

        return StaminaHelper.getUpgradePoints(player) >= this.amount;
    }

    @Override
    public boolean takeFromEntity(LivingEntity entity) {
        if (entity instanceof Player player) {
            StaminaHelper.setUpgradePoints(player, StaminaHelper.getUpgradePoints(player) - this.amount);
            return true;
        }

        return false;
    }

    @Override
    public ConditionSerializer getSerializer() {
        return CustomConditionSerializers.UPGRADE_POINT_BUYABLE.get();
    }

    public static class Serializer extends ConditionSerializer {
        public static final PalladiumProperty<Integer> POINTS = (new IntegerProperty("points")).configurable("Required upgrade points for the upgrade");
        public static final PalladiumProperty<IIcon> ICON = (new IconProperty("icon")).configurable("Icon that will be displayed during buying");
        public static final PalladiumProperty<Component> DESCRIPTION = (new ComponentProperty("description")).configurable("Name that is displayed by the points in the upgrade menu");

        public Serializer() {
            this.withProperty(POINTS, 3);
            this.withProperty(ICON, new ItemIcon(Items.COMMAND_BLOCK));
            this.withProperty(DESCRIPTION, Component.literal("Upgrade Points"));
        }

        public ConditionEnvironment getContextEnvironment() {
            return ConditionEnvironment.DATA;
        }

        public Condition make(JsonObject json) {
            return new UpgradePointBuyCondition((Integer)this.getProperty(json, POINTS), (IIcon)this.getProperty(json, ICON), (Component)this.getProperty(json, DESCRIPTION));
        }

        public String getDocumentationDescription() {
            return "A buyable condition that requires a certain amount of upgrade points";
        }
    }
}
