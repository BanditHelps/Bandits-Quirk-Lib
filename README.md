# Bandit's Quirk Lib

The Bandit's Quirk Lib is a library specifically designed to allow Palladium addon pack developers to create new quirks/abilities that use the same core systems as each other. These systems currently include:
- Fully customizable stamina system
- Upgrade point delivery via stamina usage
- Fully customization "body" system to enable per body part data tracking
- A dynamic config system, allowing developers to create configs for their addons
- A Fancy Menu integrator that allows devs to ship fancy menu G.U.Is with their product.


# Body Status Commands

## Get Commands (No permissions required)
- `/body <player> get damage <bodypart>` - Get damage value for a body part
- `/body <player> get stage <bodypart>` - Get damage stage for a body part
- `/body <player> get status <bodypart> <statusName>` - Get custom status level
- `/body <player> get float <bodypart> <key>` - Get custom float value
- `/body <player> get string <bodypart> <key>` - Get custom string value
- `/body <player> get all` - Get all body status information
- `/body <player> get custom_statuses` - Get all custom statuses for all body parts

## Set Commands (Requires OP level 2)
- `/body <player> set damage <bodypart> <amount>` - Set damage amount (0.0+)
- `/body <player> set status <bodypart> <statusName> <level>` - Set custom status level (0+)
- `/body <player> set float <bodypart> <key> <value>` - Set custom float value
- `/body <player> set string <bodypart> <key> <value>` - Set custom string value

## Add Commands (Requires OP level 2)
- `/body <player> add damage <bodypart> <amount>` - Add damage to a body part (0.0+)

## Heal Commands (Requires OP level 2)
- `/body <player> heal <bodypart> <amount>` - Heal damage from a body part (0.0+)
- `/body <player> heal all` - Heal all body parts completely

## Reset Commands (Requires OP level 2)
- `/body <player> reset <bodypart>` - Reset a specific body part to default state
- `/body <player> reset all` - Reset all body parts to default state

## Admin Commands (Requires OP level 2)
- `/body <player> debug` - Show detailed debug information for all body parts
- `/body <player> test` - Run test scenarios with sample data
- `/body <player> init status <bodypart> <statusName> <defaultLevel>` - Initialize a new custom status
- `/body <player> init status_all <statusName> <defaultLevel>` - Initialize a new custom status on all body parts

## Parameters
- `<player>` - Target player name
- `<bodypart>` - Body part name (e.g., head, chest, left_arm, right_leg)
- `<amount>` - Damage amount (float, 0.0 or higher)
- `<statusName>` - Custom status identifier
- `<level>` - Status level (integer, 0 or higher)
- `<key>` - Custom data key
- `<value>` - Custom data value

---

# BQL Stamina Commands

## Get Commands (No permissions required)
- `/bql <player> get stamina` - Get current stamina statistics
- `/bql <player> get upgrade_points` - Get available upgrade points

## Set Commands (Requires OP level 2)
- `/bql <player> set max <amount>` - Set maximum stamina (0+)
- `/bql <player> set current <amount>` - Set current stamina (0+)
- `/bql <player> set exhaust <level>` - Set exhaustion level (0+)
- `/bql <player> set plus_ultra <value>` - Enable/disable Plus Ultra mode (true/false)
- `/bql <player> set upgrade_points <points>` - Set upgrade points (0+)

## Use Commands (Requires OP level 2)
- `/bql <player> use <amount>` - Force use stamina amount (0+)

## Add Commands (Requires OP level 2)
- `/bql <player> add max <amount>` - Add to maximum stamina (0+)
- `/bql <player> add current <amount>` - Add to current stamina (0+)

## Admin Commands (Requires OP level 2)
- `/bql <player> debug` - Show detailed stamina debug information

## Power System Commands (Requires OP level 2)
- `/bql_use <amount>` - Use stamina from command executor (0+)
- `/bql_use <amount> <chance>` - Use stamina with probability (chance: 0.0-1.0)

## Parameters
- `<player>` - Target player name
- `<amount>` - Stamina amount (integer, 0 or higher)
- `<level>` - Exhaustion level (integer, 0+)
- `<points>` - Upgrade points (integer, 0+)
- `<value>` - Boolean value (true/false)
- `<chance>` - Probability value (double, 0.0-1.0)
