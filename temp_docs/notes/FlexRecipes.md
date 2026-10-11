# Flex Recipes and Count-Scaled Outputs

Examples: [`examples/flex/flex_recipes.js`](../examples/flex/flex_recipes.js) (13 recipes, fixed vs flex vs count),
[`examples/solar_array/`](../examples/solar_array/) (a generator whose output scales with the panels inside).

### When do you need what?
| The output... | Use |
|---|---|
| is always the same item | a normal (`fixed`) recipe |
| gets its NAME from the input item (seed -> its essence, log -> its planks) | a `flex` recipe |
| gets its AMOUNT from how many items are in the ports (10 panels = 10M FE) | `mm:input/count` + `scale_by` |

## Recipe mode

Recipes have `"mode": "fixed" | "flex"`. `fixed` is the default, so existing recipes don't change.

```json
{
  "structureId": "mm:essence_forge", "ticks": 100, "mode": "flex",
  "inputs": [
    { "type": "mm:input/flex", "as": "seed", "max_types": 3, "chance": 0,
      "ingredient": { "type": "mm:item", "tag": "mysticalagriculture:seeds", "count": 1 } }
  ],
  "outputs": [
    { "type": "mm:output/flex", "from": "seed", "match": "(.+)_seeds", "item": "{ns}:$1_essence", "count": 4 }
  ]
}
```

KubeJS:
```js
MMEvents.createProcesses(event => {
  event.create('mm:seeds_to_essence')
    .structureId('mm:essence_forge')
    .ticks(100)
    .mode('flex')
    .flexInput('seed', { type: 'mm:item', tag: 'mysticalagriculture:seeds', count: 1 }, 3, 0) // as, ingredient, max kinds, chance
    .flexOutput('seed', '(.+)_seeds', '{ns}:$1_essence', 4)                                     // from, match, item, count
})
```

### `mm:input/flex`
Takes **every kind** of matching item in the input ports in one run (`count` of each kind) and remembers the kinds
under the name `as`. Mixed seeds go in together and their outputs come out together at the end of the run.

| Field | Default | |
|---|---|---|
| `as` | required | name the outputs refer to with `from` |
| `ingredient` | required | item ingredient (`item` or `tag`), `count` is taken per kind |
| `max_types` | no limit | most kinds per run |
| `chance` | `1` | like `mm:input/consume`; `0` keeps the items in the port |
| `exclude` | none | `["mod:item", "#some:tag", "modid:*"]` are never taken |
| `min_tier` / `max_tier` | none | Mystical Agriculture seeds only: keep crop tiers in this range |

### `mm:output/flex`
One output per kind the flex input took.

| Field | Default | |
|---|---|---|
| `from` | required | the flex input's `as` |
| `match` | `(.*)` | regex on the input id's path (the part after `:`), the whole path must match |
| `item` | required | output id; `{ns}` = input namespace, `{path}` = input path, `$1`..`$n` = regex groups |
| `count` | `1` | |
| `copy_nbt` | `false` | keep the input's NBT (enchantments, name, damage) |
| `chance`, `per_tick`, `scale_by` | | as on `mm:output/simple` |

If the output item doesn't exist, that input kind isn't accepted and stays in the port.

### `mm:input/flex_match`
Each kind also needs the item its template names, e.g. the seed's crux block. Kinds whose matching item isn't in
the ports wait for a later run.
```js
.flexMatch('seed', '{ma:crux}', 1, 0) // from, item template, count, chance (0 = not consumed)
```

### Outputs must fit
Before a run starts, the outputs of all kinds are put into copies of the output ports, so they are known to fit
together. A kind that wouldn't fit waits for a later run; the others still start.

### Script overrides
Pick the output for single items, or reject them. Results are cached per item kind until `/reload`. Server side
only: JEI shows the template results.
```js
MMEvents.flexOutput('mm:seeds_to_essence', event => {
  if (event.input.id == 'mysticalagriculture:cursed_seeds') event.reject()
  else if (event.input.id == 'mysticalagriculture:inferium_seeds') event.setOutput('mysticalagriculture:inferium_essence')
})
```

## Mystical Agriculture (optional)
MA isn't a dependency; its API is read when it's installed.
- `{ma:essence}` / `{ma:crux}` as an item template: the seed's essence / crux straight from MA (crux blocks don't
  follow a naming pattern, e.g. allthemodium seeds -> `allthemodium:allthemodium_block`).
- `min_tier` / `max_tier` on a flex input keep only seeds of those crop tiers.
- Seeds of crops disabled in MA's config are never taken.
```js
.input({ type: 'mm:input/flex', as: 'seed', ingredient: { type: 'mm:item', tag: 'mysticalagriculture:seeds', count: 1 },
         chance: 0, min_tier: 6, max_tier: 6 })
.flexMatch('seed', '{ma:crux}', 1, 0)
.flexOutput('seed', '(.+)', '{ma:essence}', 64)
```

## Cost per kind
The other consumed inputs of a flex recipe (energy, fluids, items...) are paid once per run, multiplied by a factor
that depends on how many kinds the run takes. Default in `mm-common.toml`:
```toml
[flex]
    # LINEAR: cost x kinds (default) / ONCE: same cost for any number of kinds / TABLE: from costTable
    costScaling = "LINEAR"
    # TABLE: "kinds=multiplier" steps, e.g. 1-3 kinds x1, 4-6 x2, 7+ x3.5
    costTable = ["1=1", "4=2", "7=3.5"]
```
A recipe overrides it with `"flex_cost": "once"` or `"flex_cost": {"mode": "table", "table": {"1": 1, "4": 2}}`
(KubeJS: `.flexCost('once')` / `.flexCost({ mode: 'table', table: { '1': 1, '4': 2 } })`).

If the inputs can't be paid for every kind, the run takes fewer kinds instead of not starting. Amounts are rounded
up. The number of kinds is also stored under the flex input's `as` name, so outputs can `scale_by` it.

## Count + `scale_by` (fixed recipes too)
```json
{
  "structureId": "mm:solar_array", "ticks": 20,
  "inputs":  [ { "type": "mm:input/count", "as": "panels", "max": 64,
                 "ingredient": { "type": "mm:item", "item": "solarflux:sp_1", "count": 1 } } ],
  "outputs": [ { "type": "mm:output/simple", "scale_by": "panels",
                 "ingredient": { "type": "mm:energy", "amount": 1000000 } } ]
}
```
- `mm:input/count` counts matching items in the input ports without consuming them. The ingredient's `count` is the
  minimum to start, `max` caps the number. The count is taken when the recipe starts.
- `scale_by` multiplies the output amount (items, fluids, energy, Mekanism chemicals).
- KubeJS: `.countInput(as, ingredient, max)`.

## JEI
JEI shows at most 100 ingredients per slot, so a flex recipe shows as several JEI pages: one per MA crop tier with a
small "Tier N" label (bigger lists are split into parts of 100). The input, its matching items and its outputs cycle
together, and looking up an output (e.g. iron essence) finds the recipe. The machine still has one recipe.

## Notes
- One flex run counts as one recipe for the machine's parallel limit; `max_types` limits kinds per run.
- Errors name the recipe: a flex input without `as`, an unknown `from` / `scale_by`, flex entries in a recipe without
  `"mode": "flex"`.
