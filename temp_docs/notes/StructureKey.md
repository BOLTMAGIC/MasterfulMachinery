# Structure Part Key


### Block Id

```json
{
  "block": "minecraft:glass"
}
```

### Block Tag

```json
{
  "tag": "forge:fences"
}
```

### Port Type

```json
{
  "portType": "mm:item"
}
```

### Port Block

```json
{
  "port": "mm:my_fluid_port"
}
```

## Port slots

A key with `"allowPort": true` is a port slot: a port of the structure may be built there in place of the key's own
block. The position that port left then takes a block of a port slot key, so the machine keeps its shape. Off unless
declared, like `allowHelper`.

```json
{
  "G": {
    "block": "minecraft:glass",
    "allowPort": true
  }
}
```

How many ports of a port key the structure takes is the key's `minPorts` to `maxPorts`. Left out, both are the number
of positions the layout draws of that key: ports can move to slots, but there are never more or fewer of them. A
negative `maxPorts` means no limit. Both need an `allowPort` key in the layout or a `replaceWith` on the port key.

```json
{
  "I": {
    "portType": "mm:item",
    "input": true,
    "minPorts": 1,
    "maxPorts": 6
  }
}
```

With the layout drawing four `I`, this machine forms with one to six item input ports: fewer than four leave their
positions to the slot block, more than four stand on slots. A port that fits no key, or one more than its key's
`maxPorts`, keeps the machine from forming.

### Leaving ports out

A port key may name, with `replaceWith`, the key whose block goes where one of its ports is left out. The machine then
needs no port slot at all, and `minPorts` defaults to 0: build as few of these ports as you need.

```json
{
  "F": {
    "portType": "mm:fluid",
    "input": true,
    "replaceWith": "B"
  },
  "B": {
    "block": "minecraft:iron_block"
  }
}
```

With two `F` in the layout this machine forms with zero, one or two fluid inputs; every `F` without a port holds an
iron block. Set `minPorts` to require some. `replaceWith` names one key of the same structure that is not a port key;
the key does not have to appear in the layout. Without `replaceWith`, a left-out port's position takes a block of an
`allowPort` key.

In KubeJS the same fields go into the key's JSON: `.key("G", { block: "minecraft:glass", allowPort: true })`,
`.key("F", { portType: "mm:fluid", input: true, replaceWith: "B" })`.

## N.Y.I. - Idea Example

```json
{
  "A": {
    "block": "mm:controller_a",
    "properties": [
      {
        "property": "facing",
        "value": "north"
      },
      {
        "property": "axis",
        "value": "y"
      }
    ]
  }
}
```