package io.ticticboom.mods.mm.compat.geckolib;

import io.ticticboom.mods.mm.client.geo.GeoModelConfig;
import io.ticticboom.mods.mm.controller.machine.register.ControllerState;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.util.RenderUtils;

/**
 * One machine's model, client side: loops "idle" while formed and "working" while a recipe runs.
 */
public class MachineGeoAnimatable implements GeoAnimatable {
    private static final int TRANSITION_TICKS = 5;

    private final GeoModelConfig config;
    @Nullable
    private final RawAnimation idle;
    @Nullable
    private final RawAnimation working;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private ControllerState state = ControllerState.IDLE;

    /**
     * @param idleName    null if the model has no idle animation (it stands still)
     * @param workingName null if the model has no working animation
     */
    public MachineGeoAnimatable(GeoModelConfig config, @Nullable String idleName, @Nullable String workingName) {
        this.config = config;
        this.idle = idleName == null ? null : RawAnimation.begin().thenLoop(idleName);
        this.working = workingName == null ? null : RawAnimation.begin().thenLoop(workingName);
    }

    public GeoModelConfig config() {
        return config;
    }

    public void setState(ControllerState state) {
        this.state = state;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        if (idle == null && working == null) {
            return;
        }
        controllers.add(new AnimationController<>(this, "machine", TRANSITION_TICKS, animState -> {
            RawAnimation animation = state == ControllerState.WORKING ? working : idle;
            return animation == null ? PlayState.STOP : animState.setAndContinue(animation);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public double getTick(Object object) {
        return RenderUtils.getCurrentTick();
    }
}
