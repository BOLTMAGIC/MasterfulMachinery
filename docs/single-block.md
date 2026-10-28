# Single-block machines

A single-block machine is a whole machine in one block, like a furnace. It holds its own items,
fluids, energy and chemicals, runs normal MM recipes, and builds its screen from the config. No
structure or ports are needed.

## The controller

```json
{
  "id": "crusher",
  "type": "mm:single",
  "name": "Crusher",
  "slots": [
    { "id": "ore", "type": "mm:item", "input": true, "config": { "rows": 1, "columns": 1 } },
    { "id": "power", "type": "mm:energy", "input": true, "config": { "capacity": 100000, "maxReceive": 1000, "maxExtract": 0 } },
    { "id": "dust", "type": "mm:item", "input": false, "config": { "rows": 2, "columns": 2 } },
    { "id": "waste", "type": "mm:fluid", "input": false, "config": { "rows": 1, "columns": 1, "slotCapacity": 8000 } }
  ]
}
```

It goes in `config/mm/controllers/` like any controller and takes the same keys (`texture`,
`overlay`, `model`, `workingSound` and the rest). `type` is `mm:single`, and `slots` lists what the
machine holds.

| Slot key | Meaning |
|---|---|
| `id` | A name for the slot, unique in this machine. The screen shows it when you hover the slot |
| `type` | `mm:item`, `mm:fluid`, `mm:energy` or `mm:mekanism/chemical` |
| `input` | `true` for an input slot, `false` for an output slot |
| `config` | The same options as a port of that type. See [Port types](reference.md#port-types) |

Other port types can not be used as slots. The game stops at load and names the slot if one is.

## Recipes

A single-block machine's recipes use its controller id as the `structureId`:

```json
{
  "structureId": "mm:crusher",
  "ticks": 100,
  "inputs": [
    { "type": "mm:input/consume", "ingredient": { "type": "mm:item", "item": "minecraft:raw_iron", "count": 1 } },
    { "type": "mm:input/consume", "per_tick": true, "ingredient": { "type": "mm:energy", "amount": 2000 } }
  ],
  "outputs": [
    { "type": "mm:output/simple", "ingredient": { "type": "mm:item", "item": "minecraft:raw_iron", "count": 2 } }
  ]
}
```

Everything recipes can do works here: chances, ranges, `per_tick`, weighted outputs, conditions,
`requestOnly` and the KubeJS recipe events.

## Pipes, hoppers and cables

Pipes, hoppers, cables and tubes connect to any side. They fill the input slots and take from the
output slots. They can not take from an input slot or put into an output slot.

Energy output slots give energy only up to their `maxExtract`.

Each fluid stays in one tank of a fluid slot, so one fluid can not fill every tank and block the
others. To decide which fluids a slot takes, give it `fluids` in its config:

```json
{ "id": "fuel", "type": "mm:fluid", "input": true, "config": { "rows": 1, "columns": 1, "slotCapacity": 8000, "fluids": ["minecraft:lava"] } }
```

## The screens

Right-click the block for its slots. The button in the top corner opens the controller screen, with
the name, redstone mode, recipe order, sound and the AE2 link. The chest button there goes back to
the slots.

The slots screen lays the slots out on its own:

- Inputs on the left, the progress bar in the middle, outputs on the right.
- Energy goes on the outer edges, then fluid and chemical tanks, then item slots next to the
  progress bar. Inside each group the slots keep the order of the config.
- When there is room under the tanks, an item grid goes there in a flatter shape to save width.
- The screen grows wider to fit big machines.

## From KubeJS

```js
// kubejs/startup_scripts/
MMEvents.registerControllers(event => {
    event.create('crusher')
        .type('mm:single')
        .name('Crusher')
        .slot('ore', 'mm:item', true, c => { c.rows(1); c.columns(1) })
        .slot('power', 'mm:energy', true, c => { c.capacity(100000); c.maxReceive(1000); c.maxExtract(0) })
        .slot('dust', 'mm:item', false, c => { c.rows(2); c.columns(2) })
        .slot('waste', 'mm:fluid', false, c => { c.rows(1); c.columns(1); c.slotCapacity(8000) })
})
```

`.slot(id, type, input, config)` takes the same config methods as a port of that type.

## See also

- [`reference.md`](reference.md) — every key and option.
- [`example.md`](example.md) — building a multiblock machine.
