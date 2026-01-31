package dev.zenith.species.system;

import dev.zenith.species.component.SpeciesComponent;

import javax.annotation.Nonnull;
import java.util.logging.Logger;

/**
 * System that handles visual model swaps when a player's species changes.
 *
 * This is a RefChangeSystem that listens for changes to the SpeciesComponent
 * and applies the appropriate visual model based on the new species.
 *
 * In Hytale's ECS architecture:
 * - Extends RefChangeSystem<EntityStore, SpeciesComponent>
 * - Reacts to component add/set/remove events
 * - Updates player visual appearance based on species
 *
 * Model Assets (to be created by modeler):
 * - species:newspecies - Base new species model
 * - species:newspecies/subtype_a - Alternate subtype (future)
 */
public class SpeciesChangeSystem {

    private static final Logger LOGGER = Logger.getLogger("SpeciesChangeSystem");

    // Component type references (set during system registration)
    // In actual Hytale API: ComponentType<EntityStore, SpeciesComponent>
    private final Object speciesType;
    // In actual Hytale API: ComponentType<EntityStore, PlayerRef>
    private final Object playerRefType;

    /**
     * Creates a new SpeciesChangeSystem.
     *
     * @param speciesType The ComponentType for SpeciesComponent
     * @param playerRefType The ComponentType for PlayerRef
     */
    public SpeciesChangeSystem(@Nonnull Object speciesType, @Nonnull Object playerRefType) {
        this.speciesType = speciesType;
        this.playerRefType = playerRefType;
    }

    /**
     * Returns the component type this system monitors.
     *
     * @return The SpeciesComponent type
     */
    public Object componentType() {
        return this.speciesType;
    }

    // =========================================================================
    // Component Lifecycle Events
    // =========================================================================

    /**
     * Called when a SpeciesComponent is added to an entity.
     *
     * This typically happens when a player joins and their species data is loaded.
     *
     * @param ref The entity reference
     * @param component The newly added component
     * @param store The entity store
     * @param commandBuffer The command buffer for deferred operations
     */
    public void onComponentAdded(Object ref, SpeciesComponent component, Object store, Object commandBuffer) {
        // Get the PlayerRef component to access player data
        Object playerRef = getPlayerRef(ref, store);
        if (playerRef != null) {
            LOGGER.info("Species component added for player: " + component.getSpecies());
            applySpeciesAppearance(playerRef, component);
        }
    }

    /**
     * Called when a SpeciesComponent is modified.
     *
     * This is the main handler for transformation - when species changes,
     * we swap the visual model.
     *
     * @param ref The entity reference
     * @param oldComponent The previous component state
     * @param newComponent The new component state
     * @param store The entity store
     * @param commandBuffer The command buffer for deferred operations
     */
    public void onComponentSet(Object ref, SpeciesComponent oldComponent,
                               SpeciesComponent newComponent, Object store, Object commandBuffer) {
        // Check if species actually changed
        if (!oldComponent.getSpecies().equals(newComponent.getSpecies())) {
            Object playerRef = getPlayerRef(ref, store);
            if (playerRef != null) {
                LOGGER.info("Species changed: " + oldComponent.getSpecies() +
                           " -> " + newComponent.getSpecies());

                // Apply new visual appearance
                applySpeciesAppearance(playerRef, newComponent);

                // Notify the player
                notifyTransformation(playerRef, oldComponent, newComponent);
            }
        }

        // Check if subtype changed (for future dream mechanic)
        String oldSubtype = oldComponent.getSubtype();
        String newSubtype = newComponent.getSubtype();
        if (oldSubtype == null ? newSubtype != null : !oldSubtype.equals(newSubtype)) {
            Object playerRef = getPlayerRef(ref, store);
            if (playerRef != null && !newComponent.isHuman()) {
                LOGGER.info("Species subtype changed: " + oldSubtype + " -> " + newSubtype);
                // Apply subtype-specific visual changes
                applySubtypeAppearance(playerRef, newComponent);
            }
        }
    }

    /**
     * Called when a SpeciesComponent is removed from an entity.
     *
     * This shouldn't normally happen during gameplay.
     *
     * @param ref The entity reference
     * @param component The removed component
     * @param store The entity store
     * @param commandBuffer The command buffer for deferred operations
     */
    public void onComponentRemoved(Object ref, SpeciesComponent component, Object store, Object commandBuffer) {
        LOGGER.warning("Species component removed unexpectedly");
        // If needed, could restore default human appearance
    }

    // =========================================================================
    // Appearance Management
    // =========================================================================

    /**
     * Applies the visual appearance based on the player's species.
     *
     * @param player The PlayerRef
     * @param species The SpeciesComponent
     */
    private void applySpeciesAppearance(Object player, SpeciesComponent species) {
        if (species.isHuman()) {
            restoreHumanAppearance(player, species);
        } else {
            applyNewSpeciesModel(player, species.getSpecies(), species.getSubtype());
        }
    }

    /**
     * Restores the player's original human appearance.
     *
     * Uses the stored originalSkin from when they first transformed.
     *
     * @param player The PlayerRef
     * @param species The SpeciesComponent (contains stored skin)
     */
    private void restoreHumanAppearance(Object player, SpeciesComponent species) {
        Object originalSkin = species.getOriginalSkin();
        if (originalSkin != null) {
            LOGGER.info("Restoring human appearance");
            applyPlayerSkin(player, originalSkin);
        } else {
            LOGGER.info("No stored skin, using default human appearance");
            // Apply default human model if no stored skin
        }
    }

    /**
     * Applies the custom species model to the player.
     *
     * The model asset should be provided by the modeler and registered
     * with a resource ID like "species:newspecies".
     *
     * @param player The PlayerRef
     * @param speciesId The species ID (e.g., "newspecies")
     * @param subtype Optional subtype for variant models (e.g., "subtype_a")
     */
    private void applyNewSpeciesModel(Object player, String speciesId, String subtype) {
        // Build the model resource ID
        String modelId = "species:" + speciesId;
        if (subtype != null && !subtype.isEmpty()) {
            modelId += "/" + subtype;
        }

        LOGGER.info("Applying species model: " + modelId);

        /*
         * Actual Hytale implementation would look like:
         *
         * ModelRef modelRef = ContentModule.get().getModel(modelId);
         * if (modelRef != null) {
         *     // Apply the model to the player entity
         *     EntityVisuals visuals = player.getHolder().getComponent(EntityVisuals.TYPE);
         *     if (visuals != null) {
         *         visuals.setModel(modelRef);
         *     }
         * }
         *
         * Alternatively, using CosmeticsModule:
         * CosmeticsModule.get().applyModelOverride(player, modelRef);
         */
    }

    /**
     * Applies subtype-specific visual changes.
     *
     * Called when only the subtype changes (not the base species).
     *
     * @param player The PlayerRef
     * @param species The SpeciesComponent
     */
    private void applySubtypeAppearance(Object player, SpeciesComponent species) {
        // Reapply the model with the new subtype
        applyNewSpeciesModel(player, species.getSpecies(), species.getSubtype());
    }

    /**
     * Applies a stored PlayerSkin to the player.
     *
     * @param player The PlayerRef
     * @param skin The PlayerSkin to apply
     */
    private void applyPlayerSkin(Object player, Object skin) {
        LOGGER.info("Applying player skin");

        /*
         * Actual Hytale implementation:
         *
         * if (skin instanceof PlayerSkin playerSkin) {
         *     Model model = CosmeticsModule.get().createModel(playerSkin);
         *     EntityVisuals visuals = player.getHolder().getComponent(EntityVisuals.TYPE);
         *     if (visuals != null) {
         *         visuals.setModel(model);
         *     }
         * }
         */
    }

    // =========================================================================
    // Player Notification
    // =========================================================================

    /**
     * Notifies the player of their transformation.
     *
     * @param player The PlayerRef
     * @param from The previous species state
     * @param to The new species state
     */
    private void notifyTransformation(Object player, SpeciesComponent from, SpeciesComponent to) {
        /*
         * Actual Hytale implementation:
         *
         * PlayerRef playerRef = (PlayerRef) player;
         *
         * if (to.isHuman()) {
         *     playerRef.sendMessage(Message.raw("You have returned to your human form.")
         *         .color(new Color(255, 215, 0)));  // Gold color
         * } else {
         *     playerRef.sendMessage(Message.raw("You have transformed into a " + to.getSpecies() + "!")
         *         .color(new Color(138, 43, 226)));  // Purple/mystical color
         * }
         */

        if (to.isHuman()) {
            LOGGER.info("Player returned to human form");
        } else {
            LOGGER.info("Player transformed into: " + to.getSpecies());
        }
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    /**
     * Gets the PlayerRef component from an entity reference.
     *
     * @param ref The entity reference
     * @param store The entity store
     * @return The PlayerRef or null if not a player entity
     */
    private Object getPlayerRef(Object ref, Object store) {
        /*
         * Actual Hytale implementation:
         *
         * Store<EntityStore> entityStore = (Store<EntityStore>) store;
         * Ref<EntityStore> entityRef = (Ref<EntityStore>) ref;
         * return entityStore.getComponent(entityRef, playerRefType);
         */
        return null; // Placeholder - actual implementation depends on Hytale API
    }
}
