package com.github.b4ndithelps.forge.abilities;

import com.github.b4ndithelps.forge.entities.BlockStackEntity;
import com.github.b4ndithelps.forge.network.BQLNetwork;
import com.github.b4ndithelps.forge.network.BlackwhipMultiBlockWhipPacket;
import com.github.b4ndithelps.forge.network.BlackwhipTethersPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("removal")
public class BlackwhipBlockGrabAbility extends Ability {

	public static final PalladiumProperty<Float> RANGE = new FloatProperty("range").configurable("Max range to grab a 1x3 stack");
	public static final PalladiumProperty<Integer> TRAVEL_TICKS = new IntegerProperty("travel_ticks").configurable("Tendril travel time to the stack");
	public static final PalladiumProperty<Float> WHIP_CURVE = new FloatProperty("whip_curve").configurable("Visual arc of the whip");
	public static final PalladiumProperty<Float> WHIP_THICKNESS = new FloatProperty("whip_thickness").configurable("Visual thickness of the whip");
	public static final PalladiumProperty<Float> THROW_SPEED = new FloatProperty("throw_speed").configurable("Throw speed for the captured stack");
	public static final PalladiumProperty<Float> DAMAGE = new FloatProperty("damage").configurable("Damage when the thrown stack hits a target");

	// Unique state
	public static final PalladiumProperty<Boolean> HAS_CARRIED = new BooleanProperty("has_carried");
	public static final PalladiumProperty<Integer> CARRIED_ENTITY_ID = new IntegerProperty("carried_entity_id");
	// 0=idle, 1=traveling_to_blocks, 2=retracting_with_stack, 3=carried_attached
	public static final PalladiumProperty<Integer> STATE = new IntegerProperty("state");
	public static final PalladiumProperty<Integer> CAPTURE_TICKS_LEFT = new IntegerProperty("capture_ticks_left");
	public static final PalladiumProperty<Integer> RETRACT_TICKS_LEFT = new IntegerProperty("retract_ticks_left");
	public static final PalladiumProperty<Double> P0X = new DoubleProperty("p0x");
	public static final PalladiumProperty<Double> P0Y = new DoubleProperty("p0y");
	public static final PalladiumProperty<Double> P0Z = new DoubleProperty("p0z");
	public static final PalladiumProperty<Double> P1X = new DoubleProperty("p1x");
	public static final PalladiumProperty<Double> P1Y = new DoubleProperty("p1y");
	public static final PalladiumProperty<Double> P1Z = new DoubleProperty("p1z");
	public static final PalladiumProperty<Double> P2X = new DoubleProperty("p2x");
	public static final PalladiumProperty<Double> P2Y = new DoubleProperty("p2y");
	public static final PalladiumProperty<Double> P2Z = new DoubleProperty("p2z");

	public BlackwhipBlockGrabAbility() {
		super();
		this.withProperty(RANGE, 16.0F)
				.withProperty(TRAVEL_TICKS, 8)
				.withProperty(WHIP_CURVE, 0.6F)
				.withProperty(WHIP_THICKNESS, 1.0F)
				.withProperty(THROW_SPEED, 1.6F)
				.withProperty(DAMAGE, 8.0F);
	}

	@Override
	public void registerUniqueProperties(PropertyManager manager) {
		manager.register(HAS_CARRIED, false);
		manager.register(CARRIED_ENTITY_ID, -1);
		manager.register(STATE, 0);
		manager.register(CAPTURE_TICKS_LEFT, 0);
		manager.register(RETRACT_TICKS_LEFT, 0);
		manager.register(P0X, 0.0);
		manager.register(P0Y, 0.0);
		manager.register(P0Z, 0.0);
		manager.register(P1X, 0.0);
		manager.register(P1Y, 0.0);
		manager.register(P1Z, 0.0);
		manager.register(P2X, 0.0);
		manager.register(P2Y, 0.0);
		manager.register(P2Z, 0.0);
	}

	@Override
	public void firstTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
		if (!enabled) return;
		if (!(entity instanceof ServerPlayer player)) return;

		// If currently carrying, throw it
		if (Boolean.TRUE.equals(entry.getProperty(HAS_CARRIED)) || entry.getProperty(STATE) == 3) {
			int id = entry.getProperty(CARRIED_ENTITY_ID);
			if (id >= 0 && player.level().getEntity(id) instanceof BlockStackEntity stack && stack.isAlive()) {
				stack.setDamageAmount(Math.max(0.0F, entry.getProperty(DAMAGE)));
				stack.setThrowSpeed(Math.max(0.1F, entry.getProperty(THROW_SPEED)));
				stack.throwForward(player);
				// stop rendering persistent tether from player to carried entity
				BQLNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
						new BlackwhipTethersPacket(player.getId(), false, entry.getProperty(WHIP_CURVE), entry.getProperty(WHIP_THICKNESS), List.of(id)));
				player.level().playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 0.9f, 1.0f);
			}
			entry.setUniqueProperty(HAS_CARRIED, false);
			entry.setUniqueProperty(CARRIED_ENTITY_ID, -1);
			entry.setUniqueProperty(STATE, 0);
			return;
		}

		// Not carrying: try to capture a 1x3 column the player is looking at
		float range = Math.max(1.0F, entry.getProperty(RANGE));
		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(player.getLookAngle().scale(range));

		BlockHitResult bhr = player.level().clip(new ClipContext(
				eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
		if (bhr.getType() != HitResult.Type.BLOCK) {
			return;
		}
		BlockPos hit = bhr.getBlockPos();

		List<BlockPos> column = findVerticalColumn((ServerLevel) player.level(), hit);
		if (column == null) return;

		// Tendril visuals to each block center
		List<Double> xs = new ArrayList<>(3);
		List<Double> ys = new ArrayList<>(3);
		List<Double> zs = new ArrayList<>(3);
		for (BlockPos p : column) {
			Vec3 c = Vec3.atCenterOf(p);
			xs.add(c.x); ys.add(c.y); zs.add(c.z);
		}
		BQLNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
				new BlackwhipMultiBlockWhipPacket(player.getId(), true, xs, ys, zs, Math.max(1, entry.getProperty(TRAVEL_TICKS)), entry.getProperty(WHIP_CURVE), entry.getProperty(WHIP_THICKNESS)));
		player.level().playSound(null, player.blockPosition(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 0.7f, 1.0f);

		// Save target positions and wait for travel to complete before capture
		Vec3 c0 = Vec3.atCenterOf(column.get(0));
		Vec3 c1 = Vec3.atCenterOf(column.get(1));
		Vec3 c2 = Vec3.atCenterOf(column.get(2));
		entry.setUniqueProperty(P0X, c0.x); entry.setUniqueProperty(P0Y, c0.y); entry.setUniqueProperty(P0Z, c0.z);
		entry.setUniqueProperty(P1X, c1.x); entry.setUniqueProperty(P1Y, c1.y); entry.setUniqueProperty(P1Z, c1.z);
		entry.setUniqueProperty(P2X, c2.x); entry.setUniqueProperty(P2Y, c2.y); entry.setUniqueProperty(P2Z, c2.z);
		entry.setUniqueProperty(CAPTURE_TICKS_LEFT, Math.max(1, entry.getProperty(TRAVEL_TICKS)));
		entry.setUniqueProperty(STATE, 1);
	}

	@Override
	public void tick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
		if (!(entity instanceof ServerPlayer player)) return;
		int state = entry.getProperty(STATE);
		if (state == 1) {
			int left = entry.getProperty(CAPTURE_TICKS_LEFT);
			if (left > 0) {
				entry.setUniqueProperty(CAPTURE_TICKS_LEFT, left - 1);
			} else {
				// Travel finished: remove blocks, spawn stack at source, begin retract
				BlockPos p0 = BlockPos.containing(entry.getProperty(P0X), entry.getProperty(P0Y), entry.getProperty(P0Z));
				BlockPos p1 = BlockPos.containing(entry.getProperty(P1X), entry.getProperty(P1Y), entry.getProperty(P1Z));
				BlockPos p2 = BlockPos.containing(entry.getProperty(P2X), entry.getProperty(P2Y), entry.getProperty(P2Z));
				BlockState b = player.level().getBlockState(p0);
				BlockState m = player.level().getBlockState(p1);
				BlockState t = player.level().getBlockState(p2);
				((ServerLevel)player.level()).setBlock(p0, Blocks.AIR.defaultBlockState(), 3);
				((ServerLevel)player.level()).setBlock(p1, Blocks.AIR.defaultBlockState(), 3);
				((ServerLevel)player.level()).setBlock(p2, Blocks.AIR.defaultBlockState(), 3);

				BlockStackEntity stack = BlockStackEntity.create((ServerLevel) player.level(), player, b, m, t,
						Math.max(0.0F, entry.getProperty(DAMAGE)), Math.max(0.1F, entry.getProperty(THROW_SPEED)));
				Vec3 spawn = Vec3.atCenterOf(p1).add(0, -0.5, 0);
				stack.setPos(spawn.x, spawn.y, spawn.z);
				// Start retract phase: free-move and lerp toward shoulder
				stack.setAttached(false);
				player.level().addFreshEntity(stack);

				entry.setUniqueProperty(CARRIED_ENTITY_ID, stack.getId());
				entry.setUniqueProperty(RETRACT_TICKS_LEFT, 8);
				entry.setUniqueProperty(STATE, 2);

				// Stop showing static anchors; start tether to moving stack
				BQLNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
						new BlackwhipMultiBlockWhipPacket(player.getId(), false, List.of(), List.of(), List.of(),
								0, entry.getProperty(WHIP_CURVE), entry.getProperty(WHIP_THICKNESS)));
				BQLNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
						new BlackwhipTethersPacket(player.getId(), true, entry.getProperty(WHIP_CURVE), entry.getProperty(WHIP_THICKNESS), List.of(stack.getId())));

				player.level().playSound(null, player.blockPosition(), SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.PLAYERS, 0.7f, 1.1f);
			}
		} else if (state == 2) {
			int left = entry.getProperty(RETRACT_TICKS_LEFT);
			int id = entry.getProperty(CARRIED_ENTITY_ID);
			if (id >= 0 && player.level().getEntity(id) instanceof BlockStackEntity stack && stack.isAlive()) {
				// Lerp position toward shoulder target
				Vec3 shoulder = shoulderAnchor(player);
				Vec3 start = new Vec3(entry.getProperty(P1X), entry.getProperty(P1Y) - 0.5, entry.getProperty(P1Z));
				int total = 8;
				int used = Math.max(0, total - left);
				double u = Math.min(1.0, (double)used / Math.max(1, total));
				Vec3 pos = start.add(shoulder.subtract(start).scale(u));
				stack.setPos(pos.x, pos.y, pos.z);
				stack.setDeltaMovement(Vec3.ZERO);
			}
			if (left > 0) {
				entry.setUniqueProperty(RETRACT_TICKS_LEFT, left - 1);
			} else {
				// Attach to shoulder
				if (id >= 0 && player.level().getEntity(id) instanceof BlockStackEntity stack && stack.isAlive()) {
					stack.setAttached(true);
				}
				entry.setUniqueProperty(HAS_CARRIED, true);
				entry.setUniqueProperty(STATE, 3);
			}
		}
	}

	@Override
	public void lastTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
		// If an animation is in progress or we're carrying, keep state; do not clean up on release
		if (entry.getProperty(STATE) != 0) {
			return;
		}
		// Otherwise, on true disable, stop persistent tether visuals (if any)
		if (entity instanceof ServerPlayer player) {
			int id = entry.getProperty(CARRIED_ENTITY_ID);
			if (id >= 0) {
				BQLNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
						new BlackwhipTethersPacket(player.getId(), false, entry.getProperty(WHIP_CURVE), entry.getProperty(WHIP_THICKNESS), List.of(id)));
			}
		}
		entry.setUniqueProperty(HAS_CARRIED, false);
		entry.setUniqueProperty(CARRIED_ENTITY_ID, -1);
		entry.setUniqueProperty(STATE, 0);
	}

	private static List<BlockPos> findVerticalColumn(ServerLevel level, BlockPos center) {
		// Choose y-1,y,y+1 if all valid non-air, harvestable, and not block entities
		BlockPos p0 = center.below(1);
		BlockPos p1 = center;
		BlockPos p2 = center.above(1);
		if (isGood(level, p0) && isGood(level, p1) && isGood(level, p2)) {
			return Arrays.asList(p0, p1, p2);
		}

		// else try y,y+1,y+2
		BlockPos q0 = center;
		BlockPos q1 = center.above(1);
		BlockPos q2 = center.above(2);
		if (isGood(level, q0) && isGood(level, q1) && isGood(level, q2)) {
			return Arrays.asList(q0, q1, q2);
		}

		// else try y-2,y-1,y
		BlockPos r0 = center.below(2);
		BlockPos r1 = center.below(1);
		BlockPos r2 = center;

		if (isGood(level, r0) && isGood(level, r1) && isGood(level, r2)) {
			return Arrays.asList(r0, r1, r2);
		}
		return null;
	}

	private static boolean isGood(ServerLevel level, BlockPos pos) {
		BlockState st = level.getBlockState(pos);
		if (st.isAir()) return false;
		BlockEntity be = level.getBlockEntity(pos);
		if (be != null) return false;

		// disallow unbreakable
		if (st.getDestroySpeed(level, pos) < 0) return false;
		return !st.is(BlockTags.WITHER_IMMUNE);
	}

	private static Vec3 shoulderAnchor(ServerPlayer player) {
		Vec3 up = new Vec3(0, 1, 0);
		Vec3 fwdYaw = Vec3.directionFromRotation(0, player.getYRot()).normalize();
		Vec3 rightYaw = fwdYaw.cross(up);

		if (rightYaw.lengthSqr() < 1.0e-6) rightYaw = new Vec3(1, 0, 0);
		rightYaw = rightYaw.normalize();
		float sideDir = player.getMainArm() == HumanoidArm.RIGHT ? 1.0f : -1.0f;
		double shoulderHeight = Math.max(0.45, Math.min(0.9, player.getBbHeight() * 0.78));
		Vec3 base = player.position().add(0, shoulderHeight + 0.25, 0);
		Vec3 target = base.add(rightYaw.scale(1.10 * sideDir)).add(fwdYaw.scale(0.05));
		return target.add(0, -0.20, 0);
	}
}