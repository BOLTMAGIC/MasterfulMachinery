package io.ticticboom.mods.mm.client.event;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.config.WorkingEffectsConfig;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import io.ticticboom.mods.mm.Ref;

@EventBusSubscriber(modid = Ref.ID, value = Dist.CLIENT)
public final class WorkingEffectsEvents {

    private WorkingEffectsEvents() {
    }

    @SubscribeEvent
    public static void onRegisterReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(Ref.id("working_effects"), (ResourceManagerReloadListener) resourceManager -> WorkingEffectsConfig.reload());
    }
}
