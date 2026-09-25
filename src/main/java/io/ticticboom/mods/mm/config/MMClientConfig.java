package io.ticticboom.mods.mm.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class MMClientConfig {
    public final ForgeConfigSpec.BooleanValue tintControllerScreen;
    public final ForgeConfigSpec.ConfigValue<String> controllerUnformedColor;
    public final ForgeConfigSpec.ConfigValue<String> controllerIdleColor;
    public final ForgeConfigSpec.ConfigValue<String> controllerWorkingColor;
    public final ForgeConfigSpec.BooleanValue workingEffects;
    public final ForgeConfigSpec.BooleanValue bigControllerScreen;
    public final ForgeConfigSpec.BooleanValue portStatusLight;

    public MMClientConfig(ForgeConfigSpec.Builder builder) {
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
        workingEffects = builder.comment("Play the sound and show the particles a controller sets with workingSound / workingParticle (KubeJS or JSON)",
                        "while its machine is working. Controllers that set neither stay silent either way.")
                .define("workingEffects", true);
        bigControllerScreen = builder.comment("Open the controller screen large (sized to the window, everything on one page) instead of small.",
                        "The button in the screen's top-right corner switches it too.")
                .define("bigScreen", true);
        builder.pop();

        builder.push("ports");
        portStatusLight = builder.comment("Show a status light on item, fluid, energy, chemical and mana ports in the machine's state color:",
                        "the controller's own unformedColor / idleColor / workingColor if it sets them, else the colors above.",
                        "When false the light blends into the port.")
                .define("statusLight", true);
        builder.pop();
    }

    private static boolean isColor(Object value) {
        return value instanceof String s && parseColor(s) != null;
    }

    /**
     * @return the RGB value of "#RRGGBB" / "RRGGBB", or null if it isn't a color
     */
    public static Integer parseColor(String value) {
        if (value == null) return null;
        var hex = value.trim();
        if (hex.startsWith("#")) hex = hex.substring(1);
        if (hex.length() != 6) return null;
        try {
            return Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
