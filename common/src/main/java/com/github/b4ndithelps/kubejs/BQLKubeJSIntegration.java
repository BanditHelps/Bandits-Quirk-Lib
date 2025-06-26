package com.github.b4ndithelps.kubejs;

import com.github.b4ndithelps.util.FileManager;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingsEvent;
import dev.latvian.mods.kubejs.util.ConsoleJS;

public class BQLKubeJSIntegration extends KubeJSPlugin {

    @Override
    public void registerBindings(BindingsEvent event) {
        event.add("FileUtils", new FileUtilsJS());
    }

    public static class FileUtilsJS {

        public static boolean moveToConfig(String sourcePath, String fileName) {
            ConsoleJS.STARTUP.info("Moving file to config: " + sourcePath + " -> " + fileName);
            return FileManager.moveFileToConfig(sourcePath, fileName);
        }

        public static boolean moveFromGameDirToConfig(String relativePath, String fileName) {
            ConsoleJS.STARTUP.info("Moving file from game directory to config: " + relativePath + " -> " + fileName);
            return FileManager.moveFileToConfigFromGameDir(relativePath, fileName);
        }
    }
}
