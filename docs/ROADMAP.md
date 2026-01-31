# Species Transformation Plugin - Development Roadmap

This document outlines the phased development plan for the Species Transformation plugin. The roadmap prioritizes modular model assembly (robot parts) as the core feature, with a single test species (robot) for v1.

## Technology Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Data Storage | PlayerConfigData | Native Hytale system, automatic persistence |
| Initial Species | Robot | Modular assembly showcase, clear visual distinction |
| Priority Feature | Modular Assembly | User-requested, demonstrates attachment system |
| Subspecies Support | Yes | Height variations with cosmetic filtering |

---

## Phase 1: Core Infrastructure

**Goal:** Establish the foundational data layer and configuration system.

### 1.1 PlayerConfigData Integration

- [ ] Create `SpeciesPlayerData` class extending PlayerConfigData
- [ ] Implement codec for serialization/deserialization
- [ ] Register data type with Hytale's player data system
- [ ] Add helper methods for reading/writing species state

```java
// Target structure
public class SpeciesPlayerData {
    private ResourceLocation speciesId;      // e.g., "species:robot"
    private ResourceLocation subspeciesId;   // e.g., "species:robot/tall"
    private Map<String, ResourceLocation> equippedParts;  // slot -> part
    private CompoundTag originalSkinData;    // for restoration
    private boolean transformed;
}
```

### 1.2 Species Configuration System

- [ ] Create `SpeciesDefinition` record/class
- [ ] Define robot species in configuration
- [ ] Load species definitions at plugin startup
- [ ] Create species registry for lookups

### 1.3 Basic SpeciesComponent

- [ ] Implement `SpeciesComponent` using Hytale's component system
- [ ] Attach component to players on join
- [ ] Sync component data between server and client
- [ ] Handle component persistence through PlayerConfigData

### Deliverables
- Working data persistence layer
- Single species (robot) defined and loadable
- Component attached to players

---

## Phase 2: Modular Model Assembly (Priority)

**Goal:** Implement the robot species with swappable parts.

### 2.1 Robot ModelAsset Setup

- [ ] Create base robot ModelAsset with attachment points
- [ ] Define attachment slots: `head`, `torso`, `left_arm`, `right_arm`, `left_leg`, `right_leg`, `back`
- [ ] Create default robot parts for each slot
- [ ] Ensure parts use consistent pivot points

### 2.2 Part Selection System

- [ ] Create `PartDefinition` class with slot, model, and metadata
- [ ] Implement `PartRegistry` for available parts per species
- [ ] Build part validation (ensure part fits slot)
- [ ] Create part swapping API

```java
// Example API
speciesManager.setPart(player, "left_arm", "species:robot/arm_cannon");
speciesManager.getPart(player, "left_arm"); // Returns current part
speciesManager.getAvailableParts("robot", "left_arm"); // List options
```

### 2.3 ModularSpeciesBuilder Utility

- [ ] Create builder class for assembling species models
- [ ] Implement attachment point resolution
- [ ] Handle default parts when none specified
- [ ] Support part animation state inheritance

```java
// Usage pattern
ModularSpeciesBuilder.forPlayer(player)
    .species("robot")
    .part("head", "species:robot/head_sensor")
    .part("back", "species:robot/jetpack")
    .build();
```

### 2.4 Part Persistence

- [ ] Store equipped parts in PlayerConfigData
- [ ] Restore parts on player login
- [ ] Handle missing/removed parts gracefully (fallback to defaults)

### Deliverables
- Robot model with functional attachment system
- Parts swappable via API
- Part choices persist across sessions

---

## Phase 3: Transformation Flow

**Goal:** Complete transform/revert lifecycle with user interactions.

### 3.1 Login Detection & Model Application

- [ ] Hook into player login event
- [ ] Check if player has species data
- [ ] Apply species model on login if transformed
- [ ] Handle first-time players (no species data)

### 3.2 Mid-Game Part Swapping

- [ ] Create part swap command: `/species part <slot> <part>`
- [ ] Implement live model update without re-transform
- [ ] Add part swap cooldown (optional)
- [ ] Trigger visual effect on part change

### 3.3 Transform/Revert Commands

- [ ] `/species transform <species>` - Transform player to species
- [ ] `/species revert` - Restore original form
- [ ] `/species info` - Show current species state
- [ ] Add permission nodes for each command

### 3.4 Altar Block Interaction

- [ ] Create transformation altar block
- [ ] Right-click opens species selection UI (or transforms directly)
- [ ] Add altar placement/breaking permissions
- [ ] Visual/sound effects on transformation

### Deliverables
- Full transform/revert cycle working
- Commands functional with permissions
- Altar block as alternative transformation method

---

## Phase 4: Subspecies & Cosmetic Filtering

**Goal:** Add height variants with cosmetic compatibility.

### 4.1 Height Variant System

- [ ] Add `heightScale` property to subspecies definitions
- [ ] Define robot subspecies: `robot/standard`, `robot/tall`, `robot/compact`
- [ ] Apply height scaling to player model
- [ ] Persist subspecies choice in player data

### 4.2 Cosmetic Filter Sets

- [ ] Create `CosmeticFilterSet` class
- [ ] Define compatible cosmetics per subspecies
- [ ] Build filter matching logic (tag-based or explicit list)
- [ ] Handle "no compatible cosmetics" case

```java
// Example filter definition
CosmeticFilterSet tallRobotFilter = CosmeticFilterSet.builder()
    .allowTag("cosmetic:tall_compatible")
    .allowExplicit("cosmetic:antenna")
    .denyTag("cosmetic:short_only")
    .build();
```

### 4.3 Filtered Skin Application

- [ ] Hook into cosmetic application system
- [ ] Filter cosmetics based on current subspecies
- [ ] Show/hide cosmetics dynamically on subspecies change
- [ ] Cache filter results for performance

### Deliverables
- Height variants working for robot species
- Cosmetics filter correctly per variant
- Subspecies persists and affects visuals

---

## Phase 5: Cosmetics Preservation & Eye Glow

**Goal:** Store original appearance and extract eye color for glow effects.

### 5.1 Original Skin Storage

- [ ] Capture player skin data before transformation
- [ ] Store in PlayerConfigData as CompoundTag
- [ ] Restore skin on revert command
- [ ] Handle skin changes while transformed (update stored data?)

### 5.2 Eye Color Extraction

- [ ] Access PlayerSkin texture data
- [ ] Sample eye region pixels (define coordinates)
- [ ] Calculate dominant eye color
- [ ] Store extracted color in species data

```java
// Eye extraction utility
Color eyeColor = EyeColorExtractor.extract(playerSkin);
// Returns average color from eye region
```

### 5.3 Eye Glow Data Sync

- [ ] Add eye color to SpeciesComponent sync data
- [ ] Create packet for client-side glow rendering
- [ ] Define glow intensity/animation properties
- [ ] Document client-side implementation requirements

> **Note:** Actual glow rendering requires client-side shader work. This phase prepares the data pipeline.

### Deliverables
- Original appearance stored and restorable
- Eye color extracted and available
- Glow data synced to client

---

## Phase 6: Polish & Testing

**Goal:** Harden the plugin for production use.

### 6.1 Error Handling

- [ ] Handle missing species definitions gracefully
- [ ] Validate all user input in commands
- [ ] Add meaningful error messages
- [ ] Log warnings for misconfigurations

### 6.2 Edge Cases

- [ ] Player disconnects mid-transformation
- [ ] Species definition removed while players transformed
- [ ] Part removed while equipped
- [ ] Concurrent modification protection

### 6.3 Admin Commands

- [ ] `/speciesadmin reload` - Reload species definitions
- [ ] `/speciesadmin set <player> <species>` - Force transform player
- [ ] `/speciesadmin reset <player>` - Clear player's species data
- [ ] `/speciesadmin list` - List all registered species

### 6.4 Documentation

- [ ] Update README with usage instructions
- [ ] Document configuration format
- [ ] Add Javadoc to public API
- [ ] Create example species definition

### Deliverables
- Robust error handling
- Admin tools functional
- Documentation complete

---

## Dependencies Between Phases

```
Phase 1 (Core Infrastructure)
    │
    ├──► Phase 2 (Modular Assembly) ──► Phase 3 (Transformation Flow)
    │                                          │
    │                                          ▼
    │                                   Phase 4 (Subspecies)
    │                                          │
    └──────────────────────────────────────────┤
                                               ▼
                                        Phase 5 (Cosmetics & Glow)
                                               │
                                               ▼
                                        Phase 6 (Polish)
```

- **Phase 2** requires Phase 1's data layer
- **Phase 3** requires Phase 2's model system
- **Phase 4** requires Phase 3's transformation flow
- **Phase 5** can start after Phase 1 but integrates with Phase 4
- **Phase 6** runs parallel to/after all features complete

---

## Version Milestones

| Version | Phases | Description |
|---------|--------|-------------|
| v0.1.0-alpha | 1, 2 | Core data + modular parts working |
| v0.2.0-alpha | 3 | Full transformation flow |
| v0.3.0-alpha | 4 | Subspecies and cosmetic filtering |
| v0.4.0-beta | 5 | Skin preservation and eye glow data |
| v1.0.0 | 6 | Production-ready release |

---

## Reference Documents

- [Cosmetics System Research](./COSMETICS_SYSTEM.md)
- [Model Attachments Research](./MODEL_ATTACHMENTS.md)
- [Species Integration Guide](./SPECIES_INTEGRATION_GUIDE.md)

---

## Open Questions

1. **Part Unlock System:** Should parts be unlocked through gameplay, or all available from start?
2. **Transformation Restrictions:** Any world/dimension restrictions on transformation?
3. **Multiple Species:** Future support for switching between unlocked species?
4. **Visual Effects:** Particle effects during transformation (defined per-species)?

These can be addressed as the implementation progresses.
