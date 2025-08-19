package com.github.b4ndithelps.util;

import com.github.b4ndithelps.BanditsQuirkLib;
import dev.architectury.platform.Platform;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Stream;

public class FileManager {
    
    // Track if we've already shown the options replacement screen this session
    private static boolean optionsReplacementScreenShown = false;

    public static boolean moveFileToConfig(String sourcePath, String fileName) {
        try {
            Path configDir = Platform.getConfigFolder();
            Path sourceFile = Path.of(sourcePath);
            Path targetFile = configDir.resolve(fileName);

            // Make sure the config directory is there
            Files.createDirectories(configDir);

            // Attempt to copy the file to the correct directory
            Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);

            BanditsQuirkLib.LOGGER.info("Successfully moved file to config: " + targetFile.toString());
            return true;

        } catch (IOException e) {
            BanditsQuirkLib.LOGGER.error("Failed to move file to config: " + e.getMessage());
            return false;
        }
    }

    public static boolean moveFileToConfigFromGameDir(String relativePath, String fileName) {
        try {
            Path gameDir = Platform.getGameFolder();
            Path configDir = Platform.getConfigFolder();
            Path sourceFile = gameDir.resolve(relativePath);
            Path targetFile = configDir.resolve(fileName);

            if (!Files.exists(sourceFile)) {
                BanditsQuirkLib.LOGGER.error("Source file does not exist: " + sourceFile.toString());
                return false;
            }

            // Verify config folder exists
            Files.createDirectories(configDir);

            // Move the file to the config directory
            Files.move(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);

            BanditsQuirkLib.LOGGER.info("Successfully moved file to config: " + targetFile.toString());
            return true;

        } catch (IOException e) {
            BanditsQuirkLib.LOGGER.error("Failed to move file to config: " + e.getMessage());
            return false;
        }
    }

    /**
     * Copies FancyMenu layouts from the addon to config and merges the GUI screens
     * This method detects the addon context and handles FancyMenu-specific file operations
     * @param addonName - Identifier of said addon
     * @return
     */
    public static boolean setupFancyMenuFiles(String addonName, String addonId) {
        try {
            Path configDir = Platform.getConfigFolder();
            Path markerFile = configDir.resolve(".delete_to_reload_fancymenu");

            // Check if the marker file exists - if so, skip execution
            if (Files.exists(markerFile)) {
                BanditsQuirkLib.LOGGER.info("FancyMenu setup already completed for " + addonName);
                BanditsQuirkLib.LOGGER.info("Delete the marker file to re-run FancyMenu setup.");
                return true;
            }

            Path gameDir = Platform.getGameFolder();

            // Construct the paths for the fancymenu files inside the addon
            Path addonPath = gameDir.resolve("addonpacks").resolve(addonName);
            Path layoutsPath = addonPath.resolve("assets").resolve(addonId).resolve("fancy_menu").resolve("layouts");
            Path guiPath = addonPath.resolve("assets").resolve(addonId).resolve("fancy_menu").resolve("gui");
            Path variablesPath = addonPath.resolve("assets").resolve(addonId).resolve("fancy_menu").resolve("variables");
            Path assetsPath = addonPath.resolve("assets").resolve(addonId).resolve("fancy_menu").resolve("assets");

            // Find the target files inside of the /config/fancymenu folder of the minecraft instance
            Path fancyMenuConfigDir = configDir.resolve("fancymenu");
            Path customizationDir = fancyMenuConfigDir.resolve("customization");
            Path customGuiScreensFile = fancyMenuConfigDir.resolve("custom_gui_screens.txt");
            Path userVariablesFile = fancyMenuConfigDir.resolve("user_variables.db");
            Path fancyMenuAssetDir = fancyMenuConfigDir.resolve("assets");

            BanditsQuirkLib.LOGGER.info("Setting up FancyMenu files for addon: " + addonName);

            // Ensure target directories exist
            Files.createDirectories(customizationDir);
            Files.createDirectories(fancyMenuConfigDir);
            Files.createDirectories(fancyMenuAssetDir);

            // Copy all layout files
            if (Files.exists(layoutsPath)) {
                copyFiles(layoutsPath, customizationDir);
            } else {
                BanditsQuirkLib.LOGGER.info("Layouts directory not found: " + layoutsPath.toString());
            }

            // Copy all asset files (mostly custom images)
            if (Files.exists(assetsPath)) {
                copyFiles(assetsPath, fancyMenuAssetDir);
            } else {
                BanditsQuirkLib.LOGGER.info("Assets directory not found: " + assetsPath.toString());
            }

            // Handle GUI screen files
            if (Files.exists(guiPath)) {
                mergeGuiScreenFiles(guiPath, customGuiScreensFile);
            } else {
                BanditsQuirkLib.LOGGER.info("GUI directory not found: " + guiPath.toString());
            }

            // Handle the user_variables merge
            if (Files.exists(variablesPath)) {
                mergeUserVariables(variablesPath, userVariablesFile);
            } else {
                BanditsQuirkLib.LOGGER.info("Variables directory not found: " + variablesPath.toString());
            }

            // Create marker file to prevent re-running
            createMarkerFile(markerFile, addonName);

            return true;


        } catch (IOException e) {
            BanditsQuirkLib.LOGGER.error("Failed to setup FancyMenu Files: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Creates a marker file to indicate FancyMenu setup has been completed
     */
    private static void createMarkerFile(Path markerFile, String addonName) throws IOException {
        String markerContent = "# FancyMenu Setup Marker File\n" +
                "# This file was created to prevent re-running FancyMenu setup on every world load.\n" +
                "# Delete this file to force FancyMenu files to be reloaded from the addon.\n" +
                "# \n" +
                "# Addon: " + addonName + "\n" +
                "# Created: " + java.time.LocalDateTime.now().toString() + "\n" +
                "# \n" +
                "# This file can be safely deleted if you want to refresh your FancyMenu configuration\n" +
                "# from the addon files.\n";

        Files.write(markerFile, markerContent.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        BanditsQuirkLib.LOGGER.info("Created marker file: " + markerFile.toString());
        BanditsQuirkLib.LOGGER.info("Delete this file to re-run FancyMenu setup in the future.");
    }

    private static void copyFiles(Path sourceDir, Path targetDir) throws IOException {
        BanditsQuirkLib.LOGGER.info("Copying layout files from: " + sourceDir.toString());

        try (Stream<Path> files = Files.walk(sourceDir)) {
            files.filter(Files::isRegularFile)
                    .forEach(sourceFile -> {
                        try {
                            Path relativePath = sourceDir.relativize(sourceFile);
                            Path targetFile = targetDir.resolve(relativePath);

                            // Ensure parent directories exist
                            Files.createDirectories(targetFile.getParent());

                            // Copy file
                            Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
                            BanditsQuirkLib.LOGGER.info("Copied file: " + relativePath.toString());

                        } catch (IOException e) {
                            BanditsQuirkLib.LOGGER.error("Failed to copy file: " + sourceFile.toString());
                        }
                    });
        }
    }

    private static void mergeGuiScreenFiles(Path sourceDir, Path targetFile) throws IOException {
        BanditsQuirkLib.LOGGER.info("Merging GUI screen files from: " + sourceDir.toString());

        StringBuilder contentBuilder = new StringBuilder();

        // Check if target file exists and read existing content
        if (Files.exists(targetFile)) {
            List<String> existingLines = Files.readAllLines(targetFile, StandardCharsets.UTF_8);
            for (String line : existingLines) {
                contentBuilder.append(line).append("\n");
            }
            BanditsQuirkLib.LOGGER.info("Appending to existing custom_gui_screens.txt");
        } else {
            // Create a new file with header
            contentBuilder.append("type = custom_gui_screens\n");
            contentBuilder.append("overridden_screens {\n");
            contentBuilder.append("}\n");
            BanditsQuirkLib.LOGGER.info("Creating new custom_gui_screens.txt");
        }

        // Read all GUI files and append their content
        try (Stream<Path> files = Files.walk(sourceDir)) {
            files.filter(Files::isRegularFile)
                    .forEach(guiFile -> {
                        try {
                            BanditsQuirkLib.LOGGER.info("Processing GUI file: " + guiFile.getFileName().toString());
                            List<String> lines = Files.readAllLines(guiFile, StandardCharsets.UTF_8);

                            // Add the file content
                            for (String line : lines) {
                                contentBuilder.append(line).append("\n");
                            }

                        } catch (IOException e) {
                            BanditsQuirkLib.LOGGER.error("Failed to read GUI file: " + guiFile.toString() + " - " + e.getMessage());
                        }
                    });
        }

        // Write the merged content to the target file
        Files.write(targetFile, contentBuilder.toString().getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        BanditsQuirkLib.LOGGER.info("Successfully merged GUI screen files to: " + targetFile.toString());
    }

    private static void mergeUserVariables(Path sourceDir, Path targetFile) throws IOException {
        BanditsQuirkLib.LOGGER.info("Merging user_variable files from: " + sourceDir.toString());

        StringBuilder contentBuilder = new StringBuilder();

        // Check if target file exists and read existing content
        if (Files.exists(targetFile)) {
            List<String> existingLines = Files.readAllLines(targetFile, StandardCharsets.UTF_8);
            for (String line : existingLines) {
                contentBuilder.append(line).append("\n");
            }
            BanditsQuirkLib.LOGGER.info("Appending to existing user_variables.db");
        } else {
            // Create a new file with header
            contentBuilder.append("type = user_variables\n");
            BanditsQuirkLib.LOGGER.info("Creating new user_variables.db");
        }

        // Read all variable files and append their content
        try (Stream<Path> files = Files.walk(sourceDir)) {
            files.filter(Files::isRegularFile)
                    .forEach(variableFile -> {
                        try {
                            BanditsQuirkLib.LOGGER.info("Processing Variable file: " + variableFile.getFileName().toString());
                            List<String> lines = Files.readAllLines(variableFile, StandardCharsets.UTF_8);

                            // Add the file content
                            for (String line : lines) {
                                contentBuilder.append(line).append("\n");
                            }

                        } catch (IOException e) {
                            BanditsQuirkLib.LOGGER.error("Failed to read Variable file: " + variableFile.toString() + " - " + e.getMessage());
                        }
                    });
        }

        // Write the merged content to the target file
        Files.write(targetFile, contentBuilder.toString().getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        BanditsQuirkLib.LOGGER.info("Successfully merged variable files to: " + targetFile.toString());
    }

    /**
     * Copies an options.txt file from mod resources to /config/fancymenu/options.txt
     * Only copies the file if the marker file .delete_to_reload_fancymenu is not present
     * @param resourcePath The path to the resource within the mod JAR (e.g., "options.txt")
     * @return true if successful, false if failed or setup already completed
     */
    public static boolean copyOptionsFileFromResources(String resourcePath) {
        try {
            Path configDir = Platform.getConfigFolder();
            Path fancyMenuDir = configDir.resolve("fancymenu");
            Path targetFile = fancyMenuDir.resolve("options.txt");
            Path markerFile = configDir.resolve(".delete_to_reload_fancymenu");

            // Check if the marker file exists - if so, skip execution
            if (Files.exists(markerFile)) {
                BanditsQuirkLib.LOGGER.info("FancyMenu setup already completed, skipping options.txt copy from resources");
                BanditsQuirkLib.LOGGER.info("Delete the marker file to re-run FancyMenu setup.");
                return true; // Return true since this is expected behavior
            }

            // Get the resource from the mod JAR
            InputStream resourceStream = FileManager.class.getClassLoader().getResourceAsStream(resourcePath);
            if (resourceStream == null) {
                BanditsQuirkLib.LOGGER.error("Could not find options.txt resource at: " + resourcePath);
                return false;
            }

            // Ensure fancymenu directory exists
            Files.createDirectories(fancyMenuDir);

            // Copy the resource to the target location
            Files.copy(resourceStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
            resourceStream.close();

            BanditsQuirkLib.LOGGER.info("Successfully copied options.txt from mod resources to FancyMenu config: " + targetFile.toString());
            return true;

        } catch (IOException e) {
            BanditsQuirkLib.LOGGER.error("Failed to copy options.txt from mod resources to FancyMenu config: " + e.getMessage());
            return false;
        }
    }

    /**
     * Checks if the options.txt file was recently placed by checking if options.txt exists 
     * but the .delete_to_reload_fancymenu marker doesn't exist yet
     * Only returns true once per session to avoid showing the screen multiple times
     * @return true if the options.txt file was placed and FancyMenu setup hasn't completed
     */
    public static boolean wasOptionsFileReplaced() {
        try {
            // If we've already shown the screen this session, don't show it again
            if (optionsReplacementScreenShown) {
                return false;
            }
            
            Path configDir = Platform.getConfigFolder();
            Path markerFile = configDir.resolve(".delete_to_reload_fancymenu");
            Path optionsFile = configDir.resolve("fancymenu").resolve("options.txt");
            
            // If options.txt exists but the marker file doesn't, then options.txt was just placed
            boolean result = Files.exists(optionsFile) && !Files.exists(markerFile);
            
            if (result) {
                BanditsQuirkLib.LOGGER.info("Options file was placed but FancyMenu setup not completed - showing info screen");
                optionsReplacementScreenShown = true; // Mark that we've shown the screen
            }
            
            return result;
        } catch (Exception e) {
            BanditsQuirkLib.LOGGER.error("Failed to check options replacement status: " + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes the marker file to force a re-run of FancyMenu setup
     */
    public static boolean deleteMarkerFile() {
        try {
            Path configDir = Platform.getConfigFolder();
            Path markerFile = configDir.resolve(".delete_to_reload_fancymenu");

            if (Files.exists(markerFile)) {
                Files.delete(markerFile);
                System.out.println("Deleted marker file: " + markerFile.toString());
                System.out.println("FancyMenu setup will run again on next world load.");
                return true;
            } else {
                System.out.println("Marker file does not exist: " + markerFile.toString());
                return true; // Not an error if it doesn't exist
            }

        } catch (IOException e) {
            System.err.println("Failed to delete marker file: " + e.getMessage());
            return false;
        }
    }

}
