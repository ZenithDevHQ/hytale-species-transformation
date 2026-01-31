package dev.zenith.species.command;

import dev.zenith.species.component.SpeciesComponent;

import org.jetbrains.annotations.NotNull;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Command to view a player's current species.
 *
 * Usage: /species [player]
 *
 * Arguments:
 *   player - Optional target player UUID (defaults to command sender)
 *
 * Permission: species.view
 *
 * This command displays:
 * - Current species ID
 * - Subtype (if set)
 * - Time since last transformation (optional)
 */
public class SpeciesCommand {

    private static final Logger LOGGER = Logger.getLogger("SpeciesCommand");

    // Permission node required to use this command
    public static final String PERMISSION = "species.view";

    // Command name
    public static final String NAME = "species";

    // Component type reference
    private final Object speciesType;

    /**
     * Creates a new SpeciesCommand.
     *
     * @param speciesType The ComponentType for SpeciesComponent
     */
    public SpeciesCommand(@NotNull Object speciesType) {
        this.speciesType = speciesType;
    }

    /**
     * Returns the command name.
     */
    public String getName() {
        return NAME;
    }

    /**
     * Returns the required permission.
     */
    public String getPermission() {
        return PERMISSION;
    }

    /**
     * Executes the species command.
     *
     * @param context The command context
     */
    public void executeSync(Object context) {
        // Get target player UUID (self if not specified)
        UUID targetUuid = getTargetUuidArg(context);

        // Get the target player
        Object targetPlayer = getPlayer(targetUuid);
        if (targetPlayer == null) {
            sendErrorMessage(context, "Player not found");
            return;
        }

        // Get species component
        SpeciesComponent species = getSpeciesComponent(targetPlayer);

        // Build display info
        String playerName = getPlayerName(targetPlayer);
        String speciesName = (species != null) ? species.getSpecies() : SpeciesComponent.HUMAN;
        String subtype = (species != null) ? species.getSubtype() : null;
        long transformedAt = (species != null) ? species.getTransformedAt() : 0;

        // Format and send message
        sendSpeciesInfo(context, playerName, speciesName, subtype, transformedAt);
    }

    // =========================================================================
    // Argument Parsing
    // =========================================================================

    /**
     * Gets the target player UUID argument (or sender's UUID if not specified).
     */
    private UUID getTargetUuidArg(Object context) {
        /*
         * Actual Hytale implementation:
         *
         * CommandContext ctx = (CommandContext) context;
         * OptionalArg<UUID> targetArg = ...;  // defined in constructor
         * return targetArg.getOrDefault(ctx, ctx.getPlayer().getUuid());
         */
        return null; // Placeholder
    }

    // =========================================================================
    // Player Access
    // =========================================================================

    /**
     * Gets a player by UUID.
     */
    private Object getPlayer(UUID uuid) {
        /*
         * Actual Hytale implementation:
         *
         * return Universe.get().getPlayer(uuid);
         */
        return null; // Placeholder
    }

    /**
     * Gets a player's display name.
     */
    private String getPlayerName(Object player) {
        /*
         * Actual Hytale implementation:
         *
         * PlayerRef playerRef = (PlayerRef) player;
         * return playerRef.getUsername();
         */
        return "Unknown";
    }

    // =========================================================================
    // Component Access
    // =========================================================================

    private SpeciesComponent getSpeciesComponent(Object player) {
        /*
         * Actual Hytale implementation:
         *
         * PlayerRef playerRef = (PlayerRef) player;
         * Holder<EntityStore> holder = playerRef.getHolder();
         * ComponentType<EntityStore, SpeciesComponent> type =
         *     (ComponentType<EntityStore, SpeciesComponent>) speciesType;
         * return holder.getComponent(type);
         */
        return null;
    }

    // =========================================================================
    // Messages
    // =========================================================================

    private void sendErrorMessage(Object context, String error) {
        /*
         * Actual Hytale implementation:
         *
         * CommandContext ctx = (CommandContext) context;
         * ctx.sendMessage(Message.raw(error).color(Color.RED));
         */
    }

    /**
     * Sends formatted species information to the command sender.
     */
    private void sendSpeciesInfo(Object context, String playerName, String species,
                                  String subtype, long transformedAt) {
        // Build display string
        StringBuilder display = new StringBuilder(playerName);
        display.append(" is a ").append(formatSpeciesName(species));

        if (subtype != null && !subtype.isEmpty()) {
            display.append(" (").append(subtype).append(")");
        }

        // Optionally add transformation time
        if (transformedAt > 0 && !SpeciesComponent.HUMAN.equals(species)) {
            long elapsed = System.currentTimeMillis() - transformedAt;
            String timeStr = formatDuration(elapsed);
            display.append(" - transformed ").append(timeStr).append(" ago");
        }

        /*
         * Actual Hytale implementation:
         *
         * CommandContext ctx = (CommandContext) context;
         * ctx.sendMessage(Message.raw(display.toString())
         *     .color(new Color(100, 200, 255)));  // Light blue
         */

        LOGGER.info(display.toString());
    }

    /**
     * Formats a species ID for display.
     */
    private String formatSpeciesName(String species) {
        // Capitalize first letter
        if (species == null || species.isEmpty()) {
            return "Unknown";
        }
        return species.substring(0, 1).toUpperCase() + species.substring(1);
    }

    /**
     * Formats a duration in milliseconds to a human-readable string.
     */
    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        if (seconds < 60) {
            return seconds + " second" + (seconds != 1 ? "s" : "");
        }

        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + " minute" + (minutes != 1 ? "s" : "");
        }

        long hours = minutes / 60;
        if (hours < 24) {
            return hours + " hour" + (hours != 1 ? "s" : "");
        }

        long days = hours / 24;
        return days + " day" + (days != 1 ? "s" : "");
    }
}
