# Masterful Machinery reference

Every file, key and option in one place. For a machine built step by step, see
[`example.md`](example.md).

## Where files go

| What | Where | Reload |
|---|---|---|
| Controllers | `config/mm/controllers/*.json` | Restart |
| Ports | `config/mm/ports/*.json` | Restart |
| Extra blocks | `config/mm/extras/*.json` | Restart |
| Structures | `data/<namespace>/mm/structures/*.json` in a datapack | `/reload` |
| Processes (recipes) | `data/<namespace>/mm/processes/*.json` in a datapack | `/reload` |

Subfolders are fine. With KubeJS installed, `kubejs/data/` works as a datapack, so structures can go
in `kubejs/data/mypack/mm/structures/`.

A structure at `data/mypack/mm/structures/alloy_kiln.json` has the id `mypack:alloy_kiln`.
Processes work the same way.

Ports that have no input or output side — `mm:ae2/pattern` and `mm:replication/link` — make a
single block named `mm:<id>`, with no suffix on the name.

Controllers, ports and extra blocks register as `mm:<id>`. A port makes two blocks, `mm:<id>_input`
and `mm:<id>_output`, unless `only` limits it to one side.

The same things can be made from KubeJS:

| Event | Script folder |
|---|---|
| `MMEvents.registerControllers` | `kubejs/startup_scripts/` |
| `MMEvents.registerPorts` | `kubejs/startup_scripts/` |
| `MMEvents.registerExtraBlocks` | `kubejs/startup_scripts/` |
| `MMEvents.createStructures` | `kubejs/server_scripts/` |
| `MMEvents.createProcesses` | `kubejs/server_scripts/` |
| `MMEvents.recipeStarted` / `MMEvents.recipeFinished` | `kubejs/server_scripts/` — see [Recipe events](#recipe-events) |
| `MMEvents.builderStructures` | `kubejs/server_scripts/` — see [Structure Builder](#structure-builder) |

An event in the wrong folder silently never runs.

## Controllers

```json
{
  "id": "alloy_kiln",
  "type": "mm:machine",
  "name": "Alloy Kiln"
}
```

| Key | Required | Meaning |
|---|---|---|
| `id` | yes | Block id, registered as `mm:<id>` |
| `type` | yes | `mm:machine`, or `mm:single` for a machine in one block — see [`single-block.md`](single-block.md) |
| `name` | yes | Display name — see [Names and colours](#names-and-colours) |
| `texture` | no | Base texture, e.g. `kubejs:block/kiln_base` |
| `overlay` | no | Texture drawn on top of the base |
| `model` | no | A full block model to use instead |
| `parallelProcessingDefault` | no | Whether recipes on this controller run in parallel when the recipe does not say |
| `maxParallelRecipes` | no | How many recipes can run at once, `0`–`100`. Falls back to the global config |
| `recipeSelectionMode` | no | `default`, `avoid_same_recipe`, `round_robin_input_item` or `manual` |
| `workingSound` | no | A sound played while the machine works, e.g. `minecraft:block.blastfurnace.fire_crackle` |
| `workingSoundInterval` | no | Ticks between sounds. Default `40` |
| `workingParticle` | no | A particle shown around the controller while the machine works, e.g. `minecraft:smoke` |
| `unformedColor` / `idleColor` / `workingColor` | no | The controller screen's colour in each state, as `#RRGGBB` |

Recipe selection modes:

- **`default`** — the first recipe that can run, runs.
- **`avoid_same_recipe`** — skips the recipe that just ran when another one can run, so a machine
  with several recipes alternates between them.
- **`round_robin_input_item`** — takes turns between the different input items, so one item cannot
  hog the machine.
- **`manual`** — players pick the recipe in the controller's screen with the arrows, and the machine
  only runs that one. With nothing picked it waits. Useful when recipes share inputs, like a machine
  that only takes matter. Matter input ports with free tanks then pull only what the picked recipe
  uses.

KubeJS:

```js
MMEvents.registerControllers(event => {
    event.create('alloy_kiln')
        .type('mm:machine')
        .name('Alloy Kiln')
        .texture('kubejs:block/kiln_base')
        .overlay('kubejs:block/kiln_front')
        .parallelProcessingDefault(true)
        .maxParallelRecipes(4)
        .recipeSelectionMode('avoid_same_recipe')
        .workingSound('minecraft:block.blastfurnace.fire_crackle')
        .workingSoundInterval(30)
        .workingParticle('minecraft:smoke')
        .workingColor('#FF8800')
})
```

Players can mute a machine's sound from its screen, and turn all working sounds and particles off in
the client config. `config/mm/working_effects.json` overrides the sound, interval and particle per
controller without touching the pack's files; the in-game config screen edits it too.

### The controller screen

Right-click a formed controller to open its screen:

- **Name** — click it to rename the machine. An empty name restores the original.
- **Status** — Running, Idle, Stuck (a recipe started but cannot continue), Paused by redstone or Not
  formed. Hover it for the reason. When the machine is not formed, the screen lists the missing
  blocks.
- **Redstone** — Ignored, With Signal or Without Signal.
- **Mode** — the recipe selection mode for this machine.
- **Sound** — mute or unmute the machine. Only shown when the controller has a `workingSound`.
- **Linked to** — the ME network link, with AE2 installed. See [`ae2.md`](ae2.md).
- **Recipe** — in `manual` mode, pick the recipe with the arrows.
- **Inputs / Outputs** — pages listing every port and what is in it. With JEI, hover an item and
  press R or U to look it up.
- **Assemble** — places the missing blocks of the machine from your inventory. Pick the tier of
  each part first when the structure allows several.

The button in the top-right corner switches between a large and a small screen.

A comparator next to the controller gives 0 while the machine is not working, and 1–15 by recipe
progress while it works.

## Ports

```json
{
  "id": "kiln_items",
  "controllerIds": "mm:alloy_kiln",
  "name": "Kiln Hatch",
  "type": "mm:item",
  "config": { "rows": 2, "columns": 2 }
}
```

| Key | Required | Meaning |
|---|---|---|
| `id` | yes | Base id. The blocks become `mm:<id>_input` and `mm:<id>_output` |
| `controllerIds` | yes | One controller id, or a list of them |
| `type` | yes | The port type — see below |
| `config` | yes | Settings for that port type |
| `name` | yes | Base name. " Input" / " Output" is added to it |
| `inputName` / `outputName` | no | The full name for one side, replacing `name` for it |
| `only` | no | `input`, `output` or `both` (default) |
| `texture`, `overlay`, `model` | no | As on controllers |
| `inputTexture`, `outputTexture`, `inputOverlay`, `outputOverlay`, `inputModel`, `outputModel` | no | The same, for one side only |

### The port screen

Every port's screen has a side panel:

- **Auto I/O per side** — click a side to make the port push to it (output ports) or pull from it
  (input ports). `autoPush` only sets which sides start on. The Machine Wrench does the same from the
  world: right-click a port's side to toggle it.
- **Lock** — on fluid and chemical ports, lock a tank to what is in it, so it keeps that fluid or
  chemical even when it empties.
- **Dump** — Shift+click to delete everything in the port.

Ports with more slots than fit on screen get pages; scroll or use the arrows. A light on each port
shows its machine's state, in the screen colours.

### Port types

| Type | Needs | `config` | Recipe ingredient |
|---|---|---|---|
| `mm:item` | — | `rows`, `columns`, `slotCapacity`, `autoPush` | `item` or `tag`, `count` |
| `mm:fluid` | — | `rows`, `columns`, `slotCapacity`, `autoPush`, `fluids` | `fluid`, `amount` (mB) |
| `mm:energy` | — | `capacity`, `maxReceive`, `maxExtract`, `autoPush` | `amount` (FE) |
| `mm:entity` | — | see [`entity.md`](entity.md) | `entity` or `tag`, `amount` |
| `mm:mekanism/chemical` | Mekanism | see [`mekanism.md`](mekanism.md) | `chemical`, `amount` |
| `mm:mekanism/heat` | Mekanism | see [`mekanism.md`](mekanism.md) | see [`mekanism.md`](mekanism.md) |
| `mm:create/kinetic` | Create | `stress` | `speed` |
| `mm:pneumaticcraft/air` | PneumaticCraft | `volume`, `danger`, `critical` | `air`, `pressure` (optional) |
| `mm:botania/mana` | Botania | `capacity` | `mana` |
| `mm:replication/matter` | Replication | see [`replication.md`](replication.md) | `matter`, `amount` |
| `mm:nuclear_radiation/radiation` | Nuclear Radiation | see [`radiation.md`](radiation.md) | `isotope` (optional), `amount` (Bq) |
| `mm:ars_nouveau/source` | Ars Nouveau | see [`ars-nouveau.md`](ars-nouveau.md) | `source` |
| `mm:projecte/emc` | ProjectE | see [`projecte.md`](projecte.md) | `emc` |
| `mm:replication/link` | Replication | none — see [`replication.md`](replication.md) | cannot be used in recipes |
| `mm:ae2/pattern` | AE2 | see [`ae2.md`](ae2.md) | cannot be used in recipes |

Notes on the config keys:

- **`slotCapacity`** — on item ports it is how many items fit in one slot; leave it out for the
  normal stack size. On fluid ports it is required and is the mB each tank holds.
- **`fluids`** — on fluid ports, the fluids the port takes, as one id or tag (`#c:oil`) or a list of
  them. Pipes and recipe outputs can not put anything else in. Leave it out to take any fluid.
- **`autoPush`** — the port pushes its contents into neighbouring blocks on its own. Off by default;
  the default can be changed in the config.
- **Energy above 2.1 billion** — `capacity`, `maxReceive`, `maxExtract` and recipe amounts can go
  past 2,147,483,647 FE, up to 9,223,372,036,854,775,807. Other mods still move at most
  2,147,483,647 FE per transfer, and their displays stop at that number.
  From KubeJS, whole numbers are only exact up to **9,007,199,254,740,991**. Write a bigger number
  than that in a script and it is rounded before MM ever sees it, so use that number or less for a
  "creative" hatch. JSON config files take the full range exactly.
- **`tierRank`** — any port can have a `tierRank` number in its `config`. Structures can then ask for
  "any energy port of tier 2 or higher" — see [Structure keys](#structure-keys).

KubeJS:

```js
MMEvents.registerPorts(event => {
    event.create('kiln_items')
        .name('Kiln Hatch')
        .controllerId('mm:alloy_kiln')
        .config('mm:item', config => {
            config.rows(2)
            config.columns(2)
            config.slotCapacity(128)
            config.autoPush(true)
        })

    event.create('kiln_energy')
        .name('Kiln Power')
        .controllerId('mm:alloy_kiln')
        .only('input')
        .config('mm:energy', config => {
            config.capacity(100000)
            config.maxReceive(1000)
            config.maxExtract(0)
            config.tierRank(2)
        })
})
```

Config methods per type:

| Type | Methods |
|---|---|
| `mm:item` | `rows`, `columns`, `slotCapacity`, `autoPush`, `tierRank` |
| `mm:fluid` | `rows`, `columns`, `slotCapacity`, `autoPush`, `fluids`, `tierRank` |
| `mm:energy` | `capacity`, `maxReceive`, `maxExtract`, `autoPush`, `tierRank` |
| `mm:create/kinetic` | `stress` |
| `mm:pneumaticcraft/air` | `volume`, `danger`, `critical` |
| `mm:botania/mana` | `capacity` |
| `mm:ars_nouveau/source` | `capacity`, `range` |
| `mm:projecte/emc` | `capacity`, `kleinSlot`, `kleinRate` |
| `mm:ae2/pattern` | `patternPriority` |

The entity, Mekanism and Replication methods are in their own docs. Call `.controllerId(...)` once
per controller to attach a port to several machines.

## Structures

```json
{
  "name": "Alloy Kiln",
  "controllerIds": "mm:alloy_kiln",
  "layout": [
    ["BBB", "BBB", "BBB"],
    ["ICO", "BEB", "BBB"]
  ],
  "key": {
    "B": { "block": "minecraft:bricks" },
    "I": { "port": "mm:kiln_items", "input": true },
    "O": { "port": "mm:kiln_items", "input": false },
    "E": { "portType": "mm:energy", "input": true }
  }
}
```

### Layout

- `layout` is a list of **layers**, from the **top** layer down to the bottom.
- Each layer is a list of **rows**, running front to back.
- Each character in a row is one block, left to right.
- **`C`** is the controller. It must appear exactly once.
- A **space** means "anything goes" — the position is not checked.
- Every other character must be in `key`.

The machine forms in any of the four horizontal rotations, so it does not matter which way the
player builds it.

### Structure keys

| Key form | Matches |
|---|---|
| `{ "block": "minecraft:bricks" }` | That block |
| `{ "tag": "minecraft:logs" }` | Any block in the tag |
| `{ "port": "mm:kiln_items" }` | That port, either side |
| `{ "port": "mm:kiln_items", "input": true }` | That port, input side only |
| `{ "portType": "mm:energy" }` | Any port of that type |
| `{ "portType": "mm:energy", "minTier": 2, "maxTier": 3 }` | Only ports whose `tierRank` is in range |
| `{ "stateList": "casing" }` | Any option from a named state list |
| `{ "anyOf": [ { ... }, { ... } ] }` | Any one of the listed keys — ports, blocks, tags, each with their own settings |

`port` and `portType` also accept `"anywhere": true`. The port then does not have to sit in that
exact position — any of the port positions in the structure will do, as long as every port still
gets a place. Set `"portsAnywhere": true` at the top of the structure to do this for every port.

Add `properties` to a key to also check block states:

```json
"L": {
  "block": "minecraft:oak_log",
  "properties": [{ "property": "axis", "value": "y" }]
}
```

### State lists

A state list is a named set of options that one position can take:

```json
{
  "stateLists": {
    "casing": {
      "iron": { "block": "minecraft:iron_block" },
      "gold": { "block": "minecraft:gold_block" }
    }
  },
  "key": {
    "X": { "stateList": "casing" }
  }
}
```

Add `"defaultIgnored": true` to a list to also accept any block at all. State lists are JSON only.

### Other structure keys

| Key | Meaning |
|---|---|
| `name` | Name shown in JEI and on the blueprint |
| `controllerIds` | One controller id or a list. Several controllers can share one structure |
| `maxParallelRecipes` | Overrides the controller's limit for this structure |
| `portsAnywhere` | As above |
| `tier` | The structure's tier number, used by `minTier` / `maxTier` recipe conditions. Without it, the tier is read from the name: "Tier 2", "Mk II", "Level 3" and so on, or `1` |

KubeJS:

```js
MMEvents.createStructures(event => {
    event.create('kubejs:alloy_kiln')
        .name('Alloy Kiln')
        .controllerId('mm:alloy_kiln')
        .layout(layout => {
            layout.layer(['BBB', 'BBB', 'BBB'])
            layout.layer(['ICO', 'BEB', 'BBB'])
            layout.key('B', { block: 'minecraft:bricks' })
            layout.key('I', { port: 'mm:kiln_items', input: true })
            layout.key('O', { port: 'mm:kiln_items', input: false })
            layout.key('E', { portType: 'mm:energy', input: true })
        })
})
```

Layers go top to bottom, the same as in JSON. Give the structure id a namespace — `kubejs:alloy_kiln`,
not `alloy_kiln`. `.portsAnywhere(true)` goes on the layout, `.maxParallelRecipes(n)` on the
structure.

## Processes

```json
{
  "structureId": "mypack:alloy_kiln",
  "ticks": 100,
  "inputs": [
    { "type": "mm:input/consume", "ingredient": { "type": "mm:item", "item": "minecraft:copper_ingot", "count": 3 } },
    { "type": "mm:input/consume", "ingredient": { "type": "mm:item", "tag": "c:ingots/tin", "count": 1 } },
    { "type": "mm:input/consume", "per_tick": true, "ingredient": { "type": "mm:energy", "amount": 4000 } }
  ],
  "outputs": [
    { "type": "mm:output/simple", "ingredient": { "type": "mm:item", "item": "minecraft:copper_block", "count": 1 } }
  ]
}
```

| Key | Required | Meaning |
|---|---|---|
| `structureId` | yes | Which structure runs this recipe |
| `ticks` | yes | How long one craft takes. 20 ticks is one second |
| `inputs` | yes | List of input entries |
| `outputs` | yes | List of output entries |
| `structureIds` | no | A list of more structures that can run this recipe too |
| `conditions` | no | Extra requirements — see below |
| `parallelProcessing` | no | Let this recipe run several times at once |
| `requestOnly` | no | Only run when something asks for it: the Replication Terminal, AE2, or a pick in `manual` mode |

### Entries

Inputs use `mm:input/consume` and outputs use `mm:output/simple`. Both take:

| Key | Meaning |
|---|---|
| `ingredient` | What is taken or made. The `type` is the port type |
| `chance` | `0`–`1`. `0.25` means a 25% chance each craft. Default `1` |
| `per_tick` | Spread the ingredient over the recipe instead of taking it at the start. For energy, `amount` is the total for the whole recipe; for heat, matter and radiation it is taken every tick |

An item ingredient can also be written as just a string: `"ingredient": "minecraft:diamond"`.

### Weighted outputs

`mm:output/weighted` gives exactly one of several outputs each craft, picked by weight:

```json
{
  "type": "mm:output/weighted",
  "options": [
    { "weight": 6, "ingredient": { "type": "mm:item", "item": "minecraft:iron_nugget", "count": 3 } },
    { "weight": 3, "ingredient": { "type": "mm:item", "item": "minecraft:gold_nugget", "count": 2 } },
    { "weight": 1 }
  ]
}
```

Here iron comes out 60% of the time, gold 30%, and an option with no ingredient means nothing, 10%.
`chance` works on the whole entry as on other outputs. JEI shows each option with its odds.

### Random amounts

Any `count` or `amount` can be a range:

```json
"count": { "min": 2, "max": 5 }
```

Each craft rolls a value between the two, inclusive. Ranges that share a **`rollGroup`** roll
together — the input and output below always land on the same point of their ranges, so 1 gold in
always gives 4 nuggets and 5 gold always gives 8:

```json
"inputs":  [{ "type": "mm:input/consume", "ingredient": { "type": "mm:item", "item": "minecraft:gold_ingot", "count": { "min": 1, "max": 5, "rollGroup": "yield" } } }],
"outputs": [{ "type": "mm:output/simple", "ingredient": { "type": "mm:item", "item": "minecraft:gold_nugget", "count": { "min": 4, "max": 8, "rollGroup": "yield" } } }]
```

### Conditions

```json
"conditions": [
  { "type": "mm:dimension", "dimension": "minecraft:the_nether" },
  { "type": "mm:weather", "weather": "rain" }
]
```

Every condition has to pass. While one fails, the recipe does not start, and a running one waits.

| Type | Keys | Passes when |
|---|---|---|
| `mm:dimension` | `dimension` | The machine is in that dimension |
| `mm:weather` | `weather`: `clear`, `rain` or `thunder` | The weather matches |
| `mm:biome` | `biome`: a biome, or a biome tag with `#` | The controller is in that biome |
| `mm:time` | `time`: `day` or `night` | It is that time of day |
| `mm:height` | `minY`, `maxY` (either or both) | The controller's Y is in range |
| `mm:redstone` | `redstone`: `powered` or `unpowered` | The controller does or does not get a signal |
| `mm:tier` | `minTier`, `maxTier` (either or both) | The structure's `tier` is in range |

JEI lists a recipe's conditions under it.

### Item NBT

Item ingredients can require data on the item:

```json
{ "type": "mm:item", "item": "minecraft:potion", "count": 1, "nbt": "{custom:1b}", "nbt_match": "strong" }
```

`nbt` is SNBT text or a JSON object. By default the item only needs to contain those values;
`"nbt_match": "strong"` makes it match exactly.

KubeJS:

```js
MMEvents.createProcesses(event => {
    event.create('kubejs:bronze')
        .structureId('kubejs:alloy_kiln')
        .ticks(100)
        .parallelProcessing(true)
        .input({ type: 'mm:input/consume', ingredient: { type: 'mm:item', item: 'minecraft:copper_ingot', count: 3 } })
        .input({ type: 'mm:input/consume', per_tick: true, ingredient: { type: 'mm:energy', amount: 4000 } })
        .output({ type: 'mm:output/simple', chance: 0.5, ingredient: { type: 'mm:item', item: 'minecraft:copper_block', count: 1 } })
})
```

More builder methods:

| Method | Same as |
|---|---|
| `.structureIds('kubejs:a', 'kubejs:b')` | `structureId` plus `structureIds` |
| `.requestOnly(true)` | `requestOnly` |
| `.dimension('minecraft:the_nether')` | `mm:dimension` |
| `.weather('rain')` | `mm:weather` |
| `.biome('#minecraft:is_ocean')` | `mm:biome` |
| `.time('night')` | `mm:time` |
| `.minY(-64)`, `.maxY(0)` | `mm:height` |
| `.redstone('powered')` | `mm:redstone` |
| `.minTier(2)`, `.maxTier(3)` | `mm:tier` |

### Recipe events

```js
// kubejs/server_scripts/
MMEvents.recipeStarted('kubejs:bronze', event => {
    if (event.level.isNight()) event.cancel()
})

MMEvents.recipeFinished('kubejs:bronze', event => {
    console.info('Bronze made at ' + event.pos)
})
```

Both run for one recipe id, or for every recipe when no id is given. `event.recipeId`,
`event.controllerId`, `event.structureId`, `event.level`, `event.pos` and `event.block` describe the
machine. Cancelling `recipeStarted` stops the recipe from starting; it tries again later.

## Names and colours

`name` on controllers and ports, and `inputName` / `outputName` on ports, take any of these:

| Form | Example |
|---|---|
| Plain text | `"Alloy Kiln"` |
| Translation key | `{ "translation": "block.mypack.alloy_kiln" }` |
| Coloured | `{ "text": "Alloy Kiln", "color": "#FF8800", "bold": true }` |
| Gradient | `{ "text": "Alloy Kiln", "gradient": ["#FFC72C", "#DA291C"] }` |
| Rainbow | `{ "text": "Alloy Kiln", "rainbow": true, "speed": 0.5, "spread": 0.05 }` |

- A gradient takes any number of colours.
- A rainbow's `speed` is how fast it cycles, and `spread` is how much the colour changes from one
  letter to the next. `spread: 0` makes the whole name one colour that cycles together.
- Add `colors` to a rainbow to cycle through only those colours instead of the whole spectrum:
  `{ "text": "Alloy Kiln", "rainbow": true, "colors": ["#00FFFF", "#0000FF"], "spread": 0 }` fades
  the whole name between cyan and blue.
- The coloured form also takes `italic`, `underlined`, `strikethrough` and `obfuscated`.

From KubeJS, `.name()`, `.inputName()` and `.outputName()` take the same forms:

```js
.name({ text: 'Alloy Kiln', rainbow: true })
```

A plain text name can be translated or overridden by a resource pack through the `block.mm.<id>`
lang key.

## Parallel processing

A formed machine can run several crafts of the same recipe at once. A recipe runs in parallel when:

1. the recipe says `"parallelProcessing": true`, or the controller has
   `"parallelProcessingDefault": true` and the recipe does not say otherwise, and
2. the ports have enough inputs and output space for more than one craft.

The limit is the structure's `maxParallelRecipes`, then the controller's, then `maxParallelRecipes`
in the config (default `5`).

## Extra blocks

Decorative blocks for building structures out of:

```json
{ "id": "kiln_casing", "name": "Kiln Casing", "type": "mm:vent" }
```

`type` is `mm:circuit`, `mm:gearbox` or `mm:vent`. The block registers as `mm:<id>`.

Extra blocks placed next to each other join their frames into one panel, each block keeping its own
face. `connectGroup` decides what joins with what:

```json
{ "id": "kiln_casing", "name": "Kiln Casing", "type": "mm:vent", "connectGroup": "kiln" }
```

Blocks sharing a `connectGroup` join each other. Without one, a block only joins copies of itself.
Using the same block in as many structures as you like changes nothing.

```js
MMEvents.registerExtraBlocks(event => {
    event.create('kiln_casing').name('Kiln Casing').type('mm:vent').connectGroup('kiln')
})
```

## Items and commands

| Item | Use |
|---|---|
| **Blueprint** | Every structure has one in the **MM Structures** creative tab. In creative, sneak to preview the structure in the world and sneak + right-click to place it |
| **Multiblock Saver** | Right-click two blocks to mark opposite corners, then right-click the air to save what is between them as a structure. The files go to `config/mm/structures/`, as JSON and as a KubeJS script. Sneak + right-click the air to clear the corners |
| **Priority Setter** | Right-click to raise the number, up to 10. Sneak + right-click a port to apply it. Ports with higher priority are used first |
| **Debug Tool** | Right-click a controller to write a report of why it is or is not forming and running |
| **Structure Builder** | Builds and takes apart whole structures. See [Structure Builder](#structure-builder) |
| **Machine Wrench** | Right-click a port side to toggle its auto input/output. Sneak + right-click a port to show all its sides |
| **Input Gateway** | A block to build into a machine in place of a casing or glass, or to place next to an input port. Items, fluids, energy and Mekanism chemicals piped into it go to the machine's input ports |
| **Network Linker** | With AE2 installed, links a machine to an ME network. See [`ae2.md`](ae2.md) |

`/mm reform` (operators only) rechecks every machine near online players. Use it after changing
structures with `/reload`.

JEI shows every structure and its recipes. In the structure preview, drag rotates, the scroll wheel
or right-drag zooms and shift-drag moves it. The bar at the bottom shows one layer at a time; hover
the blue "i" for the controls. Recipes with more than six rows of inputs or outputs scroll.

With Jade, looking at a controller shows its status, progress, how many recipes run, its redstone
mode and its owner. Looking at a port shows its machine and its auto I/O.

### Structure Builder

The Structure Builder builds a whole structure in one go.

- **Shift+use** opens its screen. Pick a structure on the **Structures** tab. While holding it,
  **Shift+scroll** turns the structure before you build.
- **Right-click** a block to build there. Blocks come from the builder's own store, your inventory,
  and an ME network if bound.
- **Shift+right-click** a machine, or hold the dismantle key, to take it apart. Shift+right-click again
  to confirm. A builder structure with no controller can be taken apart too, while it is the one
  selected.
- It runs on FE. Charge it in any charger.

Its tabs:

| Tab | What |
|---|---|
| Structures | Every MM structure and builder structure, grouped by category, with a search box |
| Materials | What the selected structure needs, against what you carry and what is in the store |
| Settings | The tier to use for each kind of part, **Instant build**, and the ME options |
| Config | The in-game config screen |
| Admin | Operators only: create, rename and delete categories, and put structures in them |

**Instant build** places the whole structure in one tick instead of a few blocks at a time. It still
uses blocks and energy.

Builder structures are `.nbt` structure files in a datapack, under
`data/<namespace>/mm_builder_structures/`. Files under `mbtool_structures/` and `spatial_structures/`
are read too. From KubeJS:

```js
// kubejs/server_scripts/
MMEvents.builderStructures(event => {
    event.add('mypack:hut', 'mypack:mm_builder_structures/hut.nbt')
    event.remove('mypack:old_hut')
    event.removeNamespace('someothermod')
})
```

## Config

Most options can be changed in game: open **Mods → Masterful Machinery → Config**, or the Config tab
of the Structure Builder. Server options need operator access.

`config/mm-common.toml`:

| Option | Default | Meaning |
|---|---|---|
| `portsAutoExtractByDefault` | `false` | The `autoPush` value for ports that do not set it |
| `portAutoIOInterval` | `10` | How often ports with auto I/O sides move things, in ticks |
| `parallelProcessingDefault` | `false` | Whether recipes run in parallel when nothing else says |
| `maxParallelRecipes` | `5` | The global parallel limit |
| `structureValidationRate` | `10` | How often a controller rechecks its structure, in ticks |
| `splitRecipesJei` | `true` | One JEI category per structure |
| `showJeiMaxParallel` | `true` | Show the parallel limit in JEI |
| `debugTool` | `true` | Whether the Debug Tool works |
| `assemblyBlocksPerTick` | `2` | How many blocks Assemble and the Structure Builder place per tick |
| `networkLinkOutputInterval` | `20` | See [`ae2.md`](ae2.md#config) |
| `networkLinkSendOnRemove` | `true` | See [`ae2.md`](ae2.md#config) |
| `networkLinkOpBypass` | `true` | See [`ae2.md`](ae2.md#config) |
| `tool.toolEnergyCapacity` | `1000000` | FE the Structure Builder holds |
| `tool.toolEnergyPerPlacedBlock` | `50` | FE per block built |
| `tool.toolEnergyPerDismantledBlock` | `25` | FE per block taken apart |
| `tool.toolEnergyReceiveRate` | `10000` | FE per tick it accepts from a charger |

`config/mm-client.toml`:

| Option | Default | Meaning |
|---|---|---|
| `controller.tintControllerScreen` | `true` | Colour the controller screen by the machine's state |
| `controller.unformedColor` / `idleColor` / `workingColor` | red / green / yellow | The colours, as `#RRGGBB` |
| `controller.workingEffects` | `true` | Play working sounds and show working particles |
| `controller.bigScreen` | `true` | Open the controller screen large |
| `ports.statusLight` | `true` | Show the state light on ports |

## Troubleshooting

- **The game crashes with `Unknown port type`.** The type is misspelled, or the mod that adds it is
  not installed.
- **The game crashes naming a file under `config/mm/`.** That file has a mistake; the message says
  which key.
- **KubeJS crashes with `TypeError: Cannot find function`.** A method name is wrong. They are case
  sensitive.
- **A port written in KubeJS never appears in game.** Check the KubeJS log first. One mistake kills
  the whole file, so everything after it silently never registers — a misspelled variable is enough.
  Then check the file is in `kubejs/startup_scripts/`: ports registered from `server_scripts` never
  run. Port and controller changes need a full restart, not `/reload`.
- **A block shows a raw name like `block.mm.my_port_input`.** Delete `config/mm/pack/` and
  restart. It is generated again on launch.
- **The machine will not form.** Use the Debug Tool on the controller. Check that each port's
  `controllerIds` includes this controller, and that the layout has exactly one `C`.
- **The machine forms but the recipe never starts.** Check the recipe's `structureId`, that every
  input is in an input port, and that the output ports have room. A recipe with `requestOnly` only
  runs when asked for, and one with conditions waits until they pass; JEI lists both.
- **The screen says Stuck.** A recipe started but cannot go on, usually because its outputs do not
  fit or a `per_tick` input ran out. Empty the output ports or refill the input.
- **The game stops saying two config files define the same id.** Two files under `config/mm/` gave
  the same `id`. The message names both files; rename one. Filenames and folders do not matter, only
  the `id` inside. Using one block in several structures is fine.
- **Chat says structures use ports that do not exist.** A structure names a port no config defines,
  so that machine can never form. The log and the structure's JEI page name the missing port. Add
  the port config, or correct the name in the structure.
- **A recipe with a `per_tick` input never finishes.** The machine pauses on any tick it cannot pay
  for in full, shows Stuck, and carries on once the port is supplied again. Check the port is
  actually supplied. Note `per_tick` energy is the total spread across the recipe, while other
  types take the amount every tick.
