package io.ticticboom.mods.mm.event;

import io.ticticboom.mods.mm.controller.MMControllerRegistry;
import io.ticticboom.mods.mm.controller.single.register.SingleMachineScreen;
import io.ticticboom.mods.mm.setup.MMRegisters;
import net.minecraft.client.gui.screens.MenuScreens;
import io.ticticboom.mods.mm.datagen.PackEventHandler;
import io.ticticboom.mods.mm.port.MMPortRegistry;
import io.ticticboom.mods.mm.recipe.MachineRecipeManager;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import io.ticticboom.mods.mm.setup.loader.ControllerLoader;
import io.ticticboom.mods.mm.setup.loader.ExtraBlockLoader;
import io.ticticboom.mods.mm.setup.loader.PortLoader;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;

@EventBusSubscriber
public class SetupEventHandler {

    @SubscribeEvent
    public static void onConstruction(FMLConstructModEvent event) {
        event.enqueueWork(() -> {

            PackEventHandler.ensureConfigPath();
            ControllerLoader.loadAll();
            PortLoader.loadAll();
            ExtraBlockLoader.loadAll();
        });
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            for (RegistryGroupHolder controller : MMControllerRegistry.CONTROLLERS) {
                Identifier type = controller.getRegistryId();
                MMControllerRegistry.get(type).registerScreen(controller);
            }

            for (RegistryGroupHolder port : MMPortRegistry.PORTS) {
                Identifier type = port.getRegistryId();
                MMPortRegistry.get(type).registerScreen(port);
            }
            MenuScreens.register(MMRegisters.SINGLE_MACHINE_MENU.get(), SingleMachineScreen::new);
            MachineRecipeManager.init();
        });
    }
}
