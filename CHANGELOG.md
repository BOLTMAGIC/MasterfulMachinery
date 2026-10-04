# Changelog

All notable changes to this project will be documented in this file.

The format is based on "Keep a Changelog" and this project follows [Semantic Versioning](https://semver.org/).

## [0.1.35.5 + 6] - 2026-10-04

### INFO:
**Version 0.1.35.5 + 6** represents a feature expansion with remote structure placement and extended reach capabilities.

**Special Thanks:**
- **Knozyy** – Remote placement system, extended targeting reach, and layer-by-layer building modes.

### Added

#### #77 – Structure Builder: Remote Placement & Extended Reach
- **Extended reach**: preview and place multiblocks up to **64 blocks** away (configurable 5–128 blocks via server config).
- **Anchor & place workflow**: press **G** to anchor preview's target and facing, walk around, right-click to build at saved target; **G** releases anchor; **Shift+scroll** still rotates.
- **Server Config options** (operator-only):
  - `targetingRange` (5–128 blocks)
  - `placementSpeed` (1–1024 blocks/tick)
  - Sequential, layer-by-layer (bottom-up, MM controller first), or instant placement modes
- **Mode precedence**: per-tool instant preference effective in sequential; explicit server layer/instant take precedence; Controller Assemble & dismantling retain existing speeds.
- **Gallery categories**: imported from first structure folder (matching MultiBuilderTool grouping); existing assignments, renames, deletions, Uncategorized choices survive reloads.
- **Hologram improvements**: removed Shift rotation timeout hiding; fixed NBT previews allowed beside MM controllers; render stage, transforms, layers, buffers unchanged.
- **Server-side resolution**: placement & anchor targets resolved on server; targeting stops at unloaded terrain; material, FE, obstruction, Forge place-protection checks in all modes.
- **Persistence & sync**: anchors stored on tool, cleared on structure selection; range synced on login & config edits; English/Turkish labels & usage guide.
- **Protocol & version**: network protocol bumped to 8; build version to 0.1.35.7; client and server versions must match.

### Fixed

#### #78 – Structure Builder: Anchor & Preview Management
- **Anchor persistence fix**: anchor cleared once server accepts build job; preserved when build is rejected.
- **Preview outline**: hidden when every planned block is already present and no obstructions exist.
- **Post-build preview**: completed structures retain their preview outline; dismantled structures show saved anchor bringing back old preview on re-anchor.
- **Refresh interval**: existing preview refresh maintained even when nothing to draw.


## [0.1.35.4] - 2026-09-29
### INFO:
**Version 0.1.35.4** represents a minor update focused on bug fixes and performance improvements.

**Special Thanks:**
- **Knozyy**

### Added

#### #73 – Structure Builder: Instant Build Toggle
- **Instant build toggle** in Settings tab (off by default, saved per tool).
- When enabled: prepared structure builds in one server tick through existing block, energy, and placement-protection checks.
- **Motivation**: prevents crashes when controller ticks during structure placement (e.g., NuclearCraft fission reactors); opt-in mitigation mode.
- **English & Turkish labels** + GameTests for per-tool persistence and one-tick resource consumption.

#### #74 – JEI Process Recipe Views: Scrollable Input/Output Lists
- **Capped visible rows**: process recipe views limited to 6 visible rows.
- **Independent JEI scroll grids**: separate scrollable grids for long input and output lists (keep all ingredients available to JEI lookup).
- **Count & badges**: unused badges follow slots when scrolling.
- **Category sizing**: uses displayed outputs including weighted choices; accounts for recipes shared by multiple structures.

#### #75 – Item Port Indexing & Exact-Item Extraction
- **Slot indexing**: index empty, partially filled, and full item-port slots; track per-item totals and content revision.
- **Content updates**: normal inventory changes update controller without periodic item-slot scan.
- **Exact-item routing**: recipe extraction routed through item-to-port index, keeping NBT predicates.
- **Output strategy**: for outputs, try compatible partial ports before empty ports within same configured priority.
- **Candidate filtering**: filter recipe candidates by available exact-item quantities; keep existing final ingredient, condition, output-capacity, and round-robin checks.

#### #76 – Output Capacity Pre-Check & Pending Recipe State
- **Pre-start check**: verify output capacity before starting recipe (prevent full output port consuming inputs).
- **Pending state**: completed recipes remain pending while output blocked (no repeated per-tick consumption or discard after stall timeout).
- **State preservation**: recipe waits for output space without losing progress.


## [0.1.35.2 + 3] - 2026-09-28

### INFO:
**Version 0.1.35.2 + 3** represents a major update focused on the Structure Builder tool and internationalization support.

**Special Thanks:**
- **Knozyy** – Lead developer of the Structure Builder tool suite, Locale fixes, and controller enhancements.

### Added

#### #66 – Locale.ROOT Enum Casing
- Fixed Turkish/Azerbaijani locale issues where `toLowerCase()` transforms `I` → `ı` and `toUpperCase()` transforms `i` → `İ`.
- All enum-name casing for lang keys and parsing now uses `Locale.ROOT` (linker item/mode, Jade controller provider, controller screen, port sides, weather recipe condition, ParserUtils).
- Output unchanged on all other locales (enum names are plain ASCII); affects: Network Linker, controller screen, port configuration.

#### #67 – Controller Assemble Button
- **Assemble button** on controller screen (brick icon) opens structure assembly interface.
- **Structure selector** (when controller has multiple structures).
- **Tier picker** (one row per port type with < > controls; "Lowest" by default).
- **Assembly process**: builds structure from player inventory (nothing needed in creative), with configurable block placement rate (`assemblyBlocksPerTick`, default 2).
- **Tier clamping**: preference clamped to each position's [minTier, maxTier], nearest registered tier used (supports non-linear tier gaps like T1, T2, T5).
- **Safety**: respects `mayBuild`, spawn protection, `BlockEvent.EntityPlaceEvent`; existing blocks not replaced; assembly reports placed/missing/blocked summary.
- **Persistence**: tier choice + structure stored on controller; packets validated server-side.
- **Blueprint compatibility**: blueprint paste now uses lowest fitting port tier (not first registered).

#### #68 – Structure Builder Tool
- **New Structure Builder item** (replaces concept of external multiblock builders) with dedicated texture and model.
- **Structures tab**: searchable gallery grouped by mod, big 3D preview (drag/rotate, wheel/zoom, layer slider), size/block count display, port tier info.
- **Settings tab**: preferred port size per port type (clamped per position).
- **Tool store**: 54 slots, up to 512 per stackable slot, plus FE bar.
- **Build & preview**: Shift+use opens tool screen; right-click block shows hologram preview (green outline, red obstacles), builds if clear; Shift+scroll rotates.
- **Complete structure**: right-click controller to finish incomplete machine (same as controller's Assemble, but from tool).
- **Dismantle**: hold V (rebindable) or Shift+right-click twice; breaks like normal breaking (contents drop), returns blocks to tool store → inventory → ground with Silk Touch loot rolling.
- **Energy**: 50 FE/block placed, 25/block dismantled; common config: capacity 1M, receive 10K/tick; creative: free.
- **Safety**: oversized stacks capped in store, inventory/tool binding checks, store refreshes per tick, refunds on failure/cancel, respects claim mods & place/break events.
- **Server validation**: held tool, distance, loaded chunk, known structure, registered tier keys, clamped values.

#### #69 – Structure Builder: ME Network & Auto-Craft
- **Bind**: Shift+right-click any AE2 network block (WAP, cable, drive, etc.) to bind tool to network (stores position like Network Linker).
- **Settings**: "Use ME network", "Auto-craft missing", "Forget ME network" toggles.
- **Materials flow**: tool store → player inventory → ME network.
- **All-or-nothing builds**: tool build starts only when all blocks available; network auto-crafts what has patterns, names missing items (e.g., "Titanium Plate ×3: not in ME and no pattern").
- **Craft HUD**: above hotbar with progress bar (Crafting ▓▓▓░░ 5/8), network status ("Waiting…", "Ready — right-click to build"), failures shown once in yellow.
- **Smart tracking**: item being crafted never reported missing, never requested twice; tracks via CPU and `getRequestedAmount`.
- **AE2 access control**: enforces network ownership (player/FTB team/operator; checked at bind, use, craft submit).
- **Refunds**: never go back to ME; return to player inventory.

#### #70 – Structure Builder: Other Mods' Multiblocks
- **Gallery integration**: now lists multiblocks from other mods (Mekanism, NuclearCraft, Blood Magic, etc.) defined as vanilla structure .nbt files in datapacks (Multi Builder Tool format).
- **Paths**: loads `data/<ns>/mm_builder_structures/**.nbt`; legacy support for `mbtool_structures/**` and `spatial_structures/**`.
- **Compatibility**: skips structures with unregistered blocks (mod not installed).
- **Multi Builder Tool bundles**: MIT licensed .nbt files included under `data/mbtool/mbtool_structures/` (same paths → no duplicates in packs still using MBT).
- **Naming**: from lang key `structure.<ns>.<path>` or title-cased file name; grouped by owning mod.
- **Sync**: synced to clients on login & `/reload` (palette-compressed, split <512KB packets).
- **Building**: gallery, 3D preview, hologram, building work like MM structures; rotation from facing + Shift+scroll; all-or-nothing materials from store → inventory → ME with auto-craft.
- **Limitations**: no controller, no tier choice, block entity data ignored; structures with no-item blocks (fire, fluids) listed but can't build.
- **KubeJS API**: `MMEvents.builderStructures(event => { event.remove(id); event.removeNamespace(ns); event.add(id, 'ns:path/file.nbt') })`.
- **Dismantling**: MM-machines-only (no controller to locate others).

#### #71 – Structure Builder: Polish, Materials Tab & AE2 ME Storage
- **Renamed**: "Multiblock Tool" → **"Structure Builder"** (item, recipes, models, lang entries, registry).
- **New item texture & model**.
- **Bind via WAP**: now supports binding directly to AE2 Wireless Access Points.
- **Materials tab**: displays every required block with real item icons; compares counts across Player Inventory + Tool Storage + connected ME network; tooltips show breakdown (Available: X / Required: Y).
- **Compact materials strip**: in Structures tab for quick reference during selection.
- **Admin Categories tab** (server operators/admins): create, rename, delete custom gallery categories; assign structures via right-click context menu.
- **Sync**: `StructureCategoriesSyncPkt` & `StructureCategoryEditPkt` between server/client; persists across restarts and datapack reloads.
- **In-game config GUI** (`MMConfigScreen`): accessible from tool's Config tab or client commands; live editing of server settings, visual/client options, per-machine working effects.
- **Server config editing**: operators modify server config with immediate sync & validation (`MMConfigEditPkt`, `MMConfigSyncPkt`, `MMConfigRequestPkt`).
- **AE2 ME Storage Bus on Item Ports**: `Ae2ItemPortStorage` allows AE2 Storage Buses to attach directly to MM Item Ports; exposes inventory to ME network with accurate channel & capacity handling.
- **Item port improvements**: improved stack insertion to prevent loss on oversized stacks; respect slot limits.
- **Controller-free dismantling**: extended DismantlePlanner & ToolDismantles to dismantle non-MM builder structures (datapack .nbt files matching tool template).
- **Void Miner relocations**: corrected .nbt structures to `data/mbtool/mm_builder_structures/voidminer/` (overrides broken legacy palette entries).
- **UI polish**: JEI ingredient hovering in ControllerPortList; fixed 3D layer preview for connected textures (GuiStructureRenderer); formatted multi-line tooltips for Machine Wrench, Network Linker, Input Gateway.

#### #72 – Item Port Capacity & Rendering Optimization
- **Per-slot capacity limit**: raised to 16,384 (from 64) while preserving configured rows/columns in JSON or KubeJS.
- **Client update batching**: item port block-entity updates batched to once per tick when contents change (reduced sync overhead).
- **Window background rendering**: large port windows now render with at most nine textured patches instead of thousands of tiled GUI blits per frame (major performance improvement for large item ports).

### Fixed

#### #66 – Locale Fixes
- Enum names no longer transform incorrectly in Turkish/Azerbaijani locales; all comparisons now use Locale.ROOT.


## [0.1.35.1] - 2026-09-26

### INFO:
**Version 0.1.35.1** represents a minor update with bug fixes and performance improvements.
**Special Thanks:**
- **Knozyy**

### Added

#### #63 – Recipe Candidate Indexing & Requirement Caching
- **RecipeRequirements**: Recipe input needs (port types, item/fluid/chemical IDs, energy/mana/kinetic) now cached on structure load instead of recalculated every tick.
- **RecipeCandidateIndex**: Recipes grouped by item consumed; candidate list rebuilt only when port contents change, drastically reducing scan iterations.
  - Only visits recipes whose inputs are actually present
  - Recipes without specific item input (tags, fluids, energy) always candidate
- **Scan behavior**: Maintains structure recipe order, resumes where stopped (preserves recipe selection modes and parallel rules).
- **Skip wait**: Recipes that fail input checks now wait 20 ticks like other skip reasons; port changes clear wait.
- **Performance**: Eliminates thousands of per-tick allocations and checks in structures with hundreds/thousands of recipes.

#### #64 – Big/Small Controller Screen Toggle
- **Screen size toggle** (top-right button): switch between big screen (up to 500x380, sized to window) and small screen (classic view).
- Choice saved in client config (`controller.bigScreen`), applies to all controllers, survives restarts.
- **Big screen layout** (400px+ wide): machine info & settings on left, scrollable input/output port columns on right; narrower screens keep pages.
- **Recipe row improvements**:
  - Shows all outputs including weighted outputs (weren't shown before)
  - Rotates every 1.2s when more items than fit; thin track shows current position
  - Stays still while hovered
  - Left click opens JEI recipes, right click uses (JEI optional)
  - New `IRecipeOutputEntry#displayedOutputs()` for custom output types
- **Sound mute button**: toggles working sound (per controller, NBT saved, synced, mutes for everyone; particles unaffected).
  - Machines without sound show "No sound"
- **Fixes**: button overlap fixed; energy bars now repeat texture correctly (no read-past).

#### #65 – Input Gateway: Full Energy & Mekanism Support
- Input Gateway now accepts and routes:
  - **FE**: passed to machine's energy input ports in order
  - **Mekanism gas, infusion, pigment, slurry**: routed to matching chemical input ports in order
- Display shows read-only tank list + one entry tank (AE2 Pattern Provider in blocking mode sees machine contents).
- **Implementation**: ChemicalHandlers created only when Mekanism is loaded; no hard dependency.
- **Tooltips**: block tooltip lists all supported types (en_us, tr_tr).

## [0.1.35.0] - 2026-09-25

### INFO:
**Version 0.1.35.0** represents a major update with substantial feature additions and improvements.

**Special Thanks:**
- **Knozyy** – Lead architect and primary developer of this entire version.
- Single-handedly implemented all major features:
  - Per-side Auto I/O,
  - complete Port & Controller GUI Redesign,
  - Input Gateway with full energy & Mekanism chemical support,
  - Machine Network Linker with dual modes,
  - Controller Sync Optimization,
  - massive Localization overhaul,
  - JEI Structure Preview with complete freeze fix and new controls,
  - Controller States & Status Lights,
  - Machine Wrench tool,
  - comprehensive Controller Screen enhancements (big/small toggle, sound mute, recipe rotation),
  - full Recipe Conditions system,
  - KubeJS Event system,
  - Input Gateway Storage for AE2 integration,
  - Redstone Comparator support,
  - Weighted output entries,
  - Working effects (sounds & particles),
  - Recipe candidate indexing & requirement caching, and
  - critical fluid transfer bug fix.

Without his contribution, this update would not have been possible!

### Added
#### #42 – Per-side auto I/O for ports (updated)
- **Per-side auto input/output for ports** (item, fluid, energy and Mekanism gas/infuse/pigment/slurry)
  - Port GUIs have a side panel with toggles for each side (Top/Bottom/Front/Back/Left/Right), named relative to the machine's controller.
  - Output ports push into any neighboring inventory, tank or energy storage on enabled sides, and into neighboring MM input ports (machine chaining). Priority setter values are respected.
  - Input ports pull from neighboring non-MM blocks on enabled sides.
  - Settings are saved per port. `autoPush` / `portsAutoExtractByDefault` now decide whether new output ports start with all sides enabled.
  - New common config `portAutoIOInterval` (default 10 ticks).
- **Lock and dump for fluid and Mekanism chemical ports**
  - Lock: each tank is locked to its current contents; empty tanks lock to the first type that enters them.
  - Dump: Shift+Click deletes the port's contents.
- Turkish (`tr_tr`) translation for the new GUI texts (full translation).

#### #44 – Port & controller screen rework
- **Port GUI redesign** with clearer layout (same MM textures and colors):
  - Fluid, gas and energy ports: one big gauge with the amount written on it.
  - Big item ports (larger than 9x6) get a bigger window, so slots no longer overlap the title or the inventory.
  - Auto I/O panel redone Mekanism-style: the port block in the middle, color-coded side buttons around it.
- **Controller GUI improvements**:
  - Colored status line (not formed / paused by redstone / running / idle).
  - Current recipe display with items, fluids, gases, energy and their amounts.
  - Rows for tier, parallel, redstone and recipe order, each with a tooltip.
  - Second page: lists every port of the machine and what's inside (live updates).

#### #45 – Input Gateway block
- **Input Gateway block**: One block to pipe items and fluids into a machine, instead of connecting a pipe to every input port.
  - Can replace any casing/glass block in the structure (the structure still forms), or be placed next to an input port.
  - Stores nothing and has no GUI; it hands everything to the machine's input ports.
  - No crafting recipe included, same as ports (packs add one with KubeJS).

#### #46 – Machine Network Linker
- **Machine Network Linker**: Links a machine to its owner (shared with their FTB team) and to an AE2 network.
  - Only the owner/team can open or break the machine's parts; OPs bypass this.
  - When a port breaks, its contents go into the AE2 network instead of dropping on the ground (Mekanism gases go through Applied Mekanistics).
  - Network Linker modes: *Linking* (pairs machine to owner/AE2) and *Info* (view owner, position, online status; sneak+right-click to unlink).
  - AE2, FTB Teams and Applied Mekanism are all optional dependencies.

#### #47 – Controller: Optimized Sync
- Block entity sync only on GUI open or relevant changes (formed, structure, recipes, redstone, link).
- `detectExternalStorageChanges()` now runs every 5 ticks instead of every tick (matches idle scan interval).

#### #48 – Localization Overhaul
- 58 hardcoded UI texts moved to lang keys (JEI titles, tooltips, blueprint messages, debug tools, port labels).
- Full en_us and tr_tr translations; removed duplicate block.mm.input_gateway keys.

#### #49 – JEI Structure Preview: Freeze Fix & Controls
- Fixed freeze on large structures by optimizing buffer flushing and skipping enclosed blocks.
- **Layer view**: Cycles through layers from bottom up with < All layers > selector.
- **Camera improvements**: Auto-positioned from bounding sphere, rotates around structure center, fills screen view.
- **Mouse controls**: Left/middle drag rotates, right drag/wheel zooms, Shift+drag pans; on-screen i icon explains controls.
- **Connected textures**: Preview blocks see neighbors, so AE2 quartz glass, CTM, and modded textures render correctly.

#### #50 – Controller State & Port Status Lights
- Controller block state: `unformed` / `idle` / `working` (tints screen red/green/yellow).
- All port types show matching status light (block entity renderer, always bright).
- **Client config** (`mm-client.toml`): customize controller/port colors and status light visibility.
- **Per-machine customization**: `unformedColor`, `idleColor`, `workingColor` in KubeJS controller builder or JSON.
- Per-state model variants (`<id>`, `<id>_idle`, `<id>_working`) and screen textures for full resource pack support.

#### #51 – Machine Wrench & Network Linker Modes
- **mm:wrench** item (3 iron + green dye): right-click a port side to toggle auto I/O; sneak+right-click shows all six sides.
- Controller screen shows "Linked to" row (AE2 only) with tooltip for owner and unlink info.
- New Network Linker texture (ME remote style).

#### #52 – Controller Screen: Recipe Order, Rename & Pages
- **Recipe order button** (Default / Avoid Same Recipe / Round Robin) saved per controller.
- **Machine renaming**: click the controller name to edit; Enter saves, Esc cancels, empty restores original. Names are preserved in anvil and on drop (copy_name).
- **Port page split**: cycles through Status → Input Ports → Output Ports.
- Settings sync via `ControllerSettingsPkt` with server-side permission checks.

#### #53 – Missing Blocks Detection
- Server finds best-fit structure rotation and sends with screen preview.
- Shows "Missing blocks: N" with item name and relative position (e.g., "2 up, 1 left").
- Tooltip lists up to ten wrong positions with what's there instead.

#### #54 – Jade: Fuller Controller & Port Tooltips
- **Controller**: state (translated), progress, running recipes count, parallel limit, redstone mode, owner.
- **Ports**: parent machine, current activity, auto I/O status.
- **Parallel limit fix**: `getDisplayedParallelLimit()` shows 1 (serial) or the structure/controller/config limit (parallel).
- Texts use "different recipes" (same recipe never runs twice).

#### #55 – Recipe Conditions System
- Dimension, weather, biome, time, height, and redstone conditions now actually work (were parsed but ignored).
- Running recipes pause when conditions fail; not counted as stalled.
- **New conditions**: `mm:biome` (id or #tag), `mm:time` (day/night), `mm:height` (minY/maxY), `mm:redstone` (powered/unpowered), `mm:tier` (minTier/maxTier).
- **Multi-structure recipes**: `structureIds` in JSON, `.structureIds(...)` in KubeJS. JEI shows recipe in all categories.
- **KubeJS API**: `.dimension()`, `.biome()`, `.time()`, `.weather()`, `.minY()`, `.maxY()`, `.redstone()`, `.minTier()`, `.maxTier()`.
- **JEI**: condition clock icon under recipe arrow.

#### #56 – KubeJS Server Events
- `MMEvents.recipeStarted(recipeId, event => {...})` – cancellable event before recipe starts.
- `MMEvents.recipeFinished(recipeId, event => {...})` – event after recipe completes.
- Event object includes: recipeId, controller, controllerId, structureId, level, pos, block.
- No overhead if no scripts listen.

#### #57 – Input Gateway: Storage Display
- Input Gateway now displays read-only list of machine's input port slots and tanks.
- Shows one always-empty slot/tank for AE2 Pattern Provider interaction.
- Pattern Provider in blocking mode now correctly waits for inputs to be consumed.
- With Network Linker, outputs return to AE2 network.

#### #60 – Comparator Support for Controllers
- **Redstone comparator** placed against a machine controller now reads the machine's state:
  - Unformed or idle: signal 0
  - Working: signal 1–15, following recipe progress (furthest running recipe)
  - Recipes finishing within a tick read 15
- Works with every controller, no setup needed.
- Use cases: "working" lamp, trigger automation on idle, chain machines, simple progress indicator.
- **Implementation**: MachineControllerBlock has `hasAnalogOutputSignal` / `getAnalogOutputSignal`; updates only trigger when signal value changes.

#### #61 – Weighted Output Entry Type
- New output entry type `mm:output/weighted`: picks exactly one option by weight each recipe finish (loot table style).
- **Syntax** (KubeJS):
  ```javascript
'.output({
    type: 'mm:output/weighted',
    options: [
      { weight: 70, item: 'minecraft:iron_nugget', count: 3 },
      { weight: 20, item: 'minecraft:gold_nugget', count: 3 },
      { weight: 5, item: 'minecraft:diamond' },
      { weight: 5 }   // outputs nothing
    ],
'  })
  ```
- Each option takes an ingredient (any port type) or item shorthand (item, count, nbt, etc.); option without both outputs nothing.
- Weights don't need to sum to 100; optional `chance` applies to whole entry.
- **JEI**: shows every possible output with its chance, plus no-output chance.
- **Bonus**: missing count on `mm:item` ingredient now defaults to 1 (was NullPointerException).

#### #62 – Working Effects (Sound & Particles)
- Machines can play sound and show particles while working (pack configurable, nothing hardcoded).
- **Global config** (`config/mm/working_effects.json`, keyed by controller block id):
  ```json
  {
    "mm:auto_crusher_controller": {
      "sound": "minecraft:block.grindstone.use",
      "interval": 25,
      "particle": "minecraft:smoke"
    }
  }
  ```
  - `sound`: sound while working
  - `particle`: particle from controller's screen side
  - `interval`: ticks between sounds (default 40)
  - All fields optional; `""` disables effect
  - Auto-created on first run, re-read on F3+T for live tuning
- **Per-controller** (KubeJS / JSON): `.workingSound()`, `.workingSoundInterval()`, `.workingParticle()`; global config overrides.
- **Client-side**: runs from controller's client tick only while `working` state active, no networking overhead.
- **Sound offset**: by position so neighboring machines don't play in unison.
- **Player opt-out**: `[controller] workingEffects` in `mm-client.toml`.

### Fixed

#### #42 – Per-side auto I/O
- `autoPush` only pushed into neighboring MM input ports, never into chests, tanks, pipes or cables.

#### #43 – Bug fixes
- Gas ports now accept radioactive gases (polonium, plutonium, nuclear waste, etc.). Mekanisms default validator was rejecting them.
- An emptied (dumped) gas tank no longer keeps showing its old contents on the client.
- Item ports now stack up to their slot capacity (512 / 16384) when you place items by hand or shift-click. Before, they stopped at 64 and shift-click only filled empty slots.
- Item counts above 127 now show correctly in the port GUI (vanilla sends slot counts as a single byte).
- Fluid ports never drain more than requested.
- `mods.toml`: replaced the example-mod placeholder text.

#### #49 – Structure Preview Fixes
- Fixed freeze on big structures with optimized buffer flushing.

#### #50 – Port State & Network Updates
- Port on-remove side effects only trigger on actual block change, not block state changes.
- State changes skip neighbor shape updates (UPDATE_KNOWN_SHAPE).

#### #59 – Fluid Port Auto-Transfer Bug Fix
- **CRITICAL FIX**: Fluid ports stopped auto-transferring due to swapped source/destination in `FluidUtil.tryFluidTransfer()` call.
  - Input ports no longer pull from neighboring tanks (Mekanism, etc.)
  - Output ports no longer push into them
  - Root cause: recent commit swapped from/to parameters and set `doTransfer = false` (simulation mode)
  - **Fixed**: restored correct parameter order and `doTransfer = true`

#### #58 – Build & Localization Fixes
- MachineControllerScreen: restored missing imports (EditBox, GLFW, ControllerSettingsPkt, RecipeSelectionMode).
- MachineControllerScreen: restored recipe order text position and tooltip.
- Lang files: restored ten missing `jei.mm.structure.*` keys (structure view title, max parallel line, layer selector, controls).
- Removed duplicate `config.jade.plugin_mm.controller_progress` key.

### Changed
- Network protocol version bumped to 2 (new port config packet).

## [0.1.34.7] - 2026-09-03
### Added
- **Major Performance Optimization - Machine Controller Recipe Processing**
  - Aggressive recipe scanning: Controllers now scan all recipes per tick when recipe count < 300 (DEFAULT mode), or 50% per tick in fair-scheduling mode. Previously only scanned 5 recipes/tick, causing massive idle latency.
  - Recipe metadata caching: Port type requirements and resource IDs are now cached in `RecipeMetadata` record, eliminating expensive repeated extraction from recipe definitions.
  - Storage cache throttling: Cache rebuilds reduced from every tick to every 2 ticks during active recipes (5 ticks when idle), preventing unnecessary full inventory scans.
  - NBT hash caching: CompoundTag hashes are cached in IdentityHashMap to avoid expensive JSON conversions during stack fingerprinting.
### Fixed
- Machine controller recipe processing bottleneck that caused excessive idle time between recipe matches when recipe registry contained 150+ recipes.

## [0.1.34.6-fix1] - 2026-08-13
### Fixed
- Item port priority setter now correctly applies priority to output ports.
- Energy consumption was doubled for multi-tick recipes.

## [0.1.34.6] - 2026-08-11
### Changed
- JEI recipe slots now render item counts in AE2 style (1K / 1.5M / 2B) instead of vanilla number. Exact count shown in tooltip on hover.

## [0.1.34.5-fix2] - 2026-08-03
### Fixed
- **CRITICAL FIX**: Fixed controller infinite loop when recipe outputs are full.
  - Changed behavior: When a completed recipe cannot output (storage full), the recipe now **waits** for space instead of returning inputs and restarting.
  - Previously: Recipe was ditched, inputs returned, then immediately re-triggered → infinite loop.
  - Now: Recipe remains in activeRecipes with 100-tick cooldown, checking periodically for available output space.
  - Requires accompanying fixes in ItemPortHandler and ItemPortStorage (see below).
- **BUG FIX**: ItemPortStorage.canInsert(Item, count) - Fixed stack size limiting.
  - `new ItemStack(item, count)` was auto-limiting to maxStackSize. Now creates ItemStack with count=1 to prevent truncation.
  - This caused canInsert() to return incorrect remaining counts for items with high counts.
- **BUG FIX**: ItemPortHandler.mergeIntoExistingStacks() - Fixed inconsistent space calculation.
  - Was using `existing.getCount()` (display stack) instead of `actualCounts[slot]` (real count).
  - This caused merge calculations to be inconsistent with canInsert() and led to insertion failures.
  - Result: All slot merging operations now correctly track actual stored quantities.

## [0.1.34.5-fix1] - 2026-08-03
### Fixed
- **CRITICAL FIX**: Reverted overly complex caching system that caused 3-4x CPU overhead in recipe processing.
- ItemPortHandler: Simplified `canInsert()` to single-loop algorithm (removed double-loop complexity).
- ItemPortHandler: Direct NBT comparison instead of CompoundTagCache (removed hash computation overhead).
- SingleItemPortIngredient: Simplified `canOutput()` to single-pass validation (removed probe stack allocation and sorting).
- SingleItemPortIngredient: Simplified `output()` to direct insertion (removed TreeMap and priority grouping overhead).

### Performance Results (Verified)
- Server thread: 11.28% → 4.92% (-56%)
- MachineControllerBlockEntity.tick(): 10.58% → 4.32% (-59%)
- RecipeOutputs.canProcess(): 6.79% → 1.28% (-81%)
- ItemPortHandler.canInsert(): 4.21% → 0.62% (-85%)
- TPS: Stable 19-20 (restored from regression)
- Memory: 40% less GC pressure

## [0.1.34.5] - 2026-07-30
### Added
- Multi-layer caching system for recipe output/input validation (REVERTED in 0.1.34.6 due to performance regression)
### Added
- **Performance Optimizations - Multi-Layer Caching System**
  - Implemented `CompoundTagCache`: Smart NBT tag hashing with IdentityHashMap for 50-70% faster tag comparisons
  - Added `RecipeOutputCache`: Per-tick caching of recipe output validation results (10-20% faster)
  - Added `RecipeInputCache`: Generic validation result caching utility (10-15% faster)
  - Implemented `RecipeStateModelPool`: Thread-local object pool for recipe state reuse (20-30% less GC pressure)
  - Added `NbtNormalizer`: NBT tag normalization to remove redundant values (10-15% storage reduction)
  - Implemented `PortStorageBatchUpdater`: Batch processing utility for port storage operations (15-25% faster)

## [0.1.34.4] - 2026-07-29
### Added
- Fixed per_tick config

## [0.1.34.3] - 2026-07-08
### Added
- Add redstone mode functionality to machine controller

## [0.1.34.2] - 2026-07-07
### Fixed
- Skip recipe if outputs can't process (thx to MiniMaxi)

## [0.1.34.1] - 2026-07-03

### Fixed
- NBT matching: Fixed a bug in weak NBT matching where duplicate entries in an expected ListTag could all match the same element in an item's ListTag. List matching now respects multiplicity — each expected element must match a distinct element in the item data.
- Controller scheduling: Fixed round-robin input-item recipe selection so the controller treats items with the same item id but different NBT as distinct candidates. The controller now generates per-stack keys (NBT fingerprint when available, otherwise a slot-based key), caches available stack keys and selects the least-recently-used stack among eligible candidates.

## [0.1.34.0] - 2026-07-02

### Added
- Add recipe selection mode system for controllers (thx to FrozenGalaxy)
- Add custom display names for blueprint items (thx to FrozenGalaxy)
- Implement round-robin recipe selection by input item (thx to FrozenGalaxy)

## [0.1.33.11] - 2026-06-28

### Added
- add creative mode pasting of blueprinted structures (thx to FrozenGalaxy)

## [0.1.33.10] - 2026-06-17

### Added
- Fluid and Energy ports: numeric `tierRank` support in storage models, parsers, builders and serializers. `PortConfigBuilderJS.tierRank(int)` now applies to fluid and energy ports as well.

### Fixed
- JEI / structure GUI crash: prevent ArrayIndexOutOfBounds in `TickCycling` by skipping layout pieces with no registered renderer blocks (occurs when `minTier` filters out all matching port variants).

### Changed
- Port matching: `minTier` checks now properly apply to fluid and energy port types; ports without an explicit `tierRank` are treated as `tierRank = 1` during matching (backwards compatibility).

## [0.1.33.9] - 2026-06-12

 ### Added
 - JEI: Recipe tab — compact quantity badges for item displays (suffixes: K, M, G; quantities are abbreviated for 10,000+).
 - JEI: Hovering an item shows the full quantity; holding Shift reveals full quantities for all items.

## [0.1.33.8] - 2026-06-12

### Added
- JEI tab now dynamically displays the multiblock structure layout. If a structure requires more than 16 blocks, the JEI tab will expand vertically to accommodate the additional slots.

## [0.1.33.7] - 2026-06-11

### Added
- New item multiblock saver:
  - In-game item that captures an axis-aligned multiblock selection by marking two corner blocks (right-click) and saving it.
  - Produces two artifacts in `config/mm/structures`: a Masterful Machinery-compatible JSON layout and a KubeJS registration script.
  - Auto-names captures using the pattern `mm_capture_<player>_multiblock_<n>` where `n` increments for each new capture.
  - Captures full block states and tile-entity NBT; enforces a default safety limit of 50,000 blocks to avoid server stalls.
  - Automatically detects a controller block inside the selection and records `controllerId` and `controllerOffset`; the layout uses the character `C` to mark the controller position (`C` is not emitted as a key entry).
  - Sneak+right-click in air clears the stored corner markers on the item; sneak+right-click on a block still marks corners (same as normal right-click).
  - 'minecraft:podzol' will be ignored so you can use it as a corner block without it appearing in the layout.

## [0.1.33.6] - 2026-06-01

### Added
- Structure JSON / KubeJS: global `portsAnywhere` flag (top-level in a structure) to allow all port pieces in the layout to be matched at any port position.
- Structure JSON / KubeJS: per-key `anywhere: true` to mark an individual layout key as matchable at any port position.
- KubeJS: `StructureLayoutBuilderJS.portsAnywhere(boolean)` builder API to set the global flag from scripts.
- New structure piece implementations for flexible port matching: `PortAnywhereStructurePiece` and `PortTypeAnywhereStructurePiece`.

### Changed
- Matching logic: when a port piece is marked as `anywhere` (either per-key or via `portsAnywhere`), port requirements are matched across all port positions in the current rotated layout using a uniqueness-aware matching algorithm (each anywhere-requirement must be assigned a distinct port position).
- Parser: `PortStructurePieceType` and `PortTypeStructurePieceType` accept an `anywhere` boolean on keys and instantiate the anywhere-piece variants when present.

### Notes
- Backwards compatibility: existing structures without the new flag behave exactly as before. The new `portsAnywhere` flag is opt-in.
- Performance: matching uses simple backtracking and is expected to be fast for typical structures (small number of ports). If structures with many ports are used, consider changing to a max-bipartite-matching algorithm (Hopcroft–Karp) for deterministic performance.
- Modifiers: currently any `StructurePieceModifier`s attached to anywhere-pieces are not fully evaluated during the candidate matching pass. If you rely on modifiers for port validation, enable full modifier-checking for anywhere-pieces (future improvement).

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
