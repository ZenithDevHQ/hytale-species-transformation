# Species Transformation Plugin

A Hytale plugin that enables players to transform between human and custom species via transformation altar blocks. Serves as a reference implementation for custom player species models.

## Features

- **ECS-based Species Tracking**: Uses Hytale's Entity Component System with `SpeciesComponent`
- **Visual Model Swap**: `SpeciesChangeSystem` handles model changes when species changes
- **Block-based Transformation**: Interact with transformation altars to transform
- **Reversible Transformations**: Human ↔ Custom Species via the same altar
- **Admin Commands**: `/transform` and `/species` for testing
- **Session Persistence**: Species data persists via BuilderCodec serialization

## Installation

1. Build the plugin:
   ```bash
   ./gradlew shadowJar
   ```

2. Copy the JAR to your Hytale mods folder:
   ```bash
   cp build/libs/SpeciesTransformation-1.0.0.jar ~/Documents/Hytale/mods/
   ```

3. Ensure the content pack with species models is installed.

## Required Content Pack Assets

The modeler needs to create these assets in a content pack:

| Asset ID | Type | Description |
|----------|------|-------------|
| `species:newspecies` | Player Model | The custom species player model |
| `species:transformation_altar` | Block | The transformation altar block |

## Commands

### /transform

Transform a player to a specific species.

**Usage:** `/transform <species> [player]`

**Permission:** `species.transform`

**Arguments:**
- `species` - The species ID (e.g., `human`, `newspecies`)
- `player` - Optional target player UUID (defaults to self)

**Examples:**
```
/transform newspecies          # Transform yourself
/transform human               # Revert to human
/transform newspecies 550e8400-e29b-41d4-a716-446655440000  # Transform specific player
```

### /species

View a player's current species.

**Usage:** `/species [player]`

**Permission:** `species.view`

**Arguments:**
- `player` - Optional target player UUID (defaults to self)

**Examples:**
```
/species                       # View your species
/species 550e8400-e29b-41d4-a716-446655440000  # View specific player
```

## Architecture

```
SpeciesTransformationPlugin
├── component/
│   └── SpeciesComponent      # ECS component tracking species state
├── system/
│   └── SpeciesChangeSystem   # RefChangeSystem for visual model swaps
├── block/
│   └── TransformationAltarHandler  # Block interaction handler
└── command/
    ├── TransformCommand      # /transform admin command
    └── SpeciesCommand        # /species view command
```

### Component Fields

`SpeciesComponent` stores:
- `species` - Current species ID (default: "human")
- `subtype` - Species subtype (for future extensibility)
- `transformedAt` - Timestamp of last transformation
- `originalSkin` - Stored human skin for reverting

### Transformation Flow

1. Player interacts with transformation altar block
2. `TransformationAltarHandler.onInteract()` is called
3. Handler updates `SpeciesComponent` on the player entity
4. `SpeciesChangeSystem.onComponentSet()` detects the change
5. System applies the new species model and notifies player

## Extending the Plugin

### Adding New Species

1. Create the model asset in your content pack
2. Add a constant in `SpeciesComponent`:
   ```java
   public static final String MY_SPECIES = "myspecies";
   ```
3. Register an altar handler for the new species:
   ```java
   new TransformationAltarHandler(speciesType, "myspecies")
   ```

## Building

Requirements:
- Java 25 (Temurin recommended)
- Gradle 9.0+

```bash
# Build the plugin
./gradlew shadowJar

# Clean build
./gradlew clean shadowJar
```

Output: `build/libs/SpeciesTransformation-1.0.0.jar`

## License

MIT License - see [LICENSE](LICENSE)

## Discord

Join our community: https://discord.gg/SNPjyfkYPc

## Credits

Created by ZenithDevHQ for the Hytale modding community.
