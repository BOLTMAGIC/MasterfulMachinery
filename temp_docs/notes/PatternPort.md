# Pattern Port

A block that an AE2 Pattern Provider sees as a crafting machine. Patterns pushed into it run on the machine straight
away: the pattern's items, fluids and Mekanism chemicals never go into the input ports, and the outputs go back to the
Pattern Provider that pushed the pattern.

Needs Applied Energistics 2. Mekanism chemicals need Applied Mekanistics.

## Placing it
Like the Input Gateway: build it into the machine in place of a casing or glass block, or put it next to one of the
machine's input ports. Then put a Pattern Provider next to the Pattern Port.

## Which recipe a pattern runs
- The pattern's inputs must be exactly what the recipe consumes: the same items (with their NBT, as the recipe checks
  it), fluids and chemicals, in the same amounts.
- Everything a pattern can't carry comes from the machine's ports as usual: energy, mana, per-tick inputs and
  catalysts (inputs with `chance: 0`).
- The pattern's main output must be one of the recipe's sure outputs (chance 1).
- Flex recipes can't be run from a pattern.

## While it runs
- A pushed pattern starts only if the machine can start it now: built, allowed by redstone and the recipe's conditions,
  room under the parallel limit, and the other inputs in its ports. Otherwise the Pattern Port says no and AE2 tries
  again later.
- The same pattern can run several times at once, up to the machine's parallel limit (pattern runs and normal recipes
  count together).
- A pattern run pauses with the machine (broken structure, unloaded chunk, redstone).

## Outputs
- All outputs that AE2 can hold (items, fluids, chemicals, byproducts included) go back to the Pattern Provider.
  Outputs it can't hold (energy, mana...) go to the machine's output ports.
- If the Pattern Provider is full or gone, the outputs wait and are given back as soon as it takes them. Nothing is
  thrown away.

## Jade
The tooltip shows the machine, how many patterns are running, outputs waiting to go back and why the last pattern was
refused.

## Hiding it
To keep only the Input Gateway in a pack, remove the recipe and hide the item with KubeJS:

```js
// server_scripts
ServerEvents.recipes(event => {
  event.remove({ output: 'mm:pattern_port' })
})

// client_scripts
JEIEvents.hideItems(event => {
  event.hide('mm:pattern_port')
})
```
