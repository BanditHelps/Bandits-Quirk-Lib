// This file will contain all of the commands for the decay power
// It starts with /mineha_decay and from there, it will have subcommands, each that has it's own function to handle it.
// Goal is to reduce the number of actual commands, and prioritize functional programming.

// Define PI for use in calculations cause apparently it's not a constant in JS
const PI = 3.14159265358979323846;

// Define rot-affected blocks
const plantBlocks = [
    'minecraft:grass', 'minecraft:tall_grass', 'minecraft:fern', 'minecraft:large_fern',
    'minecraft:dandelion', 'minecraft:poppy', 'minecraft:blue_orchid', 'minecraft:allium',
    'minecraft:azure_bluet', 'minecraft:red_tulip', 'minecraft:orange_tulip', 'minecraft:white_tulip',
    'minecraft:pink_tulip', 'minecraft:oxeye_daisy', 'minecraft:cornflower', 'minecraft:lily_of_the_valley',
    'minecraft:sunflower', 'minecraft:lilac', 'minecraft:rose_bush', 'minecraft:peony',
    'minecraft:dead_bush', 'minecraft:wheat', 'minecraft:carrots', 'minecraft:potatoes',
    'minecraft:beetroots', 'minecraft:sweet_berry_bush', 'minecraft:bamboo', 'minecraft:sugar_cane',
    'minecraft:cactus', 'minecraft:brown_mushroom', 'minecraft:red_mushroom',
    'minecraft:oak_leaves', 'minecraft:spruce_leaves', 'minecraft:birch_leaves',
    'minecraft:jungle_leaves', 'minecraft:acacia_leaves', 'minecraft:dark_oak_leaves',
    'minecraft:azalea_leaves', 'minecraft:flowering_azalea_leaves',
    'minecraft:oak_sapling', 'minecraft:spruce_sapling', 'minecraft:birch_sapling',
    'minecraft:jungle_sapling', 'minecraft:acacia_sapling', 'minecraft:dark_oak_sapling',
    'minecraft:kelp', 'minecraft:kelp_plant', 'minecraft:seagrass', 'minecraft:tall_seagrass',
    'minecraft:vine', 'minecraft:glow_lichen', 'minecraft:moss_carpet', 'minecraft:moss_block'
];

ServerEvents.commandRegistry(event => {
    const { commands: Commands, arguments: Arguments } = event

    event.register(
        Commands.literal("mineha_decay")
            .requires(src => src.hasPermission(2))
            .then(Commands.argument('target', Arguments.PLAYER.create(event))
            .then(Commands.argument('type', Arguments.WORD.create(event))
            .suggests((ctx, builder) => {
                builder.suggest("unstable");
                builder.suggest("rot");
                builder.suggest("env_single");
                builder.suggest("env_circular");
                builder.suggest("env_cone");
                builder.suggest("env_vertical");
                builder.suggest("env_horizontal");
                builder.suggest("env_explosive");
                return builder.buildFuture();
            })
            .then(Commands.argument('value', Arguments.INTEGER.create(event))
            .executes(ctx => { 
                const type = Arguments.WORD.getResult(ctx, 'type');
                const value = Arguments.INTEGER.getResult(ctx, 'value'); // Could be damage or other
                let player = Arguments.PLAYER.getResult(ctx, 'target');
                let server = ctx.source.getServer();
                let username = player.getGameProfile().getName();

                if (type === "unstable") {
                    // Decay self due to instability of the power
                    unstableDecay(player, username, server, value);
                } else if (type === "rot") {
                    // Rotate the environment with different levels of decay
                    rotStuff(player, username, server, value);

                } else if (type.startsWith("env_")) {
                    envDecay(player, username, server, value, type.replace("env_", ""));
                } else {
                    // Default
                    server.runCommandSilent('No option selected');
                }

                return 1;
            })
        )))
    );
});

// Unstable decay - Randomly applies damage to armor, main hand, or off hand item
// If damage is 0, it checks the MineHa.Decay.ControlDamage score and uses that
function unstableDecay(entity, username, server, damage) {
    // Get a random number to determine which piece to decay
    let randomNumber = Math.random();
    let scoreboard = server.scoreboard;
    let quirkFactor = getQuirkFactor(entity);
    let damageToUse = damage;

    if (damage == 0) {
        damageToUse = scoreboard.getOrCreatePlayerScore(username, scoreboard.getObjective('MineHa.Decay.ControlDamage')).getScore();
    }

    /*
        ================================
        Random number breakdown:
        - 0.0 - 0.65: Main Hand or Off Hand (65% chance)
        - 0.65 - 1.0: Armor (35% chance)
        ================================
    */

    try {
        if (randomNumber <= 0.65) {
            let whichHand = Math.random();
            if (whichHand <= 0.5) {
                // Main Hand
                damageHeldItem(entity, username, server, "main_hand", damageToUse + (damageToUse * quirkFactor));
            } else {
                // Off Hand
                damageHeldItem(entity, username, server, "off_hand", damageToUse + (damageToUse * quirkFactor));
            }
            
            
        } else {
            // Loop through the armor slots, and damage the armor. If no amror is in that slot, attempt another slot
            
            // Get a list of valid breakables
            let validSlots = [];
            for (let slot of entity.getArmorSlots()) {
                if (!slot.isEmpty() && slot.isDamageableItem()) {
                    validSlots.push(slot);
                }
            }

            // If there are valid slots, damage a random one
            if (validSlots.length > 0) {
                let randomSlot = validSlots[Math.floor(Math.random() * validSlots.length)];
                randomSlot.setDamageValue(randomSlot.getDamageValue() + damageToUse + (damageToUse * quirkFactor));

                // Check if the armor piece should break
                if (randomSlot.getDamageValue() >= randomSlot.getMaxDamage()) {
                    randomSlot.shrink(randomSlot.getCount());
                    server.runCommandSilent(`execute as ${username} run playsound minecraft:entity.item.break player @s ~ ~ ~ 100 1`);
                }
            }
        }
    } catch (error) {
        console.log(`Error: ${error}`);
    }
}

// Rot Stuff - Apply a rot effect to the environment. Destroys grass, turns grass blocks into dirt, destroys leaves, basically all plant life.
function rotStuff(player, username, server, damage) {
    let quirkFactor = getQuirkFactor(player);
    let level = player.level;

    // First, if the player is holding a tool, damage it
    damageHeldItem(player, username, server, "main_hand", damage + (damage * quirkFactor));

    try {
        // Get current rot radius from scoreboard, or initialize it
        let scoreboard = server.scoreboard;
        let rotRadiusObj = scoreboard.getObjective('MineHa.Decay.RotRadius');
        if (!rotRadiusObj) {
            // No one has the decay power so they can't use this command
            server.runCommandSilent(`say No one has the decay power, skipping rot command`);
            return;
        }
        
        let currentRadius = scoreboard.getOrCreatePlayerScore(username, rotRadiusObj).getScore();
        
        // Calculate new radius based on damage and quirk factor
        let radiusIncrease = Math.max(1, Math.floor(damage));
        let newRadius = Math.min(currentRadius + radiusIncrease, 10 + (quirkFactor * 10)); // Cap at 50 blocks
        
        // Update scoreboard
        scoreboard.getOrCreatePlayerScore(username, rotRadiusObj).setScore(newRadius);
        
        // Get player position and look direction
        let centerPos = player.blockPosition();
        let playerY = centerPos.getY();
        let lookAngle = player.lookAngle;
        
        let blocksProcessed = 0;
        let maxBlocksPerTick = 80;
        
        // Create a cone-shaped rot pattern extending forward from the player
        let coneLength = newRadius;
        let coneAngle = 45; // Degrees - how wide the cone spreads
        
        // Convert look direction to horizontal only (ignore Y component for cone calculation)
        let lookX = lookAngle.x();
        let lookZ = lookAngle.z();
        let lookLength = Math.sqrt(lookX * lookX + lookZ * lookZ);
        if (lookLength > 0) {
            lookX /= lookLength; // Normalize
            lookZ /= lookLength;
        }
        
        // Process blocks in a cone shape
        for (let distance = Math.max(0, currentRadius); distance <= coneLength && blocksProcessed < maxBlocksPerTick; distance++) {
            // Calculate cone width at this distance (gets wider as distance increases)
            let coneWidth = Math.tan(coneAngle * PI / 180) * distance;
            let maxWidth = Math.max(1, Math.floor(coneWidth));
            
            // For close distances, include some area behind the player
            let includeCenter = distance <= 2;
            
            for (let side = -maxWidth; side <= maxWidth; side++) {
                // Calculate the perpendicular direction (90 degrees from look direction)
                let perpX = -lookZ; // Perpendicular to look direction
                let perpZ = lookX;
                
                // Calculate the position in the cone
                let baseX = lookX * distance;
                let baseZ = lookZ * distance;
                let offsetX = perpX * side;
                let offsetZ = perpZ * side;
                
                let x = Math.round(baseX + offsetX);
                let z = Math.round(baseZ + offsetZ);
                
                // If we're including the center area, also process blocks around the player
                if (includeCenter) {
                    for (let centerX = -1; centerX <= 1; centerX++) {
                        for (let centerZ = -1; centerZ <= 1; centerZ++) {
                            if (Math.abs(centerX) + Math.abs(centerZ) <= distance) {
                                processRotBlock(level, centerPos, centerX, centerZ, playerY, plantBlocks, distance);
                                blocksProcessed++;
                                if (blocksProcessed >= maxBlocksPerTick) break;
                            }
                        }
                        if (blocksProcessed >= maxBlocksPerTick) break;
                    }
                }
                
                                 // Process the main cone block
                 if (blocksProcessed < maxBlocksPerTick) {
                     processRotBlock(level, centerPos, x, z, playerY, plantBlocks, distance);
                     blocksProcessed++;
                 }
             }
         }
        
        // Apply decay effect to entities within the rot radius
        let entitiesInRadius = level.getEntitiesWithin(AABB.of(
            centerPos.getX() - newRadius, centerPos.getY() - 6, centerPos.getZ() - newRadius,
            centerPos.getX() + newRadius, centerPos.getY() + 10, centerPos.getZ() + newRadius
        ));
        
        for (let entity of entitiesInRadius) {
            // Skip the player who cast the rot
            if (entity === player) continue;
            
            // Only affect living entities
            if (!entity.isLiving()) continue;
            
            // Calculate distance from center
            let entityX = entity.getX();
            let entityZ = entity.getZ();
            let distanceFromCenter = Math.sqrt(
                (entityX - centerPos.getX()) * (entityX - centerPos.getX()) + 
                (entityZ - centerPos.getZ()) * (entityZ - centerPos.getZ())
            );
            
            // Only affect entities within the current rot radius
            if (distanceFromCenter <= newRadius) {
                // Calculate effect duration and amplifier based on proximity to center
                let proximityFactor = 1 - (distanceFromCenter / newRadius); // 1.0 at center, 0.0 at edge
                let effectDuration = Math.floor(60 + (proximityFactor * 140)); // 3-10 seconds (60-200 ticks)
                let effectAmplifier = Math.floor(proximityFactor * 2); // 0-2 amplifier based on proximity
                
                // Apply the decay effect
                entity.potionEffects.add('mineha:decay_effect', effectDuration, effectAmplifier);
                
                // Add visual indication that entity is affected
                level.spawnParticles("minecraft:soul", false, 
                    entityX, entity.getY() + 1, entityZ, 
                    0.5, 0.5, 0.5, 5, 0.05);
            }
                 }
        
        // Add continuous particle effects to visualize the rot zone
        addRotVisualization(level, centerPos, newRadius, playerY);
        
        // Add visual and audio effects at player position
        if (newRadius > currentRadius) {
            level.spawnParticles("minecraft:large_smoke", false, 
                centerPos.getX(), centerPos.getY() + 1, centerPos.getZ(), 
                1, 0.5, 1, 10, 0.1);
                
            level.spawnParticles("minecraft:ash", false, 
                centerPos.getX(), centerPos.getY() + 1, centerPos.getZ(), 
                2, 1, 2, 15, 0.1);
                
            // Play ominous sound
            level.playLocalSound(centerPos.getX(), centerPos.getY(), centerPos.getZ(), 
                "minecraft:entity.wither.ambient", "players", 0.5, 0.8, false);
        }
        
        // server.runCommandSilent(`say Rot expanded to radius ${newRadius} (${blocksProcessed} blocks processed)`);
        
    } catch (error) {
        console.log(`Error: ${error}`);
    }
}

// Helper function to process a single block position for rot
function processRotBlock(level, centerPos, x, z, playerY, plantBlocks, distance) {
    for (let y = -6; y <= 5; y++) { // Limited vertical range: 6 down, 5 up
        let checkY = playerY + y;
        
        // Don't go too deep underground or too high
        if (checkY < -60 || checkY > playerY + 10) continue;
        
        let checkPos = new BlockPos(centerPos.getX() + x, checkY, centerPos.getZ() + z);
        let blockState = level.getBlockState(checkPos);
        let blockId = blockState.block.getId();
        
        // Skip air blocks
        if (blockId === 'minecraft:air') continue;
        
        // Convert grass blocks to dirt
        if (blockId === 'minecraft:grass_block') {
            level.setBlock(checkPos, Block.getBlock('minecraft:dirt').defaultBlockState(), 3);
            
            // Add decay particles
            level.spawnParticles("minecraft:large_smoke", false, 
                checkPos.getX() + 0.5, checkPos.getY() + 1, checkPos.getZ() + 0.5, 
                0.3, 0.3, 0.3, 3, 0.05);
        }
        // Destroy plant life
        else if (plantBlocks.includes(blockId)) {
            // Schedule destruction with slight delay for visual effect
            let delay = Math.floor(Math.random() * 20) + Math.floor(distance * 2); // Distance-based delay
            scheduleBlockDestroy(level, checkPos, delay);
            
            // Add withering particles
            level.spawnParticles("minecraft:smoke", false, 
                checkPos.getX() + 0.5, checkPos.getY() + 0.5, checkPos.getZ() + 0.5, 
                0.2, 0.2, 0.2, 2, 0.02);
        }
    }
}

// Add continuous visual effects to show the rot zone
function addRotVisualization(level, centerPos, radius, playerY) {
    if (radius <= 0) return;
    
    let playerBuffer = 2.5; // Don't spawn particles within 2.5 blocks of player
    
    // Create particle ring around the perimeter
    let circumference = 2 * PI * radius;
    let numPerimeterParticles = Math.min(Math.max(8, Math.floor(circumference / 2)), 24); // Reduced max particles
    
    for (let i = 0; i < numPerimeterParticles; i++) {
        let angle = (i / numPerimeterParticles) * 2 * PI;
        let x = centerPos.getX() + Math.cos(angle) * radius;
        let z = centerPos.getZ() + Math.sin(angle) * radius;
        let y = playerY - 1 + Math.random() * 2; // Keep particles closer to ground level
        
        // Spawn smoke particles around the perimeter
        level.spawnParticles("minecraft:large_smoke", false, 
            x, y, z, 
            0.1, 0.2, 0.1, 1, 0.02);
            
        // Add some ash particles for variety
        if (Math.random() < 0.3) {
            level.spawnParticles("minecraft:ash", false, 
                x, y + 0.3, z, 
                0.1, 0.3, 0.1, 1, 0.01);
        }
    }
    
    // Add scattered particles throughout the rot area, but avoid player position
    let areaParticles = Math.min(Math.floor(radius * 1.5), 12); // Reduced particle count
    for (let i = 0; i < areaParticles; i++) {
        let angle = Math.random() * 2 * PI;
        let distance = Math.random() * radius;
        
        // Skip if too close to player
        if (distance < playerBuffer) continue;
        
        let x = centerPos.getX() + Math.cos(angle) * distance;
        let z = centerPos.getZ() + Math.sin(angle) * distance;
        let y = playerY - 1 + Math.random() * 1.5; // Keep lower to ground
        
        // Spawn fewer, more subtle particles
        let particleType = Math.random();
        if (particleType < 0.6) {
            level.spawnParticles("minecraft:smoke", false, 
                x, y, z, 
                0.05, 0.1, 0.05, 1, 0.01);
        } else {
            level.spawnParticles("minecraft:ash", false, 
                x, y, z, 
                0.05, 0.1, 0.05, 1, 0.01);
        }
    }
    
    // Add just a few particles at the player's feet instead of dramatic center effects
    if (radius >= 3) {
        level.spawnParticles("minecraft:smoke", false, 
            centerPos.getX(), playerY - 0.3, centerPos.getZ(), 
            0.3, 0.1, 0.3, 2, 0.01);
    }
}

// Environment decay - Decay the environment, destroying blocks in the path. This version
// will be a bit random in the path that the decay takes, and can be configured to go further
// or decay more blocks.
function envDecay(player, username, server, damage, decayType) {
    let quirkFactor = getQuirkFactor(player);
    let level = player.level;

    // First, if the player is holding a tool, damage it
    damageHeldItem(player, username, server, "main_hand", damage + (damage * quirkFactor));

    try {
        // Get the target block from player's look direction
        let dist = 3;
        let ray = player.rayTrace(dist, false);

        if (!ray.block) {
            server.runCommandSilent('say No target block found!');
            return;
        }

        let rayPos = ray.block.pos;
        
        // Ensure we have a proper BlockPos object with accessible coordinates
        let targetPos = new BlockPos(rayPos.x || rayPos.getX(), rayPos.y || rayPos.getY(), rayPos.z || rayPos.getZ());

        // Calculate scaled parameters based on quirk factor
        let maxBlocks = Math.max(5, damage + Math.floor(damage * quirkFactor));
        let decayIntensity = Math.min(4, Math.floor(1 + quirkFactor * 0.5)); // 1-4 intensity levels

        // server.runCommandSilent(`say Environmental Decay: ${decayType} (Intensity: ${decayIntensity}, Blocks: ${maxBlocks})`);

        // Execute the appropriate decay pattern
        switch (decayType) {
            case "single":
                singleDecay(level, targetPos, decayIntensity);
                break;
            case "circular":
                circularDecay(level, targetPos, maxBlocks, decayIntensity);
                break;
            case "cone":
                coneEnvironmentDecay(level, targetPos, player.lookAngle, maxBlocks, decayIntensity);
                break;
            case "vertical":
                verticalDecay(level, targetPos, maxBlocks, decayIntensity, player);
                break;
            case "horizontal":
                horizontalDecay(level, targetPos, maxBlocks, decayIntensity, player);
                break;
            case "explosive":
                explosiveDecay(level, targetPos, maxBlocks, decayIntensity);
                break;
            default:
                circularDecay(level, targetPos, maxBlocks, decayIntensity);
        }

        // Add enhanced visual effects based on intensity
        addDecayVisualEffects(level, targetPos, decayIntensity);
    } catch (error) {
        server.runCommandSilent(`say Error: ${error}`);
    }

    
}

// Connected decay algorithm - spreads only through touching blocks
function connectedDecay(level, startPos, maxBlocks, intensity, selectionBias) {
    const decayableBlocks = getDecayableBlocks(intensity);
    const isDecayable = (blockState) => decayableBlocks.includes(blockState.block.getId());
    
    // Check if starting position is decayable
    let startState = level.getBlockState(startPos);
    if (!isDecayable(startState)) {
        return 0; // Can't start decay from non-decayable block
    }
    
    // Queue for breadth-first search: [position, distance_from_start]
    let queue = [{pos: startPos, distance: 0}];
    let visited = new Set();
    let decayList = [];
    
    // Add starting position to visited set
    let startKey = `${startPos.getX()},${startPos.getY()},${startPos.getZ()}`;
    visited.add(startKey);
    
    // 6-directional adjacency (no diagonals to ensure true connectivity)
    const directions = [
        {x: 0, y: 1, z: 0},   // Up
        {x: 0, y: -1, z: 0},  // Down
        {x: 1, y: 0, z: 0},   // East
        {x: -1, y: 0, z: 0},  // West
        {x: 0, y: 0, z: 1},   // South
        {x: 0, y: 0, z: -1}   // North
    ];
    
    while (queue.length > 0 && decayList.length < maxBlocks) {
        let current = queue.shift();
        decayList.push(current);
        
        // Check all adjacent blocks
        for (let dir of directions) {
            if (decayList.length >= maxBlocks) break;
            
            let newX = current.pos.getX() + dir.x;
            let newY = current.pos.getY() + dir.y;
            let newZ = current.pos.getZ() + dir.z;
            let newPos = new BlockPos(newX, newY, newZ);
            let newKey = `${newX},${newY},${newZ}`;
            
            // Skip if already visited
            if (visited.has(newKey)) continue;
            
            let blockState = level.getBlockState(newPos);
            if (isDecayable(blockState)) {
                // Apply selection bias if provided (for directional patterns)
                let shouldAdd = true;
                if (selectionBias) {
                    shouldAdd = selectionBias(newPos, startPos, current.distance + 1);
                }
                
                if (shouldAdd) {
                    visited.add(newKey);
                    queue.push({pos: newPos, distance: current.distance + 1});
                }
            }
        }
    }
    
    // Schedule destruction of all blocks in decay list
    for (let i = 0; i < decayList.length; i++) {
        let item = decayList[i];
        scheduleBlockDestroy(level, item.pos, item.distance * 8);
    }
    
    return decayList.length;
}

// Single decay pattern - decays a single block
function singleDecay(level, targetPos, intensity) {
    // Add 1 to intensity because it is such a focused decay, it should affect stronger blocks
    return connectedDecay(level, targetPos, 1, intensity + 1);
}

// Circular decay pattern - spreads outward through connected blocks
function circularDecay(level, centerPos, maxBlocks, intensity) {
    // No directional bias - spreads equally in all directions
    return connectedDecay(level, centerPos, maxBlocks, intensity);
}

// Cone decay pattern - spreads in the direction player is looking through connected blocks
function coneEnvironmentDecay(level, startPos, lookAngle, maxBlocks, intensity) {
    // Create a bias function that favors blocks in the look direction
    let coneBias = function(newPos, startPos, distance) {
        // Calculate vector from start to new position
        let dx = newPos.getX() - startPos.getX();
        let dy = newPos.getY() - startPos.getY();
        let dz = newPos.getZ() - startPos.getZ();
        
        // Calculate dot product with look direction (alignment score)
        let alignment = dx * lookAngle.x() + dy * lookAngle.y() + dz * lookAngle.z();
        
        // Normalize by distance to get directional preference
        let totalDistance = Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (totalDistance === 0) return true; // Starting block
        
        let directionScore = alignment / totalDistance;
        
        // Higher chance for blocks aligned with look direction
        // Intensity affects how strict the cone is (higher = more focused)
        let coneStrictness = 0.3 + (intensity * 0.15); // 0.3 to 0.9
        let baseChance = 0.4; // Base 40% chance for any connected block
        let directionBonus = Math.max(0, directionScore) * (0.6 * intensity / 4); // Up to 60% bonus
        
        let finalChance = baseChance + directionBonus;
        
        // Reduce chance with distance to create natural falloff
        let distanceFalloff = Math.max(0.1, 1 - (distance * 0.1));
        finalChance *= distanceFalloff;
        
        return Math.random() < finalChance;
    };
    
    return connectedDecay(level, startPos, maxBlocks, intensity, coneBias);
}

// Vertical decay pattern - creates a vertical shaft straight up and down, stops at air blocks
// Width is affected by quirk factor
function verticalDecay(level, centerPos, maxBlocks, intensity, player) {
    const decayableBlocks = getDecayableBlocks(intensity);
    const isDecayable = (blockState) => decayableBlocks.includes(blockState.block.getId());
    
    let quirkFactor = getQuirkFactor(player);
    let totalDestroyed = 0;
    let maxRadius = 5;
    
    // Calculate shaft dimensions based on quirk factor
    let shaftRadius = Math.min(maxRadius, Math.floor(0 + quirkFactor)); // 0 to 2 radius (1x1 to 5x5 shaft)
    let maxHeight = Math.floor(2 + quirkFactor); // 1 to 3 blocks height based on quirk
    
    // Always destroy blocks at the center level first
    for (let x = -shaftRadius; x <= shaftRadius && totalDestroyed < maxBlocks; x++) {
        for (let z = -shaftRadius; z <= shaftRadius && totalDestroyed < maxBlocks; z++) {
            let centerLevelPos = new BlockPos(centerPos.getX() + x, centerPos.getY(), centerPos.getZ() + z);
            let centerState = level.getBlockState(centerLevelPos);
            if (isDecayable(centerState) && centerState.block.getId() !== 'minecraft:air') {
                scheduleBlockDestroy(level, centerLevelPos, 0);
                totalDestroyed++;
            }
        }
    }
    
    // Go straight up from center position
    for (let y = 1; y <= maxHeight && totalDestroyed < maxBlocks; y++) {
        let hitAir = false;
        
        // Check all positions in the `cross-section at this Y level
        for (let x = -shaftRadius; x <= shaftRadius && totalDestroyed < maxBlocks; x++) {
            for (let z = -shaftRadius; z <= shaftRadius && totalDestroyed < maxBlocks; z++) {
                let upPos = new BlockPos(centerPos.getX() + x, centerPos.getY() + y, centerPos.getZ() + z);
                let blockState = level.getBlockState(upPos);
                
                // Check if we hit air
                if (blockState.block.getId() === 'minecraft:air') {
                    // If center column hits air, stop going up entirely
                    if (x === 0 && z === 0) {
                        hitAir = true;
                        break;
                    }
                    // For non-center positions, just skip this block
                    continue;
                }
                
                // Only destroy if block is decayable
                if (isDecayable(blockState)) {
                    scheduleBlockDestroy(level, upPos, y * 5); // Stagger the destruction for visual effect
                    totalDestroyed++;
                }
            }
            if (hitAir) break;
        }
        if (hitAir) break;
    }
    
    // Go straight down from center position
    for (let y = 1; y <= maxHeight && totalDestroyed < maxBlocks; y++) {
        let hitAir = false;
        
        // Check all positions in the cross-section at this Y level
        for (let x = -shaftRadius; x <= shaftRadius && totalDestroyed < maxBlocks; x++) {
            for (let z = -shaftRadius; z <= shaftRadius && totalDestroyed < maxBlocks; z++) {
                let downPos = new BlockPos(centerPos.getX() + x, centerPos.getY() - y, centerPos.getZ() + z);
                let blockState = level.getBlockState(downPos);
                
                // Check if we hit air
                if (blockState.block.getId() === 'minecraft:air') {
                    // If center column hits air, stop going down entirely
                    if (x === 0 && z === 0) {
                        hitAir = true;
                        break;
                    }
                    // For non-center positions, just skip this block
                    continue;
                }
                
                // Only destroy if block is decayable
                if (isDecayable(blockState)) {
                    scheduleBlockDestroy(level, downPos, y * 5); // Stagger the destruction for visual effect
                    totalDestroyed++;
                }
            }
            if (hitAir) break;
        }
        if (hitAir) break;
    }
    

    
    return totalDestroyed;
}

// Horizontal decay pattern - creates a horizontal shaft straight left and right, stops at air blocks
// Width and length are affected by quirk factor
function horizontalDecay(level, centerPos, maxBlocks, intensity, player) {
    const decayableBlocks = getDecayableBlocks(intensity);
    const isDecayable = (blockState) => decayableBlocks.includes(blockState.block.getId());
    
    let quirkFactor = getQuirkFactor(player);
    let totalDestroyed = 0;
    let maxRadius = 5;
    
    // Calculate shaft dimensions based on quirk factor
    let shaftRadius = Math.min(maxRadius, Math.floor(0 + quirkFactor)); // Width in Y and Z directions
    let maxLength = Math.floor(2 + quirkFactor); // Length in X direction
    
    // Always destroy blocks at the center level first
    for (let y = -shaftRadius; y <= shaftRadius && totalDestroyed < maxBlocks; y++) {
        for (let z = -shaftRadius; z <= shaftRadius && totalDestroyed < maxBlocks; z++) {
            let centerLevelPos = new BlockPos(centerPos.getX(), centerPos.getY() + y, centerPos.getZ() + z);
            let centerState = level.getBlockState(centerLevelPos);
            if (isDecayable(centerState) && centerState.block.getId() !== 'minecraft:air') {
                scheduleBlockDestroy(level, centerLevelPos, 0);
                totalDestroyed++;
            }
        }
    }
    
    // Go straight in positive X direction from center position
    for (let x = 1; x <= maxLength && totalDestroyed < maxBlocks; x++) {
        let hitAir = false;
        
        // Check all positions in the cross-section at this X level
        for (let y = -shaftRadius; y <= shaftRadius && totalDestroyed < maxBlocks; y++) {
            for (let z = -shaftRadius; z <= shaftRadius && totalDestroyed < maxBlocks; z++) {
                let forwardPos = new BlockPos(centerPos.getX() + x, centerPos.getY() + y, centerPos.getZ() + z);
                let blockState = level.getBlockState(forwardPos);
                
                // Check if we hit air
                if (blockState.block.getId() === 'minecraft:air') {
                    // If center column hits air, stop going forward entirely
                    if (y === 0 && z === 0) {
                        hitAir = true;
                        break;
                    }
                    // For non-center positions, just skip this block
                    continue;
                }
                
                // Only destroy if block is decayable
                if (isDecayable(blockState)) {
                    scheduleBlockDestroy(level, forwardPos, x * 5); // Stagger the destruction for visual effect
                    totalDestroyed++;
                }
            }
            if (hitAir) break;
        }
        if (hitAir) break;
    }
    
    // Go straight in negative X direction from center position
    for (let x = 1; x <= maxLength && totalDestroyed < maxBlocks; x++) {
        let hitAir = false;
        
        // Check all positions in the cross-section at this X level
        for (let y = -shaftRadius; y <= shaftRadius && totalDestroyed < maxBlocks; y++) {
            for (let z = -shaftRadius; z <= shaftRadius && totalDestroyed < maxBlocks; z++) {
                let backwardPos = new BlockPos(centerPos.getX() - x, centerPos.getY() + y, centerPos.getZ() + z);
                let blockState = level.getBlockState(backwardPos);
                
                // Check if we hit air
                if (blockState.block.getId() === 'minecraft:air') {
                    // If center column hits air, stop going backward entirely
                    if (y === 0 && z === 0) {
                        hitAir = true;
                        break;
                    }
                    // For non-center positions, just skip this block
                    continue;
                }
                
                // Only destroy if block is decayable
                if (isDecayable(blockState)) {
                    scheduleBlockDestroy(level, backwardPos, x * 5); // Stagger the destruction for visual effect
                    totalDestroyed++;
                }
            }
            if (hitAir) break;
        }
        if (hitAir) break;
    }
    
    return totalDestroyed;
}

// Explosive decay pattern - intense connected destruction with multiple waves
function explosiveDecay(level, centerPos, maxBlocks, intensity) {
    // For explosive pattern, we'll do multiple waves of connected decay
    let totalDecayed = 0;
    let wavesCount = Math.min(4, 1 + intensity);
    let blocksPerWave = Math.floor(maxBlocks / wavesCount);
    
    for (let wave = 0; wave < wavesCount && totalDecayed < maxBlocks; wave++) {
        let remainingBlocks = maxBlocks - totalDecayed;
        let currentWaveBlocks = Math.min(blocksPerWave, remainingBlocks);
        
        // Create a bias that favors rapid, aggressive spread
        let explosiveBias = function(newPos, startPos, distance) {
            // Very high chance for close connections, decreasing with distance
            let maxDistance = 3 + intensity;
            if (distance > maxDistance) return false;
            
            let baseChance = 0.9 - (distance * 0.15); // 90% down to ~45%
            
            // Intensity increases the aggression
            let intensityBonus = intensity * 0.05;
            baseChance += intensityBonus;
            
            // Later waves are slightly less aggressive
            let waveReduction = wave * 0.1;
            baseChance -= waveReduction;
            
            return Math.random() < Math.max(0.1, baseChance);
        };
        
        let waveDecayed = connectedDecay(level, centerPos, currentWaveBlocks, intensity, explosiveBias);
        totalDecayed += waveDecayed;
        
        // Small delay between waves for visual effect
        if (wave < wavesCount - 1) {
            Utils.server.scheduleInTicks(wave * 10, () => {
                // Visual effect between waves
                level.spawnParticles("minecraft:explosion", false, 
                    centerPos.getX(), centerPos.getY() + 1, centerPos.getZ(), 
                    1, 1, 1, 3, 0);
            });
        }
    }
    
    return totalDecayed;
}

// Helper function to get decayable blocks based on intensity
function getDecayableBlocks(intensity) {
    let baseBlocks = [
        'minecraft:oak_leaves', 'minecraft:spruce_leaves', 'minecraft:birch_leaves',
        'minecraft:jungle_leaves', 'minecraft:acacia_leaves', 'minecraft:dark_oak_leaves',
        'minecraft:oak_log', 'minecraft:spruce_log', 'minecraft:birch_log',
        'minecraft:jungle_log', 'minecraft:acacia_log', 'minecraft:dark_oak_log'
    ];
    
    if (intensity >= 2) {
        baseBlocks.push(
            'minecraft:grass_block', 'minecraft:grass', 'minecraft:dirt', 'minecraft:coarse_dirt',
            'minecraft:podzol', 'minecraft:mycelium', 'minecraft:sand', 'minecraft:red_sand',
            'minecraft:gravel', 'minecraft:clay'
        );
    }
    
    if (intensity >= 3) {
        baseBlocks.push(
            'minecraft:stone', 'minecraft:cobblestone', 'minecraft:mossy_cobblestone',
            'minecraft:stone_bricks', 'minecraft:andesite', 'minecraft:diorite', 'minecraft:granite'
        );
    }
    
    if (intensity >= 4) {
        baseBlocks.push(
            'minecraft:deepslate', 'minecraft:tuff', 'minecraft:blackstone',
            'minecraft:basalt', 'minecraft:smooth_basalt', 'minecraft:obsidian'
        );
    }
    
    return baseBlocks;
}

// Helper function to schedule block destruction with effects
function scheduleBlockDestroy(level, pos, delay) {
    Utils.server.scheduleInTicks(delay, () => {
        level.destroyBlock(pos, false);
        level.spawnParticles("minecraft:large_smoke", false, pos.getX(), pos.getY(), pos.getZ(), 0.3, 0.3, 0.3, 5, 0.1);
    });
}

// Helper function to add visual effects based on intensity
function addDecayVisualEffects(level, centerPos, intensity) {
    // Base effects
    level.spawnParticles("minecraft:large_smoke", false, centerPos.getX(), centerPos.getY() + 1, centerPos.getZ(), 
                        1 * intensity, 1, 1 * intensity, 20 * intensity, 0.1);
    
    if (intensity >= 2) {
        level.spawnParticles("minecraft:ash", false, centerPos.getX(), centerPos.getY() + 1, centerPos.getZ(), 
                            2 * intensity, 1, 2 * intensity, 30 * intensity, 0.1);
    }
    
    if (intensity >= 3) {
        level.spawnParticles("minecraft:explosion", false, centerPos.getX(), centerPos.getY() + 1, centerPos.getZ(), 
                            0.5, 0.5, 0.5, 3, 0);
        level.playLocalSound(centerPos.getX(), centerPos.getY(), centerPos.getZ(), "minecraft:entity.wither.hurt", "players", 1.0, 0.5, false);
    }
    
    if (intensity >= 4) {
        level.spawnParticles("minecraft:dragon_breath", false, centerPos.getX(), centerPos.getY() + 1, centerPos.getZ(), 
                            3, 2, 3, 50, 0.2);
        level.playLocalSound(centerPos.getX(), centerPos.getY(), centerPos.getZ(), "minecraft:entity.ender_dragon.hurt", "players", 0.7, 0.3, false);
    }
}

// Damages held item of the entity, handles the breaking of the item as well. Not for armor.
function damageHeldItem(player, username, server, hand, damage) {
    let handItem = hand === "main_hand" ? player.getMainHandItem() : player.getOffHandItem();
    
    if (handItem.isDamageableItem()) {
        handItem.setDamageValue(handItem.getDamageValue() + damage);
        
        checkBreakHandItem(player, username, server, handItem, hand);
    }
}

// Checks the durability of the item to see if it should break, then do it.
// Needed because simply setting durability does not trigger the break event.
function checkBreakHandItem(player, username, server, item, hand) {
    
    let handId = hand === "main_hand" ? "weapon.mainhand" : "weapon.offhand";

    if (item.getDamageValue() >= item.getMaxDamage()) {
        server.runCommandSilent(`execute as ${username} run item replace entity @s ${handId} with minecraft:air`)
        server.runCommandSilent(`execute as ${username} run playsound minecraft:entity.item.break player @s ~ ~ ~ 100 1`)
        // player.playLocalSound(player.getX(), player.getY(), player.getZ(), "minecraft:entity.item.break", "players", 1, 1, true)
    }
}