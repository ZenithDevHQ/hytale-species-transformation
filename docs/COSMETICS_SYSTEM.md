# Hytale Cosmetics System Documentation

This document provides a comprehensive analysis of Hytale's cosmetics system based on decompiled server sources. This research supports the species transformation mod's goal of preserving cosmetics during player transformations.

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [CosmeticsModule API](#cosmeticsmodule-api)
3. [PlayerSkin Structure](#playerskin-structure)
4. [PlayerSkinPart Details](#playerskinpart-details)
5. [Texture and Gradient System](#texture-and-gradient-system)
6. [Eye Color System](#eye-color-system)
7. [Cosmetic Registry](#cosmetic-registry)
8. [ECS Integration](#ecs-integration)
9. [Code Examples](#code-examples)

---

## Architecture Overview

The Hytale cosmetics system follows a layered architecture:

```
┌─────────────────────────────────────────────────────────────┐
│                     CosmeticsModule                         │
│  (Main coordinator, validation, model generation)           │
├─────────────────────────────────────────────────────────────┤
│                     CosmeticRegistry                        │
│  (Loads all cosmetic assets from JSON files)                │
├─────────────────────────────────────────────────────────────┤
│     PlayerSkin          │      Model                        │
│  (Skin configuration)   │  (Rendered model with textures)   │
├─────────────────────────────────────────────────────────────┤
│  PlayerSkinPart  │  PlayerSkinPartTexture  │  Gradients     │
│  (Individual cosmetic pieces and their variations)          │
└─────────────────────────────────────────────────────────────┘
```

**Key Insight:** PlayerSkin and Model are **separate concepts**. A PlayerSkin defines the cosmetic configuration, while a Model is the actual rendered 3D model. This separation is crucial for species transformation.

---

## CosmeticsModule API

**Location:** `com.hypixel.hytale.server.core.cosmetics.CosmeticsModule`

The `CosmeticsModule` is the main entry point for all cosmetics operations.

### Key Methods

#### Get Instance
```java
CosmeticsModule.get()
```
Returns the singleton instance of the cosmetics module.

#### Create Model from Skin
```java
Model createModel(PlayerSkin skin)
Model createModel(PlayerSkin skin, float scale)
```
Creates a renderable Model from a PlayerSkin configuration. The model always uses the "Player" ModelAsset.

**Important:** This method:
1. Validates the skin first
2. Returns `null` if validation fails
3. Creates a scaled model from `ModelAsset.getAssetMap().getAsset("Player")`

#### Validate Skin
```java
void validateSkin(PlayerSkin skin) throws InvalidSkinException
```
Validates all parts of a PlayerSkin against the registry. Throws `InvalidSkinException` with details on which part is invalid.

#### Generate Random Skin
```java
PlayerSkin generateRandomSkin(Random random)
```
Generates a random valid skin configuration. Useful for NPCs or testing.

#### Get Registry
```java
CosmeticRegistry getRegistry()
```
Returns the cosmetic registry containing all loaded cosmetic assets.

### Validation Logic

The validation process checks each cosmetic part in this order:
1. **Required parts** (must not be null):
   - `face`, `ears`, `mouth`
   - `bodyCharacteristic`, `underwear`, `eyes`

2. **Optional parts** (can be null):
   - All clothing items
   - Accessories
   - Haircut, facial hair
   - Cape

3. **Special validation**:
   - Haircut validation considers head accessory type
   - `HalfCovering` accessories require a generic haircut fallback
   - `FullyCovering` accessories allow any haircut

---

## PlayerSkin Structure

**Location:** `com.hypixel.hytale.server.core.cosmetics.PlayerSkin`

PlayerSkin contains 20 cosmetic fields representing a complete character appearance.

### Required Fields (Non-Nullable)
| Field | Type | Description |
|-------|------|-------------|
| `face` | String | Face shape ID (simple ID, no texture) |
| `ears` | String | Ear shape ID (simple ID, no texture) |
| `mouth` | String | Mouth shape ID (simple ID, no texture) |
| `bodyCharacteristic` | PlayerSkinPartId | Body type with texture |
| `underwear` | PlayerSkinPartId | Base underwear with texture |
| `eyes` | PlayerSkinPartId | Eye style with color |

### Optional Fields (Nullable)
| Field | Type | Description |
|-------|------|-------------|
| `haircut` | PlayerSkinPartId | Hair style and color |
| `facialHair` | PlayerSkinPartId | Beard/mustache |
| `eyebrows` | PlayerSkinPartId | Eyebrow style and color |
| `pants` | PlayerSkinPartId | Pants/lower body clothing |
| `overpants` | PlayerSkinPartId | Pants overlay (belts, etc.) |
| `undertop` | PlayerSkinPartId | Undershirt/base top |
| `overtop` | PlayerSkinPartId | Outer shirt/jacket |
| `shoes` | PlayerSkinPartId | Footwear |
| `gloves` | PlayerSkinPartId | Hand coverings |
| `headAccessory` | PlayerSkinPartId | Hats, helmets, etc. |
| `faceAccessory` | PlayerSkinPartId | Glasses, masks, etc. |
| `earAccessory` | PlayerSkinPartId | Earrings, etc. |
| `skinFeature` | PlayerSkinPartId | Tattoos, scars, etc. |
| `cape` | PlayerSkinPartId | Back cape |

### PlayerSkinPartId Format

Part IDs follow the format: `{assetId}.{textureId}.{variantId}`

```java
public class PlayerSkinPartId {
    public final String assetId;    // e.g., "ShortHair"
    public final String textureId;  // e.g., "Brown"
    public final String variantId;  // e.g., "Curly" (optional)

    public static PlayerSkinPartId fromString(String stringId) {
        String[] parts = stringId.split("\\.");
        return new PlayerSkinPartId(
            parts[0],
            parts.length > 1 ? parts[1] : null,
            parts.length > 2 ? parts[2] : null
        );
    }
}
```

**Examples:**
- `"ShortHair.Brown"` - Short hair with brown color
- `"TShirt.Red.Striped"` - T-shirt with red striped variant
- `"DefaultEyes.Blue"` - Blue eyes

---

## PlayerSkinPart Details

**Location:** `com.hypixel.hytale.server.core.cosmetics.PlayerSkinPart`

Each cosmetic piece is defined by a `PlayerSkinPart` with these properties:

### Core Properties
| Property | Type | Description |
|----------|------|-------------|
| `id` | String | Unique identifier |
| `name` | String | Display name |
| `model` | String | Path to .blockymodel file |
| `greyscaleTexture` | String | Greyscale texture for gradient coloring |
| `gradientSet` | String | Which gradient set to use for coloring |
| `textures` | Map<String, PlayerSkinPartTexture> | Available texture variations |
| `variants` | Map<String, Variant> | Model/texture variants |
| `isDefaultAsset` | boolean | Whether this is a default/base asset |
| `tags` | String[] | Categorization tags |

### Haircut-Specific Properties
| Property | Type | Description |
|----------|------|-------------|
| `hairType` | HaircutType | `Short`, `Medium`, or `Long` |
| `requiresGenericHaircut` | boolean | Needs generic version with headwear |

### Head Accessory Properties
| Property | Type | Description |
|----------|------|-------------|
| `headAccessoryType` | HeadAccessoryType | `Simple`, `HalfCovering`, `FullyCovering` |

### Variant Structure

Variants allow the same cosmetic to have different models/textures:

```java
public class Variant {
    private String model;          // Different model path
    private String greyscaleTexture;
    private Map<String, PlayerSkinPartTexture> textures;
}
```

---

## Texture and Gradient System

### PlayerSkinPartTexture

**Location:** `com.hypixel.hytale.server.core.cosmetics.PlayerSkinPartTexture`

```java
public class PlayerSkinPartTexture {
    private String texture;      // Texture file path
    private String[] baseColor;  // RGB hex values for gradient coloring
}
```

### Gradient Sets

**Location:** `com.hypixel.hytale.server.core.cosmetics.PlayerSkinGradientSet`

Gradient sets define color palettes that can be applied to greyscale textures:

```java
public class PlayerSkinGradientSet {
    private String id;                                    // e.g., "Skin", "Hair"
    private Map<String, PlayerSkinPartTexture> gradients; // Color options
}
```

**Common Gradient Sets:**
- `"Skin"` - Skin tone colors
- `"Hair"` - Hair colors
- `"Eye"` - Eye colors

---

## Eye Color System

**Location:** `com.hypixel.hytale.server.core.cosmetics.PlayerSkinTintColor`

Eye colors are loaded from `EyeColors.json` and stored in `CosmeticRegistry.getEyeColors()`.

```java
public class PlayerSkinTintColor {
    protected String id;          // e.g., "Blue", "Green"
    protected String[] baseColor; // RGB hex values as strings

    public String getId() { return id; }
    public String[] getBaseColor() { return baseColor; }
}
```

### Accessing Eye Color

```java
// Get registry
CosmeticRegistry registry = CosmeticsModule.get().getRegistry();

// Get all eye colors
Map<String, PlayerSkinTintColor> eyeColors = registry.getEyeColors();

// Get specific eye color
PlayerSkinTintColor blueEyes = eyeColors.get("Blue");
String[] rgb = blueEyes.getBaseColor(); // ["#4A90D9", "#2E5B99", "#1A3D66"]

// From a player's skin
PlayerSkin skin = playerSkinComponent.getPlayerSkin();
PlayerSkinPartId eyesPartId = skin.getEyes();
String eyeColorId = eyesPartId.getTextureId(); // e.g., "Blue"
PlayerSkinTintColor eyeColor = eyeColors.get(eyeColorId);
```

### Eye Color BaseColor Array

The `baseColor` array typically contains 3 hex color values representing a gradient:
- Index 0: Highlight/light color
- Index 1: Base/mid color
- Index 2: Shadow/dark color

---

## Cosmetic Registry

**Location:** `com.hypixel.hytale.server.core.cosmetics.CosmeticRegistry`

The registry loads all cosmetic data from JSON files in `Cosmetics/CharacterCreator/`.

### Asset Paths
```
Cosmetics/CharacterCreator/
├── BodyCharacteristics.json
├── Capes.json
├── EarAccessory.json
├── Ears.json
├── EyeColors.json
├── Eyebrows.json
├── Eyes.json
├── FaceAccessory.json
├── Faces.json
├── FacialHair.json
├── Gloves.json
├── GradientSets.json
├── Haircuts.json
├── HeadAccessory.json
├── Mouths.json
├── Overpants.json
├── Overtops.json
├── Pants.json
├── Shoes.json
├── SkinFeatures.json
├── Underwear.json
└── Undertops.json
```

### Registry Methods

```java
CosmeticRegistry registry = CosmeticsModule.get().getRegistry();

// Get cosmetics by type
Map<String, PlayerSkinPart> haircuts = registry.getHaircuts();
Map<String, PlayerSkinPart> pants = registry.getPants();
// ... etc for each cosmetic type

// Get eye colors
Map<String, PlayerSkinTintColor> eyeColors = registry.getEyeColors();

// Get gradient sets
Map<String, PlayerSkinGradientSet> gradientSets = registry.getGradientSets();

// Get by CosmeticType enum
Map<String, ?> cosmetics = registry.getByType(CosmeticType.HAIRCUTS);
```

---

## ECS Integration

The cosmetics system integrates with Hytale's Entity Component System (ECS).

### PlayerSkinComponent

**Location:** `com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent`

Stores the player's skin configuration on an entity:

```java
public class PlayerSkinComponent implements Component<EntityStore> {
    private final PlayerSkin playerSkin;
    private boolean isNetworkOutdated = true;

    public PlayerSkin getPlayerSkin() { return playerSkin; }
    public void setNetworkOutdated() { isNetworkOutdated = true; }
    public boolean consumeNetworkOutdated() { ... }
}
```

### ModelComponent

**Location:** `com.hypixel.hytale.server.core.modules.entity.component.ModelComponent`

Stores the rendered model on an entity:

```java
public class ModelComponent implements Component<EntityStore> {
    private final Model model;
    private boolean isNetworkOutdated = true;

    public Model getModel() { return model; }
    public boolean consumeNetworkOutdated() { ... }
}
```

### Updating Entity Appearance

To change an entity's appearance:

```java
// Get components
PlayerSkinComponent skinComponent = store.getComponent(ref, PlayerSkinComponent.getComponentType());
PlayerSkin skin = skinComponent.getPlayerSkin();

// Create new model from skin
Model newModel = CosmeticsModule.get().createModel(skin, scale);

// Update model component
store.putComponent(ref, ModelComponent.getComponentType(), new ModelComponent(newModel));

// Mark skin as needing network sync
skinComponent.setNetworkOutdated();
```

### Network Synchronization

The `LegacyEntityTrackerSystems` handles syncing visual changes to clients:

- `LegacyEntityModel` - Syncs Model changes
- `LegacyEntitySkin` - Syncs PlayerSkin changes

Both track the `isNetworkOutdated` flag to know when to send updates.

---

## Code Examples

### Reading a Player's Eye Color for Glow Effect

```java
public Vector3f getPlayerEyeGlowColor(PlayerSkinComponent skinComponent) {
    PlayerSkin skin = skinComponent.getPlayerSkin();
    PlayerSkinPartId eyesId = skin.getEyes();

    if (eyesId == null || eyesId.getTextureId() == null) {
        return new Vector3f(1.0f, 1.0f, 1.0f); // Default white
    }

    CosmeticRegistry registry = CosmeticsModule.get().getRegistry();
    PlayerSkinTintColor eyeColor = registry.getEyeColors().get(eyesId.getTextureId());

    if (eyeColor == null || eyeColor.getBaseColor() == null) {
        return new Vector3f(1.0f, 1.0f, 1.0f);
    }

    // Parse the primary eye color (index 0)
    String hexColor = eyeColor.getBaseColor()[0];
    return parseHexToVector3f(hexColor);
}

private Vector3f parseHexToVector3f(String hex) {
    // Remove # if present
    hex = hex.startsWith("#") ? hex.substring(1) : hex;

    int r = Integer.parseInt(hex.substring(0, 2), 16);
    int g = Integer.parseInt(hex.substring(2, 4), 16);
    int b = Integer.parseInt(hex.substring(4, 6), 16);

    return new Vector3f(r / 255f, g / 255f, b / 255f);
}
```

### Preserving Cosmetics During Transformation

```java
public void transformWithCosmetics(Ref<EntityStore> ref,
                                   Store<EntityStore> store,
                                   String speciesModelAssetId) {
    // 1. Get current skin (cosmetics)
    PlayerSkinComponent skinComponent = store.getComponent(ref, PlayerSkinComponent.getComponentType());
    PlayerSkin originalSkin = skinComponent.getPlayerSkin();

    // 2. Create new model for species
    ModelAsset speciesAsset = ModelAsset.getAssetMap().getAsset(speciesModelAssetId);
    Model speciesModel = Model.createScaledModel(speciesAsset, 1.0f);

    // 3. Update model but KEEP the skin component
    store.putComponent(ref, ModelComponent.getComponentType(), new ModelComponent(speciesModel));

    // 4. Mark skin as needing sync (clients will receive both)
    skinComponent.setNetworkOutdated();

    // Note: The original PlayerSkin is preserved and can be restored later
}
```

### Creating a Filtered Skin (Disabling Parts)

```java
public PlayerSkin createFilteredSkin(PlayerSkin original, Set<String> disabledPartTypes) {
    return new PlayerSkin(
        disabledPartTypes.contains("bodyCharacteristic") ? null : original.getBodyCharacteristic(),
        disabledPartTypes.contains("underwear") ? null : original.getUnderwear(),
        original.getFace(), // Required - always keep
        original.getEars(), // Required - always keep
        original.getMouth(), // Required - always keep
        disabledPartTypes.contains("eyes") ? null : original.getEyes(),
        disabledPartTypes.contains("facialHair") ? null : original.getFacialHair(),
        disabledPartTypes.contains("haircut") ? null : original.getHaircut(),
        disabledPartTypes.contains("eyebrows") ? null : original.getEyebrows(),
        disabledPartTypes.contains("pants") ? null : original.getPants(),
        disabledPartTypes.contains("overpants") ? null : original.getOverpants(),
        disabledPartTypes.contains("undertop") ? null : original.getUndertop(),
        disabledPartTypes.contains("overtop") ? null : original.getOvertop(),
        disabledPartTypes.contains("shoes") ? null : original.getShoes(),
        disabledPartTypes.contains("headAccessory") ? null : original.getHeadAccessory(),
        disabledPartTypes.contains("faceAccessory") ? null : original.getFaceAccessory(),
        disabledPartTypes.contains("earAccessory") ? null : original.getEarAccessory(),
        disabledPartTypes.contains("skinFeature") ? null : original.getSkinFeature(),
        disabledPartTypes.contains("gloves") ? null : original.getGloves(),
        disabledPartTypes.contains("cape") ? null : original.getCape()
    );
}
```

---

## Summary

The Hytale cosmetics system provides:

1. **Separation of concerns** - PlayerSkin (configuration) vs Model (rendering)
2. **Flexible part system** - 20+ customizable parts with variants
3. **Color system** - Gradient sets and tint colors for recoloring
4. **ECS integration** - Clean component-based architecture
5. **Network sync** - Automatic client synchronization

For species transformation, the key insight is that **PlayerSkin and Model are independent**. You can:
- Change the Model (species) while preserving PlayerSkin (cosmetics)
- Read eye colors for glow effects
- Create filtered skins with disabled parts
- Restore original appearance by recreating the Model from stored PlayerSkin
