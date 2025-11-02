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
        public long lastGameTimeDecrement = -1L;

        public ClientWhipState(int sourcePlayerId) {
            this.sourcePlayerId = sourcePlayerId;
        }
    }

    private static final Map<Integer, ClientWhipState> PLAYER_TO_WHIP = new HashMap<>();

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
        state.lastGameTimeDecrement = -1L;
        if (!active) {
            // allow quick cleanup on next render
            state.ticksLeft = 0;
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

        long gameTime = level.getGameTime();
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
                float total = Math.max(1, state.missRetractTicks);
                float progress = 1.0F - Math.max(0F, Math.min(1F, (float) state.ticksLeft / total));
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
			// Outer glow first (slightly wider, semi-transparent) — push slightly away from camera to avoid coplanar artifacts
			drawRibbonGradient(poseStack, buffer, cameraPos, points,
					base * 1.25F,
					0xB319E8DB, // start teal with alpha
					0xB321E59A, // mid greenish-teal
					0xB333F2FF, // end aqua
					0.0015F
			);
			// Inner core on top (narrower, very dark blue-black) — pull slightly toward camera
			drawRibbonSolid(poseStack, buffer, cameraPos, points, base * 0.54F, 0xFF0A0F11, -0.0007F);

            // Decrement once per game tick to match server tick rate
            if (!state.restraining && state.lastGameTimeDecrement != gameTime) {
                state.lastGameTimeDecrement = gameTime;
                state.ticksLeft -= 1;
                if (state.ticksLeft <= 0 && state.targetEntityId < 0) {
                    state.active = false; // end local-only miss retract
                }
            }
        }
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
                                           float depthBias) {
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

            Vec3 normal = camForward.cross(tangent).normalize();
            if (normal.lengthSqr() < 1e-6) normal = new Vec3(0, 1, 0);

            // Taper towards the tip
            float halfWidth = (float)(baseHalfWidth * (0.85 + 0.15 * (1.0 - t)));
            Vec3 left = normal.scale(halfWidth);
            Vec3 right = normal.scale(-halfWidth);
            Vec3 bias = camForward.scale(depthBias);

            int argb = lerpGradient(t, startColor, midColor, endColor);
            float a = ((argb >> 24) & 0xFF) / 255f;
            float r = ((argb >> 16) & 0xFF) / 255f;
            float g = ((argb >> 8) & 0xFF) / 255f;
            float b = (argb & 0xFF) / 255f;

            consumer.vertex(last.pose(), (float) (p.x + left.x + bias.x), (float) (p.y + left.y + bias.y), (float) (p.z + left.z + bias.z))
                    .color(r, g, b, a).uv2(light).endVertex();
            consumer.vertex(last.pose(), (float) (p.x + right.x + bias.x), (float) (p.y + right.y + bias.y), (float) (p.z + right.z + bias.z))
                    .color(r, g, b, a).uv2(light).endVertex();
        }
        poseStack.popPose();
    }

    private static void drawRibbonSolid(PoseStack poseStack, MultiBufferSource buffer, Vec3 cameraPos,
                                       List<Vec3> points, float halfWidth, int argb, float depthBias) {
		if (points.size() < 2) return;

		VertexConsumer consumer = buffer.getBuffer(RenderType.leash());
		int light = LightTexture.FULL_BRIGHT;

		poseStack.pushPose();
		com.mojang.blaze3d.vertex.PoseStack.Pose last = poseStack.last();

		EntityRenderDispatcher erd = Minecraft.getInstance().getEntityRenderDispatcher();
		Vec3 camForward = Vec3.directionFromRotation(erd.camera.getXRot(), erd.camera.getYRot());

		float a = ((argb >> 24) & 0xFF) / 255f;
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

			Vec3 normal = camForward.cross(tangent).normalize();
			if (normal.lengthSqr() < 1e-6) normal = new Vec3(0, 1, 0);
            Vec3 left = normal.scale(halfWidth);
            Vec3 right = normal.scale(-halfWidth);
            Vec3 bias = camForward.scale(depthBias);

            consumer.vertex(last.pose(), (float) (p.x + left.x + bias.x), (float) (p.y + left.y + bias.y), (float) (p.z + left.z + bias.z))
					.color(r, g, b, a).uv2(light).endVertex();
            consumer.vertex(last.pose(), (float) (p.x + right.x + bias.x), (float) (p.y + right.y + bias.y), (float) (p.z + right.z + bias.z))
					.color(r, g, b, a).uv2(light).endVertex();
		}

		poseStack.popPose();
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


