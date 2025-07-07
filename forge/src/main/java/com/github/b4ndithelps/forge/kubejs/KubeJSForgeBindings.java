package com.github.b4ndithelps.forge.kubejs;

import com.github.b4ndithelps.forge.systems.StaminaHelper;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingsEvent;

public class KubeJSForgeBindings extends KubeJSPlugin {

    @Override
    public void registerBindings(BindingsEvent event) {
        event.add("Stamina", new StaminaHelper());
    }



}
