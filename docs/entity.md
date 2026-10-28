# Entity ports and Masterful Machinery

How to make a machine work with mobs.

## The port

| Port type | What it holds |
|---|---|
| `mm:entity` | Mobs, either swallowed into the block or standing on top of it |

A mob that walks into the port's zone is picked up on its own — there is no item to insert and no
tool to use. What happens next is the port's `mode`:

- **`stored`** — the mob is swallowed into the block. It stops ticking, cannot be hurt, cannot
  despawn, and is not in the world any more.
- **`standing`** — the mob stays on top of the block and is registered there, where you can see it. It can be killed.

## Port config

```json
{
  "id": "mob_pedestal",
  "controllerIds": "mm:my_machine",
  "name": "Mob Pedestal",
  "type": "mm:entity",
  "config": {
    "capacity": 3,
    "mode": "standing",
    "invulnerable": true,
    "immobile": true,
    "silent": true,
    "consume": false,
    "speedPerEntity": 1.0,
    "zone": { "width": 3, "height": 1, "depth": 3 },
    "entities": ["minecraft:chicken"]
  }
}
```

| Option | Default | Meaning |
|---|---|---|
| `capacity` | `1` | How many mobs the port holds at once |
| `mode` | `stored` | `stored` or `standing` |
| `invulnerable` | `false` | Nothing can hurt a mob the port is holding |
| `immobile` | `true` | Pins the mob in place with its AI switched off |
| `silent` | `false` | Mutes the mob's own sounds while it is held |
| `persistent` | see below | Whether a held mob may despawn |
| `consume` | see below | Whether a recipe destroys the mob or keeps it |
| `speedPerEntity` | `0` | How much each extra mob speeds the recipe up |
| `zone` | `1` | How much space above the block the port watches |
| `entities` / `tags` | any | Restricts what the port will accept |

Two options follow the port rather than a fixed default:

- **`consume`** defaults to on for `stored` and off for `standing`. So a pedestal runs forever off
  one mob without any extra config, and a capture hatch eats what it takes.
- **`persistent`** defaults to on for an **input** port, so automation cannot quietly break, and off
  for an **output** port, so mobs a machine spawned despawn normally instead of piling up.

All of these work from KubeJS under the same names: `.capacity(3)`, `.mode('standing')`,
`.invulnerable(true)`, `.immobile(true)`, `.silent(true)`, `.persistent(true)`, `.consume(false)`,
`.speedPerEntity(1.0)`, `.zone(3, 1, 3)`, `.entity('minecraft:chicken')`, `.tag('minecraft:raiders')`.

## Recipe ingredient

Name one mob:

```json
{ "type": "mm:entity", "entity": "minecraft:zombie", "amount": 1 }
```

Or a whole entity tag:

```json
{ "type": "mm:entity", "tag": "minecraft:skeletons", "amount": 1 }
```

`amount` is how many mobs, and defaults to `1` if you leave it out. Ranges and `rollGroup` work on
it like everywhere else. JEI shows the mob's spawn egg for the ingredient.

### Consuming versus using

Whether the recipe destroys the mob is decided by the **port**, not the recipe. A port with
`consume: false` lets the same mob drive the recipe over and over, which is what makes a pedestal
machine work. A port with `consume: true` eats one per craft.

## Producing mobs

An entity port on the **output** side makes mobs instead of taking them:

```json
"outputs": [
  { "type": "mm:output/simple", "ingredient": { "type": "mm:entity", "entity": "minecraft:zombie", "amount": 1 } }
]
```

- In `standing` mode the mob spawns on top of the port.
- In `stored` mode it goes into the block, and only comes out when the block is broken.

For anything spawner-shaped, set **`immobile: false`** on the output port. The default is `true`,
which is right for a pedestal but wrong here — the mob would spawn with its AI off and stand on the
pad forever.

`capacity` then acts as a throttle rather than storage: the port tracks the mobs it spawned while
they are still in its zone, and makes more as they wander out. Without that a fast recipe would
spawn mobs without limit.

Mobs made this way get default NBT, so a zombie is a plain adult zombie — no gear, no variants.

## The zone

`zone` sizes the area the port watches, sitting on top of the block. It defaults to the single
block above.

| Written as | Shape |
|---|---|
| `"zone": 3` | 3x3x3 cube |
| `"zone": [3, 1, 3]` | Flat 3x3 pad, one floor |
| `"zone": [1, 3, 1]` | A 3-tall chimney, for mobs falling down a shaft |
| `{ "width": 3, "height": 1, "depth": 3 }` | The same, spelled out |

Width and depth centre on the port; height starts at its top face and grows upward. 16 is the
maximum on any side.

Two behaviours follow from the zone rather than being separate settings: on a 1x1x1 zone a
registered mob snaps to the block centre, and on any larger zone it stays where it is standing. A
mob is dropped from the port when it leaves the zone, so a bigger zone is a bigger leash.

## Speed

`speedPerEntity` makes a crowd worth having — every mob past the first adds that much speed.

| `speedPerEntity` | 1 mob | 2 mobs | 3 mobs |
|---|---|---|---|
| `0` (default) | 1x | 1x | 1x |
| `0.5` | 1x | 1.5x | 2x |
| `1.0` | 1x | 2x | 3x |

`capacity` is the ceiling, so capacity 3 at `1.0` tops out at 3x. The rate is per port, not per mob
type — every mob on the same port is worth the same.

## Notes

- **Breaking the port gives the mobs back.** Stored mobs respawn on the spot with their health,
  name, gear and taming intact; a pinned mob gets its AI and its vulnerability back.
- **A port captures whether or not it is part of a formed machine**, which is how you preload one
  before building the rest.
- **`invulnerable` is off by default.** Turning it on means nothing can kill that mob, including
  other mods' mechanics.
- Players are never captured.

## From KubeJS

```js
// kubejs/startup_scripts/
MMEvents.registerPorts(event => {
    event.create('mob_pedestal')
        .name('Mob Pedestal')
        .controllerId('mm:my_machine')
        .config('mm:entity', config => {
            config.capacity(3)
            config.mode('standing')
            config.invulnerable(true)
            config.immobile(true)
            config.silent(true)
            config.persistent(true)
            config.consume(false)
            config.speedPerEntity(1.0)
            config.zone(3, 1, 3)
            config.entity('minecraft:chicken')
            config.tag('minecraft:raiders')
        })
})
```

`config.zone(3)` is the short form for a cube. Every option above is optional; the defaults are in
the table further up.

```js
// kubejs/server_scripts/
MMEvents.createProcesses(event => {
    event.create('kubejs:chicken_eggs')
        .structureId('kubejs:mob_altar')
        .ticks(40)
        .input({
            type: 'mm:input/consume',
            ingredient: { type: 'mm:entity', entity: 'minecraft:chicken', amount: 1 }
        })
        .output({
            type: 'mm:output/simple',
            ingredient: { type: 'mm:item', item: 'minecraft:egg', count: 1 }
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

### One side only

By default a port definition registers **both** an input and an output block. `only` restricts it
to one:

```json
{ "id": "mob_intake", "type": "mm:entity", "only": "input", "config": { } }
```

```js
event.create('mob_intake').only('input').config('mm:entity', config => { })
```

Accepts `input`, `output` or `both` (the default). With `only: "input"` the `_output` block is
never registered, so a structure referring to it will fail to load.

### Naming and colour

A port's `name` gets " Input" / " Output" appended, which gives "Basic Chemical Port Input". To
control the whole name per side instead, use `inputName` / `outputName` — they replace the name for
that side, suffix included:

```json
{
  "name": "Basic Chemical Port",
  "inputName": "Basic Chemical Input Port",
  "outputName": "Basic Chemical Output Port"
}
```

Any name field also accepts a full text component, so names can be coloured and formatted:

```json
"inputName": { "text": "Basic Chemical Input Port", "color": "#55FFFF", "bold": true }
```

Two extra forms build the colours for you. A **gradient** spreads colours across the text:

```json
"inputName": { "text": "Gradient Input Port", "gradient": ["#FF0000", "#FFFF00", "#00FFFF"] }
```

Any number of stops is allowed and the colours are interpolated evenly across the characters.

A **rainbow** cycles through the spectrum and animates on its own:

```json
"outputName": { "text": "Rainbow Output Port", "rainbow": true, "speed": 0.5, "spread": 0.08 }
```

`speed` is full colour cycles per second (default `0.5`) and `spread` is how far the hue shifts per
character (default `0.05`), so a small spread makes the whole name pulse together and a larger one
makes the colours travel along it.

From KubeJS, `.name(...)`, `.inputName(...)` and `.outputName(...)` take a string or an object with
the same fields. Controllers take the same in `.name(...)`:

```js
MMEvents.registerPorts(event => {
    event.create('service_counter')
        .name('Service Counter')
        .inputName({ text: 'Service Counter Input', rainbow: true, speed: 0.5, spread: 0.08 })
        .outputName({ text: 'Service Counter Output', gradient: ['#FFC72C', '#DA291C'] })
        .controllerId('mm:my_machine')
        .config('mm:item', config => { config.rows(1); config.columns(1) })
})
```

## See also

- [`mekanism.md`](mekanism.md) — Mekanism chemicals and heat.
- [`replication.md`](replication.md) — Replication matter, including custom matter types.
