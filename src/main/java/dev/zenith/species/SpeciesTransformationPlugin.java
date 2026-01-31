package dev.zenith.species;

import dev.zenith.species.block.TransformationAltarHandler;
import dev.zenith.species.command.SpeciesCommand;
import dev.zenith.species.command.TransformCommand;
import dev.zenith.species.component.SpeciesComponent;
import dev.zenith.species.system.SpeciesChangeSystem;

import org.jetbrains.annotations.NotNull;
import java.util.logging.Logger;

/**
 * Hytale Species Transformation Plugin
 *
 * Enables players to transform between human and custom species via
 * transformation altar blocks. Serves as a reference implementation
 * for custom player species models.
 *
 * Features:
 * - ECS-based species tracking via SpeciesComponent
 * - Visual model swap via SpeciesChangeSystem
 * - Block interaction for transformation (TransformationAltarHandler)
 * - Admin commands for testing (/transform, /species)
 * - Persistence across player sessions
 * - Future extensibility for species subtypes
 *
 * Required Content Pack Assets (to be created by modeler):
 * - species:newspecies - The custom species player model
 * - species:transformation_altar - The transformation altar block
 *
 * @author ZenithDevHQ
 * @version 1.0.0
 */
public class SpeciesTransformationPlugin {

    private static final Logger LOGGER = Logger.getLogger("SpeciesTransformation");

    // Singleton instance
    private static SpeciesTransformationPlugin instance;

    // Component type for species tracking
    // In actual Hytale API: ComponentType<EntityStore, SpeciesComponent>
    private Object speciesComponentType;

    // Plugin initialization data
    // In actual Hytale API: JavaPluginInit
    private final Object init;

    /**
     * Creates the plugin instance.
     *
     * @param init The plugin initialization data from Hytale
     */
    public SpeciesTransformationPlugin(@NotNull Object init) {
        this.init = init;
    }

    /**
     * Gets the singleton plugin instance.
     *
     * @return The plugin instance
     */
    public static SpeciesTransformationPlugin getInstance() {
        return instance;
    }

    /**
     * Called during plugin setup phase.
     *
     * Used for early initialization before the server is fully loaded.
     */
    public void setup() {
        instance = this;
        LOGGER.info("Species Transformation Plugin setting up...");
    }

    /**
     * Called when the plugin starts.
     *
     * This is where we register components, systems, handlers, and commands.
     */
    public void start() {
        LOGGER.info("Species Transformation Plugin starting...");

        // Register species component
        registerComponent();

        // Register species change system
        registerSystem();

        // Register block interaction handler
        registerBlockHandler();

        // Register commands
        registerCommands();

        LOGGER.info("Species Transformation Plugin loaded successfully!");
    }

    /**
     * Called when the plugin stops.
     */
    public void stop() {
        LOGGER.info("Species Transformation Plugin stopping...");
        instance = null;
    }

    // =========================================================================
    // Component Registration
    // =========================================================================

    /**
     * Registers the SpeciesComponent with the entity component registry.
     */
    private void registerComponent() {
        LOGGER.info("Registering SpeciesComponent...");

        /*
         * Actual Hytale implementation:
         *
         * ComponentRegistry<EntityStore> registry = getEntityComponentRegistry();
         *
         * this.speciesComponentType = registry.registerComponent(
         *     SpeciesComponent.class,
         *     "species:species",  // Resource ID for the component
         *     SpeciesComponent.CODEC  // BuilderCodec for serialization
         * );
         *
         * // Store type reference in component class for easy access
         * SpeciesComponent.setComponentType(this.speciesComponentType);
         */

        // Placeholder - store dummy reference
        this.speciesComponentType = new Object();
        SpeciesComponent.setComponentType(this.speciesComponentType);
    }

    // =========================================================================
    // System Registration
    // =========================================================================

    /**
     * Registers the SpeciesChangeSystem with the entity system registry.
     */
    private void registerSystem() {
        LOGGER.info("Registering SpeciesChangeSystem...");

        /*
         * Actual Hytale implementation:
         *
         * ComponentRegistry<EntityStore> registry = getEntityComponentRegistry();
         *
         * // Get PlayerRef component type (built-in)
         * ComponentType<EntityStore, PlayerRef> playerRefType = PlayerRef.getComponentType();
         *
         * // Create and register the system
         * SpeciesChangeSystem changeSystem = new SpeciesChangeSystem(
         *     speciesComponentType,
         *     playerRefType
         * );
         * registry.registerSystem(changeSystem);
         */

        // Create system (will be registered when actual API is available)
        SpeciesChangeSystem changeSystem = new SpeciesChangeSystem(
            speciesComponentType,
            null  // PlayerRef type would go here
        );
    }

    // =========================================================================
    // Block Handler Registration
    // =========================================================================

    /**
     * Registers the TransformationAltarHandler for the altar block.
     */
    private void registerBlockHandler() {
        LOGGER.info("Registering TransformationAltarHandler...");

        /*
         * Actual Hytale implementation:
         *
         * // Create handler for the transformation altar
         * TransformationAltarHandler altarHandler = new TransformationAltarHandler(
         *     speciesComponentType,
         *     SpeciesComponent.NEW_SPECIES
         * );
         *
         * // Register handler for the altar block type
         * BlockInteractionRegistry.get().registerHandler(
         *     "species:transformation_altar",
         *     altarHandler
         * );
         *
         * // Alternative: Register via content module
         * // ContentModule.get().getBlockType("species:transformation_altar")
         * //     .setInteractionHandler(altarHandler);
         */

        // Create handler (will be registered when actual API is available)
        TransformationAltarHandler altarHandler = new TransformationAltarHandler(
            speciesComponentType,
            SpeciesComponent.NEW_SPECIES
        );
    }

    // =========================================================================
    // Command Registration
    // =========================================================================

    /**
     * Registers the plugin commands.
     */
    private void registerCommands() {
        LOGGER.info("Registering commands...");

        /*
         * Actual Hytale implementation:
         *
         * CommandRegistry commandRegistry = getCommandRegistry();
         *
         * // Register /transform command (admin/testing)
         * commandRegistry.addCommand(new TransformCommand(speciesComponentType));
         *
         * // Register /species command (view current species)
         * commandRegistry.addCommand(new SpeciesCommand(speciesComponentType));
         */

        // Create commands (will be registered when actual API is available)
        TransformCommand transformCommand = new TransformCommand(speciesComponentType);
        SpeciesCommand speciesCommand = new SpeciesCommand(speciesComponentType);
    }

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Gets the registered SpeciesComponent type.
     *
     * This can be used by other plugins to access species data.
     *
     * @return The ComponentType for SpeciesComponent
     */
    public Object getSpeciesComponentType() {
        return speciesComponentType;
    }

    /**
     * Gets the plugin logger.
     *
     * @return The logger instance
     */
    public Logger getLogger() {
        return LOGGER;
    }
}
