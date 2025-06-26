package com.github.b4ndithelps.util;

import com.github.b4ndithelps.BanditsQuirkLib;
import dev.architectury.platform.Platform;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class FileManager {

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

}
