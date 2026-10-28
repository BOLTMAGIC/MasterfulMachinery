package io.ticticboom.mods.mm.compat.geckolib;

import net.minecraft.resources.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class MachineGeoModel extends GeoModel<MachineGeoAnimatable> {

    @Override
    public Identifier getModelResource(MachineGeoAnimatable animatable) {
        return animatable.config().model();
    }

    @Override
    public Identifier getTextureResource(MachineGeoAnimatable animatable) {
        return animatable.config().texture();
    }

    @Override
    public Identifier getAnimationResource(MachineGeoAnimatable animatable) {
        Identifier animations = animatable.config().animations();
        return animations != null ? animations : animatable.config().model();
    }
}
