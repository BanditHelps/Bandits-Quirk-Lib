package com.github.b4ndithelps.forge.client.blackwhip;

import com.github.b4ndithelps.BanditsQuirkLib;
import com.github.b4ndithelps.forge.entities.BlockStackEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(modid = BanditsQuirkLib.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BlackwhipRenderHandler {

    public static void applyPacket(int sourcePlayerId, boolean active, boolean restraining, int targetEntityId, int ticksLeft, int missRetractTicks, float range, float curve, float thickness) {
        ClientWhipState s = PLAYER_TO_WHIP.computeIfAbsent(sourcePlayerId, ClientWhipState::new);
        s.active = active;
        s.restraining = restraining;
        s.targetEntityId = targetEntityId;
        s.ticksLeft = Math.max(0, ticksLeft);
        s.initialTravelTicks = Math.max(1, ticksLeft);
        s.missRetractTicks = Math.max(0, missRetractTicks);
        s.range = Math.max(1.0F, range);
        s.curve = Math.max(0.0F, curve);
        s.thickness = Math.max(0.01F, thickness);
        s.lastGameTimeDecrement = -1L;
        // reset sweeping-related fields when receiving direct state packets
        s.sweeping = false;
        s.chargeTicks = 0;
        if (!active) {
            s.active = false;
        }
    }

    // Flag to force the start anchor to the player's right-hand side (with extra height) for restrain animation
    private static final Set<Integer> FORCE_RIGHT_HAND_ANCHOR = new HashSet<>();
    public static void applyAnchorOverride(int sourcePlayerId, boolean active) {
        if (active) FORCE_RIGHT_HAND_ANCHOR.add(sourcePlayerId);
        else FORCE_RIGHT_HAND_ANCHOR.remove(sourcePlayerId);
    }

    public static final class ClientWhipState {
        public final int sourcePlayerId;
        public boolean active;
        public boolean restraining;
        public int targetEntityId; // -1 for miss
        public int ticksLeft;
        public int missRetractTicks;
        public float range;
        public float curve;
        public float thickness;
        public int initialTravelTicks;
        public long lastGameTimeDecrement = -1L;
        // sweeping-lash fields
        public boolean sweeping = false;
        public float sweepArcDeg = 0f;
        public float baseYawDeg = 0f;
        public int chargeTicks = 0;

        public ClientWhipState(int sourcePlayerId) {
            this.sourcePlayerId = sourcePlayerId;
        }
    }

    private static final Map<Integer, ClientWhipState> PLAYER_TO_WHIP = new HashMap<>();
    private static final class ClientBlockWhipState {
        public final int sourcePlayerId;
        public boolean active;
        public Vec3 target;
        public int ticksLeft;
        public int initialTravelTicks;
        public float curve;
        public float thickness;
        public long lastGameTimeDecrement = -1L;
        public double maxLen = 0.0; // fixed rope length captured after arrival
        public ClientBlockWhipState(int sourcePlayerId) { this.sourcePlayerId = sourcePlayerId; }
    }
    private static final Map<Integer, ClientBlockWhipState> PLAYER_TO_BLOCK_WHIP = new HashMap<>();
    private static final class ClientMultiBlockWhipState {
        public final int sourcePlayerId;
        public boolean active;
        public List<Vec3> targets = new ArrayList<>();
        public int ticksLeft;
        public int initialTravelTicks;
        public float curve;
        public float thickness;
        public long lastGameTimeDecrement = -1L;
        public ClientMultiBlockWhipState(int sourcePlayerId) { this.sourcePlayerId = sourcePlayerId; }
    }
    private static final Map<Integer, ClientMultiBlockWhipState> PLAYER_TO_MULTI_BLOCK = new HashMap<>();
    private static final class ClientMultiTethers {
        public final int sourcePlayerId;
        public boolean active;
        public float curve;
        public float thickness;
        public List<Integer> targetEntityIds = new ArrayList<>();

        public ClientMultiTethers(int sourcePlayerId) { this.sourcePlayerId = sourcePlayerId; }
    }
    private static final Map<Integer, ClientMultiTethers> PLAYER_TO_MULTI = new HashMap<>();

    // Aura state per player
    private static final class ClientAuraState {
        public final int sourcePlayerId;
        public boolean active;
        public boolean targetVisible;
        public float visibility; // 0..1 fade
        public int tentacleCount;
        public float length;
        public float curve;
        public float thickness;
        public float jaggedness;
        public float orbitSpeed;
        public long seed;
        // Cached anchor offsets in player-local space (back area), built from seed
        public List<Vec3> localAnchors = new ArrayList<>();
        // Smoothed world velocity and last world position for movement-based lag
        public Vec3 lastWorldPos;
        public Vec3 smoothedVel = Vec3.ZERO;
        // Activation tick used to animate tentacle growth on first enable
        public long activateGameTime = -1L;

        public ClientAuraState(int sourcePlayerId) { this.sourcePlayerId = sourcePlayerId; }
    }
    private static final Map<Integer, ClientAuraState> PLAYER_TO_AURA = new HashMap<>();

    // Bubble shield state per player
    private static final class ClientBubbleState {
        public final int sourcePlayerId;
        public boolean active;
        public int tentacleCount;
        public float radius;
        public float forwardOffset;
        public float curve;
        public float thickness;
        public float jaggedness;
        public long seed;
        public List<Vec3> localAnchors = new ArrayList<>();
        // Activation tick used to animate tentacle growth on first enable
        public long activateGameTime = -1L;
        // Deactivation animation state (shrink all tentacles simultaneously)
        public boolean deactivating = false;
        public long deactivateGameTime = -1L;
        public ClientBubbleState(int id) { this.sourcePlayerId = id; }
    }
    private static final Map<Integer, ClientBubbleState> PLAYER_TO_BUBBLE = new HashMap<>();

    public static void applyAuraPacket(int sourcePlayerId, boolean active, int tentacleCount, float length, float curve, float thickness, float jaggedness, float orbitSpeed, long seed) {
        ClientAuraState s = PLAYER_TO_AURA.computeIfAbsent(sourcePlayerId, ClientAuraState::new);
        if (active) {
            s.active = true;
            s.targetVisible = true;
            s.tentacleCount = Math.max(1, tentacleCount);
            s.length = Math.max(0.5f, length);
            s.curve = Math.max(0.0f, curve);
            s.thickness = Math.max(0.05f, thickness);
            s.jaggedness = Math.max(0.0f, jaggedness);
            s.orbitSpeed = Math.max(0.0f, orbitSpeed);
            s.seed = seed;
            if (s.localAnchors.size() != s.tentacleCount) rebuildAuraAnchors(s);
            // Reset activation time so growth animation restarts on enable; will be initialized on next render
            s.activateGameTime = -1L;
        } else {
            // begin fade-out; keep active until visibility reaches 0
            s.targetVisible = false;
            if (!s.active) s.active = true;
        }
    }

    public static void applyBubbleShieldPacket(int sourcePlayerId, boolean active, int tentacleCount, float radius, float forwardOffset, float curve, float thickness, float jaggedness, long seed) {
        ClientBubbleState s = PLAYER_TO_BUBBLE.computeIfAbsent(sourcePlayerId, ClientBubbleState::new);
        if (active) {
            s.active = true;
            s.deactivating = false;
            s.tentacleCount = Math.max(1, tentacleCount);
            s.radius = Math.max(0.25f, radius);
            s.forwardOffset = Math.max(0.0f, forwardOffset);
            s.curve = Math.max(0.0f, curve);
            s.thickness = Math.max(0.05f, thickness);
            s.jaggedness = Math.max(0.0f, jaggedness);
            s.seed = seed;
            if (s.localAnchors.size() != s.tentacleCount) rebuildAuraAnchorsLike(s);
            // Reset activation time so animation restarts on enable; will be initialized on next render
            s.activateGameTime = -1L;
            s.deactivateGameTime = -1L;
        } else {
            // Begin deactivation: keep rendering while shrinking until finished
            s.active = false;
            s.deactivating = true;
            s.deactivateGameTime = -1L; // will be set on first render tick
        }
    }

    public static void applyBlockPacket(int sourcePlayerId, boolean active, double x, double y, double z,
                                        int travelTicks, float curve, float thickness) {
        ClientBlockWhipState s = PLAYER_TO_BLOCK_WHIP.computeIfAbsent(sourcePlayerId, ClientBlockWhipState::new);
        s.active = active;
        s.target = new Vec3(x, y, z);
        s.ticksLeft = Math.max(0, travelTicks);
        s.initialTravelTicks = Math.max(1, travelTicks);
        s.curve = curve;
        s.thickness = thickness;
        s.lastGameTimeDecrement = -1L;
        if (!active) s.ticksLeft = 0;
    }

    public static void applyMultiBlockPacket(int sourcePlayerId, boolean active, List<Double> xs, List<Double> ys, List<Double> zs,
                                             int travelTicks, float curve, float thickness) {
        ClientMultiBlockWhipState s = PLAYER_TO_MULTI_BLOCK.computeIfAbsent(sourcePlayerId, ClientMultiBlockWhipState::new);
        s.active = active;
        s.targets.clear();
        int n = Math.min(Math.min(xs.size(), ys.size()), zs.size());
        for (int i = 0; i < n; i++) {
            s.targets.add(new Vec3(xs.get(i), ys.get(i), zs.get(i)));
        }
        s.ticksLeft = Math.max(0, travelTicks);
        s.initialTravelTicks = Math.max(1, travelTicks);
        s.curve = curve;
        s.thickness = thickness;
        s.lastGameTimeDecrement = -1L;
        if (!active) s.ticksLeft = 0;
        if (!active || s.targets.isEmpty()) {
            s.active = false;
        }
    }

    public static void applyTethersPacket(int sourcePlayerId, boolean active, float curve, float thickness, List<Integer> targetIds) {
        ClientMultiTethers multi = PLAYER_TO_MULTI.computeIfAbsent(sourcePlayerId, ClientMultiTethers::new);
        multi.active = active;
        multi.curve = curve;
        multi.thickness = thickness;
        multi.targetEntityIds.clear();
        if (active && targetIds != null) multi.targetEntityIds.addAll(targetIds);
        if (!active || multi.targetEntityIds.isEmpty()) {
            // mark inactive for cleanup
            multi.active = false;
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return;

        Vec3 cameraPos = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource buffer = mc.renderBuffers().bufferSource();
        float partial = event.getPartialTick();

        // Cleanup for players no longer present or inactive
        PLAYER_TO_WHIP.entrySet().removeIf(e -> level.getEntity(e.getKey()) == null || !e.getValue().active);
        PLAYER_TO_BLOCK_WHIP.entrySet().removeIf(e -> level.getEntity(e.getKey()) == null || !e.getValue().active);
        PLAYER_TO_MULTI_BLOCK.entrySet().removeIf(e -> level.getEntity(e.getKey()) == null || !e.getValue().active);
        PLAYER_TO_MULTI.entrySet().removeIf(e -> level.getEntity(e.getKey()) == null || !e.getValue().active);
        PLAYER_TO_AURA.entrySet().removeIf(e -> level.getEntity(e.getKey()) == null || (!e.getValue().active && e.getValue().visibility <= 0f));
        PLAYER_TO_BUBBLE.entrySet().removeIf(e -> {
            Entity ent = level.getEntity(e.getKey());
            BlackwhipRenderHandler.ClientBubbleState bs = e.getValue();
            return ent == null || (!bs.active && !bs.deactivating);
        });

        long gameTime = level.getGameTime();
        double time = gameTime + partial;
        for (ClientWhipState state : PLAYER_TO_WHIP.values()) {
            if (!state.active) continue;
            Entity src = level.getEntity(state.sourcePlayerId);
            if (!(src instanceof Player player)) continue;

            // Compute start from hand (camera-smooth); force right hand + a bit higher for restrain visuals
            boolean forceRight = state.restraining || FORCE_RIGHT_HAND_ANCHOR.contains(state.sourcePlayerId);
            Vec3 start;
            if (forceRight) {
                float yaw = Mth.rotLerp(partial, player.yRotO, player.getYRot());
                Vec3 fwdYaw = Vec3.directionFromRotation(0, yaw).normalize();
                start = getHandPositionForSide(player, partial, 1.0f)
                        .add(0, 0.35, 0)        // move up about 2x more
                        .add(fwdYaw.scale(0.60)); // push forward roughly arm length
            } else {
                start = getHandPosition(player, partial);
            }

            // Resolve end
            Vec3 end;
            Entity restrainTarget = null;
            if (state.restraining && state.targetEntityId >= 0) {
                Entity target = level.getEntity(state.targetEntityId);
                if (target == null || !target.isAlive()) continue;
                restrainTarget = target;
                end = getAttachPoint(target, partial);
            } else if (!state.restraining && state.sweeping) {
                // Two-phase motion: charge-up reveal (no sweep), then sweeping arc
                float total = state.initialTravelTicks > 0 ? state.initialTravelTicks : Math.max(1, state.missRetractTicks);
                float u = 1.0F - Math.max(0F, Math.min(1F, (float) state.ticksLeft / total));
                float halfArc = Math.max(0f, state.sweepArcDeg) * 0.5f;
                boolean rightHand = player.getMainArm() == HumanoidArm.RIGHT;
                float angleStart = rightHand ? +halfArc : -halfArc;
                float dir = rightHand ? -1f : +1f; // right hand sweeps right->left

                int charge = Math.max(0, state.chargeTicks);
                float chargeFrac = total <= 0 ? 0f : Math.min(1f, (float)charge / total);
                float yawNow;
                double extend;
                if (u < chargeFrac) {
                    // Reveal phase: grow outward at starting angle
                    float ru = u / Math.max(0.0001f, chargeFrac);
                    float r = easeOutQuad(ru);
                    yawNow = state.baseYawDeg + angleStart;
                    extend = 0.15 + 0.55 * r; // up to ~70% of reach
                } else {
                    // Sweep phase
                    float su = (u - chargeFrac) / Math.max(0.0001f, 1f - chargeFrac);
                    float s = easeInOutCubic(su);
                    float currentOffset = angleStart + dir * (2f * halfArc * s);
                    yawNow = state.baseYawDeg + currentOffset;
                    extend = 0.70 + 0.30 * s; // from reveal extent to full
                }

                Vec3 eye = player.getEyePosition(partial);
                Vec3 dirVec = Vec3.directionFromRotation(0, yawNow).normalize();
                double reach = Math.max(1.0F, state.range);
                end = eye.add(dirVec.scale(reach * extend));
            } else if (!state.restraining && state.targetEntityId >= 0) {
                // Traveling toward a target: extend toward the target based on progress
                Entity target = level.getEntity(state.targetEntityId);
                if (target == null || !target.isAlive()) continue;
                Vec3 eye = player.getEyePosition(partial);
                Vec3 toTarget = getAttachPoint(target, partial).subtract(eye);
                float total = state.initialTravelTicks > 0 ? state.initialTravelTicks : Math.max(1, state.missRetractTicks);
                float u = 1.0F - Math.max(0F, Math.min(1F, (float) state.ticksLeft / total));
                float progress = easeInOutCubic(u);
                // ensure minimal visible extension to avoid first-frame pop
                if (progress < 0.05F) progress = 0.05F;
                end = eye.add(toTarget.scale(progress));
            } else {
                // Miss: extend and retract along look vector
                Vec3 look = player.getViewVector(partial);
                Vec3 eye = player.getEyePosition(partial);
                Vec3 fullEnd = eye.add(look.scale(Math.max(1.0F, state.range)));
                float ratio = state.missRetractTicks <= 0 ? 0F : Math.max(0F, Math.min(1F, (float) state.ticksLeft / (float) state.missRetractTicks));
                end = eye.add(fullEnd.subtract(eye).scale(ratio));
            }

			// Build points along a simple arcing curve between start and end
            double len = end.subtract(start).length();
            int segments = Math.max(20, (int)Math.min(96, len * 6.0));
            List<Vec3> points = buildCurve(start, end, state.curve, segments);
			// Two-pass ribbon: outer teal glow, inner dark core
            float base = Math.max(0.02F, state.thickness * 0.065F);
            // Reduce noise during charge-up to avoid jumpiness
            float noiseScale = 1.0f;
            if (state.sweeping && state.chargeTicks > 0 && state.initialTravelTicks > 0) {
                float total = state.initialTravelTicks;
                float u = 1.0F - Math.max(0F, Math.min(1F, (float) state.ticksLeft / total));
                float chargeFrac = Math.min(1f, (float)state.chargeTicks / total);
                if (u < chargeFrac) {
                    float ru = u / Math.max(0.0001f, chargeFrac);
                    noiseScale = 0.4f + 0.6f * ru; // ramp up noise through reveal
                }
            }
            float noiseAmp = Math.min(base * 0.75F, 0.08F) * noiseScale;
			// Outer glow first (slightly wider, semi-transparent) — push slightly away from camera to avoid coplanar artifacts
            drawRibbonGradient(poseStack, buffer, cameraPos, points,
					base * 1.25F,
					BlackwhipColors.OUTER_GLOW_START, // start teal with alpha
					BlackwhipColors.OUTER_GLOW_MID, // mid greenish-teal
					BlackwhipColors.OUTER_GLOW_END, // end aqua
					0.0015F,
                    noiseAmp,
                    time,
                    1.0f
			);
			// Inner core on top (narrower, very dark blue-black) — pull slightly toward camera
            drawRibbonSolid(poseStack, buffer, cameraPos, points, base * 0.54F, BlackwhipColors.INNER_CORE, -0.0007F, noiseAmp, time, 1.0f);

            // If restraining a target, render wrap-around rings around it too
            if (restrainTarget != null) {
                drawEntityWrapRings(poseStack, buffer, cameraPos, restrainTarget, partial, base, noiseAmp, time);
            }

            // Decrement once per game tick to match server tick rate
            if (!state.restraining && state.lastGameTimeDecrement != gameTime) {
                state.lastGameTimeDecrement = gameTime;
                state.ticksLeft -= 1;
                if (state.ticksLeft <= 0 && state.targetEntityId < 0) {
                    state.active = false; // end local-only miss retract
                }
            }
        }

        // Render block-anchored whips (travel animation only)
        for (ClientBlockWhipState state : PLAYER_TO_BLOCK_WHIP.values()) {
            if (!state.active) continue;
            Entity src = level.getEntity(state.sourcePlayerId);
            if (!(src instanceof Player player)) continue;

            Vec3 start = getHandPosition(player, partial);
            Vec3 eye = player.getEyePosition(partial);
            Vec3 toTarget = state.target.subtract(eye);
            float total = state.initialTravelTicks > 0 ? state.initialTravelTicks : 1;
            float u = 1.0F - Math.max(0F, Math.min(1F, (float) state.ticksLeft / total));
            float progress = easeInOutCubic(u);
            if (progress < 0.05F) progress = 0.05F;
            Vec3 end = eye.add(toTarget.scale(progress));

            // After arrival, always render the full rope from hand to the fixed anchor
            if (state.ticksLeft <= 0) {
                end = state.target;
            }

            double len = end.subtract(start).length();
            int segments = Math.max(20, (int)Math.min(96, len * 6.0));
            List<Vec3> points = buildCurve(start, end, state.curve, segments);
            float base = Math.max(0.02F, state.thickness * 0.065F);
            float noiseAmp = Math.min(base * 0.75F, 0.08F);
            drawRibbonGradient(poseStack, buffer, cameraPos, points,
                    base * 1.25F, BlackwhipColors.OUTER_GLOW_START, BlackwhipColors.OUTER_GLOW_MID, BlackwhipColors.OUTER_GLOW_END, 0.0015F, noiseAmp, time, 1.0f);
            drawRibbonSolid(poseStack, buffer, cameraPos, points, base * 0.54F, BlackwhipColors.INNER_CORE, -0.0007F, noiseAmp, time, 1.0f);

            if (state.lastGameTimeDecrement != gameTime) {
                state.lastGameTimeDecrement = gameTime;
                if (state.ticksLeft > 0) state.ticksLeft -= 1; // stop at 0 to keep anchored visual
            }
        }

        // Render multi block-anchored whips
        for (ClientMultiBlockWhipState state : PLAYER_TO_MULTI_BLOCK.values()) {
            if (!state.active || state.targets.isEmpty()) continue;
            Entity src = level.getEntity(state.sourcePlayerId);
            if (!(src instanceof Player player)) continue;

            Vec3 eye = player.getEyePosition(partial);
            float total = state.initialTravelTicks > 0 ? state.initialTravelTicks : 1;
            float u = 1.0F - Math.max(0F, Math.min(1F, (float) state.ticksLeft / total));
            float progress = easeInOutCubic(u);
            if (progress < 0.05F) progress = 0.05F;

            int nTargets = state.targets.size();
            int half = Math.max(1, nTargets / 2);
            for (int idx = 0; idx < nTargets; idx++) {
                Vec3 t = state.targets.get(idx);
                // First half from right hand, second half from left hand
                boolean rightSide = idx < half;
                Vec3 start = getHandPositionForSide(player, partial, rightSide ? 1.0f : -1.0f);
                Vec3 toTarget = t.subtract(eye);
                Vec3 end = eye.add(toTarget.scale(progress));
                if (state.ticksLeft <= 0) {
                    end = t;
                }
                double len = end.subtract(start).length();
                int segments = Math.max(20, (int)Math.min(96, len * 6.0));
                List<Vec3> points = buildCurve(start, end, state.curve, segments);
                float base = Math.max(0.02F, state.thickness * 0.065F);
                float noiseAmp = Math.min(base * 0.75F, 0.08F);
                drawRibbonGradient(poseStack, buffer, cameraPos, points,
                        base * 1.25F, BlackwhipColors.OUTER_GLOW_START, BlackwhipColors.OUTER_GLOW_MID, BlackwhipColors.OUTER_GLOW_END, 0.0015F, noiseAmp, time, 1.0f);
                drawRibbonSolid(poseStack, buffer, cameraPos, points, base * 0.54F, BlackwhipColors.INNER_CORE, -0.0007F, noiseAmp, time, 1.0f);
            }

            if (state.lastGameTimeDecrement != gameTime) {
                state.lastGameTimeDecrement = gameTime;
                if (state.ticksLeft > 0) state.ticksLeft -= 1;
            }
        }

        // Render multi tethers
        for (ClientMultiTethers multi : PLAYER_TO_MULTI.values()) {
            if (!multi.active) continue;
            Entity src = level.getEntity(multi.sourcePlayerId);
            if (!(src instanceof Player player)) continue;

            boolean forceRight = FORCE_RIGHT_HAND_ANCHOR.contains(multi.sourcePlayerId);
            Vec3 start;
            if (forceRight) {
                float yaw = Mth.rotLerp(partial, player.yRotO, player.getYRot());
                Vec3 fwdYaw = Vec3.directionFromRotation(0, yaw).normalize();
                start = getHandPositionForSide(player, partial, 1.0f)
                        .add(0, 0.30, 0)
                        .add(fwdYaw.scale(0.60));
            } else {
                start = getHandPosition(player, partial);
            }
            for (Integer targetId : multi.targetEntityIds) {
                Entity target = level.getEntity(targetId);
                if (!(target != null && target.isAlive())) continue;
                Vec3 end = getAttachPoint(target, partial);

                double len = end.subtract(start).length();
                int segments = Math.max(20, (int)Math.min(96, len * 6.0));
                List<Vec3> points = buildCurve(start, end, multi.curve, segments);

                float base = Math.max(0.02F, multi.thickness * 0.065F);
                float noiseAmp = Math.min(base * 0.75F, 0.08F);
                drawRibbonGradient(poseStack, buffer, cameraPos, points,
                        base * 1.25F,
                        BlackwhipColors.OUTER_GLOW_START,
                        BlackwhipColors.OUTER_GLOW_MID,
                        BlackwhipColors.OUTER_GLOW_END,
                        0.0015F,
                        noiseAmp,
                        time,
                        1.0f);
                drawRibbonSolid(poseStack, buffer, cameraPos, points, base * 0.54F, BlackwhipColors.INNER_CORE, -0.0007F, noiseAmp, time, 1.0f);

                // Render wrap-around rings around the target's hitbox so it looks tied up
                drawEntityWrapRings(poseStack, buffer, cameraPos, target, partial, base, noiseAmp, time);
            }
        }

        // Render aura tentacles
        for (ClientAuraState aura : PLAYER_TO_AURA.values()) {
            if (!aura.active && aura.visibility <= 0f) continue;
            Entity src = level.getEntity(aura.sourcePlayerId);
            if (!(src instanceof Player player)) continue;

            if (aura.localAnchors.size() != aura.tentacleCount) {
                rebuildAuraAnchors(aura);
            }

            Vec3 playerPos = player.getPosition(partial);
            Vec3 camera = cameraPos;
            // Build basis from player yaw only (ignore pitch) to keep anchors off-screen when looking down
            Vec3 up = new Vec3(0, 1, 0);
            float yaw = Mth.rotLerp(partial, player.yRotO, player.getYRot());
            Vec3 fwdYaw = Vec3.directionFromRotation(0, yaw).normalize();
            Vec3 back = fwdYaw.scale(-1.0);
            Vec3 right = back.cross(up);
            if (right.lengthSqr() < 1.0e-6) right = new Vec3(1, 0, 0);
            right = right.normalize();

            time = gameTime + partial;

            // Initialize growth timing on first visible render
            if (aura.targetVisible && aura.activateGameTime < 0L) aura.activateGameTime = gameTime;

            // Update fade-in/out
            float fadeSpeed = 0.15f;
            if (aura.targetVisible) {
                if (aura.visibility < 1f) aura.visibility = Math.min(1f, aura.visibility + fadeSpeed);
            } else {
                if (aura.visibility > 0f) aura.visibility = Math.max(0f, aura.visibility - fadeSpeed);
                if (aura.visibility <= 0f) {
                    aura.active = false; // allow cleanup on next pass
                }
            }
            if (aura.visibility <= 0f) continue;

            // Update smoothed player velocity for aura physics using tick positions (cape-like)
            Vec3 posNow = player.position();
            Vec3 posPrevTick = new Vec3(player.xo, player.yo, player.zo);
            Vec3 rawVel = posNow.subtract(posPrevTick); // blocks per tick
            if (rawVel.lengthSqr() < 1.0e-6) {
                // fallback to delta movement if tick pos hasn't changed yet
                rawVel = player.getDeltaMovement();
            }
            double alpha = 0.35; // smoothing factor per frame
            aura.smoothedVel = aura.smoothedVel.scale(1.0 - alpha).add(rawVel.scale(alpha));

			for (int i = 0; i < aura.localAnchors.size(); i++) {
                Vec3 local = aura.localAnchors.get(i);
                // Transform local anchor to world space with a slight breathing motion
				double phase = 0.7 * i + time * 0.03 * Math.max(0.1, aura.orbitSpeed);
				double breathe = 0.05 * Math.sin(phase);
				double bob = 0.06 * Math.sin(time * 0.04 * Math.max(0.1, aura.orbitSpeed) + i * 0.9);
                Vec3 backBase = playerPos.add(0, player.getBbHeight() * 0.50, 0);
                Vec3 anchor = backBase
                        .add(back.scale(0.15 + breathe))
                        .add(right.scale(local.x))
                        .add(up.scale(local.y + bob))
                        .add(aura.smoothedVel.scale(-0.25))
                        .add(back.scale(local.z));

                List<Vec3> pts = buildAuraTentacle(anchor, back, right, up, aura, i, time, aura.smoothedVel);

                // Growth progress with slight per-tentacle stagger
                final double growDuration = 10.0; // ticks to reach full length
                final double perTentacleStagger = 1.2; // ticks between tentacle starts
                float progress;
                if (aura.activateGameTime >= 0L) {
                    double tentacleStart = aura.activateGameTime + i * perTentacleStagger;
                    double rawT = (time - tentacleStart) / growDuration; // includes partials
                    if (rawT <= 0.0) rawT = 0.0;
                    if (rawT > 1.0) rawT = 1.0;
                    progress = easeInOutCubic((float)rawT);
                } else {
                    progress = 1.0f;
                }

                // Trim the path so it appears to extend out from the player
                int totalPts = pts.size();
                int visiblePts = Math.max(2, (int)Math.round(totalPts * progress));
                if (visiblePts < totalPts) {
                    pts = new ArrayList<>(pts.subList(0, visiblePts));
                }

                float base = Math.max(0.02F, aura.thickness * 0.06F);
                float noiseAmp = Math.min(base * 0.9F, Math.max(0.02F, aura.jaggedness));
                // Scale width with both visibility fade and growth progress; alpha scales similarly
                float widthScale = (0.2f + 0.8f * aura.visibility) * (0.35f + 0.65f * progress);
                float alphaScale = aura.visibility * (0.25f + 0.75f * progress);
                drawRibbonGradient(poseStack, buffer, camera, pts,
                        (base * widthScale) * 1.35F,
                        BlackwhipColors.OUTER_GLOW_START,
                        BlackwhipColors.OUTER_GLOW_MID,
                        BlackwhipColors.OUTER_GLOW_END,
                        0.0015F,
                        noiseAmp,
                        time,
                        alphaScale);
                drawRibbonSolid(poseStack, buffer, camera, pts, (base * widthScale) * 0.6F, BlackwhipColors.INNER_CORE, -0.0007F, noiseAmp, time, alphaScale);
            }
        }

        // Render bubble shield tentacles (flower-bud style: converge to apex, then ride the sphere rim)
        for (ClientBubbleState bubble : PLAYER_TO_BUBBLE.values()) {
            if (!bubble.active && !bubble.deactivating) continue;
            Entity src = level.getEntity(bubble.sourcePlayerId);
            if (!(src instanceof Player player)) continue;

            if (bubble.localAnchors.size() != bubble.tentacleCount) rebuildAuraAnchorsLike(bubble);

            Vec3 camera = cameraPos;
            float partialTick = partial;
            Vec3 eye = player.getEyePosition(partialTick);
            Vec3 up = new Vec3(0,1,0);
            // Yaw-only basis for stable back anchors (prevents anchors from popping into view when looking down)
            float yaw = Mth.rotLerp(partialTick, player.yRotO, player.getYRot());
            Vec3 fwdYaw = Vec3.directionFromRotation(0, yaw).normalize();
            Vec3 backYaw = fwdYaw.scale(-1.0);
            Vec3 rightYaw = backYaw.cross(up);
            if (rightYaw.lengthSqr() < 1.0e-6) rightYaw = new Vec3(1,0,0);
            rightYaw = rightYaw.normalize();

            // Sphere center, radius and forward apex point on sphere (yaw-only orientation)
            float r = Math.max(0.25f, bubble.radius);
            // Make the sphere a bit larger overall while keeping the apex at ~1.5 blocks
            float desiredApex = 1.5f; // target distance from eyes to apex
            float baseRadius = r * 1.15f; // slight overall radius increase
            float targetSum = Math.min(desiredApex, bubble.forwardOffset + baseRadius);
            float forwardEff = Math.max(0.2f, targetSum - baseRadius);
            float rEff = targetSum - forwardEff;
            // Lower the sphere center so the apex covers the whole player (legs included)
            double down = Math.max(0.30, Math.min(0.9, player.getBbHeight() * 0.35));
            Vec3 center = eye.add(0, -down, 0).add(fwdYaw.scale(Math.max(0.2f, forwardEff)));

            int n = Math.max(1, bubble.tentacleCount);
            double baseArc = (2.0 * Math.PI) / n; // azimuth distribution around forward axis

            // Initialize animation times on first render after state changes
            if (bubble.active && bubble.activateGameTime < 0L) bubble.activateGameTime = gameTime;
            if (bubble.deactivating && bubble.deactivateGameTime < 0L) bubble.deactivateGameTime = gameTime;
            // Animation timings
            final double growDuration = 5.0; // ticks for each tentacle to reach full length (snappier)
            final double perTentacleStagger = 0.5; // ticks between tentacle starts (snappier)
            final double shrinkDuration = 7.0; // ticks to retract fully (all at once)

			for (int i = 0; i < n; i++) {
                // Compute tentacle progress: either growth (staggered) or shrink (simultaneous)
                float progress;
                if (bubble.active && !bubble.deactivating) {
                    // Growth with per-tentacle stagger
                    double tentacleStart = bubble.activateGameTime + i * perTentacleStagger;
                    double rawT = (time - tentacleStart) / growDuration; // time includes partials
                    if (rawT <= 0.0) continue; // not started yet
                    if (rawT > 1.0) rawT = 1.0;
                    progress = easeInOutCubic((float)rawT);
                } else {
                    // Shrink all at once
                    double rawT = (time - bubble.deactivateGameTime) / shrinkDuration;
                    if (rawT < 0.0) rawT = 0.0;
                    if (rawT > 1.0) rawT = 1.0;
                    float easedShrink = easeInOutCubic((float)rawT);
                    progress = 1.0f - easedShrink; // 1 -> 0 over time
                }
                // Build back anchor on player's upper back
                Vec3 local = bubble.localAnchors.get(i % bubble.localAnchors.size());
                Vec3 backBase = player.getPosition(partialTick).add(0, player.getBbHeight() * 0.47, 0);
                // Push anchors further behind and slightly lower to avoid first-person visibility when looking down
                Vec3 anchor = backBase
                        .add(backYaw.scale(0.38))
                        .add(rightYaw.scale(local.x * 1.25))
                        .add(up.scale(local.y - 0.10))
                        .add(backYaw.scale(local.z + 0.07));

                // Azimuth around the forward axis for this meridian (slight jitter for organic look)
                double phi = i * baseArc + Math.sin((time * 0.05) + i * 1.7) * 0.06;
                Vec3 equatorDir = rightYaw.scale(Math.cos(phi)).add(up.scale(Math.sin(phi))).normalize();

                // Build the petal path: anchor -> back hemisphere contact -> sweep along meridian to front apex (3D)
                List<Vec3> points = new ArrayList<>();

                // Section 1: anchor to a point just outside the back hemisphere (wrap wider around the player)
                {
                    // Start deeper on the back hemisphere to increase arc around camera
                    double thetaStart = Math.PI - (0.22 + 0.08 * Math.sin(i * 1.37));
                    Vec3 backDir = fwdYaw.scale(Math.cos(thetaStart)).add(equatorDir.scale(Math.sin(thetaStart))).normalize();
                    // Slightly outside the sphere to create an outward arch from the back
                    Vec3 backContact = center.add(backDir.scale(rEff * 1.08));
                    double len = backContact.subtract(anchor).length();
                    int seg = Math.max(16, (int)Math.min(54, len * 6.5));
                    points.addAll(buildCurve(anchor, backContact, bubble.curve * 1.75f, seg));
                }

                // Section 2: sweep along the same meridian from backContact toward the front apex (theta → 0)
                {
                    double thetaStart = Math.PI - (0.22 + 0.08 * Math.sin(i * 1.37));
                    int seg = Math.max(36, (int)(rEff * 66));
                    for (int s = 1; s <= seg; s++) {
                        double t = s / (double)seg; // 0..1
                        double theta = (1.0 - t) * thetaStart; // end at 0 (apex)
                        Vec3 p = center.add(
                                fwdYaw.scale(Math.cos(theta) * rEff)
                                        .add(equatorDir.scale(Math.sin(theta) * rEff))
                        );
                        points.add(p);
                    }
                }

				// Trim the path to current growth progress so it appears to extend from the back
                int totalPts = points.size();
                int visiblePts = Math.max(2, (int)Math.round(totalPts * progress));
				if (visiblePts < totalPts) {
					points = new ArrayList<>(points.subList(0, visiblePts));
				}

				// Draw
                // Slight per-tentacle thickness variation and reduced wiggle
                Random rng = new Random(bubble.seed + (long)i * 31L);
                float thicknessJitter = 0.9f + (float)rng.nextDouble() * 0.2f; // 0.9..1.1
                float base = Math.max(0.02F, bubble.thickness * 0.065F * thicknessJitter);
                float noiseAmp = Math.min(base * 0.75F, Math.max(0.01F, bubble.jaggedness)) * 0.65F;
                // Scale width and alpha with progress for smooth appear/disappear
                float widthScale = 0.35f + 0.65f * progress;
                float alphaScale = 0.25f + 0.75f * progress;
				drawRibbonGradient(poseStack, buffer, camera, points,
						(base * 1.30F) * widthScale,
                        BlackwhipColors.OUTER_GLOW_START,
                        BlackwhipColors.OUTER_GLOW_MID,
                        BlackwhipColors.OUTER_GLOW_END,
                        0.0015F,
                        noiseAmp,
						time,
						alphaScale);
				drawRibbonSolid(poseStack, buffer, camera, points, (base * 0.56F) * widthScale, BlackwhipColors.INNER_CORE, -0.0007F, noiseAmp, time, alphaScale);
            }

            // If deactivation finished, stop rendering next frame
            if (bubble.deactivating) {
                double rawT = (time - bubble.deactivateGameTime) / shrinkDuration;
                if (rawT >= 1.0) {
                    bubble.deactivating = false;
                }
            }
        }
    }

    private static void rebuildAuraAnchors(ClientAuraState s) {
        s.localAnchors.clear();
        Random rng = new Random(s.seed);
        // Scatter anchors across an oval region on the upper back
        for (int i = 0; i < s.tentacleCount; i++) {
            double a = 2 * Math.PI * (i / Math.max(1.0, (double) s.tentacleCount)) + rng.nextDouble() * 0.9;
            double rx = 0.35 + rng.nextDouble() * 0.15; // horizontal spread
            double ry = 0.25 + rng.nextDouble() * 0.10; // vertical spread
            double x = Math.cos(a) * rx * 0.45; // straddle spine
            double y = 0.25 + (rng.nextDouble() - 0.3) * ry; // shoulder blades area
            double z = 0.05 + rng.nextDouble() * 0.12; // slight embed into back
            s.localAnchors.add(new Vec3(x, y, z));
        }
    }

    // Build anchors for bubble shield similar to aura placement on back
    private static void rebuildAuraAnchorsLike(ClientBubbleState s) {
        s.localAnchors.clear();
        Random rng = new Random(s.seed);
        for (int i = 0; i < s.tentacleCount; i++) {
            double a = 2 * Math.PI * (i / Math.max(1.0, (double) s.tentacleCount)) + rng.nextDouble() * 0.9;
            double rx = 0.35 + rng.nextDouble() * 0.15;
            double ry = 0.25 + rng.nextDouble() * 0.10;
            double x = Math.cos(a) * rx * 0.45;
            double y = 0.25 + (rng.nextDouble() - 0.3) * ry;
            double z = 0.05 + rng.nextDouble() * 0.12;
            s.localAnchors.add(new Vec3(x, y, z));
        }
    }

    private static List<Vec3> buildAuraTentacle(Vec3 anchor, Vec3 back, Vec3 right, Vec3 up, ClientAuraState aura, int index, double time, Vec3 playerVel) {
        // Downward-curving, side-biased tendril with gentle bobbing and shoulder-peek arc
        Vec3 down = up.scale(-1.0);
        Random rng = new Random(aura.seed + index * 31L);
        double sideBias = (rng.nextDouble() - 0.5) * 0.7; // left/right variation
        // Outward from the back with an initial lift to peek over the shoulder
        Vec3 dir0 = back.scale(0.55).add(up.scale(0.35)).add(right.scale(sideBias * 0.4)).normalize();

        // Per-tentacle radial spread direction around the player to diversify endpoints
        Vec3 forward = back.scale(-1.0);
        double theta = (2.0 * Math.PI * (index / Math.max(1.0, (double) aura.tentacleCount))) + (rng.nextDouble() * 0.5 - 0.25);
        Vec3 ringDir = right.scale(Math.cos(theta)).add(forward.scale(Math.sin(theta))).normalize();
        double spread = 0.45 + 0.55 * rng.nextDouble();

        double lengthBase = aura.length;
        double length = lengthBase * (0.85 + 0.3 * rng.nextDouble()); // vary end positions
        int segments = Math.max(16, (int) Math.min(64, length * 8.0));
        List<Vec3> pts = new ArrayList<>(segments + 1);
        pts.add(anchor);

        // Build a wavy, somewhat jagged spline using simple forward marching with noisy offsets
        Vec3 p = anchor;
        Vec3 t = dir0;
        double wobbleSpeed1 = 0.35 * Math.max(0.1, aura.orbitSpeed);
        double wobbleSpeed2 = 0.55 * Math.max(0.1, aura.orbitSpeed);
        double droop = 0.8 + 0.4 * rng.nextDouble(); // different downward strength per tentacle
        for (int i = 1; i <= segments; i++) {
            double u = i / (double) segments;
            // taper step size so tips move faster visually
            double step = (length / segments) * (0.85 + 0.3 * u);

            // Local orthonormals for noise directions
            Vec3 n1 = t.cross(up);
            if (n1.lengthSqr() < 1.0e-6) n1 = t.cross(right);
            n1 = n1.normalize();
            Vec3 n2 = t.cross(n1).normalize();

            // Two-band wobble + small jagged spikes
            double w1 = Math.sin(time * wobbleSpeed1 + u * 9.0 + index * 0.7) * aura.jaggedness;
            double w2 = Math.cos(time * wobbleSpeed2 + u * 15.0 + index * 1.3) * (aura.jaggedness * 0.6);
            double spike = (rng.nextDouble() - 0.5) * aura.jaggedness * 0.25;
            Vec3 offset = n1.scale(w1).add(n2.scale(w2)).add(n1.cross(n2).normalize().scale(spike));

            // Bend toward a downward arc using curve parameter; add shoulder-peek near base
            Vec3 baseBend = down.scale(droop * aura.curve).add(right.scale(sideBias * 0.7 * aura.curve));
            double uLift = Math.pow(1.0 - u, 1.5);
            double uBack = Math.pow(1.0 - u, 1.2);
            Vec3 shoulderPeek = up.scale(0.9 * aura.curve * uLift)
                    .add(back.scale(0.35 * aura.curve * uBack));
            double uDrift = Math.pow(u, 1.25);
            Vec3 radial = ringDir.scale(spread * aura.curve * uDrift);
            // Movement lag (cape-like): oppose player velocity, stronger toward tip
            double vR = playerVel.dot(right);
            double vU = playerVel.dot(up);
            double vB = playerVel.dot(back);
            Vec3 lag = right.scale(-vR * 3.0).add(up.scale(-vU * 4.0)).add(back.scale(-vB * 3.5));
            double lagScale = Math.pow(u, 1.15);
            if (playerVel.y < 0) {
                // Extra upward lag when falling
                lag = lag.add(up.scale(Math.min(3.0, -playerVel.y * 6.0) * (0.4 + 0.6 * u)));
            }
            Vec3 bend = baseBend.add(shoulderPeek).add(radial).add(lag.scale(lagScale));
            Vec3 desired = t.add(bend.scale(1.15 * (1.0 - u))).normalize();
            t = desired;

            p = p.add(t.scale(step)).add(offset.scale(0.5 * (1.0 - u))); // less jagged at tip
            pts.add(p);
        }
        return pts;
    }

    private static List<Vec3> buildCurve(Vec3 start, Vec3 end, float curveAmount, int segments) {
        Vec3 dir = end.subtract(start);
        double length = dir.length();
        if (length < 1.0e-4) return Collections.singletonList(start);
        dir = dir.scale(1.0 / length);

        Vec3 up = new Vec3(0, 1, 0);
        Vec3 side = up.cross(dir);
        if (side.lengthSqr() < 1.0e-6) side = new Vec3(1, 0, 0).cross(dir);
        side = side.normalize();
        double arc = length * curveAmount * 0.25;
        Vec3 control = start.add(dir.scale(length * 0.5)).add(up.scale(arc * 0.6)).add(side.scale(arc * 0.4));

        List<Vec3> pts = new ArrayList<>(segments + 1);
        for (int i = 0; i <= segments; i++) {
            double t = i / (double) segments;
            pts.add(cubicBezier(start, control, control, end, t));
        }
        return pts;
    }

    private static Vec3 cubicBezier(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, double t) {
        double it = 1.0 - t;
        double b0 = it * it * it;
        double b1 = 3 * it * it * t;
        double b2 = 3 * it * t * t;
        double b3 = t * t * t;
        return new Vec3(
                b0 * p0.x + b1 * p1.x + b2 * p2.x + b3 * p3.x,
                b0 * p0.y + b1 * p1.y + b2 * p2.y + b3 * p3.y,
                b0 * p0.z + b1 * p1.z + b2 * p2.z + b3 * p3.z
        );
    }

    private static void drawRibbonGradient(PoseStack poseStack, MultiBufferSource buffer, Vec3 cameraPos,
                                           List<Vec3> points, float baseHalfWidth,
                                           int startColor, int midColor, int endColor,
                                           float depthBias, float noiseAmplitude, double time, float alphaScale) {
        if (points.size() < 2) return;

        VertexConsumer consumer = buffer.getBuffer(RenderType.leash());
        int light = LightTexture.FULL_BRIGHT;

        poseStack.pushPose();
        PoseStack.Pose last = poseStack.last();

        EntityRenderDispatcher erd = Minecraft.getInstance().getEntityRenderDispatcher();
        Vec3 camForward = Vec3.directionFromRotation(erd.camera.getXRot(), erd.camera.getYRot());

        final int n = points.size();
        for (int i = 0; i < n; i++) {
            double t = i / (double)(n - 1);
            Vec3 p = points.get(i).subtract(cameraPos);

            Vec3 tangent;
            if (i == 0) tangent = points.get(i + 1).subtract(points.get(i));
            else if (i == n - 1) tangent = points.get(i).subtract(points.get(i - 1));
            else tangent = points.get(i + 1).subtract(points.get(i - 1));
            if (tangent.lengthSqr() < 1e-6) tangent = new Vec3(0, 1, 0);

            // Apply small animated noise offset independent of camera
            Vec3 noise = computeRibbonNoise(tangent, t, time, noiseAmplitude);
            p = p.add(noise);

            Vec3 normal = camForward.cross(tangent).normalize();
            if (normal.lengthSqr() < 1e-6) normal = new Vec3(0, 1, 0);

            // Taper towards the tip
            float halfWidth = (float)(baseHalfWidth * (0.85 + 0.15 * (1.0 - t)));
            Vec3 left = normal.scale(halfWidth);
            Vec3 right = normal.scale(-halfWidth);
            Vec3 bias = camForward.scale(depthBias);

            int argb = lerpGradient(t, startColor, midColor, endColor);
            float a = ((argb >> 24) & 0xFF) / 255f;
            a *= alphaScale;
            float r = ((argb >> 16) & 0xFF) / 255f;
            float g = ((argb >> 8) & 0xFF) / 255f;
            float b = (argb & 0xFF) / 255f;

            // Speckle the color to simulate texture without assets
            final float speckleStrength = 0.22f;
            float nL = speckleNoise(t, -1.0, time);
            float nR = speckleNoise(t,  1.0, time);
            float shadeL = (nL - 0.5f) * 2.0f * speckleStrength;
            float shadeR = (nR - 0.5f) * 2.0f * speckleStrength;
            float rL = clamp01(r * (1.0f + shadeL));
            float gL = clamp01(g * (1.0f + shadeL));
            float bL = clamp01(b * (1.0f + shadeL));
            float rR = clamp01(r * (1.0f + shadeR));
            float gR = clamp01(g * (1.0f + shadeR));
            float bR = clamp01(b * (1.0f + shadeR));

            consumer.vertex(last.pose(), (float) (p.x + left.x + bias.x), (float) (p.y + left.y + bias.y), (float) (p.z + left.z + bias.z))
                    .color(rL, gL, bL, a).uv2(light).endVertex();
            consumer.vertex(last.pose(), (float) (p.x + right.x + bias.x), (float) (p.y + right.y + bias.y), (float) (p.z + right.z + bias.z))
                    .color(rR, gR, bR, a).uv2(light).endVertex();
        }
        poseStack.popPose();
    }

    private static void drawRibbonSolid(PoseStack poseStack, MultiBufferSource buffer, Vec3 cameraPos,
                                       List<Vec3> points, float halfWidth, int argb, float depthBias, float noiseAmplitude, double time, float alphaScale) {
		if (points.size() < 2) return;

		VertexConsumer consumer = buffer.getBuffer(RenderType.leash());
		int light = LightTexture.FULL_BRIGHT;

		poseStack.pushPose();
		PoseStack.Pose last = poseStack.last();

		EntityRenderDispatcher erd = Minecraft.getInstance().getEntityRenderDispatcher();
		Vec3 camForward = Vec3.directionFromRotation(erd.camera.getXRot(), erd.camera.getYRot());

        float a = ((argb >> 24) & 0xFF) / 255f;
        a *= alphaScale;
		float r = ((argb >> 16) & 0xFF) / 255f;
		float g = ((argb >> 8) & 0xFF) / 255f;
		float b = (argb & 0xFF) / 255f;

        final int n = points.size();
		for (int i = 0; i < n; i++) {
			Vec3 p = points.get(i).subtract(cameraPos);
			Vec3 tangent;
			if (i == 0) tangent = points.get(i + 1).subtract(points.get(i));
			else if (i == n - 1) tangent = points.get(i).subtract(points.get(i - 1));
			else tangent = points.get(i + 1).subtract(points.get(i - 1));
			if (tangent.lengthSqr() < 1e-6) tangent = new Vec3(0, 1, 0);

			// Apply small animated noise offset independent of camera
			double t = i / (double)(n - 1);
			Vec3 noise = computeRibbonNoise(tangent, t, time, noiseAmplitude);
			p = p.add(noise);

			Vec3 normal = camForward.cross(tangent).normalize();
			if (normal.lengthSqr() < 1e-6) normal = new Vec3(0, 1, 0);
            Vec3 left = normal.scale(halfWidth);
            Vec3 right = normal.scale(-halfWidth);
            Vec3 bias = camForward.scale(depthBias);

			// Speckle the color to simulate texture
			final float speckleStrength = 0.18f;
			final float alphaDropStrength = 0.45f; // let glow peek through on bright flecks
			float nL = speckleNoise(t, -1.0, time);
			float nR = speckleNoise(t,  1.0, time);
			float shadeL = (nL - 0.5f) * 2.0f * speckleStrength;
			float shadeR = (nR - 0.5f) * 2.0f * speckleStrength;
			float rL = clamp01(r * (1.0f + shadeL));
			float gL = clamp01(g * (1.0f + shadeL));
			float bL = clamp01(b * (1.0f + shadeL));
			float rR = clamp01(r * (1.0f + shadeR));
			float gR = clamp01(g * (1.0f + shadeR));
			float bR = clamp01(b * (1.0f + shadeR));

			// Drop alpha slightly on brighter speckles to reveal blue glow beneath
			float baseA = a * 0.9f;
			float alphaDropL = Math.max(0f, (nL - 0.5f) * 2.0f);
			float alphaDropR = Math.max(0f, (nR - 0.5f) * 2.0f);
			float aL = clamp01(baseA * (1.0f - alphaDropStrength * alphaDropL));
			float aR = clamp01(baseA * (1.0f - alphaDropStrength * alphaDropR));

			consumer.vertex(last.pose(), (float) (p.x + left.x + bias.x), (float) (p.y + left.y + bias.y), (float) (p.z + left.z + bias.z))
					.color(rL, gL, bL, aL).uv2(light).endVertex();
			consumer.vertex(last.pose(), (float) (p.x + right.x + bias.x), (float) (p.y + right.y + bias.y), (float) (p.z + right.z + bias.z))
					.color(rR, gR, bR, aR).uv2(light).endVertex();
		}

		poseStack.popPose();
	}

    private static Vec3 computeRibbonNoise(Vec3 tangent, double t, double time, float amplitude) {
        if (amplitude <= 0.0f) return Vec3.ZERO;
        Vec3 tan = tangent.normalize();
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 n1 = tan.cross(up);
        if (n1.lengthSqr() < 1.0e-6) n1 = tan.cross(new Vec3(1, 0, 0));
        n1 = n1.normalize();
        Vec3 n2 = tan.cross(n1).normalize();

        // Slow temporal motion so texture remains readable
        double speed1 = 0.7;
        double speed2 = 0.95;
        double k1 = 10.0;
        double k2 = 17.0;

        double a1 = Math.sin(time * speed1 + t * k1) * amplitude;
        double a2 = Math.cos(time * speed2 + t * k2) * (amplitude * 0.7);
        return n1.scale(a1).add(n2.scale(a2));
    }

    private static float speckleNoise(double x, double y, double time) {
        // Two-octave, cheap procedural noise in [0,1]
        double v1 = Math.sin(x * 13.11 + time * 0.83) * Math.cos(y * 11.73 - time * 1.07);
        double v2 = Math.sin(x * 31.33 - time * 1.71) * Math.cos(y * 27.59 + time * 0.61);
        double v = 0.5 * (v1 * 0.5 + 0.5) + 0.5 * (v2 * 0.5 + 0.5);
        if (v < 0.0) v = 0.0;
        if (v > 1.0) v = 1.0;
        return (float)v;
    }

    private static float clamp01(float v) {
        if (v < 0f) return 0f;
        if (v > 1f) return 1f;
        return v;
    }

    private static float easeInOutCubic(float t) {
        if (t < 0.5f) return 4f * t * t * t;
        t -= 1f;
        return 1f + 4f * t * t * t;
    }

    private static float easeOutQuad(float t) {
        float it = 1f - t;
        return 1f - it * it;
    }

    private static int lerpGradient(double t, int start, int mid, int end) {
        if (t < 0.5) {
            double lt = t / 0.5;
            return lerpColor(start, mid, lt);
        } else {
            double lt = (t - 0.5) / 0.5;
            return lerpColor(mid, end, lt);
        }
    }

    private static int lerpColor(int c0, int c1, double t) {
        int a0 = (c0 >> 24) & 0xFF, r0 = (c0 >> 16) & 0xFF, g0 = (c0 >> 8) & 0xFF, b0 = c0 & 0xFF;
        int a1 = (c1 >> 24) & 0xFF, r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
        int a = (int)Math.round(a0 + (a1 - a0) * t);
        int r = (int)Math.round(r0 + (r1 - r0) * t);
        int g = (int)Math.round(g0 + (g1 - g0) * t);
        int b = (int)Math.round(b0 + (b1 - b0) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static Vec3 getAttachPoint(Entity target, float partial) {
        Vec3 base = target.getPosition(partial);
        return base.add(0, target.getBbHeight() * 0.6, 0);
    }

    private static Vec3 getHandPosition(Player player, float partial) {
		// Use yaw-only basis to place start near the main-hand side of the torso
		Vec3 up = new Vec3(0, 1, 0);
		float yaw = Mth.rotLerp(partial, player.yRotO, player.getYRot());
		Vec3 fwdYaw = Vec3.directionFromRotation(0, yaw).normalize();
		Vec3 rightYaw = fwdYaw.cross(up);
		if (rightYaw.lengthSqr() < 1.0e-6) rightYaw = new Vec3(1, 0, 0);
		rightYaw = rightYaw.normalize();
		float sideDir = player.getMainArm() == HumanoidArm.RIGHT ? 1.0f : -1.0f;

		// Base point roughly at shoulder height, offset outward to the hand side and slightly forward
		double shoulderHeight = Math.max(0.4, Math.min(0.8, player.getBbHeight() * 0.62));
		double crouchAdjust = player.isCrouching() ? -0.10 : 0.0;
		Vec3 base = player.getPosition(partial).add(0, shoulderHeight + crouchAdjust, 0);

		// Add a small component of current look to push the start forward with camera aim
		Vec3 look = player.getViewVector(partial).normalize();
		return base
				.add(rightYaw.scale(0.42 * sideDir))
				.add(fwdYaw.scale(0.28))
				.add(look.scale(0.10));
    }

    private static Vec3 getHandPositionForSide(Player player, float partial, float sideDir) {
        Vec3 up = new Vec3(0, 1, 0);
        float yaw = Mth.rotLerp(partial, player.yRotO, player.getYRot());
        Vec3 fwdYaw = Vec3.directionFromRotation(0, yaw).normalize();
        Vec3 rightYaw = fwdYaw.cross(up);
        if (rightYaw.lengthSqr() < 1.0e-6) rightYaw = new Vec3(1, 0, 0);
        rightYaw = rightYaw.normalize();

        double shoulderHeight = Math.max(0.4, Math.min(0.8, player.getBbHeight() * 0.62));
        double crouchAdjust = player.isCrouching() ? -0.10 : 0.0;
        Vec3 base = player.getPosition(partial).add(0, shoulderHeight + crouchAdjust, 0);

        Vec3 look = player.getViewVector(partial).normalize();
        return base
                .add(rightYaw.scale(0.42 * sideDir))
                .add(fwdYaw.scale(0.28))
                .add(look.scale(0.10));
    }

    // Build and draw several closed-loop ribbons that wrap around an entity's horizontal bounds.
    // This scales to any hitbox size to convey the feeling of being tied up.
    private static void drawEntityWrapRings(PoseStack poseStack, MultiBufferSource buffer, Vec3 cameraPos,
                                            Entity target, float partial, float baseHalfWidth, float noiseAmp, double time) {
        if (target == null || !target.isAlive()) return;
        var bb = target.getBoundingBox();
        double xSize = bb.getXsize();
        double zSize = bb.getZsize();
        double ySize = bb.getYsize();
        if (xSize <= 0 || zSize <= 0 || ySize <= 0) return;

        double cx = (bb.minX + bb.maxX) * 0.5;
        double cz = (bb.minZ + bb.maxZ) * 0.5;

        // Radius slightly larger than half the max horizontal size so the wrap sits around the body
        double radius = Math.max(xSize, zSize) * 0.5 + 0.08;
        // Number of bands based on height; clamp for readability
        int bands = Math.max(2, Math.min(5, (int)Math.round(ySize / 0.8)));
        // Evenly distribute wraps between ~30%..80% of height
        double yStart = bb.minY + ySize * 0.3;
        double yEnd = bb.minY + ySize * 0.8;

        // For block stacks, push rings further out and slightly higher to avoid intersecting corners
        if (target instanceof BlockStackEntity) {
            radius += 0.18; // extra clearance beyond block corners
            yStart = bb.minY + ySize * 0.40;
            yEnd = bb.minY + ySize * 0.95;
        }

        for (int i = 0; i < bands; i++) {
            double t = bands == 1 ? 0.5 : (i / (double)(bands - 1));
            double y = yStart + (yEnd - yStart) * t;

            // Slight pulsation/irregularity so it feels organic
            double phase = time * 0.12 + i * 1.73;
            double rJitter = radius * 0.06 * Math.sin(phase * 1.3);
            List<Vec3> ring = buildClosedRing(new Vec3(cx, y, cz), radius + rJitter, 64 + (int)Math.min(64, radius * 48));

            float widthScale = 0.95f + (float)(0.1 * Math.sin(phase));
            float wrapGlow = baseHalfWidth * 1.15f * widthScale;
            float wrapCore = baseHalfWidth * 0.52f * widthScale;

            drawRibbonGradient(poseStack, buffer, cameraPos, ring,
                    wrapGlow,
                    BlackwhipColors.OUTER_GLOW_START,
                    BlackwhipColors.OUTER_GLOW_MID,
                    BlackwhipColors.OUTER_GLOW_END,
                    0.0015F,
                    noiseAmp,
                    time,
                    1.0f);
            drawRibbonSolid(poseStack, buffer, cameraPos, ring, wrapCore, BlackwhipColors.INNER_CORE, -0.0007F, noiseAmp, time, 1.0f);
        }
    }

    private static List<Vec3> buildClosedRing(Vec3 center, double radius, int segments) {
        int seg = Math.max(16, Math.min(128, segments));
        List<Vec3> pts = new ArrayList<>(seg + 1);
        for (int k = 0; k <= seg; k++) {
            double a = (2.0 * Math.PI) * (k / (double)seg);
            double x = center.x + Math.cos(a) * radius;
            double z = center.z + Math.sin(a) * radius;
            pts.add(new Vec3(x, center.y, z));
        }
        return pts;
    }
}