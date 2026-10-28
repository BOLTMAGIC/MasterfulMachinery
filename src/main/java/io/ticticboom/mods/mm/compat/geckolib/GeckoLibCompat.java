package io.ticticboom.mods.mm.compat.geckolib;

import com.mojang.blaze3d.vertex.PoseStack;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.client.geo.GeoBridge;
import io.ticticboom.mods.mm.client.geo.GeoModelConfig;
import io.ticticboom.mods.mm.controller.machine.register.ControllerState;
import io.ticticboom.mods.mm.controller.machine.register.ModelDisplay;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.loading.object.BakedAnimations;
import software.bernie.geckolib.renderer.GeoObjectRenderer;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public class GeckoLibCompat implements GeoBridge {

    private final GeoObjectRenderer<MachineGeoAnimatable> renderer = new GeoObjectRenderer<>(new MachineGeoModel());
    private final Set<GeoModelConfig> broken = Collections.newSetFromMap(new IdentityHashMap<>());

    @Override
    public void render(MachineControllerBlockEntity be, GeoModelConfig config, ControllerState state, PoseStack pose,
                       MultiBufferSource buffers, int light, float partialTick) {
        if (broken.contains(config)) {
            return;
        }
        MachineGeoAnimatable animatable = animatableFor(be, config);
        if (animatable == null) {
            return;
        }
        animatable.setState(state);
        animatable.setAnimationsHidden(be.getModelDisplay() != ModelDisplay.ON);
        PoseStack local = new PoseStack();
        local.last().pose().set(pose.last().pose());
        local.last().normal().set(pose.last().normal());
        try {
            renderer.render(local, animatable, buffers, null, null, light, partialTick);
        } catch (RuntimeException e) {
            fail(config, "GeckoLib failed to draw it: " + e);
        }
    }

    @Nullable
    private MachineGeoAnimatable animatableFor(MachineControllerBlockEntity be, GeoModelConfig config) {
        if (be.getGeoAnimatable() instanceof MachineGeoAnimatable current && current.config() == config) {
            return current;
        }
        if (!GeckoLibCache.getBakedModels().containsKey(config.model())) {
            fail(config, "model " + config.model() + " not found (assets/<ns>/geo/...)");
            return null;
        }
        String idle = null;
        String working = null;
        if (config.animations() != null) {
            BakedAnimations animations = GeckoLibCache.getBakedAnimations().get(config.animations());
            if (animations == null) {
                Ref.LOG.warn("geoModel {}: animations {} not found, the model stands still", config.model(), config.animations());
            } else {
                idle = known(config, animations, config.idle());
                working = known(config, animations, config.working());
            }
        }
        var animatable = new MachineGeoAnimatable(config, idle, working);
        be.setGeoAnimatable(animatable);
        return animatable;
    }

    @Nullable
    private static String known(GeoModelConfig config, BakedAnimations animations, @Nullable String name) {
        if (name != null && !animations.animations().containsKey(name)) {
            Ref.LOG.warn("geoModel {}: no animation '{}' in {}", config.model(), name, config.animations());
            return null;
        }
        return name;
    }

    private void fail(GeoModelConfig config, String why) {
        if (broken.add(config)) {
            Ref.LOG.warn("Not drawing geoModel {}: {}", config.model(), why);
        }
    }
}
