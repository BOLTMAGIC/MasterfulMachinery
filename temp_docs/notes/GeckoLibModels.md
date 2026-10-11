# GeckoLib Models for Machines

A machine can show a GeckoLib (Blockbench) model while its multiblock is formed, e.g. a black hole inside the
structure, with one animation while idle and another while a recipe runs.

GeckoLib is **optional**. Without it nothing is drawn and the machine works as before.

Example: [`examples/geckolib/`](../examples/geckolib/) (a spinning cube inside a 3x3x3 glass box, with its model,
animation and texture files).

## Config
`geoModel` goes on a controller (used by all its structures) and/or on a structure (replaces the controller's;
`"none"` turns it off for that structure).

| Field | Required | Default | |
|---|---|---|---|
| `model` | yes | | `ns:geo/x.geo.json` |
| `texture` | yes | | `ns:textures/....png` |
| `animations` | no | none (static model) | `ns:animations/x.animation.json` |
| `idle` | no | | animation looped while formed and idle |
| `working` | no | | animation looped while a recipe runs |
| `offset` | no | `[0, 0, 0]` | blocks: x = right of a player standing in front of the controller, y = up, z = into the machine |
| `scale` | no | `1` | |
| `rotate` | no | `true` | turn the model and the offset with the controller's facing |
| `glow` | no | `false` | full bright |

### KubeJS
```js
MMEvents.createStructures(event => {
  event.create('mm:core_t4')
    .controllerId('mm:my_controller')
    .geoModel({
      model: 'mm:geo/black_hole.geo.json',
      texture: 'mm:textures/block/black_hole.png',
      animations: 'mm:animations/black_hole.animation.json',
      idle: 'animation.black_hole.idle',
      working: 'animation.black_hole.working',
      offset: [0, 1, 2],
      glow: true,
    })
    .layout(/* ... */)
})
```
- `.geoModel('none')` on a structure: no model while that structure is formed, even if the controller has one.
- Controllers: `.geoModel({...})` in `MMEvents.registerControllers`, or `"geoModel": {...}` in the controller JSON.
- JSON structures: `"geoModel": {...}` or `"geoModel": "none"`.

### Files
The files go in the usual GeckoLib folders of a resource pack, e.g. with KubeJS:
```
kubejs/assets/<ns>/geo/<name>.geo.json
kubejs/assets/<ns>/animations/<name>.animation.json
kubejs/assets/<ns>/textures/block/<name>.png
```

## States
| Machine | Model |
|---|---|
| structure not formed | none |
| formed, no recipe running | `idle` animation |
| recipe running | `working` animation |

## Errors
A wrong model path hides that model, a wrong animation file makes it static; each logs one warning
(`Not drawing geoModel ...`). Nothing crashes.
