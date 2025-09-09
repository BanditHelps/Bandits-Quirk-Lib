PalladiumEvents.registerAnimations((event) => {
    event.register('bql/no_legs_animation', 10, (builder) => {
        // Check if both legs are destroyed by checking if the player has the tags that enable the abilities
        const player = builder.getPlayer();

        let progress = animationUtil.getAnimationTimerAbilityValue(player, 'bql:body_status', 'no_legs_animation', builder.getPartialTicks());

        if (progress > 0.0) {
            builder.get('body').setY(-11).animate('InOutCubic', progress);
        }
    });
});