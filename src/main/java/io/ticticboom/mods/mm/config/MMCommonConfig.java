package io.ticticboom.mods.mm.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class MMCommonConfig {
    public final ForgeConfigSpec.BooleanValue debugTool;
    public final ForgeConfigSpec.BooleanValue splitRecipesJei;
    public final ForgeConfigSpec.BooleanValue portsAutoExtractByDefault;
    public final ForgeConfigSpec.BooleanValue asyncStructureValidation;
    public final ForgeConfigSpec.IntValue structureValidationRate;
    public final ForgeConfigSpec.BooleanValue previewBlueprintScreen;
    public final ForgeConfigSpec.BooleanValue parallelProcessingDefault;
    public final ForgeConfigSpec.IntValue maxParallelRecipes;
    public final ForgeConfigSpec.IntValue recipeTickRate;
    public final ForgeConfigSpec.IntValue portTickRate;

    public MMCommonConfig(ForgeConfigSpec.Builder builder) {
        asyncStructureValidation = builder.comment("Enables async structure validation to improve TPS. Disable in case of issues. Default: true")
                .define("asyncValidation", true);
        structureValidationRate = builder.comment("How often controller will check structure. 1 means every tick, 20 means every second. Default: 20")
                .defineInRange("structureValidationRate", 20, 1, 100);
        debugTool = builder.comment("Enables the Debug Tool Item's functionality (Disable when on server). Default: true")
                .define("debugTool", true);
        splitRecipesJei = builder.comment("Splits JEI recipe viewer categroies by the structure they belong to. Default: true")
                .define("splitRecipesJei", true);
        portsAutoExtractByDefault = builder.comment("The default value of 'autoPush' (when not set) on ports that support automatic extract to nearby storages. Default: false")
                .define("portsAutoExtractByDefault", false);
        parallelProcessingDefault = builder.comment("The default value of 'parallelProcessing' (when not set) on structures that support parallel processing. Default: false")
                .define("parallelProcessingDefault", false);
        maxParallelRecipes = builder.comment("The max Parallel Recipes per controller. Default: 5")
                .defineInRange("maxParallelRecipes", 5, 1, 100);
        recipeTickRate = builder.comment("How often recipes are ticked. 1 means every tick, 5 means every 5 ticks. Default: 1")
                .defineInRange("recipeTickRate", 1, 1, 20);
        portTickRate = builder.comment("How often ports are ticked. 1 means every tick, 10 means every 10 ticks. Default: 1")
                .defineInRange("portTickRate", 1, 1, 20);

        builder.comment("Preview features that are not yet stable or ready for use.")
                .push("preview_features");

        previewBlueprintScreen = builder.comment("Blueprint screen (JEI structure view with more fancy buttons). Default: false")
                .define("previewBlueprintScreen", false);

        builder.pop();
    }
}
