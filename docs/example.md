# Building a machine

This walks through one complete machine — an **Alloy Kiln** that turns copper and energy into copper
blocks — first with JSON files, then the same thing in KubeJS. After that are short recipes for the
things packs ask for most. Every key is listed in [`reference.md`](reference.md).

A machine is four pieces:

1. a **controller** — the block that runs the machine,
2. **ports** — the blocks that hold inputs and outputs,
3. a **structure** — the shape the player has to build,
4. **processes** — the recipes it runs.

Controllers and ports are blocks, so they need a restart after changes. Structures and processes
are datapack files and reload with `/reload`.

## With JSON

```
config/mm/
  controllers/alloy_kiln.json
  ports/kiln_items.json
  ports/kiln_energy.json
kubejs/data/mypack/mm/          (or any datapack's data/mypack/mm/)
  structures/alloy_kiln.json
  processes/copper_block.json
```

### 1. The controller

`config/mm/controllers/alloy_kiln.json`

```json
{
  "id": "alloy_kiln",
  "type": "mm:machine",
  "name": "Alloy Kiln"
}
```

This adds the block `mm:alloy_kiln`.

### 2. The ports

`config/mm/ports/kiln_items.json` — a 2x2 item hatch:

```json
{
  "id": "kiln_items",
  "controllerIds": "mm:alloy_kiln",
  "name": "Kiln Hatch",
  "type": "mm:item",
  "config": { "rows": 2, "columns": 2 }
}
```

This adds two blocks: `mm:kiln_items_input` ("Kiln Hatch Input") and `mm:kiln_items_output`
("Kiln Hatch Output").

`config/mm/ports/kiln_energy.json` — energy, input only:

```json
{
  "id": "kiln_energy",
  "controllerIds": "mm:alloy_kiln",
  "name": "Kiln Power",
  "type": "mm:energy",
  "only": "input",
  "config": { "capacity": 100000, "maxReceive": 1000, "maxExtract": 0 }
}
```

`only: "input"` means there is no `mm:kiln_energy_output` block.

### 3. The structure

`kubejs/data/mypack/mm/structures/alloy_kiln.json` — a 3x3 brick base with a solid brick roof:

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
    "E": { "port": "mm:kiln_energy" }
  }
}
```

The first layer is the roof, the second is the floor. Looking at the front of the floor, the item
input is on the left of the controller (`C`), the output on the right, and the energy port sits in
the middle behind it.

This structure's id is `mypack:alloy_kiln`, from the folder and file name.

### 4. The recipe

`kubejs/data/mypack/mm/processes/copper_block.json`

```json
{
  "structureId": "mypack:alloy_kiln",
  "ticks": 100,
  "inputs": [
    { "type": "mm:input/consume", "ingredient": { "type": "mm:item", "item": "minecraft:copper_ingot", "count": 9 } },
    { "type": "mm:input/consume", "per_tick": true, "ingredient": { "type": "mm:energy", "amount": 4000 } }
  ],
  "outputs": [
    { "type": "mm:output/simple", "ingredient": { "type": "mm:item", "item": "minecraft:copper_block", "count": 1 } }
  ]
}
```

9 copper ingots and 4,000 FE, drawn evenly over 5 seconds, make one copper block.

### Trying it

Restart the game, then open JEI on the controller to see the structure and its recipe. In creative,
take the **Alloy Kiln** blueprint from the **MM Structures** tab and sneak + right-click to place
it. Power the energy port, put copper in the input hatch and the machine starts on its own.

## With KubeJS

The same machine as scripts. Ids here are prefixed `kjs_` so both versions can sit in one pack.

`kubejs/startup_scripts/alloy_kiln.js`

```js
MMEvents.registerControllers(event => {
    event.create('kjs_kiln')
        .type('mm:machine')
        .name('Alloy Kiln')
})

MMEvents.registerPorts(event => {
    event.create('kjs_kiln_items')
        .name('Kiln Hatch')
        .controllerId('mm:kjs_kiln')
        .config('mm:item', config => {
            config.rows(2)
            config.columns(2)
        })

    event.create('kjs_kiln_energy')
        .name('Kiln Power')
        .controllerId('mm:kjs_kiln')
        .only('input')
        .config('mm:energy', config => {
            config.capacity(100000)
            config.maxReceive(1000)
            config.maxExtract(0)
        })
})
```

`kubejs/server_scripts/alloy_kiln.js`

```js
MMEvents.createStructures(event => {
    event.create('kubejs:kjs_kiln')
        .name('Alloy Kiln')
        .controllerId('mm:kjs_kiln')
        .layout(layout => {
            layout.layer(['BBB', 'BBB', 'BBB'])
            layout.layer(['ICO', 'BEB', 'BBB'])
            layout.key('B', { block: 'minecraft:bricks' })
            layout.key('I', { port: 'mm:kjs_kiln_items', input: true })
            layout.key('O', { port: 'mm:kjs_kiln_items', input: false })
            layout.key('E', { port: 'mm:kjs_kiln_energy' })
        })
})

MMEvents.createProcesses(event => {
    event.create('kubejs:kjs_copper_block')
        .structureId('kubejs:kjs_kiln')
        .ticks(100)
        .input({ type: 'mm:input/consume', ingredient: { type: 'mm:item', item: 'minecraft:copper_ingot', count: 9 } })
        .input({ type: 'mm:input/consume', per_tick: true, ingredient: { type: 'mm:energy', amount: 4000 } })
        .output({ type: 'mm:output/simple', ingredient: { type: 'mm:item', item: 'minecraft:copper_block', count: 1 } })
})
```

Things to remember:

- Controllers and ports are **startup** scripts. Structures and processes are **server** scripts.
- Controllers and ports are always `mm:` — `.controllerId('mm:kjs_kiln')`, `port: 'mm:kjs_kiln_items'`.
- Structures and processes need the namespace written out: `event.create('kubejs:kjs_kiln')`.
- A wrong method name crashes the game with `TypeError: Cannot find function`.

## Common recipes

### A chance output

```json
{ "type": "mm:output/simple", "chance": 0.1, "ingredient": { "type": "mm:item", "item": "minecraft:diamond", "count": 1 } }
```

10% chance of a diamond each craft.

### A catalyst that is not used up

```json
{ "type": "mm:input/consume", "chance": 0, "ingredient": { "type": "mm:item", "item": "minecraft:blaze_rod", "count": 1 } }
```

The recipe needs the blaze rod in the port but never takes it.

### Any item from a tag

```json
{ "type": "mm:input/consume", "ingredient": { "type": "mm:item", "tag": "minecraft:logs", "count": 4 } }
```

### Random output

```json
{ "type": "mm:output/simple", "ingredient": { "type": "mm:item", "item": "minecraft:raw_iron", "count": { "min": 1, "max": 3 } } }
```

### One of several outputs

```json
{
  "type": "mm:output/weighted",
  "options": [
    { "weight": 3, "ingredient": { "type": "mm:item", "item": "minecraft:iron_nugget", "count": 2 } },
    { "weight": 1, "ingredient": { "type": "mm:item", "item": "minecraft:gold_nugget", "count": 1 } }
  ]
}
```

Each craft gives either the iron or the gold, iron three times as often.

### Input and output that scale together

```json
"inputs":  [{ "type": "mm:input/consume", "ingredient": { "type": "mm:item", "item": "minecraft:gold_ingot", "count": { "min": 1, "max": 5, "rollGroup": "yield" } } }],
"outputs": [{ "type": "mm:output/simple", "ingredient": { "type": "mm:item", "item": "minecraft:gold_nugget", "count": { "min": 4, "max": 8, "rollGroup": "yield" } } }]
```

Both ranges roll together, so taking more gold always gives more nuggets.

### Fluids

```json
{ "type": "mm:input/consume", "ingredient": { "type": "mm:fluid", "fluid": "minecraft:lava", "amount": 1000 } }
```

The port needs `"type": "mm:fluid"` with `rows`, `columns` and `slotCapacity` (mB per tank).

### Only in the Nether, only in rain

```json
"conditions": [
  { "type": "mm:dimension", "dimension": "minecraft:the_nether" },
  { "type": "mm:weather", "weather": "rain" }
]
```

### Only at night, only below Y 0

```json
"conditions": [
  { "type": "mm:time", "time": "night" },
  { "type": "mm:height", "maxY": 0 }
]
```

Biome, redstone and structure tier conditions are in [`reference.md`](reference.md#conditions).

### Running several crafts at once

Add `"parallelProcessing": true` to the recipe, and `"maxParallelRecipes": 4` to the controller or
structure. The machine runs as many copies as its ports can feed, up to the limit.

## Common structures

### Ports can go anywhere

Mark the port spots in the layout, then let the player choose which port goes where:

```json
{
  "portsAnywhere": true,
  "layout": [["PCP", "BBB"]],
  "key": {
    "B": { "block": "minecraft:bricks" },
    "P": { "portType": "mm:item" }
  }
}
```

Any item port fits either `P`.

### Flexible hatch spots

Let one spot take any of several ports, each with its own tiers, or plain casing:

```json
"F": { "anyOf": [
  { "portType": "mm:item",   "input": true, "maxTier": 8 },
  { "portType": "mm:fluid",  "input": true, "maxTier": 8 },
  { "portType": "mm:energy", "input": true, "maxTier": 4 },
  { "block": "minecraft:iron_block" }
] }
```

```js
layout.key('F', { anyOf: [
    { portType: 'mm:item', input: true, maxTier: 8 },
    { portType: 'mm:fluid', input: true, maxTier: 8 },
    { portType: 'mm:energy', input: true, maxTier: 4 },
    { block: 'minecraft:iron_block' }
] })
```

Every `F` can be any one of those. The machine uses whatever ports end up in it, so a player who
needs no fluids just builds casing there instead.

### Tiered parts

Give ports a `tierRank` in their config:

```json
{ "id": "basic_power",    "type": "mm:energy", "config": { "capacity": 10000,  "maxReceive": 100,  "maxExtract": 0, "tierRank": 1 } }
{ "id": "advanced_power", "type": "mm:energy", "config": { "capacity": 100000, "maxReceive": 1000, "maxExtract": 0, "tierRank": 2 } }
```

Then have the structure ask for a minimum:

```json
"E": { "portType": "mm:energy", "minTier": 2 }
```

Only the advanced port (or better) forms this machine.

### A choice of casing

```json
{
  "stateLists": {
    "casing": {
      "iron": { "block": "minecraft:iron_block" },
      "gold": { "block": "minecraft:gold_block" }
    }
  },
  "key": { "X": { "stateList": "casing" } }
}
```

Each `X` can be either block.

### Copying a build from the world

Build the machine in a world, take the **Multiblock Saver**, right-click two opposite corners, then
right-click the air. The structure is saved to `config/mm/structures/` as JSON and as a KubeJS
script, ready to edit and drop into your pack.

## Making it look good

### Textures

```json
{
  "id": "alloy_kiln",
  "type": "mm:machine",
  "name": "Alloy Kiln",
  "texture": "kubejs:block/kiln_base",
  "overlay": "kubejs:block/kiln_front"
}
```

With KubeJS, the image goes in `kubejs/assets/kubejs/textures/block/kiln_base.png`. Ports take the
same keys, plus `inputTexture` / `outputTexture` and `inputOverlay` / `outputOverlay` to style each
side differently.

### Names

```json
"name": "Kiln Hatch",
"inputName":  { "text": "Kiln Intake",  "color": "#FF8800", "bold": true },
"outputName": { "text": "Kiln Exhaust", "gradient": ["#FFC72C", "#DA291C"] }
```

```js
event.create('kjs_kiln')
    .type('mm:machine')
    .name({ text: 'Alloy Kiln', rainbow: true, spread: 0 })
```

`spread: 0` makes the whole name one colour that cycles; leave it out for colours that travel along
the name.

## More

- [`reference.md`](reference.md) — every key and option.
- [`entity.md`](entity.md) — machines that use mobs.
- [`mekanism.md`](mekanism.md) — Mekanism chemicals and heat.
- [`replication.md`](replication.md) — Replication matter.
- [`ae2.md`](ae2.md) — AE2 autocrafting and the Network Linker.
- [`ars-nouveau.md`](ars-nouveau.md) — Ars Nouveau Source.
- [`projecte.md`](projecte.md) — ProjectE EMC.
- [`single-block.md`](single-block.md) — machines in one block, like a furnace.
