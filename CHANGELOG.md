# Changelog

All notable changes to this project will be documented in this file.

The format is based on "Keep a Changelog" and this project follows [Semantic Versioning](https://semver.org/).

## [1.21.1-0.15.0] - 2026-10-17

### Added
- **Controllers can look different while working.** Add `idleModel` and `workingModel`, or
  `idleOverlay` and `workingOverlay`, to a controller. Without them, every state uses `model` or
  `overlay`. From KubeJS, `.idleModel(...)`, `.workingModel(...)`, `.idleOverlay(...)` and
  `.workingOverlay(...)`. This works for single-block machines too.
- **Oritech support.** Machines can now contain an Oritech black hole: a black hole that is part of
  a formed machine no longer pulls in blocks, and black holes nearby no longer pull blocks out of
  formed machines.

## [1.21.1-0.14.0] - 2026-10-14

### Added
- **The controller shows the selected recipe's cost in Manual mode.** On the big controller screen,
  picking a recipe shows its inputs and outputs under the settings. Hover them for details, or click
  to open them in JEI.
- **Fill bars for more ports in the controller.** Source, Mekanism heat, PneumaticCraft air,
  Replication matter, nuclear radiation and entity ports now show a bar instead of plain text. Hover
  the bar for temperature, pressure, isotopes or mob types.
- **Recipe icons for every port type in the controller.** Source, EMC, mana, rotation, heat, air,
  radiation, matter and entity costs now show up in the recipe preview and the running recipe, not
  just items, fluids, energy and chemicals.
- **Jade fill bars use the port's colour.** Source ports show purple and EMC ports show gold.

### Fixed
- **The Structure Builder** no longer crashes when previewing Extreme Reactors turbines.
- **Void miner tiers** now show up in the Structure Builder.

## [1.21.1-0.13.0] - 2026-10-12

### Added
- **Animated GeckoLib models** for machines (needs GeckoLib). Add a `geoModel` object to a
  controller or structure with `model`, `texture` and optionally `animations`, plus `idle` and
  `working` animation names. `offset`, `scale`, `rotate` and `glow` place and light it. A structure's
  `geoModel` wins over the controller's; `"geoModel": "none"` turns it off for that structure. From
  KubeJS, `.geoModel({...})` on controllers and structures.
- **`components` on item ingredients** match items by their data components, like enchantments or a
  custom name, and give outputs those components.
- **Output priority for fluid, energy, chemical and heat ports.** The Priority Setter now works on
  them like it does on item ports: higher-priority ports fill first.
- **The missing blocks tooltip** lists every port tier that fits a spot, not just one.

### Fixed
- **Machines no longer lose outputs** when a port's chunk unloads and loads again.
- **Item ports keep enchanted items** when saving, and no longer stack items with different
  components together.
- **Structure previews** no longer crash on blocks whose models need a world.
- **Source, heat and EMC port screens** show their bar in the right color (purple, orange and gold)
  instead of the red energy bar. The source JEI icon is purple too.

## [1.21.1-0.12.0] - 2026-10-12

### Added
- **Fluid volumes** show a port's fluid inside the machine, filling up as the port fills. Add a
  `visuals` list to a structure with `{ "type": "fluid_volume", "from": [x, y, z], "to": [x, y, z] }`,
  or `"area": "K"` to fill every spot of layout key `K`. `"port": "K"` picks which fluid port it
  shows; without it, any fluid port in the machine. From KubeJS, `.fluidVolume(from, to, port)` and
  `.fluidArea(key, port)`.
- **PneumaticCraft air ports show their pressure** on the controller screen.

### Fixed
- **`/reload` now updates placed controllers.** New or changed processes used to stay unused until
  the controller was broken and placed again.
- **The debug tool** no longer fails in singleplayer with `duplicate entry`.

## [1.21.1-0.11.0] - 2026-10-09

### Added
- **Single-block machines** (`"type": "mm:single"`). A whole machine in one block, like a furnace,
  with no structure or ports. List what it holds under `slots`: item, fluid, energy and Mekanism
  chemical slots, each with the same options as a port of that type. Recipes use the controller id
  as their `structureId`. See [`single-block.md`](docs/single-block.md).
  - Its screen is built from the slots: energy on the outside, then tanks, then item slots around
    the progress bar. A button opens the controller screen for its name, redstone mode and the rest.
  - Pipes, hoppers and cables fill its inputs and take from its outputs, from any side.
  - From KubeJS, `.type('mm:single')` and `.slot(id, type, input, config)`.
  - Each fluid stays in one tank of a fluid slot, so one fluid can not fill them all.
- **`fluids` on fluid ports and slots** limits them to certain fluids, as ids or tags
  (`"fluids": ["minecraft:lava", "#c:oil"]`). From KubeJS, `.fluids(...)`.
- **EMC ports show a bar** on the controller screen, like energy.

### Fixed
- **A typo in a KubeJS port or controller script** no longer crashes the game with
  `Unknown port type 'null'`. The KubeJS error that names the file and line is shown instead.
- **Shift-clicking into a port** no longer puts items into slots that do not take them.

## [1.21.1-0.10.0] - 2026-10-07

### Added
- **ProjectE EMC ports** (`mm:projecte/emc`, needs ProjectE). Set `capacity` in the port config.
  - Recipes consume or produce EMC with `{ "type": "mm:projecte/emc", "emc": 2048 }`. A `min`/`max`
    range works too, and `per_tick` spreads the cost over the recipe like energy.
  - Relays and collectors next to an input port fill it. An output port sends its EMC into the blocks
    around it, such as relays.
  - `kleinSlot: true` gives the port a Klein Star slot: the input port empties the star into the
    machine, the output port charges it. `kleinRate` limits how much EMC moves per tick.
  - EMC shows in JEI and Jade, and KubeJS ports use `capacity()`, `kleinSlot()` and `kleinRate()`.

### Fixed
- **`tierRank` was ignored on most port types**, including Mekanism chemical, PneumaticCraft air,
  Create kinetic, Ars Source and Nuclear Radiation ports. Structures with `minTier`/`maxTier` on
  those ports never formed. It now works on every port type, from KubeJS and from `config` in JSON.

## [1.21.1-0.9.0] - 2026-10-01

Most of this release is ported from
[BOLTMAGIC's Masterful Machinery for 1.20.1](https://github.com/BOLTMAGIC/MasterfulMachinery),
versions 0.1.35 to 0.1.35.4. Thanks to **BOLTMAGIC** for maintaining it and to **Knozyy** for
building most of these features: the controller and port screens, Assemble and the Structure Builder,
the Network Linker, Machine Wrench and Input Gateway, weighted outputs, the new conditions, working
sounds and particles, comparator output, the KubeJS recipe events, the Jade and JEI improvements, the
in-game config and the faster item ports.

### Added
- **A new controller screen.** Rename the machine, see its status (Running, Idle, Stuck, Paused or
  Not formed) and what is missing when it is not built, switch redstone and recipe mode, mute its
  sound, and page through every input and output port with their contents. It opens large or small;
  the button in the corner switches it.
- **Assemble.** A button in the controller screen places the machine's missing blocks from your
  inventory, with a choice of tier for each part.
- **Structure Builder.** A powered tool that builds a whole structure in one go and takes machines
  apart again. Pick a structure in its screen, right-click to build, shift+right-click to dismantle.
  - Takes blocks from its own store, your inventory, and an ME network it is bound to, and can order
    missing blocks from AE2.
  - A Materials tab lists what the structure needs against what you have.
  - **Instant build** in its Settings tab places everything in one tick.
  - Builds structure files from datapacks (`mm_builder_structures/`) as well as MM machines, and
    `MMEvents.builderStructures` adds or removes them from KubeJS.
  - Operators can sort the structure list into categories from the Admin tab.
- **Network Linker** (with AE2). Links a machine to an ME network without cables: outputs go straight
  into ME as soon as a recipe finishes, and broken ports empty into ME. Use it with a normal Pattern
  Provider and encoded patterns to autocraft with MM machines. See [`ae2.md`](docs/ae2.md).
- **Linked machines belong to their network's owner.** Only the owner, their FTB Teams team and
  operators can open or break them.
- **Port side panel.** Each port's screen sets auto input/output per side, locks fluid and chemical
  tanks to what is in them, and empties the port with Shift+click on Dump.
- **Machine Wrench.** Right-click a port's side to toggle its auto input/output.
- **Input Gateway.** Build it into a machine, or place it next to an input port, and pipe items,
  fluids, energy and Mekanism chemicals into it.
- **Weighted outputs.** `mm:output/weighted` gives one of several outputs each craft, by weight.
- **More recipe conditions:** `mm:biome`, `mm:time` (day or night), `mm:height`, `mm:redstone` and
  `mm:tier`. Structures can set a `tier`, or it is read from their name. All conditions now work from
  KubeJS too, and JEI lists them.
- **`requestOnly` recipes** only run when ordered from the Replication Terminal or AE2, or picked in
  manual mode.
- **One recipe for several structures** with `structureIds`.
- **Working sounds and particles.** Controllers can set `workingSound`, `workingSoundInterval` and
  `workingParticle`. Players can mute a machine, turn them all off in the client config, or change
  them per machine in `config/mm/working_effects.json`.
- **Screen colours per controller** with `unformedColor`, `idleColor` and `workingColor`, and a
  status light on ports in the same colours.
- **Comparator output.** A comparator next to a working controller gives 1–15 by recipe progress.
- **KubeJS recipe events.** `MMEvents.recipeStarted` (can be cancelled) and
  `MMEvents.recipeFinished`.
- **In-game config screen**, from the Mods menu or the Structure Builder. Operators can change server
  settings while playing.
- **ME connector patterns can carry more types** with the AE2 addons installed: energy (Applied
  Flux), chemicals (Applied Mekanistics), Source (Ars Énergistique), air (Applied Pneumatics) and
  matter (Replication AE2 Bridge). `exclude` leaves a type out.
- **Item ports work as ME storage**, so a Pattern Provider fills them in whole batches and a Storage
  Bus can read them.
- **Jade** shows a controller's progress, running recipes, redstone mode and owner, and on ports their
  machine and auto I/O.
- **JEI structure preview:** view one layer at a time, connected textures join up, and a blue "i"
  shows the controls. Big recipes scroll instead of running off the screen.
- **JEI lookups** from the controller screen and the Structure Builder's materials.
- **Blueprint paste preview** shows the structure as see-through blocks, with blocked spots in red,
  like the Structure Builder.

### Changed
- **Autocrafting with AE2:** the Network Linker with a Pattern Provider is now the normal way. The
  ME Pattern Connector still works, for packs that want every recipe offered without encoding.
- **Structure preview controls:** drag rotates, the scroll wheel or right-drag zooms, shift+drag
  moves.
- **Ports with more slots than fit on screen get pages** instead of drawing over the inventory.
- **Outputs fill a port that already holds the item** before starting on an empty slot elsewhere.
- **Item ports take items faster**, which helps machines with large ports.

### Removed
- **The built-in Pulverizer machine.** Packs that used it need to define their own.
- **The `asyncValidation` config option.** Structures are now always checked on the main thread.

### Fixed
- **Ordering from the Replication Terminal made items forever** when the machine was also connected
  to AE2. It now makes exactly what was ordered.
- **Cancelled Terminal orders** no longer keep the machine working on them.
- **Machines losing crafts.** A finished recipe whose outputs do not fit now waits until they do.
- **Crashes after making a port smaller** in its config, with items still inside.
- **Chemicals in JEI** now show in their slot, and unknown chemical ids are named in the log.
- **Replication matter in JEI** shows its type icon. Light matter like metallic looked like an empty
  slot.

## [1.21.1-0.8.1] - 2026-09-21

### Changed
- **The ME connector and the Replication link are one block each.** Both used to make an "Input"
  and an "Output" block that did exactly the same thing, because neither has sides. Each config
  file now makes a single block, `mm:<id>`, and the name is used as written with no "Input" or
  "Output" added. The AE2 one is called **ME Pattern Connector**.
  - A structure that names the port the usual way, `{ "port": "mm:my_connector" }`, needs no change.
  - `"input"` is now ignored for these two, so an existing `"input": true` keeps working.
  - A structure that names the block directly, `{ "block": "mm:my_connector_output" }`, must drop
    the `_output`.
  - Any of these two blocks already placed in a world need placing again.

### Added
- Docs for everything added in 0.8.0: [`ars-nouveau.md`](docs/ars-nouveau.md) for the Source port,
  [`ae2.md`](docs/ae2.md) for autocrafting, ordering from the Terminal in
  [`replication.md`](docs/replication.md), and the new types, `connectGroup` and three more
  troubleshooting entries in [`reference.md`](docs/reference.md).

## [1.21.1-0.8.0] - 2026-09-20

### Added
- **Order MM items from the Replication Terminal.** A `mm:replication/link` port puts a machine's
  recipes in the Terminal next to everything else. Ask for an item and the machine makes it, and it
  lands in the Terminal. Items Replication has no matter value for can be ordered too. Needs
  [Replication](https://www.curseforge.com/minecraft/mc-mods/replication).
- **Ars Nouveau Source port.** With [Ars Nouveau](https://www.curseforge.com/minecraft/mc-mods/ars-nouveau)
  installed, `mm:ars_nouveau/source` ports hold Source, set with `capacity` and `range`.
  - Input ports pull from nearby source jars, and sourcelinks can fill them directly.
  - Output ports feed nearby jars and Ars machines such as the imbuement chamber.
  - Source relays cannot be pointed at a port; use jars or sourcelinks.
- **Autocraft MM recipes from AE2.** With [Applied Energistics 2](https://www.curseforge.com/minecraft/mc-mods/applied-energistics-2)
  installed, a `mm:ae2/pattern` port joins an ME network and offers the machine's recipes in the
  crafting terminal. Order one and the ingredients are taken from storage, the machine runs, and the
  result comes back. `patternPriority` picks which machine wins when several can make the same item.
  - Matter can be an ingredient when [Replication AE2 Bridge](https://www.curseforge.com/minecraft/mc-mods/replication-ae2-bridge)
    is installed.
  - Only recipes with an item or fluid on both sides can be ordered, since AE2 cannot carry energy,
    Source or radiation.
- **See how full a Source port is** three ways: the block itself brightens as it fills, its screen
  has a bar, and Jade shows a bar when you look at it.
- **Extra blocks join up.** Place them side by side and their frames merge into one panel, with each
  block keeping its own face. `connectGroup` decides what merges with what; without it a block only
  joins copies of itself.

### Fixed
- **Machines no longer craft for free.** A recipe with a `per_tick` input kept running when the port
  it draws from was empty. It now waits until there is enough, and carries on by itself. Affects
  energy, fluid, matter, Source and radiation.
- **Machines that stopped noticing their ports.** Filling a machine sometimes did nothing until a
  block was broken next to it. Idle machines now find their ports again on their own.
- **JEI no longer stacks copies** of a machine and its recipes every time resources are reloaded.
- **Two config files claiming the same `id`.** Giving two ports, controllers or extra blocks the
  same `id` has always stopped the game, but it never said where the clash was. It now names both
  files. Using one block in as many structures as you like is unaffected.
- **A missing port is reported.** A structure that uses a port no config defines now says so in
  four places: a summary when the world loads, a message to operators joining the world, a red line
  on the structure's JEI page, and the debug tool's dump. Before, the machine quietly never formed
  and nothing said why.

## [1.21.1-0.7.1] - 2026-09-14

### Added
- **Pick the recipe from the controller.** Set `"recipeSelectionMode": "manual"` on a controller
  (`.recipeSelectionMode('manual')` in KubeJS) and its screen gets arrows to choose which recipe
  runs. The machine only runs that recipe, and waits when none is picked. Matter input ports with
  free tanks only pull what the picked recipe uses.

### Fixed
- Jade's settings screen was missing a name for MM's port contents option.

## [1.21.1-0.7.0] - 2026-09-12

### Added
- **Nuclear Radiation port.** With [Nuclear Radiation](https://www.curseforge.com/minecraft/mc-mods/nuclear-radiation)
  installed, `mm:nuclear_radiation/radiation` ports store radioactive isotopes, measured in Bq.
  - Put radioactive items in an input port and it absorbs their radiation.
  - Recipes can use a specific isotope (`"isotope": "nr:u_235"`) or any radiation, and can make
    radiation as an output.
  - Output ports load radiation onto items, so it can travel from one machine to the next.
  - Stored radiation decays at its real half-life unless `"decay": false`.
  - Ports are sealed by default; `"shielded": false` makes a port irradiate the area around it.
  - `isotopes`, `carriers` and `loadPerItem` limit what a port accepts and loads.
  - See `docs/radiation.md`.
- **Energy past 2.1 billion FE.** Energy port `capacity`, `maxReceive` and `maxExtract`, and energy
  amounts in recipes, can now go above 2,147,483,647 FE. Other mods still move at most that much per
  transfer.
- **`anyOf` structure key.** One spot can accept any of several keys, each with its own settings:
  `{ "anyOf": [ { "portType": "mm:item", "maxTier": 8 }, { "block": "minecraft:iron_block" } ] }`.
  Works the same from KubeJS.

### Fixed
- **Free matter tanks now fill.** Matter input ports set up with `tanks` and no `matter` never took
  anything from the network. Each free tank now pulls one of the matter types the machine's recipes
  use.
- The docs said a `per_tick` energy amount is taken every tick. It is the total for the whole
  recipe, spread across it. The docs and examples now say so.

## [1.21.1-0.6.2] - 2026-09-10

### Added
- **Rainbows with your own colours.** Add `colors` to a rainbow name to cycle through just those
  colours: `{ "text": "My Port", "rainbow": true, "colors": ["#00FFFF", "#0000FF"], "spread": 0 }`
  fades the whole name between cyan and blue. Works from KubeJS too.
- `docs/example.md` builds a complete machine step by step, and `docs/reference.md` lists every key
  and option.

## [1.21.1-0.6.1] - 2026-09-10

### Added
- **Jade shows what's inside more ports.** Chemical, heat, matter and entity ports now show their
  contents when you look at them, like fluid ports already did.
- **Coloured names from KubeJS.** `.name()`, `.inputName()` and `.outputName()` take the same
  objects as the JSON configs, so colours, gradients and rainbows work from scripts too:
  `.inputName({ text: 'My Input Port', rainbow: true })`.

## [1.21.1-0.6.0] - 2026-09-10

### Added
- **A port can register just one side.** `only` takes `input`, `output` or `both` (the default), so
  a hatch that only ever takes things in no longer registers an output block nobody uses. Available
  from KubeJS as `.only('input')`.
- **Ports can name each side themselves.** `inputName` and `outputName` replace the whole name for
  that side, suffix included, so "Basic Chemical Port Input" can be written as "Basic Chemical
  Input Port". `name` on its own still appends " Input" / " Output" as before.
- **Names can be coloured.** Any name field now takes a full text component, so ports and
  controllers can use colour and formatting: `{ "text": "Fry Basket", "color": "#FFC72C",
  "bold": true }`.
- **Gradients and rainbows.** `{ "text": "...", "gradient": ["#FF0000", "#00FFFF"] }` spreads
  colours across the letters, and `{ "text": "...", "rainbow": true }` cycles by itself. `speed` is
  colour cycles per second and `spread` is the hue shift per character, so `spread: 0` makes the
  whole name one colour that cycles together while a larger value sends the colour travelling along
  it.
- **The structure preview rotates with left click and zooms with the scroll wheel.** The old
  middle-drag rotate, right-drag zoom and shift-drag pan still work.

## [1.21.1-0.5.1] - 2026-09-08

### Added
- `docs/mekanism.md` covers the chemical and heat ports, custom chemicals from KubeJS, and how to
  migrate off the old port types.

### Changed
- **The four Mekanism port types are now one.** `mm:mekanism/gas`, `/slurry`, `/pigment`
  and `/infuse` are gone; use `mm:mekanism/chemical`. Mekanism merged those registries into a
  single chemical type in 1.21.1, so the four ports had become the same port wearing four names.
  A pack has to change the `type` on its Mekanism ports and recipes — a config naming a removed
  type stops the game with a message saying so. Recipes still accept the old `gas`, `slurry`,
  `pigment` and `infuse` keys for naming the chemical, so only the type needs changing.

### Fixed
- The Mekanism chemical port had no auto push at all — no `autoPush` in its config, nothing on its
  KubeJS builder, and no code to move chemicals. Calling `.autoPush(true)` on one from KubeJS took
  the game down on startup, and setting it in JSON did nothing. It now works the way the energy,
  fluid, item and heat ports do.
- Auto push on the energy port only ever moved energy between MM ports, so an output bus sitting
  against a cable or a machine never emptied. It now pushes into anything that accepts energy.
  Auto push is still off unless the port or the config turns it on.

## [1.21.1-0.5.0] - 2026-09-05

### Added
- **Replication matter ports.** `mm:replication/matter` joins Replication's matter network, so a
  machine can spend matter or produce it. An output port behaves like a Disintegrator: the
  network empties it into your matter tanks on its own. An input port goes and takes what it is
  short of, the way a Replicator does, so it fills from anywhere on the network rather than only
  from what a pipe happens to push at it.
- Port config takes `capacity` for how much each tank holds, `matter` for the type each tank is
  reserved for (one string, or a list for several tanks), `tanks` for unreserved tanks that lock
  to whatever arrives first, `priority` for MM's own output ordering, and `network` to keep the
  port off the matter network entirely.
- Recipes name the type and the amount: `{ "type": "mm:replication/matter", "matter":
  "replication:metallic", "amount": 64 }`. Ranges and `rollGroup` work on it like everywhere else.
- Matter pipes connect to the port and draw the connection, the same as they do to
  Replication's own blocks.
- The port answers Replication's matter capability too, so other mods' pipes can see it.
- **Entity ports.** `mm:entity` lets a machine work with mobs. A mob that walks onto the port is
  picked up on its own — `stored` swallows it into the block, `standing` leaves it on top where you
  can still see it. A recipe can spend a mob or keep it as a catalyst, ask for one type or a whole
  entity tag, and an output port produces mobs instead of consuming them. Break the port and you
  get every mob back. Config: `capacity`, `mode`, `invulnerable`, `immobile`, `silent`,
  `persistent`, `zone`, `speedPerEntity`, `consume`, `entities` / `tags`.
- `zone` sets how much space above a port it watches — a flat 3x3 pad, a tall chimney, or a cube up
  to 16 on a side. `speedPerEntity` makes a crowd worth having: every mob past the first speeds the
  recipe up, so three on a pedestal can run it three times as fast.
- Machine recipes can run faster than one tick of progress per tick. Nothing changes unless a port
  asks for it, and today only `speedPerEntity` does.
- Both ports take their options from KubeJS under the same names.

### Changed
- The Replication matter port screen puts a divider between its tanks, tints an empty tank with the
  matter it is reserved for, and highlights the one under your cursor. It used to be a flat grey
  slab you had to hover over to read.

## [1.21.1-0.4.0] - 2026-09-02

### Added
- **Mekanism heat ports.** `mm:mekanism/heat` stores heat and lets recipes spend it, the same
  way the energy port handles FE. Thermodynamic Conductors, Resistive Heaters and Fuelwood
  Heaters feed an input port; an output port pushes its heat back out into anything Mekanism
  will take it. Recipes use `"amount"`, and ranges and `rollGroup` work on it like everywhere
  else.
- Port config takes `capacity` for how much heat it holds, plus optional `heatCapacity` for
  how much the temperature moves per unit stored, and `inverseConduction` for how fast heat
  crosses the boundary. Both have sensible defaults.
- Available from KubeJS as `.capacity(...)`, `.heatCapacity(...)` and `.inverseConduction(...)`.

### Fixed
- **Recipes no longer drain a port every tick.** An energy or heat input written as a plain
  `mm:input/consume` was consumed once per tick instead of once per craft, so a recipe asking
  for 2000 could quietly take many times that. Inputs marked `per_tick: true` were always
  handled correctly and are unchanged.
- **Jammed machines stop consuming.** A machine sitting at 100% because its output is full no
  longer eats energy or heat while it waits for room, and no longer produces per-tick outputs.

## [1.21.1-0.3.0] - 2026-08-28

### Added
- **Custom textures for controllers and ports.** `texture` changes the casing, `overlay`
  changes the symbol on it. Ports also take `inputTexture` / `outputTexture` and
  `inputOverlay` / `outputOverlay`. The casing is drawn solid, so use `overlay` for anything
  that needs transparency.
- **Custom block models.** `model` points a controller or port at any block model, for real
  3D shapes instead of the plain cube. Ports also take `inputModel` / `outputModel`. Give
  your model a vanilla parent such as `minecraft:block/block`, and keep the outside of the
  cube filled in or you will see gaps against neighbouring blocks.
- **Both work from KubeJS**, as `.texture(...)`, `.overlay(...)` and `.model(...)` on
  controller and port builders, plus the input and output variants on ports.
- **Recipes can use a range instead of a fixed amount.** Item `count`, fluid `amount` and
  energy `amount` accept `{ "min": 1, "max": 5 }`, and the machine rolls a value each craft.
  Plain numbers behave exactly as before.
- **`rollGroup` links ranges so they roll together**, so a cheap craft gives a small output
  and an expensive one gives a big output.
- JEI shows the range in the slot tooltip and names the roll group. Slots show the maximum,
  so AE2 patterns request enough to cover the worst roll.
- **The mod now comes with a new machine.** A Pulverizer that doubles raw iron, gold and copper
    for 400 FE. Build a 3x3x3 shell of smooth stone with a hollow middle, controller on one
    face, energy port opposite, item input and output on the sides.
- Its config files are written to `config/mm` on first run. Edit them to make the machine
  your own, or delete them to remove it — deleted files stay deleted.

### Fixed
- **`slotCapacity` now works when you place items by hand.** Slots stayed capped at 64 in
  the GUI no matter what the config said.
- **Item ports holding more than 99 of something in one slot no longer break on save.**
  Only affected ports with a `slotCapacity` above 99.
- **Shift-clicking into an item port tops up stacks already there** instead of spreading one
  per slot and then refusing to accept anything.
- `slotCapacity` has one maximum again, whether it comes from a config file, from KubeJS, or
  from the port itself.
- Clearing an item port now also clears its per-slot counts.
- **A recipe's `chance` roll no longer leaks between machines.** Each machine now rolls for
  itself. Per-tick entries still re-roll every tick.
- **One broken structure no longer wipes every machine out of JEI.** MM stopped setting up
  JEI at the first structure naming a controller that did not exist, taking every machine
  after it with it. Broken structures are now skipped and named in the log.


## [1.21.1-0.2.0] - 2026-08-23

### Fixed
- **Mekanism and PneumaticCraft ports can be piped into and out of again.** NeoForge
  requires a capability to be registered against a block entity type before anything
  can query it, and only the item, fluid and energy capabilities were being
  registered. The chemical and compressed-air ports answered their capability
  correctly but were never asked, so they worked inside a machine while being
  invisible to every pipe, tube and machine outside it.
- A pack naming a port type that is misspelled, or whose mod is not installed, now
  fails with a message naming the type and listing the ones that are registered.
  It used to be a bare `NullPointerException` during mod construction, which took
  every port down with it.
- Any error while loading a controller, port or extra block config now names the
  file it came from. Malformed JSON and a top-level value that is not an object are
  both reported the same way.


## [1.21.1-0.1.2] - 2026-08-12

### Fixed
- Port screens no longer draw the port's name on top of the slots. Slot grids were
  centred in a fixed height budget, which worked up to four rows but placed a
  five-row grid one pixel under the title and a six-row grid exactly on it. Grids
  of four rows and smaller are positioned exactly as before.
- A port name long enough to wrap no longer spills onto the first row of slots.
  The name is drawn on a single line and clipped if it does not fit.
- Grids too large for the window are now kept inside it rather than drawn off the
  top or side. Note that the port window is a fixed-size background, so six rows
  by nine columns is the largest grid that genuinely fits; taller grids are
  clamped but will still overlap the player inventory.

## [1.21.1-0.1.1] - 2026-08-07 — Minecraft 1.21.1 / NeoForge port
Port of the 1.20.1 Forge codebase to Minecraft 1.21.1 on NeoForge, starting a
fresh version line for this fork. Continues from 0.1.34.5-fix2 below; the version
number restarts because this is a separately published fork, not a regression.

Requires NeoForge 21.1.0+ and Java 21.

### Changed — read before updating a pack
- **Mekanism chemical types are no longer distinct.** Mekanism 1.21.1 merged gas,
  slurry, pigment and infusion into a single `Chemical` with no discriminator.
  All four port ids (`mm:mekanism/gas`, `/slurry`, `/pigment`, `/infuse`) still
  load and no pack file needs editing, but the distinction is now cosmetic: a
  port declared as gas will accept a pigment. Recipes relying on a gas port
  rejecting slurry no longer behave that way.
- **Item nbt matching is narrower.** 1.20.5 replaced the single item tag with
  typed data components; MM now matches against `minecraft:custom_data`. Existing
  items keep their data, but nbt that matched `Damage` or enchantments will not
  match, as those are separate components now.
- **Botania support is disabled.** Botania has no 1.21.1 release, so
  `mm:botania/mana` is not registered and recipes using it are skipped with a
  message naming the recipe. The port code is stubbed, not deleted.

### Fixed
- A pack referencing a port from an uninstalled mod no longer makes the world
  unloadable. This previously surfaced as a `NullPointerException` during recipe
  parsing that killed world creation. Recipes and structures now parse
  independently — a failing one is logged with its id and skipped — and unknown
  port types report which types *are* registered.
- Datapack-supplied translation keys now work for port and controller names. The
  `{ "translation": "..." }` form worked for structures but produced raw key text
  for blocks a pack registers.
- Port names no longer hardcode the English `Input`/`Output` suffix, so it
  translates with the rest of the name.
- The blueprint item's name is translatable. It was generated as hardcoded
  English and was the only item name no resource pack could override.
- `StructureManager` no longer carries an `@EventBusSubscriber` with no
  subscribers, which NeoForge rejects outright.

### Notes
- Pack configs, datapacks, KubeJS scripts and existing worlds work unchanged.
  Machine, port, structure and recipe JSON is untouched, and the KubeJS builders
  keep the same signatures.
- Sample data now uses the `c:` common tag namespace instead of `forge:`, which
  NeoForge no longer provides.
- **Verified:** structure forming, recipe processing, item and energy ports,
  capabilities to and from other mods, GUIs, save/load, dedicated server, Jade,
  KubeJS.
  **Not yet verified:** fluid, Mekanism, PneumaticCraft and Create ports,
  blueprint pasting, JEI integration, and the debug tool, multiblock saver and
  priority setter.

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
