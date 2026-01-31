package dev.zenith.species.block;

import dev.zenith.species.component.SpeciesComponent;

import org.jetbrains.annotations.NotNull;
import java.util.logging.Logger;

/**
 * Handler for the Transformation Altar block interaction.
 *
 * When a player interacts with the altar block, they transform between
 * human and the target species. If already the target species, they
 * revert to human form.
 *
 * Block Asset (to be created by modeler):
 * - species:transformation_altar - The transformation altar block
 *
 * The altar can be:
 * - Found naturally in structures (generated world)
 * - Crafted by players (if crafting recipe is added)
 *
 * Future: Different altars could transform to different species.
 */
public class TransformationAltarHandler {

    private static final Logger LOGGER = Logger.getLogger("TransformationAltarHandler");

    // Component type reference (set during handler registration)
    // In actual Hytale API: ComponentType<EntityStore, SpeciesComponent>
    private final Object speciesType;

    // The species this altar transforms players to
    private final String targetSpecies;

    /**
     * Creates a new TransformationAltarHandler.
     *
     * @param speciesType The ComponentType for SpeciesComponent
     * @param targetSpecies The species ID this altar transforms to
     */
    public TransformationAltarHandler(@NotNull Object speciesType, @NotNull String targetSpecies) {
        this.speciesType = speciesType;
        this.targetSpecies = targetSpecies;
    }

    /**
     * Handles player interaction with the transformation altar.
     *
     * This is called when a player right-clicks/interacts with the altar block.
     *
     * @param player The player interacting (PlayerRef in actual API)
     * @param pos The block position (BlockPos in actual API)
     * @param world The world (World in actual API)
     * @return true if the interaction was handled, false to pass through
     */
    public boolean onInteract(Object player, Object pos, Object world) {
        LOGGER.info("Player interacting with transformation altar");

        // Get the player's species component
        SpeciesComponent species = getSpeciesComponent(player);

        if (species == null) {
            // Initialize component if missing (shouldn't normally happen)
            LOGGER.warning("Player missing species component, initializing");
            species = new SpeciesComponent();
            addSpeciesComponent(player, species);
        }

        // Get current player skin before transformation
        Object currentSkin = getCurrentPlayerSkin(player);

        // Determine transformation direction
        if (species.getSpecies().equals(targetSpecies)) {
            // Already this species - revert to human
            performRevertTransformation(player, species, pos, world);
        } else {
            // Transform to target species
            performTransformation(player, species, currentSkin, pos, world);
        }

        return true; // Interaction handled
    }

    // =========================================================================
    // Transformation Logic
    // =========================================================================

    /**
     * Performs the transformation from current species to target species.
     *
     * @param player The PlayerRef
     * @param species The player's SpeciesComponent
     * @param currentSkin The player's current skin (to store if human)
     * @param pos The altar block position
     * @param world The world
     */
    private void performTransformation(Object player, SpeciesComponent species,
                                       Object currentSkin, Object pos, Object world) {
        LOGGER.info("Transforming player to: " + targetSpecies);

        // Store skin and transform
        species.transformTo(targetSpecies, currentSkin);

        // Update the component in ECS (triggers SpeciesChangeSystem)
        updateSpeciesComponent(player, species);

        // Send message to player
        sendTransformationMessage(player);

        // Play effects
        playTransformationEffects(player, pos, world);
    }

    /**
     * Performs the reversion from target species back to human.
     *
     * @param player The PlayerRef
     * @param species The player's SpeciesComponent
     * @param pos The altar block position
     * @param world The world
     */
    private void performRevertTransformation(Object player, SpeciesComponent species,
                                             Object pos, Object world) {
        LOGGER.info("Reverting player to human form");

        // Revert to human
        species.revertToHuman();

        // Update the component in ECS (triggers SpeciesChangeSystem)
        updateSpeciesComponent(player, species);

        // Send message to player
        sendReversionMessage(player);

        // Play effects
        playTransformationEffects(player, pos, world);
    }

    // =========================================================================
    // Player Messages
    // =========================================================================

    /**
     * Sends a message to the player when transforming.
     *
     * @param player The PlayerRef
     */
    private void sendTransformationMessage(Object player) {
        /*
         * Actual Hytale implementation:
         *
         * PlayerRef playerRef = (PlayerRef) player;
         * playerRef.sendMessage(Message.raw("The altar's power flows through you...")
         *     .color(new Color(200, 100, 255)));  // Purple glow color
         */
        LOGGER.info("Sent transformation message to player");
    }

    /**
     * Sends a message to the player when reverting.
     *
     * @param player The PlayerRef
     */
    private void sendReversionMessage(Object player) {
        /*
         * Actual Hytale implementation:
         *
         * PlayerRef playerRef = (PlayerRef) player;
         * playerRef.sendMessage(Message.raw("The altar reverses your transformation...")
         *     .color(new Color(255, 255, 200)));  // Golden light color
         */
        LOGGER.info("Sent reversion message to player");
    }

    // =========================================================================
    // Visual Effects
    // =========================================================================

    /**
     * Plays transformation visual and audio effects.
     *
     * @param player The PlayerRef
     * @param pos The altar position (center of effects)
     * @param world The world to spawn effects in
     */
    private void playTransformationEffects(Object player, Object pos, Object world) {
        LOGGER.info("Playing transformation effects");

        /*
         * Actual Hytale implementation:
         *
         * World gameWorld = (World) world;
         * BlockPos blockPos = (BlockPos) pos;
         * Vector3 effectPos = blockPos.toVector3().add(0.5, 1.0, 0.5);
         *
         * // Spawn particles around the player
         * ParticleEffect transformEffect = new ParticleEffect.Builder()
         *     .type(ParticleTypes.MAGIC_SPIRAL)
         *     .position(effectPos)
         *     .count(50)
         *     .spread(1.5f)
         *     .color(new Color(200, 100, 255))  // Purple
         *     .duration(2.0f)
         *     .build();
         * gameWorld.spawnParticle(transformEffect);
         *
         * // Play transformation sound
         * SoundEvent transformSound = SoundEvents.MAGIC_TRANSFORM;
         * gameWorld.playSound(effectPos, transformSound, 1.0f, 1.0f);
         *
         * // Optional: Brief screen effect on player
         * PlayerRef playerRef = (PlayerRef) player;
         * playerRef.showScreenEffect(ScreenEffects.TRANSFORMATION_FLASH, 0.5f);
         */
    }

    // =========================================================================
    // Component Access Helpers
    // =========================================================================

    /**
     * Gets the SpeciesComponent from a player.
     *
     * @param player The PlayerRef
     * @return The SpeciesComponent or null if not found
     */
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
        return null; // Placeholder
    }

    /**
     * Adds a SpeciesComponent to a player.
     *
     * @param player The PlayerRef
     * @param species The component to add
     */
    private void addSpeciesComponent(Object player, SpeciesComponent species) {
        /*
         * Actual Hytale implementation:
         *
         * PlayerRef playerRef = (PlayerRef) player;
         * Holder<EntityStore> holder = playerRef.getHolder();
         * ComponentType<EntityStore, SpeciesComponent> type =
         *     (ComponentType<EntityStore, SpeciesComponent>) speciesType;
         * holder.addComponent(type, species);
         */
    }

    /**
     * Updates a player's SpeciesComponent (triggers change events).
     *
     * @param player The PlayerRef
     * @param species The updated component
     */
    private void updateSpeciesComponent(Object player, SpeciesComponent species) {
        /*
         * Actual Hytale implementation:
         *
         * PlayerRef playerRef = (PlayerRef) player;
         * Holder<EntityStore> holder = playerRef.getHolder();
         * ComponentType<EntityStore, SpeciesComponent> type =
         *     (ComponentType<EntityStore, SpeciesComponent>) speciesType;
         * holder.setComponent(type, species);
         *
         * This will trigger SpeciesChangeSystem.onComponentSet()
         */
    }

    /**
     * Gets the player's current skin.
     *
     * @param player The PlayerRef
     * @return The PlayerSkin or null
     */
    private Object getCurrentPlayerSkin(Object player) {
        /*
         * Actual Hytale implementation:
         *
         * PlayerRef playerRef = (PlayerRef) player;
         * return CosmeticsModule.get().getPlayerSkin(playerRef);
         *
         * Or from account data:
         * return playerRef.getAccountData().getSkin();
         */
        return null; // Placeholder
    }
}
