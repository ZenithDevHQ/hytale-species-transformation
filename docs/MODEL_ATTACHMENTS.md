# Hytale Model Attachment System Documentation

This document details Hytale's model attachment system, which enables modular model assembly. This is key for implementing dynamic species transformations with interchangeable parts (robot arms, creature appendages, etc.).

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [ModelAsset Configuration](#modelasset-configuration)
3. [ModelAttachment Structure](#modelattachment-structure)
4. [Random Attachment System](#random-attachment-system)
5. [Model Creation Pipeline](#model-creation-pipeline)
6. [Dynamic Composition](#dynamic-composition)
7. [Code Examples](#code-examples)
8. [Limitations and Workarounds](#limitations-and-workarounds)

---

## Architecture Overview

The model attachment system allows complex models to be composed from multiple sub-models:

```
┌─────────────────────────────────────────────────────────────┐
│                      ModelAsset                             │
│  (Base model definition with attachment configuration)      │
├─────────────────────────────────────────────────────────────┤
│  defaultAttachments[]      │    randomAttachmentSets{}      │
│  (Always applied)          │    (Randomly selected per slot)│
├─────────────────────────────────────────────────────────────┤
│                    ModelAttachment                          │
│  (Individual sub-model with texture and gradient)           │
└─────────────────────────────────────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────┐
│                         Model                               │
│  (Final composed model with all attachments merged)         │
│  - attachments[] (combined default + random)                │
│  - randomAttachmentIds{} (slot -> selected attachment ID)   │
└─────────────────────────────────────────────────────────────┘
```

**Key Insight:** Attachments are defined at the `ModelAsset` level and compiled into a final `Model` at creation time. The `randomAttachmentIds` map preserves which random choices were made, enabling persistence and network sync.

---

## ModelAsset Configuration

**Location:** `com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset`

A `ModelAsset` defines the base model and all possible attachments.

### Core Properties

| Property | Type | Description |
|----------|------|-------------|
| `id` | String | Unique identifier (e.g., "Player", "Zombie") |
| `model` | String | Path to .blockymodel file |
| `texture` | String | Default texture path |
| `gradientSet` | String | Color palette ID |
| `gradientId` | String | Specific color from the palette |
| `boundingBox` | Box | Collision/hitbox dimensions |
| `eyeHeight` | float | Camera/eye position height |
| `minScale` / `maxScale` | float | Random scale range |

### Attachment Properties

| Property | Type | Description |
|----------|------|-------------|
| `defaultAttachments` | ModelAttachment[] | Always-applied attachments |
| `randomAttachmentSets` | Map<String, Map<String, ModelAttachment>> | Slot-based random attachments |

### Animation Properties

| Property | Type | Description |
|----------|------|-------------|
| `animationSetMap` | Map<String, AnimationSet> | Named animation sets |
| `particles` | ModelParticle[] | Particle effect definitions |
| `trails` | ModelTrail[] | Trail effect definitions |

---

## ModelAttachment Structure

**Location:** `com.hypixel.hytale.server.core.asset.type.model.config.ModelAttachment`

Each attachment represents a sub-model that can be added to the base model.

### Properties

| Property | Type | Description |
|----------|------|-------------|
| `model` | String | Path to .blockymodel file for this part |
| `texture` | String | Texture file path |
| `gradientSet` | String | Color palette for gradient coloring |
| `gradientId` | String | Specific color from the palette |
| `weight` | double | Selection weight for random attachments (default 1.0) |

### JSON Example

```json
{
  "Model": "Characters/Robot/RobotArm_Left.blockymodel",
  "Texture": "Characters/Robot/RobotArm.png",
  "GradientSet": "Metal",
  "GradientId": "Steel",
  "Weight": 1.0
}
```

---

## Random Attachment System

The random attachment system allows models to have varying appearances through slot-based selection.

### Slot Structure

`randomAttachmentSets` is a nested map:
- **Outer key:** Slot name (e.g., "LeftArm", "RightArm", "Tail")
- **Inner key:** Attachment option ID (e.g., "RobotArm", "ClawArm", "None")
- **Value:** `ModelAttachment` definition

### JSON Configuration Example

```json
{
  "RandomAttachmentSets": {
    "LeftArm": {
      "RobotArm": {
        "Model": "Characters/Robot/Arm_Robot.blockymodel",
        "Texture": "Characters/Robot/Arm_Robot.png",
        "Weight": 1.0
      },
      "ClawArm": {
        "Model": "Characters/Robot/Arm_Claw.blockymodel",
        "Texture": "Characters/Robot/Arm_Claw.png",
        "Weight": 0.5
      }
    },
    "RightArm": {
      "RobotArm": { ... },
      "ClawArm": { ... }
    },
    "BackPack": {
      "JetPack": { ... },
      "None": {
        "Model": null,
        "Texture": null,
        "Weight": 2.0
      }
    }
  }
}
```

### Weighted Selection

Attachments are selected using weighted random selection:

```java
// Internal weighted map structure
Map<String, IWeightedMap<String>> weightedRandomAttachmentSets;

// Selection process (in ModelAsset)
public Map<String, String> generateRandomAttachmentIds() {
    ThreadLocalRandom random = ThreadLocalRandom.current();
    Map<String, String> randomAttachmentIds = new Object2ObjectOpenHashMap<>();

    for (Entry<String, IWeightedMap<String>> entry : weightedRandomAttachmentSets.entrySet()) {
        String slotName = entry.getKey();           // e.g., "LeftArm"
        String attachmentId = entry.getValue().get(random);  // e.g., "RobotArm"

        if (attachmentId != null) {
            randomAttachmentIds.put(slotName, attachmentId);
        }
    }

    return randomAttachmentIds;
}
```

**Weight Behavior:**
- Higher weight = more likely to be selected
- `Weight: 0` = never selected
- `Weight: 2.0` = twice as likely as `Weight: 1.0`

---

## Model Creation Pipeline

### From ModelAsset to Model

```java
// 1. Get the ModelAsset
ModelAsset asset = ModelAsset.getAssetMap().getAsset("RobotNPC");

// 2. Generate random scale (within min/max range)
float scale = asset.generateRandomScale();

// 3. Generate random attachment selections
Map<String, String> randomAttachmentIds = asset.generateRandomAttachmentIds();

// 4. Create the final Model
Model model = Model.createScaledModel(asset, scale, randomAttachmentIds);
```

### Internal Model Creation

```java
public static Model createScaledModel(ModelAsset asset, float scale,
                                       Map<String, String> randomAttachmentIds) {
    // Get combined attachments (default + selected random)
    ModelAttachment[] attachments = asset.getAttachments(randomAttachmentIds);

    // Scale physics properties
    Box boundingBox = asset.getBoundingBox().clone().scale(scale);
    float eyeHeight = asset.getEyeHeight() * scale;
    // ... etc

    return new Model(
        asset.getId(),
        scale,
        randomAttachmentIds,  // Preserved for persistence/sync
        attachments,          // Combined attachment array
        boundingBox,
        asset.getModel(),
        asset.getTexture(),
        // ... other properties
    );
}
```

### Attachment Merging Logic

```java
public ModelAttachment[] getAttachments(Map<String, String> randomAttachmentIds) {
    if (randomAttachmentIds == null || randomAttachmentIds.isEmpty()) {
        return defaultAttachments;
    }

    List<ModelAttachment> attachments = new ObjectArrayList<>();

    // Add all default attachments first
    if (defaultAttachments != null) {
        Collections.addAll(attachments, defaultAttachments);
    }

    // Add selected random attachments
    for (Entry<String, String> entry : randomAttachmentIds.entrySet()) {
        String slotName = entry.getKey();
        String attachmentId = entry.getValue();

        Map<String, ModelAttachment> slotOptions = randomAttachmentSets.get(slotName);
        if (slotOptions != null) {
            ModelAttachment attachment = slotOptions.get(attachmentId);
            // Only add if it has valid model/texture
            if (attachment != null &&
                attachment.getModel() != null &&
                attachment.getTexture() != null) {
                attachments.add(attachment);
            }
        }
    }

    return attachments.toArray(ModelAttachment[]::new);
}
```

---

## Dynamic Composition

### Preserving Attachment Choices

The `randomAttachmentIds` map is stored in the `Model` and can be:
1. Persisted to save files
2. Sent over the network
3. Used to recreate identical models

### Model.ModelReference

For persistence, models can be converted to/from `ModelReference`:

```java
public class ModelReference {
    private String modelAssetId;              // e.g., "RobotNPC"
    private float scale;                      // e.g., 1.0
    private Map<String, String> randomAttachmentIds;  // Preserved selections
    private boolean staticModel;              // No animations

    // Convert back to full Model
    public Model toModel() {
        ModelAsset asset = ModelAsset.getAssetMap().getAsset(modelAssetId);
        return Model.createScaledModel(asset, scale, randomAttachmentIds, null, staticModel);
    }
}
```

### Creating Custom Attachment Combinations

```java
public Model createCustomRobotModel(String leftArm, String rightArm, String backpack) {
    ModelAsset robotAsset = ModelAsset.getAssetMap().getAsset("RobotNPC");

    // Build custom attachment selections
    Map<String, String> customAttachments = new HashMap<>();
    customAttachments.put("LeftArm", leftArm);     // e.g., "ClawArm"
    customAttachments.put("RightArm", rightArm);   // e.g., "RobotArm"
    customAttachments.put("BackPack", backpack);   // e.g., "JetPack"

    return Model.createScaledModel(robotAsset, 1.0f, customAttachments);
}
```

---

## Code Examples

### Modular Species Model Assembly

```java
public class SpeciesModelBuilder {

    /**
     * Creates a species model with specific body part selections.
     */
    public Model buildSpeciesModel(String speciesId, SpeciesPartSelection parts) {
        ModelAsset speciesAsset = ModelAsset.getAssetMap().getAsset(speciesId);
        if (speciesAsset == null) {
            throw new IllegalArgumentException("Unknown species: " + speciesId);
        }

        // Build attachment map from part selections
        Map<String, String> attachmentIds = new HashMap<>();

        if (parts.getLeftArm() != null) {
            attachmentIds.put("LeftArm", parts.getLeftArm());
        }
        if (parts.getRightArm() != null) {
            attachmentIds.put("RightArm", parts.getRightArm());
        }
        if (parts.getTail() != null) {
            attachmentIds.put("Tail", parts.getTail());
        }
        if (parts.getWings() != null) {
            attachmentIds.put("Wings", parts.getWings());
        }
        // ... more body parts

        return Model.createScaledModel(speciesAsset, parts.getScale(), attachmentIds);
    }

    /**
     * Data class for part selections
     */
    public static class SpeciesPartSelection {
        private String leftArm;
        private String rightArm;
        private String tail;
        private String wings;
        private float scale = 1.0f;

        // Getters, setters, builder pattern...
    }
}
```

### Applying Custom Model to Entity

```java
public void applySpeciesTransformation(Ref<EntityStore> ref,
                                        Store<EntityStore> store,
                                        String speciesId,
                                        Map<String, String> partSelections) {
    // Build the model
    ModelAsset speciesAsset = ModelAsset.getAssetMap().getAsset(speciesId);
    Model speciesModel = Model.createScaledModel(speciesAsset, 1.0f, partSelections);

    // Apply to entity
    store.putComponent(ref, ModelComponent.getComponentType(), new ModelComponent(speciesModel));

    // Store reference for later restoration
    Model.ModelReference modelRef = speciesModel.toReference();
    // Save modelRef to your mod's data storage...
}
```

### Querying Available Attachment Options

```java
public List<String> getAvailableAttachmentsForSlot(String speciesId, String slotName) {
    ModelAsset asset = ModelAsset.getAssetMap().getAsset(speciesId);
    if (asset == null || asset.getRandomAttachmentSets() == null) {
        return Collections.emptyList();
    }

    Map<String, ModelAttachment> slotOptions = asset.getRandomAttachmentSets().get(slotName);
    if (slotOptions == null) {
        return Collections.emptyList();
    }

    return new ArrayList<>(slotOptions.keySet());
}

// Usage
List<String> armOptions = getAvailableAttachmentsForSlot("RobotNPC", "LeftArm");
// Returns: ["RobotArm", "ClawArm", "LaserArm", ...]
```

### Creating a Subspecies with Locked Attachments

```java
public Model createSubspeciesModel(String baseSpeciesId,
                                    String subspeciesVariant,
                                    float heightMultiplier) {
    ModelAsset baseAsset = ModelAsset.getAssetMap().getAsset(baseSpeciesId);

    // Define locked attachments for this subspecies
    Map<String, String> lockedAttachments = switch (subspeciesVariant) {
        case "warrior" -> Map.of(
            "LeftArm", "ArmoredArm",
            "RightArm", "SwordArm",
            "Helmet", "WarriorHelm"
        );
        case "mage" -> Map.of(
            "LeftArm", "RuneArm",
            "RightArm", "StaffArm",
            "Cape", "MageCape"
        );
        case "scout" -> Map.of(
            "Wings", "SpeedWings",
            "Helmet", "GogglesHelm"
        );
        default -> new HashMap<>();
    };

    // Apply height scaling
    float scale = baseAsset.getMinScale() +
        (baseAsset.getMaxScale() - baseAsset.getMinScale()) * heightMultiplier;

    return Model.createScaledModel(baseAsset, scale, lockedAttachments);
}
```

---

## Limitations and Workarounds

### Limitation 1: No Runtime Slot Addition

**Issue:** You cannot add new attachment slots at runtime; slots are defined in the ModelAsset JSON.

**Workaround:** Pre-define all possible slots in your custom ModelAsset, including optional ones:

```json
{
  "RandomAttachmentSets": {
    "OptionalHorns": {
      "None": { "Model": null, "Texture": null, "Weight": 1.0 },
      "SmallHorns": { ... },
      "LargeHorns": { ... }
    }
  }
}
```

### Limitation 2: No Anchor Point System

**Issue:** Attachments don't have named anchor points; they're simply merged models.

**Workaround:** Design your .blockymodel files to include proper bone structures. Attachments should be positioned relative to the base model's origin in the modeling tool.

### Limitation 3: Attachments Don't Inherit Animations

**Issue:** Attachment models need their own animation data if they should move independently.

**Workaround:**
1. Design attachments to be driven by the base model's skeleton
2. Or define animation sets in the ModelAsset that include attachment bone animations

### Limitation 4: No Per-Entity Attachment Modification

**Issue:** You can't modify an existing Model's attachments; you must create a new Model.

**Workaround:** Store the current `randomAttachmentIds`, modify them, and create a fresh Model:

```java
public void swapAttachment(Ref<EntityStore> ref, Store<EntityStore> store,
                           String slotName, String newAttachmentId) {
    ModelComponent modelComp = store.getComponent(ref, ModelComponent.getComponentType());
    Model currentModel = modelComp.getModel();

    // Clone and modify attachment IDs
    Map<String, String> newAttachments = new HashMap<>(currentModel.getRandomAttachmentIds());
    newAttachments.put(slotName, newAttachmentId);

    // Recreate model
    ModelAsset asset = ModelAsset.getAssetMap().getAsset(currentModel.getModelAssetId());
    Model newModel = Model.createScaledModel(asset, currentModel.getScale(), newAttachments);

    // Apply
    store.putComponent(ref, ModelComponent.getComponentType(), new ModelComponent(newModel));
}
```

---

## Summary

The Hytale model attachment system provides:

1. **Default Attachments** - Always applied to every instance
2. **Random Attachment Slots** - Weighted random selection per slot
3. **Persistence** - `randomAttachmentIds` map for save/load/sync
4. **Flexible Composition** - Mix and match parts programmatically

For species transformation with modular parts:

- Define your species as a `ModelAsset` with attachment slots
- Use `randomAttachmentSets` for interchangeable body parts
- Store part selections in your mod's data structures
- Create new Models with custom attachment combinations

The system is **pull-based** (configuration defines options) rather than **push-based** (adding attachments at runtime), so plan your asset definitions accordingly.
