package io.ticticboom.mods.mm.config;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.recipe.flex.FlexCost;

public class MMConfig {
    public static boolean DEBUG_TOOL = true;
    public static boolean JEI_RECIPE_SPLIT = true;
    public static boolean JEI_SHOW_MAX_PARALLEL = true;
    public static boolean JEI_FLEX_INPUT_COPIES = true;
    public static FlexCost FLEX_COST = FlexCost.LINEAR;
    public static boolean DEFAULT_PORT_AUTO_PUSH = false;
    public static boolean PREVIEW_BP_SCREEN = false;
    public static boolean PARALLEL_PROCESSING_DEFAULT = false;
    public static int MAX_PARALLEL_RECIPES = 5;
    public static int PORT_AUTO_IO_INTERVAL = 10;
    public static int NETWORK_LINK_OUTPUT_INTERVAL = 20;
    public static int ASSEMBLY_BLOCKS_PER_TICK = 2;
    public static int TOOL_ENERGY_CAPACITY = 1_000_000;
    public static int TOOL_ENERGY_PER_PLACED_BLOCK = 50;
    public static int TOOL_ENERGY_PER_DISMANTLED_BLOCK = 25;
    public static int TOOL_ENERGY_RECEIVE_RATE = 10_000;
    public static boolean NETWORK_LINK_SEND_ON_REMOVE = true;
    public static boolean NETWORK_LINK_OP_BYPASS = true;

    public static void bake() {
        DEBUG_TOOL = MMConfigSetup.COMMON.debugTool.get();
        JEI_RECIPE_SPLIT = MMConfigSetup.COMMON.splitRecipesJei.get();
        JEI_SHOW_MAX_PARALLEL = MMConfigSetup.COMMON.showJeiMaxParallel.get();
        JEI_FLEX_INPUT_COPIES = MMConfigSetup.COMMON.showJeiFlexInputCopies.get();
        DEFAULT_PORT_AUTO_PUSH = MMConfigSetup.COMMON.portsAutoExtractByDefault.get();
        PREVIEW_BP_SCREEN = MMConfigSetup.COMMON.previewBlueprintScreen.get();
        PARALLEL_PROCESSING_DEFAULT = MMConfigSetup.COMMON.parallelProcessingDefault.get();
        MAX_PARALLEL_RECIPES = MMConfigSetup.COMMON.maxParallelRecipes.get();
        PORT_AUTO_IO_INTERVAL = MMConfigSetup.COMMON.portAutoIOInterval.get();
        NETWORK_LINK_OUTPUT_INTERVAL = MMConfigSetup.COMMON.networkLinkOutputInterval.get();
        ASSEMBLY_BLOCKS_PER_TICK = MMConfigSetup.COMMON.assemblyBlocksPerTick.get();
        TOOL_ENERGY_CAPACITY = MMConfigSetup.COMMON.toolEnergyCapacity.get();
        TOOL_ENERGY_PER_PLACED_BLOCK = MMConfigSetup.COMMON.toolEnergyPerPlacedBlock.get();
        TOOL_ENERGY_PER_DISMANTLED_BLOCK = MMConfigSetup.COMMON.toolEnergyPerDismantledBlock.get();
        TOOL_ENERGY_RECEIVE_RATE = MMConfigSetup.COMMON.toolEnergyReceiveRate.get();
        NETWORK_LINK_SEND_ON_REMOVE = MMConfigSetup.COMMON.networkLinkSendOnRemove.get();
        NETWORK_LINK_OP_BYPASS = MMConfigSetup.COMMON.networkLinkOpBypass.get();
        try {
            FLEX_COST = FlexCost.of(MMConfigSetup.COMMON.flexCostScaling.get(), MMConfigSetup.COMMON.flexCostTable.get());
        } catch (RuntimeException e) {
            Ref.LOG.error("mm-common.toml [flex]: {}; using LINEAR", e.getMessage());
            FLEX_COST = FlexCost.LINEAR;
        }
    }
}
