# Structure Builder placement

Select a structure, aim at a block, and right-click to build. The tool targets loaded blocks up to 64 blocks away by default. Other items keep their normal interaction range.

Press **G** to anchor the preview's target and facing. Walk around or look away without moving the preview, then right-click to build at the anchor. Press G again to release it. The anchor key can be rebound in Minecraft's controls. Shift+scroll still rotates the structure around its placement anchor.

The anchor is saved on the tool. Selecting a structure clears the previous anchor. An anchor can only be used in its saved dimension and within the configured build range.

## Server settings

Open the Structure Builder's **Config** tab and select **Server**, or use the mod's Config button in the Mods menu. Editing server settings requires operator level 2. Changes are saved to the common config and sent to connected clients; targeting range is also sent when a player joins.

| Setting | Default | Allowed values |
| --- | --- | --- |
| `tool.buildRange` | 64 blocks | 5–128 blocks |
| `tool.buildBlocksPerTick` | 2 | 1–1024 |
| `tool.buildMode` | `SEQUENTIAL` | `SEQUENTIAL`, `LAYER_BY_LAYER`, `INSTANT` |

- **Sequential:** follows the structure's existing placement order. The existing per-tool Instant build preference remains available in this mode.
- **Layer by layer:** builds from the lowest layer upwards, completing at most one layer per tick. MM structures place their controller first. This server mode takes precedence over a tool's Instant build preference.
- **Instant:** processes the build in one server tick, regardless of the tool's Instant build preference.

Placement mode is chosen when a job starts. Speed changes apply to running tool jobs. Controller Assemble and dismantling keep their existing speed settings.

All modes retain material and energy costs, obstruction checks, and Forge placement protection events. The tool does not load distant chunks; targets and planned blocks must be inside loaded terrain and the world border. A running build stops when its player leaves the dimension or moves beyond the configured distance from the original target.

The server resolves both placement and anchor targets itself. Client packets carry only the selected hand. Network protocol 8 requires matching client and server versions.

## Structure categories

Imported builder structures use their first folder as the default gallery category, matching MultiBuilderTool's grouping. MM structures default to Custom Multiblocks. Explicit assignments, category renames/deletions, and choosing Uncategorized survive reloads; importing newly discovered structures does not undo these edits.
