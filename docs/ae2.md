# AE2 and Masterful Machinery

How to connect machines to [Applied Energistics 2](https://www.curseforge.com/minecraft/mc-mods/applied-energistics-2).
Everything on this page only exists when AE2 is installed.

There are two ways to autocraft with an MM machine:

| | Network Linker + Pattern Provider | ME Pattern Connector |
|---|---|---|
| Recipes in the terminal | Only the ones you encode | Every recipe of the machine |
| Needs | A Pattern Provider and encoded patterns | A port in the structure, cabled to ME |
| Cost | Normal AE2 pattern cost | None: every recipe for free |
| Works at a distance | Yes, any distance or dimension | No, it needs a cable and a channel |

The **Network Linker with a Pattern Provider** is the normal way, and works like autocrafting with any
other mod's machine. The **ME Pattern Connector** is optional: a pack adds it only if it wants every
recipe offered with no encoding.

## The Network Linker

An item that links a machine to an ME network without cables.

1. Hold the linker in **Linking** mode. Sneak + mouse wheel changes the mode.
2. Right-click a Wireless Access Point or any AE2 network block to save that network.
3. Right-click a machine's controller to link it.

A linked machine:

- **Sends its outputs to ME.** As soon as a recipe finishes, everything in the output ports goes into
  the network. It also checks every 20 ticks for anything else that lands in the output ports.
- **Sends port contents to ME when a port is broken**, instead of dropping them.
- **Belongs to the network's owner.** Only the owner, their FTB Teams team, and operators can open or
  break the machine.

Hover the **Linked to** row in the controller screen to see the owner, the network's position and when
outputs were last sent.

In **Info** mode, right-click a controller or port to see its link. Sneak + right-click it to unlink the
machine, or sneak + right-click the air to forget the saved network.

## Autocrafting with a Pattern Provider

1. Link the machine with the Network Linker.
2. Put a Pattern Provider against one of the machine's **input ports**.
3. Encode a processing pattern with the recipe's inputs and outputs, and put it in the provider.
4. Order the item from a crafting terminal.

The provider puts the ingredients in the input port, the machine runs the recipe, and the linker sends
the result back to the network, which finishes the craft. Item ports take whole batches at once from a
provider.

Ingredients AE2 cannot carry, like energy, are supplied through the machine's own ports as usual.

## The ME Pattern Connector

A port that puts every recipe of the machine in the crafting terminal and runs the orders itself.

```json
{
  "id": "me_port",
  "controllerIds": "mm:alloy_kiln",
  "name": "ME Pattern Connector",
  "type": "mm:ae2/pattern",
  "config": {
    "patternPriority": 0
  }
}
```

| Option | Default | Meaning |
|---|---|---|
| `patternPriority` | `0` | Which machine AE2 prefers when several can make the same item |
| `exclude` | none | Ingredient types to leave out of the patterns, e.g. `["energy"]` |

This port has no input or output side, so one file makes one block: `mm:<id>`.

### Setting one up

1. Put the port block in the machine's structure, like any other port.
2. Run an ME cable to it. It needs a **channel** and power.
3. Give the machine its usual **input ports** and **output ports**. The connector fills the input ports
   and takes results from the output ports; it stores nothing itself.

### Which recipes appear

A recipe is offered when AE2 can carry everything on both sides. Items and fluids always work. With
these addons installed, more types can be part of a pattern:

| Type | Addon |
|---|---|
| Energy | [Applied Flux](https://www.curseforge.com/minecraft/mc-mods/applied-flux) |
| Mekanism chemicals | [Applied Mekanistics](https://www.curseforge.com/minecraft/mc-mods/applied-mekanistics) |
| Ars Nouveau Source | [Ars Énergistique](https://www.curseforge.com/minecraft/mc-mods/ars-energistique) |
| PneumaticCraft air | [Applied Pneumatics](https://www.curseforge.com/minecraft/mc-mods/applied-pneumatics) |
| Replication matter | [Replication AE2 Bridge](https://www.curseforge.com/minecraft/mc-mods/replication-ae2-bridge) |

Use `exclude` to keep a type out of the patterns, so the machine gets it from its own ports instead.
The names are `energy`, `chemical`, `source`, `air` and `matter`.

Recipes that are not offered still run normally in the machine.

### Notes

- The connector runs **one craft at a time**. AE2 queues the rest.
- Several machines can offer the same recipe. AE2 shares the work, and `patternPriority` decides who
  is asked first.

### From KubeJS

```js
// kubejs/startup_scripts/
MMEvents.registerPorts(event => {
    event.create('me_port')
        .name('ME Pattern Connector')
        .controllerId('mm:alloy_kiln')
        .config('mm:ae2/pattern', config => {
            config.patternPriority(0)
            config.exclude('energy')
        })
})
```

## The Structure Builder and ME

The Structure Builder can take blocks from an ME network and order the missing ones. Put it in a
Wireless Access Point, or sneak + right-click an AE2 network block with it. Its Settings tab turns
**Use ME network** and **Auto-craft missing** on and off. See [`reference.md`](reference.md#structure-builder).

## Config

In `config/mm-common.toml`:

| Option | Default | Meaning |
|---|---|---|
| `networkLinkOutputInterval` | `20` | How often a linked machine checks its output ports, in ticks |
| `networkLinkSendOnRemove` | `true` | Send a broken port's contents to ME instead of dropping them |
| `networkLinkOpBypass` | `true` | Operators may open and break machines linked by other players |

## See also

- [`reference.md`](reference.md) — every key and option.
