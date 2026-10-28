package io.ticticboom.mods.mm.port;

import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;

public interface IControllerAwareStorage {
    void attachController(MachineControllerBlockEntity controller, long gameTime);
}
