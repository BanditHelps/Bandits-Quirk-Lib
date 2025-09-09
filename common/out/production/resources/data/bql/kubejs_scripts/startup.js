//let myAddonConfigs = {
//    "test_gen": 6,
//    "test_gen_2": false
//}
//
//let myAddonDescriptions = {
//    "test_gen": "A test value just to see what it does",
//    "test_gen_2": "A second variable that tests other stuff"
//}
//
//Config.generateConfigTemplate("TestAddon", myAddonConfigs, myAddonDescriptions);
//
//ServerEvents.loaded(event => {
//    const server = event.server;
//
//});
//
//// Player login event
//PlayerEvents.loggedIn(event => {
//
////    BodyStatus.initializeNewStatusForAllParts(event.player, "frost", 0);
//
//});
//
//ServerEvents.commandRegistry(event => {
//    const { commands: Commands, arguments: Arguments } = event
//
//    event.register(
//            Commands.literal("co")
//                .requires(src => src.hasPermission(2))
//                .executes(ctx => {
//
//                    let player = ctx.source.player;
//                    let username = player.getGameProfile().getName();
//                    let server = ctx.source.getServer();
//
//                    player.tell(`${Config.getConfig("TestAddon.test_gen", 1.0)}`);
//
//                    return 1;
//                })
//        );
//
//});
