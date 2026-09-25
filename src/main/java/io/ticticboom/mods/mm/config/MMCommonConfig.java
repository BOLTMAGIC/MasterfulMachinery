package io.ticticboom.mods.mm.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class MMCommonConfig {
    public final ForgeConfigSpec.BooleanValue debugTool;
    public final ForgeConfigSpec.BooleanValue splitRecipesJei;
    public final ForgeConfigSpec.BooleanValue portsAutoExtractByDefault;
    public final ForgeConfigSpec.IntValue portAutoIOInterval;
    public final ForgeConfigSpec.IntValue networkLinkOutputInterval;
    public final ForgeConfigSpec.BooleanValue networkLinkOpBypass;
    public final ForgeConfigSpec.BooleanValue networkLinkSendOnRemove;
    public final ForgeConfigSpec.IntValue networkLinkSearchRadius;
    public final ForgeConfigSpec.BooleanValue asyncStructureValidation;
    public final ForgeConfigSpec.IntValue structureValidationRate;
    public final ForgeConfigSpec.BooleanValue previewBlueprintScreen;
    public final ForgeConfigSpec.BooleanValue parallelProcessingDefault;
    public final ForgeConfigSpec.IntValue maxParallelRecipes;
    public final ForgeConfigSpec.BooleanValue showJeiMaxParallel;

    public MMCommonConfig(ForgeConfigSpec.Builder builder) {
        asyncStructureValidation = builder.comment("Enables async structure validation to improve TPS. Disable in case of issues. Default: true")
                .define("asyncValidation", true);
        structureValidationRate = builder.comment("How often controller will check structure. 1 means every tick, 20 means every second. Default: 10")
                .defineInRange("structureValidationRate", 10, 1, 100);
        debugTool = builder.comment("Enables the Debug Tool Item's functionality (Disable when on server). Default: true")
                .define("debugTool", true);
        splitRecipesJei = builder.comment("Splits JEI recipe viewer categories by the structure they belong to. Default: true")
                .define("splitRecipesJei", true);
        portsAutoExtractByDefault = builder.comment("The default value of 'autoPush' (when not set) on ports that support automatic extract to nearby storages.",
                        "When true, newly placed output ports start with all sides pushing. Sides can be changed per port in its GUI. Default: false")
                .define("portsAutoExtractByDefault", false);
        portAutoIOInterval = builder.comment("How often ports with enabled auto push/pull sides transfer, in ticks. Default: 10")
                .defineInRange("portAutoIOInterval", 10, 1, 200);
        parallelProcessingDefault = builder.comment("The default value of 'parallelProcessing' (when not set) on structures that support parallel processing. Default: false")
                .define("parallelProcessingDefault", false);
        maxParallelRecipes = builder.comment("The max Parallel Recipes per controller. Default: 5")
                .defineInRange("maxParallelRecipes", 5, 1, 100);
        showJeiMaxParallel = builder.comment("Show 'Max Parallel Processing' line in JEI structure view. Default: true")
                .define("showJeiMaxParallel", true);

        builder.comment("Network linker (needs AE2): links a multiblock to an owner and an AE2 network.")
                .push("network_link");
        networkLinkOutputInterval = builder.comment("How often linked machines send their output port contents to the AE2 network, in ticks. Default: 20")
                .defineInRange("outputInterval", 20, 1, 1200);
        networkLinkOpBypass = builder.comment("Operators (permission level 2+) can open and break any linked machine. Default: true")
                .define("opBypass", true);
        networkLinkSendOnRemove = builder.comment("When a port of a linked machine is removed (broken, exploded...), send its contents to the AE2 network instead of dropping them. Default: true")
                .define("sendContentsOnRemove", true);
        networkLinkSearchRadius = builder.comment("How far from a port to look for the controller it belongs to. Must cover your largest multiblock. Default: 16")
                .defineInRange("controllerSearchRadius", 16, 4, 64);
        builder.pop();

        builder.comment("Preview features that are not yet stable or ready for use.")
                .push("preview_features");

        previewBlueprintScreen = builder.comment("Blueprint screen (JEI structure view with more fancy buttons). Default: false")
                .define("previewBlueprintScreen", false);

        builder.pop();
    }
}
