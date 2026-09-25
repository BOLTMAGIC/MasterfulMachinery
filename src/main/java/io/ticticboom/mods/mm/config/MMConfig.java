package io.ticticboom.mods.mm.config;

public class MMConfig {
    public static boolean DEBUG_TOOL = true;
    public static boolean JEI_RECIPE_SPLIT = true;
    public static boolean JEI_SHOW_MAX_PARALLEL = true;
    public static boolean DEFAULT_PORT_AUTO_PUSH = false;
    public static int PORT_AUTO_IO_INTERVAL = 10;
    public static int NETWORK_LINK_OUTPUT_INTERVAL = 20;
    public static boolean NETWORK_LINK_OP_BYPASS = true;
    public static boolean NETWORK_LINK_SEND_ON_REMOVE = true;
    public static int NETWORK_LINK_SEARCH_RADIUS = 16;
    public static boolean PREVIEW_BP_SCREEN = false;
    public static boolean PARALLEL_PROCESSING_DEFAULT = false;
    public static int MAX_PARALLEL_RECIPES = 5;

    public static void bake() {
        DEBUG_TOOL = MMConfigSetup.COMMON.debugTool.get();
        JEI_RECIPE_SPLIT = MMConfigSetup.COMMON.splitRecipesJei.get();
        JEI_SHOW_MAX_PARALLEL = MMConfigSetup.COMMON.showJeiMaxParallel.get();
        DEFAULT_PORT_AUTO_PUSH = MMConfigSetup.COMMON.portsAutoExtractByDefault.get();
        PORT_AUTO_IO_INTERVAL = MMConfigSetup.COMMON.portAutoIOInterval.get();
        NETWORK_LINK_OUTPUT_INTERVAL = MMConfigSetup.COMMON.networkLinkOutputInterval.get();
        NETWORK_LINK_OP_BYPASS = MMConfigSetup.COMMON.networkLinkOpBypass.get();
        NETWORK_LINK_SEND_ON_REMOVE = MMConfigSetup.COMMON.networkLinkSendOnRemove.get();
        NETWORK_LINK_SEARCH_RADIUS = MMConfigSetup.COMMON.networkLinkSearchRadius.get();
        PREVIEW_BP_SCREEN = MMConfigSetup.COMMON.previewBlueprintScreen.get();
        PARALLEL_PROCESSING_DEFAULT = MMConfigSetup.COMMON.parallelProcessingDefault.get();
        MAX_PARALLEL_RECIPES = MMConfigSetup.COMMON.maxParallelRecipes.get();
    }
}
