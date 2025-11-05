package com.github.b4ndithelps.forge.client.blackwhip;

import com.github.b4ndithelps.BanditsQuirkLib;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
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
    private static final class ClientMultiTethers {
        public final int sourcePlayerId;
        public boolean active;
        public float curve;
        public float thickness;
        public java.util.List<Integer> targetEntityIds = new java.util.ArrayList<>();

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
        public java.util.List<Vec3> localAnchors = new java.util.ArrayList<>();
        // Smoothed world velocity and last world position for movement-based lag
        public Vec3 lastWorldPos;
        public Vec3 smoothedVel = Vec3.ZERO;

        public ClientAuraState(int sourcePlayerId) { this.sourcePlayerId = sourcePlayerId; }
    }
    private static final Map<Integer, ClientAuraState> PLAYER_TO_AURA = new HashMap<>();

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
        } else {
            // begin fade-out; keep active until visibility reaches 0
            s.targetVisible = false;
            if (!s.active) s.active = true;
        }
    }

    public static void applyPacket(int sourcePlayerId, boolean active, boolean restraining, int targetEntityId,
                                   int ticksLeft, int missRetractTicks, float range, float curve, float thickness) {
        ClientWhipState state = PLAYER_TO_WHIP.computeIfAbsent(sourcePlayerId, ClientWhipState::new);
        state.active = active;
        state.restraining = restraining;
        state.targetEntityId = targetEntityId;
        state.ticksLeft = ticksLeft;
        state.missRetractTicks = missRetractTicks;
        state.range = range;
        state.curve = curve;
        state.thickness = thickness;
        if (active && !restraining && targetEntityId >= 0) {
            // capture initial travel duration to drive smooth progress on client
            state.initialTravelTicks = ticksLeft;
        }
        state.lastGameTimeDecrement = -1L;
        if (!active) {
            // allow quick cleanup on next render
            state.ticksLeft = 0;
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

    public static void applyTethersPacket(int sourcePlayerId, boolean active, float curve, float thickness, java.util.List<Integer> targetIds) {
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
        PLAYER_TO_MULTI.entrySet().removeIf(e -> level.getEntity(e.getKey()) == null || !e.getValue().active);
        PLAYER_TO_AURA.entrySet().removeIf(e -> level.getEntity(e.getKey()) == null || (!e.getValue().active && e.getValue().visibility <= 0f));

        long gameTime = level.getGameTime();
        double time = gameTime + partial;
        for (ClientWhipState state : PLAYER_TO_WHIP.values()) {
            if (!state.active) continue;
            Entity src = level.getEntity(state.sourcePlayerId);
            if (!(src instanceof Player player)) continue;

            // Compute start from hand (camera-smooth)
            Vec3 start = getHandPosition(player, partial);

            // Resolve end
            Vec3 end;
            if (state.restraining && state.targetEntityId >= 0) {
                Entity target = level.getEntity(state.targetEntityId);
                if (target == null || !target.isAlive()) continue;
                end = getAttachPoint(target, partial);
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
			float noiseAmp = Math.min(base * 0.75F, 0.08F);
			// Outer glow first (slightly wider, semi-transparent) — push slightly away from camera to avoid coplanar artifacts
            drawRibbonGradient(poseStack, buffer, cameraPos, points,
					base * 1.25F,
					0xB319E8DB, // start teal with alpha
					0xB321E59A, // mid greenish-teal
					0xB333F2FF, // end aqua
					0.0015F,
                    noiseAmp,
                    time,
                    1.0f
			);
			// Inner core on top (narrower, very dark blue-black) — pull slightly toward camera
            drawRibbonSolid(poseStack, buffer, cameraPos, points, base * 0.54F, 0xFF0A0F11, -0.0007F, noiseAmp, time, 1.0f);

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
                    base * 1.25F, 0xB319E8DB, 0xB321E59A, 0xB333F2FF, 0.0015F, noiseAmp, time, 1.0f);
            drawRibbonSolid(poseStack, buffer, cameraPos, points, base * 0.54F, 0xFF0A0F11, -0.0007F, noiseAmp, time, 1.0f);

            if (state.lastGameTimeDecrement != gameTime) {
                state.lastGameTimeDecrement = gameTime;
                if (state.ticksLeft > 0) state.ticksLeft -= 1; // stop at 0 to keep anchored visual
            }
        }

        // Render multi tethers
        for (ClientMultiTethers multi : PLAYER_TO_MULTI.values()) {
            if (!multi.active) continue;
            Entity src = level.getEntity(multi.sourcePlayerId);
            if (!(src instanceof Player player)) continue;

            Vec3 start = getHandPosition(player, partial);
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
                        0xB319E8DB,
                        0xB321E59A,
                        0xB333F2FF,
                        0.0015F,
                        noiseAmp,
                        time,
                        1.0f);
                drawRibbonSolid(poseStack, buffer, cameraPos, points, base * 0.54F, 0xFF0A0F11, -0.0007F, noiseAmp, time, 1.0f);
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
            Vec3 fwdYaw = Vec3.directionFromRotation(0, player.getYRot()).normalize();
            Vec3 back = fwdYaw.scale(-1.0);
            Vec3 right = back.cross(up);
            if (right.lengthSqr() < 1.0e-6) right = new Vec3(1, 0, 0);
            right = right.normalize();

            time = gameTime + partial;

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

                java.util.List<Vec3> pts = buildAuraTentacle(anchor, back, right, up, aura, i, time, aura.smoothedVel);

                float base = Math.max(0.02F, aura.thickness * 0.06F);
                float noiseAmp = Math.min(base * 0.9F, Math.max(0.02F, aura.jaggedness));
            float widthScale = 0.2f + 0.8f * aura.visibility;
            drawRibbonGradient(poseStack, buffer, camera, pts,
                        (base * widthScale) * 1.35F,
                        0xB319E8DB,
                        0xB321E59A,
                        0xB333F2FF,
                        0.0015F,
                        noiseAmp,
                        time,
                        aura.visibility);
                drawRibbonSolid(poseStack, buffer, camera, pts, (base * widthScale) * 0.6F, 0xFF0A0F11, -0.0007F, noiseAmp, time, aura.visibility);
            }
        }
    }

    private static void rebuildAuraAnchors(ClientAuraState s) {
        s.localAnchors.clear();
        java.util.Random rng = new java.util.Random(s.seed);
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

    private static java.util.List<Vec3> buildAuraTentacle(Vec3 anchor, Vec3 back, Vec3 right, Vec3 up, ClientAuraState aura, int index, double time, Vec3 playerVel) {
        // Downward-curving, side-biased tendril with gentle bobbing and shoulder-peek arc
        Vec3 down = up.scale(-1.0);
        java.util.Random rng = new java.util.Random(aura.seed + index * 31L);
        double sideBias = (rng.nextDouble() - 0.5) * 0.7; // left/right variation
        // Outward from the back with an initial lift to peek over the shoulder
        Vec3 outward = back.scale(0.8).add(up.scale(0.35)).add(right.scale(sideBias)).normalize();
        Vec3 dir0 = back.scale(0.55).add(up.scale(0.35)).add(right.scale(sideBias * 0.4)).normalize();

        // Per-tentacle radial spread direction around the player to diversify endpoints
        Vec3 forward = back.scale(-1.0);
        double theta = (2.0 * Math.PI * (index / Math.max(1.0, (double) aura.tentacleCount))) + (rng.nextDouble() * 0.5 - 0.25);
        Vec3 ringDir = right.scale(Math.cos(theta)).add(forward.scale(Math.sin(theta))).normalize();
        double spread = 0.45 + 0.55 * rng.nextDouble();

        double lengthBase = aura.length;
        double length = lengthBase * (0.85 + 0.3 * rng.nextDouble()); // vary end positions
        int segments = Math.max(16, (int) Math.min(64, length * 8.0));
        java.util.List<Vec3> pts = new java.util.ArrayList<>(segments + 1);
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
        com.mojang.blaze3d.vertex.PoseStack.Pose last = poseStack.last();

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
		com.mojang.blaze3d.vertex.PoseStack.Pose last = poseStack.last();

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
        Vec3 eye = player.getEyePosition(partial);
        Vec3 look = player.getViewVector(partial);
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 right = look.cross(up);
        if (right.lengthSqr() < 1.0e-6) right = new Vec3(1, 0, 0);
        right = right.normalize();
        float sideDir = player.getMainArm() == HumanoidArm.RIGHT ? 1.0f : -1.0f;
        return eye.add(0, -0.2, 0).add(right.scale(0.35 * sideDir));
    }
}


