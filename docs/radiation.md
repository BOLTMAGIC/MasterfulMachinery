# Nuclear Radiation and Masterful Machinery

How to make machines that store and use radiation from the
[Nuclear Radiation](https://www.curseforge.com/minecraft/mc-mods/nuclear-radiation) mod. The port
type only exists when Nuclear Radiation is installed.

## The port

| Port type | What it holds |
|---|---|
| `mm:nuclear_radiation/radiation` | Radioactive isotopes, measured in Bq |

**Bq** (becquerel) is how strongly something is radiating — the number the mod's Geiger counter
shows. It is not an amount of stuff like mB or items, and numbers get big fast: 1 kBq is 1,000 Bq,
1 MBq is 1,000,000 Bq.

## Port config

```json
{
  "id": "rad_port",
  "controllerIds": "mm:reactor",
  "name": "Radiation Port",
  "type": "mm:nuclear_radiation/radiation",
  "config": {
    "capacity": 1000000000,
    "isotopes": ["nr:u_235", "nr:pu_239"],
    "decay": true,
    "shielded": true
  }
}
```

| Option | Default | Meaning |
|---|---|---|
| `capacity` | required | The most Bq the port can hold, all isotopes together |
| `isotopes` | any | Only accept these isotopes |
| `decay` | `true` | Stored radiation decays at the isotope's real half-life |
| `shielded` | `true` | When `false`, a port holding radiation irradiates the area around it |
| `carriers` | any | Items an output port can load radiation onto — item ids or `#tags` |
| `loadPerItem` | `1000000000000` | How many Bq an output port puts on each item (1 TBq) |

Isotope ids start with `nr:` — `nr:u_235`, `nr:u_238`, `nr:pu_239`, `nr:cs_137`, `nr:co_60` and so
on. JEI lists every isotope the mod knows.

## Getting radiation in

The **input** port has one slot. Put a radioactive item in — anything the mod marks as radioactive —
and the port absorbs its radiation and uses the item up. Hoppers and pipes can feed the slot.

An item only goes in if the port accepts every isotope in it and has room for at least one of it.

## Recipe ingredient

Ask for a specific isotope:

```json
{ "type": "mm:nuclear_radiation/radiation", "isotope": "nr:u_235", "amount": 500000 }
```

Or leave `isotope` out to take any radiation in the port:

```json
{ "type": "mm:nuclear_radiation/radiation", "amount": 500000 }
```

`amount` is in Bq. Chance, `per_tick`, ranges and `rollGroup` all work like on any other
ingredient:

```json
{ "type": "mm:input/consume", "per_tick": true, "ingredient": { "type": "mm:nuclear_radiation/radiation", "amount": 2000 } }
{ "type": "mm:input/consume", "chance": 0, "ingredient": { "type": "mm:nuclear_radiation/radiation", "amount": 5000000 } }
```

The second one needs 5 MBq in the port to run but never takes any.

## Making radiation

A recipe can put radiation into an **output** port. Outputs must name an isotope:

```json
{ "type": "mm:output/simple", "ingredient": { "type": "mm:nuclear_radiation/radiation", "isotope": "nr:cs_137", "amount": 250000 } }
```

## Getting radiation out

The **output** port has two slots. Put an item in the left slot and the port loads `loadPerItem` Bq
of its radiation onto it, then moves it to the right slot. The loaded item is radioactive like any
other: it gives dose when carried, and another machine's radiation input port absorbs it. That is
how radiation travels from one machine to the next.

- `carriers` limits which items can be loaded — `"carriers": ["minecraft:glass_bottle", "#c:ingots"]`.
  Leave it out to allow any item.
- Items that are already radioactive are never loaded.
- The port only loads an item once it holds a full `loadPerItem`, and loads one item per tick.
- Loaded items collect into one stack in the port. Once taken out they decay like any other
  radioactive item, so stacks loaded at different times may not combine.
- Hoppers and pipes insert into the left slot and take from the right one.

## Decay

With `decay` on, stored radiation fades at the isotope's real half-life, so short-lived isotopes
like I-131 disappear within in-game days while U-238 barely changes. A recipe that needs more than
is left stops until the port is topped up. Set `"decay": false` to keep radiation as it is.

## Danger

A port with `"shielded": false` gives off radiation from its contents like any radioactive block,
so players nearby take dose and need shielding or a hazmat suit. Shielded ports are safe.

## From KubeJS

```js
// kubejs/startup_scripts/
MMEvents.registerPorts(event => {
    event.create('rad_port')
        .name('Radiation Port')
        .controllerId('mm:reactor')
        .config('mm:nuclear_radiation/radiation', config => {
            config.capacity(1e9)
            config.isotope('nr:u_235')
            config.isotope('nr:pu_239')
            config.decay(true)
            config.shielded(true)
            config.carrier('minecraft:glass_bottle')
            config.loadPerItem(1e12)
        })
})
```

```js
// kubejs/server_scripts/
MMEvents.createProcesses(event => {
    event.create('kubejs:enrich')
        .structureId('kubejs:reactor')
        .ticks(200)
        .input({ type: 'mm:input/consume', ingredient: { type: 'mm:nuclear_radiation/radiation', isotope: 'nr:u_235', amount: 500000 } })
        .output({ type: 'mm:output/simple', ingredient: { type: 'mm:nuclear_radiation/radiation', isotope: 'nr:cs_137', amount: 250000 } })
})
```

Call `config.isotope(...)` once per isotope to restrict the port; leave it out to accept any.

## See also

- [`reference.md`](reference.md) — every key and option.
- [`example.md`](example.md) — building a machine step by step.
