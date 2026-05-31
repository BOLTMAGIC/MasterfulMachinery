# Changelog

All notable changes to this project will be documented in this file.

The format is based on "Keep a Changelog" and this project follows [Semantic Versioning](https://semver.org/).

## [0.1.33.5] - 2026-05-31

### Added
- Structure JSON: optional `minTier` / `maxTier` on port layout pieces to restrict acceptable port tiers for that position.
- KubeJS: `PortConfigBuilderJS.tierRank(int)` allows registering ports with an explicit numeric `tierRank`.

### Changed
- Matching logic: ports without an explicit `tierRank` are now treated as `tierRank = 1` during structure matching.
- Default structure behavior: when `minTier` is not specified for a port position it defaults to `1` (i.e. ports must be at least tier 1 unless `minTier: 0` is set).

### Notes
- Backwards compatibility: to allow older / untagged ports (tier 0) in a position explicitly, set `minTier: 0` in the structure JSON / KubeJS key.
- Use `portType` (not `block`) in structure keys to enable flexible port-type matching and tier checks. Using `block` forces exact block match and bypasses tier logic.

## [0.1.33.1 + 0.1.33.2]

### Added
- Per-controller parallelism setting `maxParallelRecipes` (controller JSON / KJS) allowing different controllers to limit how many recipes can run in parallel.
- Per-structure override for `maxParallelRecipes` in structure JSON (and `StructureBuilderJS.maxParallelRecipes(int)`), so different multiblock tiers can specify different parallel limits.

### Changed
- Controller and recipe scheduling: `MachineControllerBlockEntity` now respects the following precedence when deciding how many recipes may run in parallel: structure override (if present) -> controller setting -> global config `MMConfig.MAX_PARALLEL_RECIPES`.
- `maxParallelRecipes` semantics: absent or `-1` = use fallback (controller/global); `0` = explicitly disable parallel processing (only one active recipe allowed); valid range is clamped to `0..100`.
- Backwards compatibility: controllers and structures without the new field continue to use the global configuration as before.
- Fixed Console Spam when recipe cant be processed due to a full output. (0.1.33.1)

### Notes
- The per-recipe `parallelProcessing` flag and controller defaults still apply: a recipe must allow parallel execution (or the controller must permit it) and the active parallel count must not exceed the effective `maxParallelRecipes` limit before a recipe is started.

## [0.1.33.0] - 2026-04-03 — Performance & Stability

### Added
- Per-controller cache for available capability amounts (ITEM, FLUID, ENERGY, MANA, STEAM, CREATE, MEKANISM_CHEMICAL) to reduce repeated handler queries per tick.
- Recipe requirement HashMap: recipes are preprocessed into a Map of required capability types and amounts for fast eligibility checks.
- Mekanism type-id cache for chemical normalization to avoid expensive string/object comparisons during recipe matching.

### Changed
- Early-exit paths during recipe search: controller aborts search as soon as a required capability is proven insufficient across relevant ports.
- Recipe checks now only validate capability types actually required by the recipe (no more blanket checks of all types).
- Reduced handler calls and temporary allocations (e.g., FluidStack creation) to lower MSPT under load.
- Excessive warnings/log spam reduced or moved to DEBUG level.

### Fixed
- Improved handling for multiblocks with permanent infinite inputs/outputs to avoid TPS degradation.

### Tech notes / suggested data structures
- CapabilityType (enum): ITEM, FLUID, ENERGY, MANA, STEAM, CREATE, MEKANISM_CHEMICAL
- RecipeRequirements: Map<CapabilityType, List<IngredientSpec>> (IngredientSpec: id, amount, matcher)
- ControllerCache (per-controller): stores availableAmounts per CapabilityType, lastValidatedTick, candidateRecipes; supports invalidateForPortChange()
- MekanismTypeIdCache: Map<String, MekTypeKey> with weak/TTL references to avoid long-lived heap retention


## [0.1.32.5] - 2026-03-22
### Changed
- Performance: Optimized fluid port handling to reduce server-tick overhead (TPS).
- Added early-exit checks and loop short-circuits in fluid port ingredient processing (canProcess, process, canOutput, output) 
  to avoid unnecessary handler calls and limit FluidStack allocations when nothing needs to be transferred.

## [0.1.32.4] - 2026-02-06
### Fixed
- Improved input validation and recipe selection to ensure only intended gases/fluids trigger the correct recipe and to prevent unintended recipe overrides when multiple inputs are present.

## [0.1.32.3] - 2026-01-31
### Fixed
- Output: Items with NBT data were not correctly recognized for insertion into empty output ports and therefore could not be inserted.
- JEI is now sorted by recipe ID.

## [0.1.32.2] - 2026-01-20
### Added
- New server command `/mm reform` (admin/OP only):
  - Asynchronously scans loaded chunks in players' view distances and triggers revalidation of discovered controllers.
  - Sends periodic progress updates to the command issuer and a final summary when finished.
  - Port blocks (Item/Fluid/Energy) now notify nearby controllers on removal (`onRemove`) so controllers can react immediately.

### Changed
- Controller/block-entity implementation:
  - Removed reflection-based manipulation of controller internals; replaced with explicit, public setter APIs.
  - Structure validations are executed safely on the server thread; asynchronous/delayed execution reduces races.

### Fixed
- Bug: Multiblock remained in a "dead" (not formed) state after removal and re-placement of parts.
  - Fixed race conditions by invoking immediate and delayed revalidation when parts are placed, and by notifying controllers when parts are removed.
- Sync fix: Block entity changes are now followed by `sendBlockUpdated(...)` to ensure clients see updated formed/unformed state and GUIs stay consistent.

## [0.1.31]
### Added
- New Priority Setter item:
  - Right-click increments priority (0..10). When priority reaches 10, and you right-click it again it wraps to 0.
  - Shift + Right-click in air resets the Priority Setter item to 0.
  - Shift + Right-click on an output port applies the currently selected priority to that port (no GUI needed).
  - Tooltip on the item shows the currently selected priority.
  - Jade/Waila integration: shows the currently selected priority for output ports only.
- Priority behavior for outputs:
  - Outputs now support a priority value (int, default 0). Max priority is 10.
  - Outputs will be filled by priority groups (highest priority first). When a priority group is full, filling continues to the next, lower priority group ("full-to-one" behavior).

### Changed
- Controller and storage behavior:
  - The controller uses references to port storage objects and reads priorities from those storage instances on demand. Changing a port's priority via the Priority Setter item is effective immediately.
- Tooltip and client data:
  - The server-side provider writes priority data only for output ports; input ports no longer expose priority in Jade/Waila.

### Security / Permissions
- Priority setting permissions:
  - Only players who have permissions on a port may change its priority. Integration respects claim managers (e.g., FTBChunks): only the claimer and their team can change priorities for ports in a claimed chunk.
  - Applying a priority requires the player to be able to modify the clicked block (server-side check).