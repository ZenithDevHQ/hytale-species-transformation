package dev.zenith.species.component;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Component that tracks a player's current species.
 *
 * This component is attached to player entities and persists across sessions
 * using Hytale's BuilderCodec system.
 *
 * Species IDs:
 * - "human" - Default player species
 * - "newspecies" - Placeholder for custom species (modeler will finalize)
 *
 * Future: Subtypes can be added via the "dream mechanic" for species variants.
 */
public class SpeciesComponent implements Cloneable {

    // Species ID constants
    public static final String HUMAN = "human";
    public static final String NEW_SPECIES = "newspecies";

    // Component type reference (set by plugin during registration)
    // In actual Hytale API: ComponentType<EntityStore, SpeciesComponent>
    private static Object componentType;

    // Component fields (persisted via BuilderCodec)
    private String species = HUMAN;           // Current species ID
    private String subtype = null;            // Species subtype (for future dream mechanic)
    private long transformedAt = 0;           // Timestamp of last transformation
    private Object originalSkin = null;       // Stored human skin for reverting (PlayerSkin)

    /**
     * Default constructor - creates a human species component.
     */
    public SpeciesComponent() {
        this.species = HUMAN;
    }

    /**
     * Gets the component type registered with the entity component registry.
     *
     * @return The registered ComponentType for SpeciesComponent
     */
    public static Object getComponentType() {
        return componentType;
    }

    /**
     * Sets the component type. Called by the plugin during registration.
     *
     * @param type The ComponentType from entity component registry
     */
    public static void setComponentType(Object type) {
        componentType = type;
    }

    // =========================================================================
    // Species State
    // =========================================================================

    /**
     * Gets the current species ID.
     *
     * @return The species ID (e.g., "human", "newspecies")
     */
    @NotNull
    public String getSpecies() {
        return species;
    }

    /**
     * Checks if the player is currently human.
     *
     * @return true if species is "human"
     */
    public boolean isHuman() {
        return HUMAN.equals(species);
    }

    /**
     * Gets the current species subtype.
     *
     * @return The subtype or null if no subtype is set
     */
    @Nullable
    public String getSubtype() {
        return subtype;
    }

    /**
     * Sets the species subtype (used by dream mechanic).
     *
     * @param subtype The subtype to set, or null to clear
     */
    public void setSubtype(@Nullable String subtype) {
        this.subtype = subtype;
    }

    /**
     * Gets the timestamp of the last transformation.
     *
     * @return Unix timestamp in milliseconds
     */
    public long getTransformedAt() {
        return transformedAt;
    }

    /**
     * Gets the stored original skin (when transformed away from human).
     *
     * @return The stored PlayerSkin or null if human
     */
    @Nullable
    public Object getOriginalSkin() {
        return originalSkin;
    }

    // =========================================================================
    // Transformation Methods
    // =========================================================================

    /**
     * Transforms the player to a new species.
     *
     * If transforming from human, the current skin is stored for later restoration.
     *
     * @param newSpecies The species ID to transform to
     * @param currentSkin The player's current skin (stored if transforming from human)
     */
    public void transformTo(@NotNull String newSpecies, @Nullable Object currentSkin) {
        if (isHuman() && currentSkin != null) {
            // Store human appearance for reverting later
            this.originalSkin = currentSkin;
        }
        this.species = newSpecies;
        this.transformedAt = System.currentTimeMillis();
    }

    /**
     * Reverts the player back to human form.
     *
     * The original skin is kept stored in case the player transforms again.
     */
    public void revertToHuman() {
        this.species = HUMAN;
        this.subtype = null;
        this.transformedAt = System.currentTimeMillis();
        // Note: originalSkin is kept for potential re-transformation
    }

    /**
     * Checks if the player can transform to the given species.
     *
     * @param targetSpecies The species to check
     * @return true if transformation is allowed
     */
    public boolean canTransformTo(@NotNull String targetSpecies) {
        // Basic check - can't transform to current species
        return !targetSpecies.equals(this.species);
    }

    // =========================================================================
    // Clone Implementation (required for ECS components)
    // =========================================================================

    /**
     * Creates a deep copy of this component.
     * Required for Hytale's ECS component system.
     *
     * @return A new SpeciesComponent with copied values
     */
    @Override
    public SpeciesComponent clone() {
        SpeciesComponent copy = new SpeciesComponent();
        copy.species = this.species;
        copy.subtype = this.subtype;
        copy.transformedAt = this.transformedAt;
        copy.originalSkin = this.originalSkin;
        return copy;
    }

    // =========================================================================
    // BuilderCodec (for persistence)
    // =========================================================================

    /*
     * Actual Hytale implementation would use BuilderCodec like this:
     *
     * public static final BuilderCodec<SpeciesComponent> CODEC =
     *     BuilderCodec.builder(SpeciesComponent.class, SpeciesComponent::new)
     *         .append(new KeyedCodec<>("species", Codec.STRING),
     *                 (o, i) -> o.species = i,
     *                 o -> o.species)
     *         .add()
     *         .append(new KeyedCodec<>("subtype", Codec.STRING),
     *                 (o, i) -> o.subtype = i,
     *                 o -> o.subtype)
     *         .addValidator(Validators.nullable())
     *         .add()
     *         .append(new KeyedCodec<>("transformedAt", Codec.LONG),
     *                 (o, i) -> o.transformedAt = i,
     *                 o -> o.transformedAt)
     *         .add()
     *         .append(new KeyedCodec<>("originalSkin", PlayerSkin.CODEC),
     *                 (o, i) -> o.originalSkin = i,
     *                 o -> o.originalSkin)
     *         .addValidator(Validators.nullable())
     *         .add()
     *         .build();
     */

    // =========================================================================
    // Utility
    // =========================================================================

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("SpeciesComponent{species='");
        sb.append(species).append("'");
        if (subtype != null) {
            sb.append(", subtype='").append(subtype).append("'");
        }
        if (transformedAt > 0) {
            sb.append(", transformedAt=").append(transformedAt);
        }
        sb.append("}");
        return sb.toString();
    }
}
