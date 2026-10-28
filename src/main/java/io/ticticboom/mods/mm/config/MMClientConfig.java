package io.ticticboom.mods.mm.config;

import io.ticticboom.mods.mm.util.ColorUtil;
import net.neoforged.neoforge.common.ModConfigSpec;

public class MMClientConfig {
    public final ModConfigSpec.BooleanValue tintControllerScreen;
    public final ModConfigSpec.ConfigValue<String> controllerUnformedColor;
    public final ModConfigSpec.ConfigValue<String> controllerIdleColor;
    public final ModConfigSpec.ConfigValue<String> controllerWorkingColor;
    public final ModConfigSpec.BooleanValue bigControllerScreen;
    public final ModConfigSpec.BooleanValue workingEffects;
    public final ModConfigSpec.BooleanValue portStatusLight;

    public MMClientConfig(ModConfigSpec.Builder builder) {
        builder.push("controller");
        tintControllerScreen = builder.comment("Color the controller's screen by machine state. When false the screen keeps its green look.",
                        "A controller can override these colors with unformedColor / idleColor / workingColor (KubeJS or JSON).")
                .define("tintControllerScreen", true);
        controllerUnformedColor = builder.comment("Screen color while the multiblock is not built, as #RRGGBB")
                .define("unformedColor", "#FF6B5C", MMClientConfig::isColor);
        controllerIdleColor = builder.comment("Screen color while the multiblock is built but not processing, as #RRGGBB")
                .define("idleColor", "#5CFF89", MMClientConfig::isColor);
        controllerWorkingColor = builder.comment("Screen color while a recipe is running, as #RRGGBB")
                .define("workingColor", "#FFC94D", MMClientConfig::isColor);
        workingEffects = builder.comment("Play the sound and show the particles a controller sets with workingSound / workingParticle",
                        "while its machine is working. Controllers that set neither stay silent either way.")
                .define("workingEffects", true);
        bigControllerScreen = builder.comment("Open the controller screen large (sized to the window) instead of small.",
                        "The button in the screen's top-right corner switches it too.")
                .define("bigScreen", true);
        builder.pop();

        builder.push("ports");
        portStatusLight = builder.comment("Show a status light on ports in their machine's state color.")
                .define("statusLight", true);
        builder.pop();
    }

    private static boolean isColor(Object value) {
        return value instanceof String s && ColorUtil.parse(s) != null;
    }
}
