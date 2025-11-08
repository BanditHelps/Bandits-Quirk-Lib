package com.github.b4ndithelps.forge.utils;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber
public class DelayTaskHandler {
    private static final List<Task> tasks = new ArrayList<>();

    public static void schedule(int delay, Runnable action) {
        tasks.add(new Task(delay, action));
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        List<Runnable> toRun = new ArrayList<>();

        // Decrement delays and collect ready tasks
        Iterator<Task> iter = tasks.iterator();
        while (iter.hasNext()) {
            Task task = iter.next();
            task.delay--;
            if (task.delay <= 0) {
                toRun.add(task.action);
                iter.remove();
            }
        }

        // Run outside the iterator → safe to schedule new tasks here
        for (Runnable r : toRun) {
            r.run();
        }
    }

    private static class Task {
        int delay;
        final Runnable action;

        Task(int delay, Runnable action) {
            this.delay = delay;
            this.action = action;
        }
    }
}
