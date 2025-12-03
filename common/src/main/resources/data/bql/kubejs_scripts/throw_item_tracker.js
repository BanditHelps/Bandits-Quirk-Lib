//const THROW_TAG = 'sd_thrown';
//const ACTIVE_THROWS = new Map();
//
//EntityEvents.spawned(event => {
//  const entity = event.entity;
//  if (!isTrackedThrow(entity)) return;
//  ACTIVE_THROWS.set(entity.id, createState(entity));
//});
//
//LevelEvents.tick(event => {
//  const level = event.level;
//  if (!level || level.isClientSide()) return;
//  if (ACTIVE_THROWS.size === 0) return;
//
//  for (const [id, state] of Array.from(ACTIVE_THROWS.entries())) {
//    const item = state.item;
//    if (!item || !item.isAlive() || !isTrackedThrow(item)) {
//      ACTIVE_THROWS.delete(id);
//      continue;
//    }
//
//    state.age = (state.age || 0) + 1;
//    const moved = advanceItem(level, item, state);
//    state.travelled += moved;
//
//    console.log("moving item " + moved)
//
//    if (handleEntityImpact(level, state, item)) {
//      console.log("hit entity")
//      ACTIVE_THROWS.delete(id);
//      continue;
//    }
//
//    if (state.travelled >= state.maxDistance || hasHitBlock(item, state)) {
//      console.log("hit ground")
//      explodeItem(level, item);
//      ACTIVE_THROWS.delete(id);
//    }
//  }
//});
//
//function isTrackedThrow(entity) {
//  return entity && entity.type === 'minecraft:item' && entity.tags && entity.tags.contains(THROW_TAG);
//}
//
//function createState(entity) {
//  const data = entity.persistentData;
//  return {
//    item: entity,
//    speed: readDouble(data, 'BQLThrowSpeed', 0.6),
//    damage: readDouble(data, 'BQLThrowDamage', 6.0),
//    radius: readDouble(data, 'BQLThrowRadius', 0.75),
//    maxDistance: readDouble(data, 'BQLThrowMaxDist', 20.0),
//    dirX: readDouble(data, 'BQLThrowDirX', 0.0),
//    dirY: readDouble(data, 'BQLThrowDirY', 0.0),
//    dirZ: readDouble(data, 'BQLThrowDirZ', 0.0),
//    ownerUuid: data ? data.getString('BQLThrowOwner') : '',
//    travelled: readDouble(data, 'BQLThrowTravel', 0.0),
//    age: 0
//  };
//}
//
//function readDouble(data, key, fallback) {
//  if (!data || typeof data.getDouble !== 'function') return fallback;
//  return data.contains && data.contains(key) ? data.getDouble(key) : fallback;
//}
//
//function advanceItem(level, item, state) {
//  setNoGravity(item, true);
//  const motionX = state.dirX * state.speed;
//  const motionY = state.dirY * state.speed;
//  const motionZ = state.dirZ * state.speed;
//  const vec = Vec3d(motionX, motionY, motionZ);
//  item.setDeltaMovement(vec);
//  const length = Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
//  const pos = item.position();
//  if (item.persistentData) {
//    item.persistentData.putDouble('BQLThrowLastX', pos.x());
//    item.persistentData.putDouble('BQLThrowLastY', pos.y());
//    item.persistentData.putDouble('BQLThrowLastZ', pos.z());
//  }
//  state.maxDistance = measureRealDistance(level, state, pos);
//  return length;
//}
//
//function measureRealDistance(level, state, pos) {
//  const owner = level.getPlayer ? level.getPlayer(state.ownerUuid) : null;
//  if (!owner) return state.maxDistance;
//  const dx = pos.x() - owner.x;
//  const dy = pos.y() - (owner.y + owner.getEyeHeight());
//  const dz = pos.z() - owner.z;
//  return Math.sqrt(dx * dx + dy * dy + dz * dz);
//}
//
//function hasHitBlock(item, state) {
//  if (state.travelled < state.maxDistance - 0.25) return false;
//  const data = item.persistentData;
//  if (!data) return false;
//  const lastX = data.getDouble('BQLThrowLastX');
//  const lastY = data.getDouble('BQLThrowLastY');
//  const lastZ = data.getDouble('BQLThrowLastZ');
//  const current = item.position();
//  const movedSq = (current.x() - lastX) ** 2 + (current.y() - lastY) ** 2 + (current.z() - lastZ) ** 2;
//  if (movedSq < 1.0e-4) return true;
//  return false;
//}
//
//function handleEntityImpact(level, state, item) {
//  const radius = Math.max(0.2, state.radius);
//  const box = AABB.ofSize(item.position(), radius * 2.0, radius * 2.0, radius * 2.0);
//  const entities = level.getEntities(item, box).toArray();
//  if (!entities || entities.length === 0) return false;
//
//  for (const target of entities) {
//    if (!target.isLiving()) continue;
//    if (state.ownerUuid && target.uuid === state.ownerUuid) continue;
//    if (target.distanceToSqr(item) > radius * radius) continue;
//
//    applyDamage(level, state.ownerUuid, target, state.damage);
//    spawnHitEffects(level, target);
//    item.discard();
//    return true;
//  }
//  return false;
//}
//
//function applyDamage(level, ownerUuid, target, damage) {
//  if (damage <= 0) return;
//  const tag = `bql_throw_hit_${target.id}`;
//  target.addTag(tag);
//  const damageCommand = `damage @e[tag=${tag},limit=1,sort=nearest] ${damage} minecraft:thrown by @s`;
//  if (ownerUuid && ownerUuid.length > 0) {
//    level.runCommandSilent(`execute as ${ownerUuid} at ${ownerUuid} run ${damageCommand}`);
//  } else {
//    level.runCommandSilent(`execute at ${target.uuid} run ${damageCommand}`);
//  }
//  target.removeTag(tag);
//}
//
//function spawnHitEffects(level, target) {
//  level.spawnParticles('minecraft:crit', false, target.x, target.y + target.getEyeHeight() * 0.5, target.z, 0.3, 0.3, 0.3, 8, 0.1);
//  level.runCommandSilent(`playsound minecraft:entity.player.attack.strong master @a[distance=..32] ${target.x} ${target.y} ${target.z} 0.8 1.0`);
//}
//
//function explodeItem(level, item) {
//  level.spawnParticles('minecraft:poof', false, item.x, item.y, item.z, 0.2, 0.2, 0.2, 5, 0.02);
//  level.runCommandSilent(`playsound minecraft:entity.item.break master @a[distance=..32] ${item.x} ${item.y} ${item.z} 0.6 1.2`);
//  item.discard();
//}
//
//function setNoGravity(entity, value) {
//  if (typeof entity.setNoGravity === 'function') {
//    entity.setNoGravity(value);
//  } else {
//    entity.noGravity = value;
//  }
//}
//
