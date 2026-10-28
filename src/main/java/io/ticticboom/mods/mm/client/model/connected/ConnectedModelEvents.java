package io.ticticboom.mods.mm.client.model.connected;

import io.ticticboom.mods.mm.Ref;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterBlockStateModels;

@EventBusSubscriber(modid = Ref.ID, value = Dist.CLIENT)
public class ConnectedModelEvents {

    @SubscribeEvent
    public static void registerBlockStateModels(RegisterBlockStateModels event) {
        event.registerModel(Ref.id("connected"), ConnectedBlockStateModel.Unbaked.CODEC);
    }
}
