const THROW_TAG = 'sd_thrown';

StartupEvents.registry('palladium:abilities', event => {
  event.create('super_dudes:throw')
    .icon(palladium.createItemIcon('minecraft:blaze_rod'))
    .addProperty('multiplier', 'float', 2.0, 'Speed multiplier applied to the throw')
    .addProperty('distance', 'integer', 20, 'Maximum travel distance before the item fizzles')
    .addProperty('damage', 'float', 6.0, 'Damage dealt on direct hit')
    .addProperty('proximity_radius', 'float', 0.75, 'Radius for near-miss hits')
    .documentationDescription('Throws the held item forward, damaging the first thing it collides with.')
    .firstTick((entity, entry, holder, enabled) => {
      if (!enabled || !entity || entity.level.isClientSide() || !entity.isPlayer()) return;

      const handInfo = getHeldStack(entity);
      if (!handInfo) return;

      const config = buildThrowConfig(entity, entry);
      const thrownStack = copySingle(handInfo.stack);
      consumeOne(entity, handInfo.hand);
      entity.swing();

      playThrowSound(entity);
      spawnThrownItem(entity, thrownStack, config);
    })
    .tick(() => {});
});

function buildThrowConfig(player, entry) {
  const multiplier = Math.max(0.1, parseFloat(entry.getPropertyByName('multiplier')) || 2.0);
  const maxDistance = Math.max(1.0, parseFloat(entry.getPropertyByName('distance')) || 20.0);
  const damage = Math.max(0.0, parseFloat(entry.getPropertyByName('damage')) || 6.0);
  const radius = Math.max(0.1, parseFloat(entry.getPropertyByName('proximity_radius')) || 0.75);
  const look = player.getLookAngle().normalize();
  const speed = Math.min(2.8, multiplier);
  return {
    speed: speed,
    maxDistance: maxDistance,
    damage: damage,
    radius: radius,
    dirX: look.x(),
    dirY: look.y(),
    dirZ: look.z()
  };
}

function spawnThrownItem(player, stack, config) {
  const level = player.level;
  const thrown = level.createEntity('minecraft:item');
  if (!thrown) return null;
  thrown.item = stack;
  thrown.setPickUpDelay(32767);
  setNoGravity(thrown, true);
  thrown.addTag(THROW_TAG);

  const eye = player.getEyePosition();
  thrown.x = eye.x + config.dirX * 0.15;
  thrown.y = eye.y + config.dirY * 0.15;
  thrown.z = eye.z + config.dirZ * 0.15;

  let speedVector = Vec3d(
    config.dirX * config.speed,
    config.dirY * config.speed,
    config.dirZ * config.speed
  );

  thrown.setDeltaMovement(speedVector);
  imprintThrowData(thrown, player, config);
  thrown.spawn();
  return thrown;
}

function imprintThrowData(thrown, player, config) {
  const data = thrown.persistentData;
  if (!data) return;
  data.putString('BQLThrowOwner', player.uuid);
  data.putDouble('BQLThrowSpeed', config.speed);
  data.putDouble('BQLThrowDamage', config.damage);
  data.putDouble('BQLThrowRadius', config.radius);
  data.putDouble('BQLThrowMaxDist', config.maxDistance);
  data.putDouble('BQLThrowDirX', config.dirX);
  data.putDouble('BQLThrowDirY', config.dirY);
  data.putDouble('BQLThrowDirZ', config.dirZ);
  data.putDouble('BQLThrowTravel', 0.0);
  data.putBoolean('BQLThrowActive', true);
}

function getHeldStack(player) {
  if (player.mainHandItem && !player.mainHandItem.isEmpty()) {
    return { stack: player.mainHandItem, hand: 'main' };
  }
  if (player.offHandItem && !player.offHandItem.isEmpty()) {
    return { stack: player.offHandItem, hand: 'off' };
  }
  return null;
}

function consumeOne(player, hand) {
  if (hand === 'main') {
    player.mainHandItem.shrink(1);
  } else if (hand === 'off') {
    player.offHandItem.shrink(1);
  }
}

function copySingle(stack) {
  const copy = stack.copy();
  copy.count = 1;
  return copy;
}

function setNoGravity(entity, value) {
  if (typeof entity.setNoGravity === 'function') {
    entity.setNoGravity(value);
  } else {
    entity.noGravity = value;
  }
}

function playThrowSound(player) {
  player.level.runCommandSilent(`playsound minecraft:item.trident.throw master @a[distance=..32] ${player.x} ${player.y} ${player.z} 0.8 1.1`);
}

