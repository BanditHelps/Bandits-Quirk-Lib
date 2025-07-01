package com.github.b4ndithelps.forge.fancymenu.placeholders;

import com.github.b4ndithelps.forge.BanditsQuirkLibForge;
import com.mojang.brigadier.ParseResults;
import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class CommandResultPlaceholder extends Placeholder {

    public CommandResultPlaceholder() {
        super("command_result");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String commandString = dps.values.get("command");

        if (commandString == null) {
            BanditsQuirkLibForge.LOGGER.error("Command Result placeholder requires 'command' parameter");
            return "";
        }

        try {
            // Get the server instance
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();

            if (server == null) {
                BanditsQuirkLibForge.LOGGER.error("Server is not available");
                return "";
            }

            // Get the client player
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) {
                BanditsQuirkLibForge.LOGGER.error("Player is not available");
                return "";
            }

            // Create a command source stack
            CommandSourceStack sourceStack = server.createCommandSourceStack()
                    .withSuppressedOutput() // Suppress the normal output to chat
                    .withMaximumPermission(2);

            // Use an AtomicReference to capture the result from the async operation
            AtomicReference<String> result = new AtomicReference<>("");

            // Execute the command on the server thread
            CompletableFuture<Void> future = server.submit(() -> {
                try {
                    // Create a custom command source that captures output
                    CommandSourceStack captureSource = new CommandSourceStack(
                            minecraft.player,
                            sourceStack.getPosition(),
                            sourceStack.getRotation(),
                            sourceStack.getLevel(),
                            2,
                            sourceStack.getTextName(),
                            sourceStack.getDisplayName(),
                            sourceStack.getServer(),
                            sourceStack.getEntity()
                    ) {

                        @Override
                        public void sendSuccess(Supplier<Component> messageSupplier, boolean allowLogging) {
                            result.set(messageSupplier.get().getString());
                            BanditsQuirkLibForge.LOGGER.info("Command result is: {}", result.get());
                        }

                        @Override
                        public void sendFailure(Component message) {
                            result.set("ERROR: " + message.getString());
                        }
                    };

                    // Parse the command
                    ParseResults<CommandSourceStack> parseResults = server.getCommands().getDispatcher().parse(commandString, captureSource);

                    // Execute the command
                    int commandResult = server.getCommands().performCommand(parseResults, commandString);

                    // If no message was captured but command succeeded, try to get a default result
                    if (result.get().isEmpty() && commandResult > 0) {
                        result.set(String.valueOf(commandResult));
                    }
                } catch (Exception e) {
                    result.set("Error: " + e.getMessage());
                    BanditsQuirkLibForge.LOGGER.error("Error executing command: " + commandString, e);
                }

            });

            try {
                future.get(java.util.concurrent.TimeUnit.SECONDS.toMillis(5), TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                BanditsQuirkLibForge.LOGGER.error("Command execution timed out or failed: " + commandString, e);
                return "TIMEOUT";
            }

            BanditsQuirkLibForge.LOGGER.info("COMMAND OUTPUT FOR PLACEHOLDER IS: {}", result.get());
            return result.get();

        } catch (Exception e) {
            BanditsQuirkLibForge.LOGGER.error("Error in Command result placeholder" + e.getMessage());
            return "0";
        }
    }

    @Override
    public @Nullable List<String> getValueNames() {
        List<String> values = new ArrayList<>();
        values.add("command");

        return values;
    }

    @Override
    public @NotNull String getDisplayName() {
        return "Command Result";
    }

    @Override
    public @Nullable List<String> getDescription() {
        return Arrays.asList(
                "Runs the inputted string as a command and uses the result as the value.",
                "Parameters:",
                "- command: Command to be ran"
        );
    }

    @Override
    public String getCategory() {
        return "BanditsQuirkLib";
    }

    @SuppressWarnings("removal")
    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        Map<String, String> defaultValues = new HashMap<>();
        defaultValues.put("command", "say Hello World!");
        return DeserializedPlaceholderString.build(this.getIdentifier(), defaultValues);
    }
}
