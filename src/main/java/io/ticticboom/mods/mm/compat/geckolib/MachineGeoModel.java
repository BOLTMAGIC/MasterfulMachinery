package io.ticticboom.mods.mm.compat.geckolib;

import com.geckolib.cache.GeckoLibResources;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import io.ticticboom.mods.mm.client.geo.GeoModelConfig;
import net.minecraft.resources.Identifier;

/** GeckoLib 5 resolves the model and texture from the render state, so the machine's config travels in it. */
public class MachineGeoModel extends GeoModel<MachineGeoAnimatable> {
    static final DataTicket<GeoModelConfig> CONFIG = DataTicket.create("mm_geo_model", GeoModelConfig.class);

    @Override
    public void addAdditionalStateData(MachineGeoAnimatable animatable, Object relatedObject, GeoRenderState renderState) {
        super.addAdditionalStateData(animatable, relatedObject, renderState);
        renderState.addGeckolibData(CONFIG, animatable.config());
    }

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return GeckoLibResources.stripPrefixAndSuffix(renderState.getGeckolibData(CONFIG).model());
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return renderState.getGeckolibData(CONFIG).texture();
    }

    @Override
    public Identifier getAnimationResource(MachineGeoAnimatable animatable) {
        Identifier animations = animatable.config().animations();
        return GeckoLibResources.stripPrefixAndSuffix(animations != null ? animations : animatable.config().model());
    }
}
