package com.github.b4ndithelps.forge.events;

import com.github.b4ndithelps.BanditsQuirkLib;
import com.github.b4ndithelps.forge.systems.GenomeHelper;
import com.github.b4ndithelps.genetics.Gene;
import com.github.b4ndithelps.genetics.GeneticsHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.threetag.palladium.power.SuperpowerUtil;

@Mod.EventBusSubscriber(modid = BanditsQuirkLib.MOD_ID)
public class PhantomSpawnHandler {

    @SubscribeEvent
    public static void onCheckSpawn(MobSpawnEvent.FinalizeSpawn event) {

        if (event.getEntity() instanceof Phantom phantom) {
            if (event.getLevel().isClientSide()) return;


            var players = event.getLevel().players();
            for (var player : players) {

                if (hasGene(player, "bandits_quirk_lib:gene.phantom_immunity")) {
                    event.setSpawnCancelled(true);
                    break;
                }
            }
        }
    }


    public static boolean hasGene(Player player, String geneId) {
        ListTag genome = GenomeHelper.getGenome(player);
        for (int i = 0; i < genome.size(); i++) {
            CompoundTag gene = genome.getCompound(i);
            if (geneId.equals(gene.getString("id"))) {
                return true;
            }
        }
        return false;
    }
}
