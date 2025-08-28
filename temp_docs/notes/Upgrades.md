## Upgrades

structure pieces can have "states"

root structure can have "groupedStates"

### Phase 1
example:

```json
{
  "name": "Airy Tings",
  "controllerIds": "mm:controller_a",
  "layout": [
    [
      "IOCU"
    ]
  ],
  "key": {
    "I": {
      "portType": "mm:item",
      "input": true
    },
    "O": {
      "portType": "mm:item",
      "input": false
    },
    "U": {
      "states": "speedUpgrades"
    }
  },
  "stateLists": {
    "speedUpgrades": {
      "default": {
        "optional": true
      },
      "logs": {
        "tag": "minecraft:logs"
      },
      "planks": {
        "tag": "minecraft:planks"
      },
      "diamond": {
        "block": "minecraft:diamond_block"
      }
    }
  }
}
```

needed:
- stateLists on structure (attachments in StructureModel)
- StatesStructurePiece - `U: {states: "speedUpgrades"}`
- 

## Phase 2

what if you have multiple uses for `U` key?


example:
```json
{
  "name": "Airy Tings",
  "controllerIds": "mm:controller_a",
  "layout": [
    [
      "IOCU"
    ]
  ],
  "key": {
    "I": {
      "portType": "mm:item",
      "input": true
    },
    "O": {
      "portType": "mm:item",
      "input": false
    },
    "U": {
      "states": {
        "default": {
          "optional": true
        },
        "logs": {
          "tag": "minecraft:logs"
        },
        "planks": {
          "tag": "minecraft:planks"
        },
        "diamond": {
          "block": "minecraft:diamond_block"
        }
      }
    }
  }
}
```