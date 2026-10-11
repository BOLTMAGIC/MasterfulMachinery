// Solar Array: energy scales with the number of solar panels in the item input port.
// Panels are counted, NOT used up: 1 panel = 1M FE per run, 10 = 10M, 12 = 12M (max 64 counted).
// Needs the controller from startup_scripts/mm_controller_solar_array.js (game restart after adding it).
//
// Structure (side view, bottom layer first):
//   1 C      1 = casing block, C = controller
//   2 3      2 = item input port (panels), 3 = energy output port
// The energy output port must hold one run's energy: 64 panels x 1M = 64M -> Gigantic Energy Port.
// Smaller ports work too; the machine waits while the output port is full.

const STRUCTURE = 'mm:solar_array_structure';

MMEvents.createStructures((event) => {
  event
    .create(STRUCTURE)
    .controllerId('mm:solar_array_controller')
    .name('Solar Array Tier 1')
    .layout((a) => {
      a.layer(['1C'])
        .layer(['23'])
        .key('1', { block: 'minecraft:iron_block' })
        .key('2', { portType: 'mm:item', input: true })
        .key('3', { portType: 'mm:energy', input: false });
    });
});

MMEvents.createProcesses((event) => {
  // Every 20 ticks: 1M FE per panel.
  event
    .create('mm:solar_array_power')
    .structureId(STRUCTURE)
    .ticks(20)
    .countInput('panels', { type: 'mm:item', item: 'solarflux:sp_1', count: 1 }, 64)
    .output({ type: 'mm:output/simple', scale_by: 'panels', ingredient: { type: 'mm:energy', amount: 1000000 } });

  // Per-tick version (use instead of the one above): 50k FE/t per panel, the same 1M per panel per second.
  // event
  //   .create('mm:solar_array_power')
  //   .structureId(STRUCTURE)
  //   .ticks(20)
  //   .countInput('panels', { type: 'mm:item', item: 'solarflux:sp_1', count: 1 }, 64)
  //   .output({ type: 'mm:output/simple', per_tick: true, scale_by: 'panels', ingredient: { type: 'mm:energy', amount: 50000 } });

  // Better panels count more: one recipe per panel kind with a different amount, e.g. Solar Panel II.
  // Both recipes only run at the same time if the structure allows parallel recipes (.maxParallelRecipes(2) or more).
  // event
  //   .create('mm:solar_array_power_2')
  //   .structureId(STRUCTURE)
  //   .ticks(20)
  //   .countInput('panels', { type: 'mm:item', item: 'solarflux:sp_2', count: 1 }, 64)
  //   .output({ type: 'mm:output/simple', scale_by: 'panels', ingredient: { type: 'mm:energy', amount: 4000000 } });
});
