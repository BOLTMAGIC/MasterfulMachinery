package io.ticticboom.mods.mm.client.model.connected;

import io.ticticboom.mods.mm.Ref;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

@EventBusSubscriber(modid = Ref.ID, value = Dist.CLIENT)
public class ConnectedModelEvents {

    @SubscribeEvent
    public static void registerGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register(Ref.id("connected"), ConnectedGeometryLoader.INSTANCE);
    }
}
