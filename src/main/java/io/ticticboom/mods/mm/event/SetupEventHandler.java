package io.ticticboom.mods.mm.event;

import io.ticticboom.mods.mm.controller.MMControllerRegistry;
import io.ticticboom.mods.mm.datagen.PackEventHandler;
import io.ticticboom.mods.mm.port.MMPortRegistry;
import io.ticticboom.mods.mm.port.botania.mana.register.BotaniaManaPortBlockEntity;
import io.ticticboom.mods.mm.recipe.MachineRecipeManager;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import io.ticticboom.mods.mm.setup.loader.ControllerLoader;
import io.ticticboom.mods.mm.setup.loader.ExtraBlockLoader;
import io.ticticboom.mods.mm.setup.loader.PortLoader;
import io.ticticboom.mods.mm.compat.claim.ClaimPermissionManager;
import io.ticticboom.mods.mm.compat.claim.FTBChunksClaimProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import vazkii.botania.api.BotaniaForgeClientCapabilities;
import vazkii.botania.forge.CapabilityUtil;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class SetupEventHandler {

    @SubscribeEvent
    public static void onConstruction(FMLConstructModEvent event) {
        event.enqueueWork(() -> {

            PackEventHandler.ensureConfigPath();
            ControllerLoader.loadAll();
            PortLoader.loadAll();
            ExtraBlockLoader.loadAll();

            // Optional claim provider for FTBChunks
            try {
                if (FTBChunksClaimProvider.isAvailable()) {
                    ClaimPermissionManager.registerProvider(new FTBChunksClaimProvider());
                }
            } catch (Throwable t) {
                // best-effort: do not fail loading the mod if provider fails
            }
        });
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            for (RegistryGroupHolder controller : MMControllerRegistry.CONTROLLERS) {
                ResourceLocation type = controller.getRegistryId();
                MMControllerRegistry.get(type).registerScreen(controller);
            }

            for (RegistryGroupHolder port : MMPortRegistry.PORTS) {
                ResourceLocation type = port.getRegistryId();
                MMPortRegistry.get(type).registerScreen(port);
            }
            MachineRecipeManager.init();
        });
    }
}
