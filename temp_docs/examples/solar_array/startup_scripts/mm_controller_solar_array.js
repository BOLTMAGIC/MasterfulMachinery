// Solar Array controller (id: mm:solar_array_controller). Startup script: needs a game restart.
// The structure's portType keys accept any registered item / energy port, so no new ports are needed.
MMEvents.registerControllers((event) => {
  event
    .create('solar_array_controller')
    .name('Solar Array Controller')
    .type('mm:machine');
});
