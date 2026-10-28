package io.ticticboom.mods.mm;

import io.ticticboom.mods.mm.networklink.NetworkLink;
import io.ticticboom.mods.mm.config.MMConfigSetup;
import io.ticticboom.mods.mm.client.config.MMConfigClientSetup;
import io.ticticboom.mods.mm.extra.MMExtraBlockRegistry;
import io.ticticboom.mods.mm.controller.MMControllerRegistry;
import io.ticticboom.mods.mm.datagen.DataGenManager;
import io.ticticboom.mods.mm.datagen.MMRepoType;
import io.ticticboom.mods.mm.datagen.MMRepositorySource;
import io.ticticboom.mods.mm.piece.MMStructurePieceRegistry;
import io.ticticboom.mods.mm.port.MMPortRegistry;
import io.ticticboom.mods.mm.setup.MMRegisters;
import io.ticticboom.mods.mm.structure.attachment.MMStructureAttachmentRegistry;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(Ref.ID)
public class ModRoot {

    // NeoForge removed FMLJavaModLoadingContext and injects the mod event bus into the
    // constructor instead, so the bus has to be threaded down to whatever registers with it.
    public ModRoot(IEventBus modEventBus, ModContainer modContainer) {
        MMConfigSetup.setup(modContainer);
        MMPortRegistry.init();
        MMControllerRegistry.init();
        MMExtraBlockRegistry.init();
        NetworkLink.init(modEventBus);
        MMRegisters.register(modEventBus);
        MMStructurePieceRegistry.init();
        MMStructureAttachmentRegistry.init();
        DataGenManager.registerDataProviders();
        registerClientPack(modContainer);
    }

    private void registerClientPack(ModContainer modContainer) {
        try {
            if (FMLEnvironment.getDist() == Dist.CLIENT) {
                MMConfigClientSetup.register(modContainer);
                Minecraft.getInstance().getResourcePackRepository()
                        .addPackFinder(new MMRepositorySource(MMRepoType.RESOURCES));
            }
        } catch (Exception e) {
            Ref.LOG.error("Could not register the generated resource pack", e);
        }
    }
}
