# Species Transformation Integration Guide

A practical guide for implementing advanced species transformation features in Shadelight's mod. This document provides feasibility assessments, implementation strategies, and code examples for each requested feature.

## Table of Contents

1. [Feature Summary](#feature-summary)
2. [Cosmetics Persistence During Transformation](#cosmetics-persistence-during-transformation)
3. [Cosmetic Toggle System for Tall Subspecies](#cosmetic-toggle-system-for-tall-subspecies)
4. [Eye Color Reading for Glow Effects](#eye-color-reading-for-glow-effects)
5. [Modular Model Assembly](#modular-model-assembly)
6. [Complete Implementation Example](#complete-implementation-example)
7. [Best Practices](#best-practices)

---

## Feature Summary

| Feature | Feasibility | Complexity | Notes |
|---------|-------------|------------|-------|
| Cosmetics Persistence | ✅ Possible | Low | PlayerSkin is separate from Model |
| Cosmetic Toggle | ⚠️ Workaround | Medium | Create modified PlayerSkin copy |
| Eye Color Reading | ✅ Possible | Low | Direct access via CosmeticRegistry |
| Modular Assembly | ✅ Possible | Medium | Use ModelAsset attachment system |

---

## Cosmetics Persistence During Transformation

### Goal
Allow players to keep wearing most cosmetics when transformed into a new species.

### How It Works

In Hytale's architecture, **PlayerSkin** (cosmetic configuration) and **Model** (3D rendered appearance) are stored as **separate ECS components**:

- `PlayerSkinComponent` - Stores the player's cosmetic choices
- `ModelComponent` - Stores the rendered model

When a player transforms:
1. Their `PlayerSkinComponent` remains unchanged
2. Only the `ModelComponent` is swapped to the new species model
3. The original cosmetics data is preserved and can be restored

### Implementation

```java
package dev.zenith.species.transformation;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.cosmetics.CosmeticsModule;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.protocol.PlayerSkin;

import java.util.Map;

public class SpeciesTransformationService {

    /**
     * Transforms a player into a species while preserving their cosmetics.
     * The original PlayerSkin is untouched and can be used for restoration.
     */
    public TransformationResult transformPlayer(
            Ref<EntityStore> ref,
            ComponentAccessor<EntityStore> store,
            String speciesModelAssetId,
            float scale,
            Map<String, String> attachmentSelections) {

        // 1. Get current skin component (cosmetics)
        PlayerSkinComponent skinComponent = store.getComponent(ref, PlayerSkinComponent.getComponentType());
        PlayerSkin originalSkin = skinComponent != null ? skinComponent.getPlayerSkin() : null;

        // 2. Create the species model
        ModelAsset speciesAsset = ModelAsset.getAssetMap().getAsset(speciesModelAssetId);
        if (speciesAsset == null) {
            return TransformationResult.failure("Unknown species: " + speciesModelAssetId);
        }

        Model speciesModel = Model.createScaledModel(speciesAsset, scale, attachmentSelections);

        // 3. Apply new model (cosmetics are preserved in PlayerSkinComponent)
        store.putComponent(ref, ModelComponent.getComponentType(), new ModelComponent(speciesModel));

        // 4. Mark skin as needing network sync
        if (skinComponent != null) {
            skinComponent.setNetworkOutdated();
        }

        return TransformationResult.success(originalSkin, speciesModel);
    }

    /**
     * Restores a player to their original human form with cosmetics.
     */
    public void restoreOriginalForm(
            Ref<EntityStore> ref,
            ComponentAccessor<EntityStore> store) {

        PlayerSkinComponent skinComponent = store.getComponent(ref, PlayerSkinComponent.getComponentType());
        if (skinComponent == null) {
            return; // No skin to restore
        }

        // Recreate the player model from their preserved cosmetics
        Model playerModel = CosmeticsModule.get().createModel(skinComponent.getPlayerSkin());
        if (playerModel != null) {
            store.putComponent(ref, ModelComponent.getComponentType(), new ModelComponent(playerModel));
            skinComponent.setNetworkOutdated();
        }
    }

    public static class TransformationResult {
        private final boolean success;
        private final String error;
        private final PlayerSkin preservedSkin;
        private final Model appliedModel;

        // Constructor, getters, static factories...
        public static TransformationResult success(PlayerSkin skin, Model model) {
            return new TransformationResult(true, null, skin, model);
        }

        public static TransformationResult failure(String error) {
            return new TransformationResult(false, error, null, null);
        }

        private TransformationResult(boolean success, String error, PlayerSkin skin, Model model) {
            this.success = success;
            this.error = error;
            this.preservedSkin = skin;
            this.appliedModel = model;
        }

        public boolean isSuccess() { return success; }
        public String getError() { return error; }
        public PlayerSkin getPreservedSkin() { return preservedSkin; }
        public Model getAppliedModel() { return appliedModel; }
    }
}
```

### Key Points

- **No data loss:** PlayerSkin is never modified, only the Model changes
- **Automatic restore:** Call `CosmeticsModule.get().createModel(playerSkin)` to get back original appearance
- **Network sync:** Mark `skinComponent.setNetworkOutdated()` after changes

---

## Cosmetic Toggle System for Tall Subspecies

### Goal
Disable specific cosmetics for taller subspecies (they won't fit properly).

### Challenge

There is no direct API to "toggle off" individual cosmetic parts at runtime. The cosmetic system validates that required parts exist.

### Solution: Create a Filtered Skin Copy

Instead of toggling parts, create a modified `PlayerSkin` with certain optional parts nullified:

```java
package dev.zenith.species.cosmetics;

import com.hypixel.hytale.protocol.PlayerSkin;
import java.util.Set;

public class CosmeticFilter {

    /**
     * Creates a filtered copy of a PlayerSkin with specified parts disabled.
     *
     * @param original The original skin
     * @param disabledParts Set of part names to disable (e.g., "headAccessory", "haircut")
     * @return A new PlayerSkin with filtered parts
     */
    public static PlayerSkin filterSkin(PlayerSkin original, Set<String> disabledParts) {
        return new PlayerSkin(
            // Required parts - always keep these
            original.bodyCharacteristic,
            original.underwear,
            original.face,
            original.ears,
            original.mouth,
            original.eyes,

            // Optional parts - filter based on disabledParts set
            filter(disabledParts, "facialHair", original.facialHair),
            filter(disabledParts, "haircut", original.haircut),
            filter(disabledParts, "eyebrows", original.eyebrows),
            filter(disabledParts, "pants", original.pants),
            filter(disabledParts, "overpants", original.overpants),
            filter(disabledParts, "undertop", original.undertop),
            filter(disabledParts, "overtop", original.overtop),
            filter(disabledParts, "shoes", original.shoes),
            filter(disabledParts, "headAccessory", original.headAccessory),
            filter(disabledParts, "faceAccessory", original.faceAccessory),
            filter(disabledParts, "earAccessory", original.earAccessory),
            filter(disabledParts, "skinFeature", original.skinFeature),
            filter(disabledParts, "gloves", original.gloves),
            filter(disabledParts, "cape", original.cape)
        );
    }

    private static String filter(Set<String> disabled, String partName, String value) {
        return disabled.contains(partName) ? null : value;
    }

    /**
     * Predefined filter for tall subspecies.
     * Disables parts that clip with larger head/body proportions.
     */
    public static final Set<String> TALL_SUBSPECIES_FILTER = Set.of(
        "headAccessory",    // Hats don't fit
        "haircut",          // Hair clips through
        "faceAccessory",    // Glasses positioned wrong
        "earAccessory"      // Earrings scale incorrectly
    );

    /**
     * Predefined filter for aquatic subspecies.
     */
    public static final Set<String> AQUATIC_SUBSPECIES_FILTER = Set.of(
        "shoes",            // No feet
        "pants",            // Tail instead of legs
        "overpants",
        "cape"              // Would drag in water
    );

    /**
     * Predefined filter for ethereal/ghost subspecies.
     */
    public static final Set<String> ETHEREAL_SUBSPECIES_FILTER = Set.of(
        "gloves",           // Transparent hands
        "shoes",            // Floating
        "facialHair",       // Clean ethereal look
        "skinFeature"       // Tattoos don't show
    );
}
```

### Usage with Species Transformation

```java
public void transformToTallSpecies(Ref<EntityStore> ref, ComponentAccessor<EntityStore> store) {
    PlayerSkinComponent skinComponent = store.getComponent(ref, PlayerSkinComponent.getComponentType());
    if (skinComponent == null) return;

    // Store original for later restoration
    PlayerSkin originalSkin = skinComponent.getPlayerSkin();

    // Create filtered skin for tall species
    PlayerSkin filteredSkin = CosmeticFilter.filterSkin(
        originalSkin,
        CosmeticFilter.TALL_SUBSPECIES_FILTER
    );

    // Apply the filtered skin
    store.putComponent(ref, PlayerSkinComponent.getComponentType(),
        new PlayerSkinComponent(filteredSkin));

    // Apply species model
    ModelAsset tallSpecies = ModelAsset.getAssetMap().getAsset("TallSpecies");
    Model model = Model.createScaledModel(tallSpecies, 1.5f, null);
    store.putComponent(ref, ModelComponent.getComponentType(), new ModelComponent(model));

    // Trigger network sync
    store.getComponent(ref, PlayerSkinComponent.getComponentType()).setNetworkOutdated();

    // Store original skin in your mod's data for restoration
    storeOriginalSkin(ref, originalSkin);
}
```

### Validation Consideration

When filtering skins, ensure required parts remain:
- `face`, `ears`, `mouth` - Simple string IDs, always required
- `bodyCharacteristic`, `underwear`, `eyes` - Always required with textures

The filter system only removes **optional** parts, so validation will always pass.

---

## Eye Color Reading for Glow Effects

### Goal
Read the player's selected eye color to apply matching glow effects on the species model.

### Implementation

```java
package dev.zenith.species.effects;

import com.hypixel.hytale.server.core.cosmetics.CosmeticRegistry;
import com.hypixel.hytale.server.core.cosmetics.CosmeticsModule;
import com.hypixel.hytale.server.core.cosmetics.PlayerSkinTintColor;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent;
import com.hypixel.hytale.protocol.PlayerSkin;

public class EyeGlowService {

    /**
     * Extracts the player's eye color as RGB values (0.0 - 1.0 range).
     *
     * @param skinComponent The player's skin component
     * @return float[3] with {red, green, blue} values, or white {1,1,1} if unavailable
     */
    public static float[] getEyeColorRGB(PlayerSkinComponent skinComponent) {
        if (skinComponent == null) {
            return new float[]{1.0f, 1.0f, 1.0f}; // Default white
        }

        PlayerSkin skin = skinComponent.getPlayerSkin();
        if (skin.eyes == null) {
            return new float[]{1.0f, 1.0f, 1.0f};
        }

        // Parse the eye part ID to get the color
        // Format: "EyeAssetId.ColorId" e.g., "DefaultEyes.Blue"
        String[] parts = skin.eyes.split("\\.");
        String colorId = parts.length > 1 ? parts[1] : null;

        if (colorId == null) {
            return new float[]{1.0f, 1.0f, 1.0f};
        }

        // Look up the color in the registry
        CosmeticRegistry registry = CosmeticsModule.get().getRegistry();
        PlayerSkinTintColor eyeColor = registry.getEyeColors().get(colorId);

        if (eyeColor == null || eyeColor.getBaseColor() == null ||
            eyeColor.getBaseColor().length == 0) {
            return new float[]{1.0f, 1.0f, 1.0f};
        }

        // Parse the primary color (index 0 is usually the brightest/main color)
        String hexColor = eyeColor.getBaseColor()[0];
        return parseHexColor(hexColor);
    }

    /**
     * Gets the full eye color gradient (highlight, base, shadow).
     *
     * @param skinComponent The player's skin component
     * @return Array of float[3] for each gradient stop, or null if unavailable
     */
    public static float[][] getEyeColorGradient(PlayerSkinComponent skinComponent) {
        if (skinComponent == null) return null;

        PlayerSkin skin = skinComponent.getPlayerSkin();
        if (skin.eyes == null) return null;

        String[] parts = skin.eyes.split("\\.");
        String colorId = parts.length > 1 ? parts[1] : null;
        if (colorId == null) return null;

        CosmeticRegistry registry = CosmeticsModule.get().getRegistry();
        PlayerSkinTintColor eyeColor = registry.getEyeColors().get(colorId);

        if (eyeColor == null || eyeColor.getBaseColor() == null) return null;

        String[] hexColors = eyeColor.getBaseColor();
        float[][] gradient = new float[hexColors.length][3];

        for (int i = 0; i < hexColors.length; i++) {
            gradient[i] = parseHexColor(hexColors[i]);
        }

        return gradient;
    }

    /**
     * Parses a hex color string to RGB float array.
     *
     * @param hex Color string like "#4A90D9" or "4A90D9"
     * @return float[3] with {r, g, b} in 0.0-1.0 range
     */
    private static float[] parseHexColor(String hex) {
        if (hex == null) {
            return new float[]{1.0f, 1.0f, 1.0f};
        }

        // Remove # if present
        hex = hex.startsWith("#") ? hex.substring(1) : hex;

        try {
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);

            return new float[]{r / 255.0f, g / 255.0f, b / 255.0f};
        } catch (Exception e) {
            return new float[]{1.0f, 1.0f, 1.0f};
        }
    }

    /**
     * Creates a glowing ColorLight for the species model based on eye color.
     *
     * @param skinComponent The player's skin component
     * @param intensity Glow intensity multiplier (1.0 = normal)
     * @return ColorLight for model configuration
     */
    public static com.hypixel.hytale.protocol.ColorLight createEyeGlow(
            PlayerSkinComponent skinComponent,
            float intensity) {

        float[] rgb = getEyeColorRGB(skinComponent);

        com.hypixel.hytale.protocol.ColorLight glow = new com.hypixel.hytale.protocol.ColorLight();
        glow.r = (int)(rgb[0] * 255 * intensity);
        glow.g = (int)(rgb[1] * 255 * intensity);
        glow.b = (int)(rgb[2] * 255 * intensity);
        glow.intensity = intensity;

        return glow;
    }
}
```

### Applying Eye Glow to Species Model

The `Model` class supports a `light` property for glow effects. However, since Model is immutable, you need to include the glow when creating the model:

```java
public Model createGlowingSpeciesModel(
        String speciesId,
        PlayerSkinComponent skinComponent,
        float glowIntensity) {

    // Get base species model
    ModelAsset asset = ModelAsset.getAssetMap().getAsset(speciesId);
    Model baseModel = Model.createScaledModel(asset, 1.0f);

    // Create with eye color glow
    // Note: You may need to create a custom model or use client-side effects
    // since Model's light is typically set at asset level

    // For runtime glow, consider using particle effects or client mods
    float[] eyeRgb = EyeGlowService.getEyeColorRGB(skinComponent);

    // Store the eye color for use in client-side rendering
    // Your mod would need to handle this on the client
    return baseModel;
}
```

### Client-Side Integration Note

The `Model.light` property affects the entire model. For **eye-specific** glow, you'll likely need:

1. **Particle effects** attached to the eye bone/position
2. **Custom shaders** on the client (if supported)
3. **Emissive texture maps** on the species model

The RGB values from `getEyeColorRGB()` can be sent to clients via custom packets for rendering.

---

## Modular Model Assembly

### Goal
Assemble species models on-the-fly from parts (robot arms, creature limbs, etc.).

### Implementation

Create custom `ModelAsset` definitions with attachment slots for each body part:

```json
// Example: Modular Robot Species
// assets/ModelAssets/RobotSpecies.json
{
  "Id": "RobotSpecies",
  "Model": "Characters/Species/Robot/RobotBase.blockymodel",
  "Texture": "Characters/Species/Robot/RobotBase.png",
  "HitBox": { "min": [0, 0, 0], "max": [0.6, 1.8, 0.6] },
  "EyeHeight": 1.6,
  "MinScale": 0.9,
  "MaxScale": 1.3,

  "DefaultAttachments": [
    {
      "Model": "Characters/Species/Robot/RobotCore.blockymodel",
      "Texture": "Characters/Species/Robot/RobotCore.png"
    }
  ],

  "RandomAttachmentSets": {
    "LeftArm": {
      "Standard": {
        "Model": "Characters/Species/Robot/Arms/Arm_Standard.blockymodel",
        "Texture": "Characters/Species/Robot/Arms/Arm_Standard.png",
        "Weight": 1.0
      },
      "Claw": {
        "Model": "Characters/Species/Robot/Arms/Arm_Claw.blockymodel",
        "Texture": "Characters/Species/Robot/Arms/Arm_Claw.png",
        "Weight": 0.5
      },
      "Cannon": {
        "Model": "Characters/Species/Robot/Arms/Arm_Cannon.blockymodel",
        "Texture": "Characters/Species/Robot/Arms/Arm_Cannon.png",
        "Weight": 0.3
      }
    },
    "RightArm": {
      "Standard": { ... },
      "Claw": { ... },
      "Shield": {
        "Model": "Characters/Species/Robot/Arms/Arm_Shield.blockymodel",
        "Texture": "Characters/Species/Robot/Arms/Arm_Shield.png",
        "Weight": 0.4
      }
    },
    "Legs": {
      "Bipedal": { ... },
      "Quadruped": { ... },
      "Hover": { ... },
      "Treads": { ... }
    },
    "BackModule": {
      "None": { "Model": null, "Texture": null, "Weight": 2.0 },
      "JetPack": { ... },
      "Wings": { ... },
      "CargoUnit": { ... }
    },
    "HeadUnit": {
      "Standard": { ... },
      "Scanner": { ... },
      "Visor": { ... }
    }
  }
}
```

### Java API for Modular Assembly

```java
package dev.zenith.species.modular;

import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;

import java.util.HashMap;
import java.util.Map;

public class ModularSpeciesBuilder {

    private final String speciesId;
    private final Map<String, String> partSelections = new HashMap<>();
    private float scale = 1.0f;

    public ModularSpeciesBuilder(String speciesId) {
        this.speciesId = speciesId;
    }

    public ModularSpeciesBuilder withLeftArm(String armType) {
        partSelections.put("LeftArm", armType);
        return this;
    }

    public ModularSpeciesBuilder withRightArm(String armType) {
        partSelections.put("RightArm", armType);
        return this;
    }

    public ModularSpeciesBuilder withLegs(String legType) {
        partSelections.put("Legs", legType);
        return this;
    }

    public ModularSpeciesBuilder withBackModule(String moduleType) {
        partSelections.put("BackModule", moduleType);
        return this;
    }

    public ModularSpeciesBuilder withHeadUnit(String headType) {
        partSelections.put("HeadUnit", headType);
        return this;
    }

    public ModularSpeciesBuilder withScale(float scale) {
        this.scale = scale;
        return this;
    }

    public ModularSpeciesBuilder withPart(String slotName, String partId) {
        partSelections.put(slotName, partId);
        return this;
    }

    public Model build() {
        ModelAsset asset = ModelAsset.getAssetMap().getAsset(speciesId);
        if (asset == null) {
            throw new IllegalStateException("Unknown species: " + speciesId);
        }

        return Model.createScaledModel(asset, scale, partSelections);
    }

    /**
     * Creates a randomized build using the asset's weight system.
     */
    public Model buildRandom() {
        ModelAsset asset = ModelAsset.getAssetMap().getAsset(speciesId);
        if (asset == null) {
            throw new IllegalStateException("Unknown species: " + speciesId);
        }

        // Generate random selections, then override with any manual selections
        Map<String, String> randomIds = asset.generateRandomAttachmentIds();
        if (randomIds != null) {
            randomIds.putAll(partSelections); // Manual overrides random
        } else {
            randomIds = partSelections;
        }

        float finalScale = scale > 0 ? scale : asset.generateRandomScale();
        return Model.createScaledModel(asset, finalScale, randomIds);
    }
}
```

### Usage Examples

```java
// Build a specific robot configuration
Model warriorRobot = new ModularSpeciesBuilder("RobotSpecies")
    .withLeftArm("Claw")
    .withRightArm("Shield")
    .withLegs("Bipedal")
    .withBackModule("JetPack")
    .withHeadUnit("Visor")
    .withScale(1.2f)
    .build();

// Build a randomized robot (respects weights)
Model randomRobot = new ModularSpeciesBuilder("RobotSpecies")
    .buildRandom();

// Mix: specify some parts, randomize others
Model mixedRobot = new ModularSpeciesBuilder("RobotSpecies")
    .withLegs("Hover")  // Always hover legs
    .buildRandom();     // Random arms, back, head
```

### Persisting Modular Configurations

```java
public class SpeciesConfiguration {
    private String speciesId;
    private float scale;
    private Map<String, String> partSelections;

    // Serialize to JSON or NBT for saving
    public String toJson() {
        // Use Gson, Jackson, or Hytale's codec system
        return "...";
    }

    public static SpeciesConfiguration fromJson(String json) {
        // Deserialize
        return null;
    }

    public Model toModel() {
        ModelAsset asset = ModelAsset.getAssetMap().getAsset(speciesId);
        return Model.createScaledModel(asset, scale, partSelections);
    }
}
```

---

## Complete Implementation Example

Here's how all features work together:

```java
package dev.zenith.species;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.protocol.PlayerSkin;

import dev.zenith.species.cosmetics.CosmeticFilter;
import dev.zenith.species.effects.EyeGlowService;
import dev.zenith.species.modular.ModularSpeciesBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SpeciesManager {

    // Store original skins for restoration
    private final Map<UUID, PlayerSkin> originalSkins = new HashMap<>();

    /**
     * Full species transformation with all features.
     */
    public void transformToSpecies(
            Ref<EntityStore> ref,
            ComponentAccessor<EntityStore> store,
            UUID playerId,
            SpeciesDefinition species,
            Map<String, String> partSelections) {

        // 1. Get and preserve original skin
        PlayerSkinComponent skinComponent = store.getComponent(ref, PlayerSkinComponent.getComponentType());
        if (skinComponent != null) {
            originalSkins.put(playerId, skinComponent.getPlayerSkin().clone());
        }

        // 2. Apply cosmetic filter if species requires it
        if (species.getCosmeticFilter() != null && skinComponent != null) {
            PlayerSkin filteredSkin = CosmeticFilter.filterSkin(
                skinComponent.getPlayerSkin(),
                species.getCosmeticFilter()
            );
            store.putComponent(ref, PlayerSkinComponent.getComponentType(),
                new PlayerSkinComponent(filteredSkin));
        }

        // 3. Build modular species model
        Model speciesModel = new ModularSpeciesBuilder(species.getModelAssetId())
            .withScale(species.getBaseScale())
            .build();

        // Override with player's part selections
        if (partSelections != null && !partSelections.isEmpty()) {
            for (Map.Entry<String, String> entry : partSelections.entrySet()) {
                speciesModel = new ModularSpeciesBuilder(species.getModelAssetId())
                    .withPart(entry.getKey(), entry.getValue())
                    .withScale(species.getBaseScale())
                    .build();
            }
        }

        // 4. Apply model
        store.putComponent(ref, ModelComponent.getComponentType(), new ModelComponent(speciesModel));

        // 5. Get eye color for glow effects (store for client-side use)
        if (skinComponent != null) {
            float[] eyeGlow = EyeGlowService.getEyeColorRGB(skinComponent);
            species.setActiveEyeGlow(playerId, eyeGlow);
        }

        // 6. Trigger network sync
        PlayerSkinComponent newSkinComp = store.getComponent(ref, PlayerSkinComponent.getComponentType());
        if (newSkinComp != null) {
            newSkinComp.setNetworkOutdated();
        }
    }

    /**
     * Restore player to original human form.
     */
    public void restoreHumanForm(
            Ref<EntityStore> ref,
            ComponentAccessor<EntityStore> store,
            UUID playerId) {

        PlayerSkin originalSkin = originalSkins.remove(playerId);
        if (originalSkin == null) {
            return; // Nothing to restore
        }

        // Restore original skin
        store.putComponent(ref, PlayerSkinComponent.getComponentType(),
            new PlayerSkinComponent(originalSkin));

        // Recreate human model from skin
        Model humanModel = com.hypixel.hytale.server.core.cosmetics.CosmeticsModule.get()
            .createModel(originalSkin);

        if (humanModel != null) {
            store.putComponent(ref, ModelComponent.getComponentType(), new ModelComponent(humanModel));
        }

        // Sync
        store.getComponent(ref, PlayerSkinComponent.getComponentType()).setNetworkOutdated();
    }

    /**
     * Species definition class.
     */
    public static class SpeciesDefinition {
        private final String id;
        private final String modelAssetId;
        private final float baseScale;
        private final Set<String> cosmeticFilter;
        private final Map<UUID, float[]> activeEyeGlows = new HashMap<>();

        public SpeciesDefinition(String id, String modelAssetId, float baseScale, Set<String> filter) {
            this.id = id;
            this.modelAssetId = modelAssetId;
            this.baseScale = baseScale;
            this.cosmeticFilter = filter;
        }

        // Predefined species
        public static final SpeciesDefinition TALL_GIANT = new SpeciesDefinition(
            "tall_giant", "GiantSpecies", 2.0f, CosmeticFilter.TALL_SUBSPECIES_FILTER
        );

        public static final SpeciesDefinition AQUATIC_MERFOLK = new SpeciesDefinition(
            "aquatic_merfolk", "MerfolkSpecies", 1.0f, CosmeticFilter.AQUATIC_SUBSPECIES_FILTER
        );

        public static final SpeciesDefinition ROBOT_ANDROID = new SpeciesDefinition(
            "robot_android", "RobotSpecies", 1.0f, null // No filter, cosmetics work fine
        );

        // Getters
        public String getId() { return id; }
        public String getModelAssetId() { return modelAssetId; }
        public float getBaseScale() { return baseScale; }
        public Set<String> getCosmeticFilter() { return cosmeticFilter; }

        public void setActiveEyeGlow(UUID playerId, float[] rgb) {
            activeEyeGlows.put(playerId, rgb);
        }

        public float[] getActiveEyeGlow(UUID playerId) {
            return activeEyeGlows.get(playerId);
        }
    }
}
```

---

## Best Practices

### 1. Always Preserve Original Data
```java
// Store original skin BEFORE any modifications
PlayerSkin original = skinComponent.getPlayerSkin().clone();
storeForLater(playerId, original);
```

### 2. Validate ModelAssets Exist
```java
ModelAsset asset = ModelAsset.getAssetMap().getAsset(speciesId);
if (asset == null) {
    // Handle gracefully - don't crash
    log.warn("Unknown species asset: " + speciesId);
    return;
}
```

### 3. Mark Network Outdated After Changes
```java
// Always do this after visual changes
skinComponent.setNetworkOutdated();
```

### 4. Handle Null Components Gracefully
```java
PlayerSkinComponent skinComp = store.getComponent(ref, PlayerSkinComponent.getComponentType());
if (skinComp == null) {
    // Player might be an NPC or not fully initialized
    return;
}
```

### 5. Use Predefined Filters
```java
// Define once, reuse
public static final Set<String> FILTER = Set.of("headAccessory", "haircut");

// Don't recreate sets every time
PlayerSkin filtered = CosmeticFilter.filterSkin(skin, FILTER);
```

### 6. Store Part Selections for Persistence
```java
// When player customizes their robot parts, save it
SpeciesConfiguration config = new SpeciesConfiguration();
config.speciesId = "RobotSpecies";
config.partSelections = playerPartChoices;
saveToPlayerData(playerId, config);
```

---

## Summary

All four requested features are achievable:

| Feature | Approach |
|---------|----------|
| **Cosmetics Persistence** | PlayerSkin and Model are separate - just swap Model, keep Skin |
| **Cosmetic Toggle** | Create filtered PlayerSkin copy with nullified parts |
| **Eye Color Reading** | Access via `CosmeticRegistry.getEyeColors()` and parse hex |
| **Modular Assembly** | Use ModelAsset's `randomAttachmentSets` with custom selections |

The key architectural insight is that Hytale separates **configuration** (PlayerSkin) from **rendering** (Model), giving mods flexibility to manipulate either independently.
