package io.ticticboom.mods.mmtest;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod("mmtest")
public class TestRoot {

    public TestRoot(IEventBus modBus) {
        MMGameTests.register(modBus);
    }
}
