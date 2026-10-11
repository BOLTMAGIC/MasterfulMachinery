// MM recipe examples: fixed, flex and count/scale.
// Put in kubejs/server_scripts/. Replace 'mm:my_machine_structure' with your structure id.
//
// When do you need flex?
//   - The output is always the same item          -> normal (fixed) recipe, examples 1-2
//   - The output's NAME comes from the input item  -> flex recipe, examples 3-8
//   - The output AMOUNT comes from how many items  -> countInput + scale_by, examples 9-10
//   - Mystical Agriculture (tiers, crux, essence from MA itself)            -> examples 11-13
//
// flexOutput(from, match, item, count):
//   match = regex tested against the input id's path (the part after ':'), the whole path must match
//   item  = output id; {ns} = input namespace, {path} = input path, $1 $2 ... = the regex groups
//   if the resulting item doesn't exist, that input is ignored (stays in the port)

MMEvents.createProcesses((event) => {

  // 1) 20 iron -> 50 oak logs. Input and output are fixed items: no flex needed.
  event.create('mm:iron_to_wood')
    .structureId('mm:my_machine_structure')
    .ticks(200)
    .input({ type: 'mm:input/consume', ingredient: { type: 'mm:item', item: 'minecraft:iron_ingot', count: 20 } })
    .output({ type: 'mm:output/simple', ingredient: { type: 'mm:item', item: 'minecraft:oak_log', count: 50 } });

  // 2) Fixed output, but ANY iron ingot (tag) + energy, with a 25% bonus output.
  event.create('mm:any_iron_to_wood')
    .structureId('mm:my_machine_structure')
    .ticks(200)
    .input({ type: 'mm:input/consume', ingredient: { type: 'mm:item', tag: 'forge:ingots/iron', count: 20 } })
    .input({ type: 'mm:input/consume', ingredient: { type: 'mm:energy', amount: 50000 } })
    .output({ type: 'mm:output/simple', ingredient: { type: 'mm:item', item: 'minecraft:oak_log', count: 50 } })
    .output({ type: 'mm:output/simple', chance: 0.25, ingredient: { type: 'mm:item', item: 'minecraft:apple', count: 1 } });

  // 3) Seeds -> essence (your Essence Forge). Seeds are not consumed (chance 0), max 3 kinds per run.
  //    mysticalagriculture:iron_seeds -> mysticalagriculture:iron_essence
  event.create('mm:essenceforge_t1_seeds')
    .structureId('mm:essenceforge_tier1_structure')
    .ticks(400)
    .mode('flex')
    .input({ type: 'mm:input/consume', ingredient: { type: 'mm:energy', amount: 1000000 } })
    .input({ type: 'mm:input/consume', ingredient: { type: 'mm:fluid', fluid: 'industrialforegoing:ether_gas', amount: 100 } })
    .flexInput('seed', { type: 'mm:item', tag: 'mysticalagriculture:seeds', count: 1 }, 3, 0)
    .flexOutput('seed', '(.+)_seeds', '{ns}:$1_essence', 64);

  // 4) Different item AND different amounts: 2 of any log -> 8 planks of the same wood.
  //    minecraft:oak_log -> minecraft:oak_planks, minecraft:birch_log -> minecraft:birch_planks
  //    (stripped_oak_log -> stripped_oak_planks doesn't exist, so stripped logs are ignored)
  event.create('mm:logs_to_planks')
    .structureId('mm:my_machine_structure')
    .ticks(100)
    .mode('flex')
    .flexInput('log', { type: 'mm:item', tag: 'minecraft:logs', count: 2 })   // consumed, every log kind in one run
    .flexOutput('log', '(.+)_log', '{ns}:$1_planks', 8);

  // 5) Ore doubling: 1 raw ore -> 2 ingots.  minecraft:raw_iron -> minecraft:iron_ingot
  //    Mods that name things differently are handled in example 6.
  event.create('mm:raw_to_ingots')
    .structureId('mm:my_machine_structure')
    .ticks(100)
    .mode('flex')
    .input({ type: 'mm:input/consume', ingredient: { type: 'mm:energy', amount: 20000 } })   // x number of kinds (config)
    .flexInput('ore', { type: 'mm:item', tag: 'forge:raw_materials', count: 1 })
    .flexOutput('ore', 'raw_(.+)', '{ns}:$1_ingot', 2);

  // 6) Fixed namespace in the output: 2 of any wool -> 3 carpets of that colour, always from minecraft.
  //    minecraft:white_wool -> minecraft:white_carpet
  event.create('mm:wool_to_carpet')
    .structureId('mm:my_machine_structure')
    .ticks(60)
    .mode('flex')
    .flexInput('wool', { type: 'mm:item', tag: 'minecraft:wool', count: 2 }, 4)   // max 4 colours per run
    .flexOutput('wool', '(.+)_wool', 'minecraft:$1_carpet', 3);

  // 7) Two outputs from one input + extra options (JSON form of flexOutput).
  //    Every seed gives its essence, and 10% of the time its seed back as a bonus.
  event.create('mm:seed_lab')
    .structureId('mm:my_machine_structure')
    .ticks(200)
    .mode('flex')
    .flexInput('seed', { type: 'mm:item', tag: 'mysticalagriculture:seeds', count: 1 })
    .flexOutput('seed', '(.+)_seeds', '{ns}:$1_essence', 16)
    .output({ type: 'mm:output/flex', from: 'seed', match: '(.+)', item: '{ns}:$1', count: 1, chance: 0.1 });

  // 8) Upgrade that keeps NBT (enchantments, name, damage): iron sword -> diamond sword.
  //    copy_nbt copies the input's NBT onto the output. Only iron_* swords match.
  event.create('mm:sword_upgrade')
    .structureId('mm:my_machine_structure')
    .ticks(300)
    .mode('flex')
    .input({ type: 'mm:input/consume', ingredient: { type: 'mm:item', item: 'minecraft:diamond', count: 2 } })
    .flexInput('sword', { type: 'mm:item', tag: 'minecraft:swords', count: 1 }, 1)
    .output({ type: 'mm:output/flex', from: 'sword', match: 'iron_(.+)', item: '{ns}:diamond_$1', count: 1, copy_nbt: true });

  // 9) Solar array: counts panels in the input ports, doesn't consume them.
  //    1M FE per panel per run: 10 panels -> 10M, 12 -> 12M (needs an energy OUTPUT port).
  //    Controller + recipe ready to use: solar-example/ (controller mm:solar_array_controller; you build the structure).
  event.create('mm:solar_array')
    .structureId('mm:solar_array_structure')
    .ticks(20)
    .countInput('panels', { type: 'mm:item', item: 'solarflux:sp_1', count: 1 }, 64)   // at least 1, counts up to 64
    .output({ type: 'mm:output/simple', scale_by: 'panels', ingredient: { type: 'mm:energy', amount: 1000000 } });

  // 10) Count + flex together, with this recipe's own cost curve.
  //     Each crop kind (seeds kept) gives 4 essence per fertilizer in the port (max 16 fertilizer, not consumed).
  //     Cost: 1-3 kinds x1 energy, 4-6 kinds x2, 7+ kinds x3 (overrides mm-common.toml [flex]).
  event.create('mm:fertilized_garden')
    .structureId('mm:my_machine_structure')
    .ticks(400)
    .mode('flex')
    .flexCost({ mode: 'table', table: { '1': 1, '4': 2, '7': 3 } })   // or .flexCost('once') / .flexCost('linear')
    .input({ type: 'mm:input/consume', ingredient: { type: 'mm:energy', amount: 500000 } })
    .countInput('fert', { type: 'mm:item', item: 'mysticalagriculture:fertilized_essence', count: 1 }, 16)
    .flexInput('seed', { type: 'mm:item', tag: 'mysticalagriculture:seeds', count: 1 }, 9, 0)
    .output({ type: 'mm:output/flex', from: 'seed', match: '(.+)_seeds', item: '{ns}:$1_essence', count: 4, scale_by: 'fert' });

  // ---- Mystical Agriculture support (optional: only does something when MA is installed) ----
  // '{ma:essence}' / '{ma:crux}' read the seed's essence / crux from MA itself, so names don't have to match.
  // Seeds of crops disabled in MA's config are always skipped.

  // 11) One recipe per machine tier: Essence Forge T1 takes MA tier 1-3 seeds only.
  event.create('mm:essenceforge_t1_flex')
    .structureId('mm:essenceforge_tier1_structure')
    .ticks(400)
    .mode('flex')
    .input({ type: 'mm:input/consume', ingredient: { type: 'mm:energy', amount: 1000000 } })
    .input({ type: 'mm:input/consume', ingredient: { type: 'mm:fluid', fluid: 'industrialforegoing:ether_gas', amount: 100 } })
    .input({
      type: 'mm:input/flex', as: 'seed',
      ingredient: { type: 'mm:item', tag: 'mysticalagriculture:seeds', count: 1 },
      max_types: 3, chance: 0,
      min_tier: 1, max_tier: 3,
    })
    .flexOutput('seed', '(.+)', '{ma:essence}', 64);

  // 12) T4 Essence Forge with T6 seeds: each seed needs ITS OWN crux in the input port (not consumed).
  //     allthemodium seeds need allthemodium:allthemodium_block, nether star seeds need mysticalagradditions:nether_star_crux...
  event.create('mm:essenceforge_t4_t6')
    .structureId('mm:essenceforge_tier4_structure')
    .ticks(400)
    .mode('flex')
    .input({ type: 'mm:input/consume', ingredient: { type: 'mm:energy', amount: 1000000 } })
    .input({
      type: 'mm:input/flex', as: 'seed',
      ingredient: { type: 'mm:item', tag: 'mysticalagriculture:seeds', count: 1 },
      chance: 0, min_tier: 6, max_tier: 6,
    })
    .flexMatch('seed', '{ma:crux}', 1, 0)          // (from, item, count, chance): chance 0 = the crux stays
    .flexOutput('seed', '(.+)', '{ma:essence}', 64);

  // 13) Leave out single seeds, whole mods or tags.
  event.create('mm:essenceforge_t2_no_mobs')
    .structureId('mm:essenceforge_tier2_structure')
    .ticks(400)
    .mode('flex')
    .input({ type: 'mm:input/consume', ingredient: { type: 'mm:energy', amount: 1000000 } })
    .input({
      type: 'mm:input/flex', as: 'seed',
      ingredient: { type: 'mm:item', tag: 'mysticalagriculture:seeds', count: 1 },
      chance: 0, max_tier: 4,
      exclude: [
        'mysticalagriculture:zombie_seeds',     // one item
        'mysticalagriculture:skeleton_seeds',
        'mysticalagradditions:*',               // a whole mod
        '#kubejs:essenceforge_blacklist',       // a tag (make your own with ServerEvents.tags)
      ],
    })
    .flexOutput('seed', '(.+)', '{ma:essence}', 64);
});

// For names that don't follow a pattern: pick the output yourself (or reject an input).
// Runs once per item kind (cached until /reload), server side.
MMEvents.flexOutput('mm:raw_to_ingots', (event) => {
  const id = String(event.input.id);                       // e.g. 'mekanism:raw_osmium'
  if (id.startsWith('mekanism:raw_')) {
    event.setOutput('mekanism:ingot_' + id.substring('mekanism:raw_'.length));   // -> mekanism:ingot_osmium
  }
  if (id == 'minecraft:raw_gold') event.reject();          // gold stays in the port
});
