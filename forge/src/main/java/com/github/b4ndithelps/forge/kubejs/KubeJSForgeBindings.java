package com.github.b4ndithelps.forge.kubejs;

import com.github.b4ndithelps.forge.systems.BodyStatusHelper;
import com.github.b4ndithelps.forge.systems.StaminaHelper;
import com.github.b4ndithelps.util.FileManager;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingsEvent;
import dev.latvian.mods.kubejs.util.ConsoleJS;

public class KubeJSForgeBindings extends KubeJSPlugin {

    @Override
    public void registerBindings(BindingsEvent event) {
        event.add("Stamina", new StaminaHelper());
        event.add("BodyStatus", new BodyStatusHelper());
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

        /**
         * Sets up FancyMenu files for the specified addon
         * Copies layout files to config/fancymenu/customization/
         * Merges GUI screen files into config/fancymenu/custom_gui_screens.txt
         *
         * @param addonName The name of the addon (e.g., "Mine-Hero-Addon")
         * @return true if successful, false otherwise
         */
        public static boolean setupFancyMenuFiles(String addonName, String addonId) {
            ConsoleJS.STARTUP.info("Setting up FancyMenu files for addon: " + addonName);
            boolean success = FileManager.setupFancyMenuFiles(addonName, addonId);

            if (success) {
                ConsoleJS.STARTUP.info("Successfully set up FancyMenu files for " + addonName);
                ConsoleJS.STARTUP.info("To re-run setup, delete the file: config/.delete_to_reload_fancymenu");
            } else {
                ConsoleJS.STARTUP.error("Failed to set up FancyMenu files for " + addonName);
            }

            return success;
        }

        /**
         * Forces a re-run of FancyMenu setup by deleting the marker file
         * Useful for debugging or when you want to refresh the FancyMenu configuration
         *
         * @return true if marker file was deleted or didn't exist, false if deletion failed
         */
        public static boolean forceReloadFancyMenu() {
            ConsoleJS.STARTUP.info("Forcing FancyMenu reload by deleting marker file");
            return FileManager.deleteMarkerFile();
        }
    }

}
