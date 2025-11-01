package com.github.b4ndithelps.forge.client.genegraph;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public final class ClientGeneGraphState {
    private static volatile List<ResourceLocation> builderOrder = List.of();

    private ClientGeneGraphState() {}

    public static List<ResourceLocation> getBuilderOrder() {
        return builderOrder;
    }

    public static void openWithBuilderOrder(List<ResourceLocation> order) {
        builderOrder = order == null ? List.of() : List.copyOf(order);
        var mc = Minecraft.getInstance();
        if (mc != null) {
            mc.setScreen(new GeneGraphScreen());
        }
    }
}



