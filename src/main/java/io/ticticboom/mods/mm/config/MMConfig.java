package io.ticticboom.mods.mm.config;

import net.minecraftforge.fml.config.ModConfig;

public class MMConfig {
    public static boolean DEBUG_TOOL = true;
    public static boolean JEI_RECIPE_SPLIT = true;
    public static boolean DEFAULT_PORT_AUTO_PUSH = false;

    public static void bake() {
        DEBUG_TOOL = MMConfigSetup.COMMON.debugTool.get();
        JEI_RECIPE_SPLIT = MMConfigSetup.COMMON.splitRecipesJei.get();
        DEFAULT_PORT_AUTO_PUSH = MMConfigSetup.COMMON.portsAutoExtractByDefault.get();
    }
}
