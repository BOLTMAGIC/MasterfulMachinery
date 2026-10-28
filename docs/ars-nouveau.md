# Ars Nouveau Source and Masterful Machinery

How to make machines that use Source from the
[Ars Nouveau](https://www.curseforge.com/minecraft/mc-mods/ars-nouveau) mod. The port type only
exists when Ars Nouveau is installed.

## The port

| Port type | What it holds |
|---|---|
| `mm:ars_nouveau/source` | Source, the same number Ars jars show |

## Port config

```json
{
  "id": "source_port",
  "controllerIds": "mm:enchanter",
  "name": "Source Port",
  "type": "mm:ars_nouveau/source",
  "config": {
    "capacity": 10000,
    "range": 6
  }
}
```

| Option | Default | Meaning |
|---|---|---|
| `capacity` | required | The most Source the port can hold |
| `range` | `6` | How many blocks away it looks for jars to trade with |

## Recipe ingredient

```json
{ "type": "mm:ars_nouveau/source", "source": 500 }
```

`source` takes a number, or `{ "min": 100, "max": 500 }` for a random amount like other ports.
Works as an input and as an output.

With `"per_tick": true` on the entry, the amount is taken **every tick**, not spread across the
recipe. A 40 tick recipe with `"source": 100` and `per_tick` costs 4,000 Source.

## How Source gets in and out

An **input** port fills itself:

- It pulls from Source Jars within `range`.
- Sourcelinks (volcanic, agronomic and the rest) push into it on their own.
- Ars machines cannot drain an input port, so a machine's fuel is safe.

An **output** port gives Source away:

- It fills Source Jars within `range`.
- Ars machines that draw from nearby Source, like the Imbuement Chamber and the Enchanting
  Apparatus, take from it directly.

**Source relays cannot be pointed at a port.** Ars only lets relays bind to its own blocks. Use a
jar next to the port, or a sourcelink, and relay to that instead.

## Seeing how full it is

A Source port shows its contents three ways:

- The block brightens as it fills, so a wall of ports can be read at a glance.
- Right-click it for a bar, and hover the bar for the exact numbers.
- Jade shows a bar when you look at it.

## From KubeJS

```js
// kubejs/startup_scripts/
MMEvents.registerPorts(event => {
    event.create('source_port')
        .name('Source Port')
        .controllerId('mm:enchanter')
        .config('mm:ars_nouveau/source', config => {
            config.capacity(10000)
            config.range(6)
        })
})
```

## See also

- [`reference.md`](reference.md) — every key and option.
- [`replication.md`](replication.md) — Replication matter.
- [`projecte.md`](projecte.md) — ProjectE EMC.
