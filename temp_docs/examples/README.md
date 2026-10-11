# Examples

KubeJS examples. Copy a folder's `kubejs/` contents (or the scripts) into your instance's `kubejs/` folder.
Startup scripts need a game restart, server scripts work after `/reload`.

| Folder | Shows | Docs |
|---|---|---|
| [`flex/flex_recipes.js`](flex/flex_recipes.js) | 13 recipes: fixed recipes, flex recipes (seeds -> essence, logs -> planks, ore doubling, NBT upgrades), count + `scale_by`, Mystical Agriculture tiers / crux / essence, exclude lists | [FlexRecipes](../notes/FlexRecipes.md) |
| [`solar_array/`](solar_array/) | A controller, a small structure and a generator recipe whose energy scales with the solar panels in the input port | [FlexRecipes](../notes/FlexRecipes.md#count--scale_by-fixed-recipes-too) |
| [`geckolib/`](geckolib/) | A spinning cube model inside a 3x3x3 glass structure: slow spin while idle, fast spin + pulse while working (redstone) | [GeckoLibModels](../notes/GeckoLibModels.md) |

Replace the example ids (`mm:my_machine_structure`, `mm:my_controller`, ...) with your own.
