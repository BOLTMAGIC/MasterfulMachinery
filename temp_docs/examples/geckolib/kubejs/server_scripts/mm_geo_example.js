// MM geoModel example (needs GeckoLib). Files: kubejs/assets/mm/{geo,animations,textures}/example_core*
// Structure: 3x3x3 glass box, controller in the middle of one side, center empty. The model turns in the middle.
// Redstone on the controller runs the recipe -> 'working' (fast spin + pulse); without it 'idle' (slow spin).

MMEvents.createStructures((event) => {
  event
    .create('mm:geo_test_structure')
    .controllerId('mm:my_controller') // any controller you registered
    .name('Geo Test')
    .geoModel({
      model: 'mm:geo/example_core.geo.json',
      texture: 'mm:textures/block/example_core.png',
      animations: 'mm:animations/example_core.animation.json',
      idle: 'animation.example_core.idle',
      working: 'animation.example_core.working',
      offset: [0, 0, 1], // one block into the machine: the middle of the box
      scale: 1,
    })
    .layout((a) => {
      a.layer(['AAA', 'AAA', 'AAA'])
        .layer(['ACA', 'A A', 'AAA'])
        .layer(['AAA', 'AAA', 'AAA'])
        .key('A', { block: 'minecraft:glass' });
    });
});

MMEvents.createProcesses((event) => {
  // no inputs/outputs: runs only while powered
  event.create('mm:geo_test_spin').structureId('mm:geo_test_structure').ticks(40).redstone('powered');
});
