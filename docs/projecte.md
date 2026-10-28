# ProjectE EMC and Masterful Machinery

How to make machines that store and use EMC from the
[ProjectE](https://www.curseforge.com/minecraft/mc-mods/projecte) mod. The port type only exists
when ProjectE is installed.

## The port

| Port type | What it holds |
|---|---|
| `mm:projecte/emc` | EMC, the same number ProjectE shows |

## Port config

```json
{
  "id": "emc_port",
  "controllerIds": "mm:transmuter",
  "name": "EMC Port",
  "type": "mm:projecte/emc",
  "config": {
    "capacity": 1000000,
    "kleinSlot": true,
    "kleinRate": 2048
  }
}
```

| Option | Default | Meaning |
|---|---|---|
| `capacity` | required | The most EMC the port can hold |
| `kleinSlot` | `false` | Gives the port a slot for a Klein Star |
| `kleinRate` | `0` | The most EMC moved into or out of the Klein Star each tick. `0` means no limit |

## Recipe ingredient

```json
{ "type": "mm:projecte/emc", "emc": 2048 }
```

`emc` takes a number, or `{ "min": 1000, "max": 4000 }` for a random amount like other ports.
Works as an input and as an output.

With `"per_tick": true` on the entry, the amount is **spread across the recipe**, the same as
energy. A 40 tick recipe with `"emc": 4000` and `per_tick` takes 100 EMC each tick. The recipe only
starts when the whole amount is in the ports.

EMC costs are fixed numbers. They do not follow the EMC value of items.

## How EMC gets in and out

An **input** port fills itself:

- Anti-Matter Relays and Energy Collectors placed against it push EMC in on their own.
- Nothing can pull EMC back out of an input port.

An **output** port gives EMC away:

- Every tick it pushes its EMC into the blocks touching it that take EMC, such as relays. It splits
  the EMC evenly between them.
- Nothing can push EMC into an output port.

## Klein Stars

With `kleinSlot` on, the port's screen has a slot for a Klein Star:

- On an **input** port the star is emptied into the port.
- On an **output** port the star is charged from the port.

`kleinRate` sets how fast. Leave it out to move as much as fits each tick. If the port is broken,
the star drops.

## Seeing how full it is

An EMC port shows its contents three ways:

- The block brightens as it fills.
- Right-click it for a bar, and hover the bar for the exact numbers.
- Jade shows the stored EMC when you look at it.

## From KubeJS

```js
// kubejs/startup_scripts/
MMEvents.registerPorts(event => {
    event.create('emc_port')
        .inputName({ text: 'EMC Input Port' })
        .outputName({ text: 'EMC Output Port' })
        .controllerId('mm:transmuter')
        .config('mm:projecte/emc', config => {
            config.capacity(1000000)
            config.kleinSlot(true)
            config.kleinRate(2048)
        })
})
```

## See also

- [`reference.md`](reference.md) — every key and option.
- [`ars-nouveau.md`](ars-nouveau.md) — Ars Nouveau Source.
