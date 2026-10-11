package io.ticticboom.mods.mm.compat.geckolib;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** Model, texture and animation files from the machine's geoModel. */
public class MachineGeoModel extends GeoModel<MachineGeoAnimatable> {

    @Override
    public ResourceLocation getModelResource(MachineGeoAnimatable animatable) {
        return animatable.config().model();
    }

    @Override
    public ResourceLocation getTextureResource(MachineGeoAnimatable animatable) {
        return animatable.config().texture();
    }

    @Override
    public ResourceLocation getAnimationResource(MachineGeoAnimatable animatable) {
        // only asked for when the animatable has an animation controller, i.e. when "animations" is set
        ResourceLocation animations = animatable.config().animations();
        return animations != null ? animations : animatable.config().model();
    }
}
