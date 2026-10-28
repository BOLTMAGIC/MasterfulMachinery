# Mekanism and Masterful Machinery

How to use Mekanism chemicals and heat in MM machines on 1.21.1.

## What changed in 1.21.1

Mekanism merged gas, infusion, pigment and slurry into a **single chemical registry**. There is no
longer any difference between them — `mekanism:hydrogen` and `mekanism:bio` are both just chemicals.

MM follows that: there is **one** chemical port.

| Port type | What it holds |
|---|---|
| `mm:mekanism/chemical` | Any chemical — former gases, slurries, pigments and infusion types |
| `mm:mekanism/heat` | Mekanism heat |

The old `mm:mekanism/gas`, `/slurry`, `/pigment` and `/infuse` types **no longer exist**. See
[Migrating](#migrating-from-the-old-port-types) at the bottom.

## Chemical port

### Port config

```json
{
  "id": "chemical_hatch",
  "controllerIds": "mm:my_machine",
  "name": "Chemical Hatch",
  "type": "mm:mekanism/chemical",
  "config": {
    "capacity": 64000
  }
}
```

`capacity` is how much the tank holds, in mB. It is the only option.

From KubeJS: `.capacity(64000)`.

### Recipe ingredient

```json
{ "type": "mm:mekanism/chemical", "chemical": "mekanism:hydrogen", "amount": 1000 }
```

`amount` is in mB. `chemical` is the registry id of any chemical.

The old field names still work, so a recipe ported from the four-port days only needs its `type`
changed:

```json
{ "type": "mm:mekanism/chemical", "gas": "mekanism:hydrogen", "amount": 1000 }
```

`chemical`, `gas`, `slurry`, `pigment` and `infuse` are all accepted as the field name.

## Heat port

```json
{
  "id": "heat_hatch",
  "controllerIds": "mm:my_machine",
  "name": "Heat Hatch",
  "type": "mm:mekanism/heat",
  "config": {
    "capacity": 10000,
    "heatCapacity": 1000.0,
    "inverseConduction": 1.0,
    "autoPush": true
  }
}
```

| Option | Default | Meaning |
|---|---|---|
| `capacity` | required | How much heat the port stores |
| `heatCapacity` | `1000.0` | How far the temperature moves per unit stored |
| `inverseConduction` | Mekanism's default | How fast heat crosses the boundary |
| `autoPush` | `true` | Output ports push heat into neighbours |

Note `autoPush` defaults to **true** here, unlike the energy port where it defaults to false.

Recipe ingredient:

```json
{ "type": "mm:mekanism/heat", "amount": 500 }
```

Ranges and `rollGroup` work on `amount` like everywhere else.

## Custom chemicals

Register into the unified `mekanism:chemical` registry in a **startup script**:

```js
StartupEvents.registry('mekanism:chemical', event => {
    const Chemical = Java.loadClass('mekanism.api.chemical.Chemical')
    const ChemicalBuilder = Java.loadClass('mekanism.api.chemical.ChemicalBuilder')
    event.createCustom('kubejs:propane', () => new Chemical(
        ChemicalBuilder.builder().tint(0xfaf6e3)
    ))
})
```

Then use it in a recipe like any other chemical:

```json
{ "type": "mm:mekanism/chemical", "chemical": "kubejs:propane", "amount": 500 }
```

You will see snippets that write `new $Chemical($ChemicalBuilder.builder())`. Those `$`-prefixed
names come from ProbeJS's generated typings — they work in an IDE and in packs that have ProbeJS,
but throw `ReferenceError: "$Chemical" is not defined` otherwise. `Java.loadClass` always works.

Give the chemical a name with a lang entry, or it shows as a raw key:

```json
{ "chemical.kubejs.propane": "Propane" }
```

## Things that work that you might not expect

- **Radioactive chemicals.** Fissile fuel, nuclear waste, plutonium and polonium all go into a
  chemical port. MM's tank accepts every chemical attribute, so a machine can burn fissile fuel.
- **Any chemical in any chemical port.** There is no per-kind filtering, because Mekanism no longer
  separates the kinds.
- **Pressurized tubes** push into an input port normally.

## Migrating from the old port types

1. In every port config, change `"type": "mm:mekanism/gas"` (or `/slurry`, `/pigment`, `/infuse`)
   to `"type": "mm:mekanism/chemical"`.
2. Do the same for the `type` on every Mekanism recipe ingredient.
3. Leave the field names alone — `gas`, `slurry`, `pigment` and `infuse` still resolve.

A config that still names a removed port type stops the game on startup with a message naming the
file and listing the port types that do exist.

Ports placed in a world as one of the removed types will not load, because their blocks no longer
register.

## From KubeJS

```js
// kubejs/startup_scripts/
MMEvents.registerPorts(event => {
    event.create('chemical_hatch')
        .name('Chemical Hatch')
        .controllerId('mm:my_machine')
        .config('mm:mekanism/chemical', config => {
            config.capacity(64000)
            config.autoPush(true)
        })
})
```

The heat port takes `.capacity(...)`, `.heatCapacity(...)`, `.inverseConduction(...)` and
`.autoPush(...)` instead. **`autoPush` does not exist on every port type** — asking for it on one
that has none crashes the game rather than warning you.

```js
// kubejs/server_scripts/
MMEvents.createProcesses(event => {
    event.create('kubejs:hydrogen_burn')
        .structureId('kubejs:my_structure')
        .ticks(40)
        .input({
            type: 'mm:input/consume',
            ingredient: { type: 'mm:mekanism/chemical', chemical: 'mekanism:hydrogen', amount: 1000 }
        })
        .output({
            type: 'mm:output/simple',
            ingredient: { type: 'mm:item', item: 'minecraft:blaze_powder', count: 1 }
        })
})
```

### Where the script goes

Registering a port is a **startup** event, so it belongs in `kubejs/startup_scripts/`. Processes are
a **server** event and belong in `kubejs/server_scripts/`. Put either in the wrong folder and it
silently never runs.

Controllers and ports register under `mm:`: `event.create('my_port')` makes `mm:my_port_input` and
`mm:my_port_output`, and `.controllerId(...)` takes `mm:<controller id>`. Structures and processes
need the namespace written out: `event.create('kubejs:my_structure')`.

If you call a method that does not exist, KubeJS does not log a warning — it kills the game on
startup with `TypeError: Cannot find function ... in object <SomeBuilder>`. The builder named in
that message tells you which port type you were configuring. Names are case sensitive.

## See also

- [`replication.md`](replication.md) — Replication matter, including custom matter types.
- [`entity.md`](entity.md) — entity ports, for machines that work with mobs.
