package io.ticticboom.mods.mm.compat.geckolib;

import io.ticticboom.mods.mm.client.geo.GeoModelConfig;
import io.ticticboom.mods.mm.controller.machine.register.ControllerState;
import org.jetbrains.annotations.Nullable;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;

/**
 * One machine's model, client side: loops "idle" while formed and "working" while a recipe runs, unless its
 * animations are switched off in the controller screen.
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
    // the controller screen's Animation switch: the model stands still in its rest pose
    private boolean animationsHidden = false;

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

    public void setAnimationsHidden(boolean hidden) {
        this.animationsHidden = hidden;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        if (idle == null && working == null) {
            return;
        }
        controllers.add(new AnimationController<MachineGeoAnimatable>("machine", TRANSITION_TICKS, animState -> {
            RawAnimation animation = animationsHidden ? null : state == ControllerState.WORKING ? working : idle;
            return animation == null ? PlayState.STOP : animState.setAndContinue(animation);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
