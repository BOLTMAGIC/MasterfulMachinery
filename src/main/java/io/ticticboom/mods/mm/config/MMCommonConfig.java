package io.ticticboom.mods.mm.config;

import io.ticticboom.mods.mm.recipe.flex.FlexCost;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class MMCommonConfig {
    public final ModConfigSpec.BooleanValue debugTool;
    public final ModConfigSpec.BooleanValue splitRecipesJei;
    public final ModConfigSpec.BooleanValue portsAutoExtractByDefault;
    public final ModConfigSpec.IntValue structureValidationRate;
    public final ModConfigSpec.BooleanValue previewBlueprintScreen;
    public final ModConfigSpec.BooleanValue parallelProcessingDefault;
    public final ModConfigSpec.IntValue maxParallelRecipes;
    public final ModConfigSpec.BooleanValue showJeiMaxParallel;
    public final ModConfigSpec.BooleanValue showJeiFlexInputCopies;
    public final ModConfigSpec.EnumValue<FlexCost.Mode> flexCostScaling;
    public final ModConfigSpec.ConfigValue<List<? extends String>> flexCostTable;
    public final ModConfigSpec.IntValue portAutoIOInterval;
    public final ModConfigSpec.IntValue assemblyBlocksPerTick;
    public final ModConfigSpec.IntValue toolEnergyCapacity;
    public final ModConfigSpec.IntValue toolEnergyPerPlacedBlock;
    public final ModConfigSpec.IntValue toolEnergyPerDismantledBlock;
    public final ModConfigSpec.IntValue toolEnergyReceiveRate;
    public final ModConfigSpec.IntValue networkLinkOutputInterval;
    public final ModConfigSpec.BooleanValue networkLinkSendOnRemove;
    public final ModConfigSpec.BooleanValue networkLinkOpBypass;

    public MMCommonConfig(ModConfigSpec.Builder builder) {
        structureValidationRate = builder.comment("How often controller will check structure. 1 means every tick, 20 means every second. Default: 10")
                .defineInRange("structureValidationRate", 10, 1, 100);
        debugTool = builder.comment("Enables the Debug Tool Item's functionality (Disable when on server). Default: true")
                .define("debugTool", true);
        splitRecipesJei = builder.comment("Splits JEI recipe viewer categories by the structure they belong to. Default: true")
                .define("splitRecipesJei", true);
        portsAutoExtractByDefault = builder.comment("The default value of 'autoPush' (when not set) on ports that support automatic extract to nearby storages. Default: false")
                .define("portsAutoExtractByDefault", false);
        parallelProcessingDefault = builder.comment("The default value of 'parallelProcessing' (when not set) on structures that support parallel processing. Default: false")
                .define("parallelProcessingDefault", false);
        maxParallelRecipes = builder.comment("The max Parallel Recipes per controller. Default: 5")
                .defineInRange("maxParallelRecipes", 5, 1, 100);
        showJeiMaxParallel = builder.comment("Show 'Max Parallel Processing' line in JEI structure view. Default: true")
                .define("showJeiMaxParallel", true);
        showJeiFlexInputCopies = builder.comment("In JEI, show a flex recipe's input slot once per kind a run can take (\"max_types\", up to 9),",
                        "so players see how many kinds they can put in. When false, one slot. Default: true")
                .define("showJeiFlexInputCopies", true);
        portAutoIOInterval = builder.comment("How often ports with enabled auto push/pull sides transfer, in ticks. Default: 10")
                .defineInRange("portAutoIOInterval", 10, 1, 200);
        assemblyBlocksPerTick = builder.comment("How many blocks Assemble places per tick. Default: 2")
                .defineInRange("assemblyBlocksPerTick", 2, 1, 64);
        networkLinkOutputInterval = builder.comment("How often a machine linked to an AE2 network sends its outputs to it, in ticks. Default: 20")
                .defineInRange("networkLinkOutputInterval", 20, 1, 1200);
        networkLinkSendOnRemove = builder.comment("When a port of a linked machine is removed, send its contents to the AE2 network instead of dropping them. Default: true")
                .define("networkLinkSendOnRemove", true);
        networkLinkOpBypass = builder.comment("Operators may open and break machines linked to other players. Default: true")
                .define("networkLinkOpBypass", true);

        builder.push("tool");
        toolEnergyCapacity = builder.comment("Max FE the Structure Builder can store. Default: 1,000,000")
                .defineInRange("toolEnergyCapacity", 1_000_000, 1, Integer.MAX_VALUE);
        toolEnergyPerPlacedBlock = builder.comment("FE cost per block placed by the Structure Builder. Default: 50")
                .defineInRange("toolEnergyPerPlacedBlock", 50, 0, Integer.MAX_VALUE);
        toolEnergyPerDismantledBlock = builder.comment("FE cost per block dismantled by the Structure Builder. Default: 25")
                .defineInRange("toolEnergyPerDismantledBlock", 25, 0, Integer.MAX_VALUE);
        toolEnergyReceiveRate = builder.comment("Max FE/t the Structure Builder accepts from an external charger. Default: 10,000")
                .defineInRange("toolEnergyReceiveRate", 10_000, 1, Integer.MAX_VALUE);
        builder.pop();

        builder.comment("Flex recipes (\"mode\": \"flex\"): one run takes several kinds of item at once (e.g. mixed seeds).")
                .push("flex");
        flexCostScaling = builder.comment("How the other consumed inputs of a flex run (energy, fluids, items...) grow with the number of kinds it takes.",
                        "LINEAR: cost x kinds (10 kinds cost 10 times one kind). ONCE: the same cost for any number of kinds.",
                        "TABLE: the multiplier comes from costTable. A recipe can override this with \"flex_cost\". Default: LINEAR")
                .defineEnum("costScaling", FlexCost.Mode.LINEAR);
        flexCostTable = builder.comment("For costScaling = TABLE: \"kinds=multiplier\" steps; a run uses the step with the most kinds not above its own.",
                        "Example: [\"1=1\", \"4=2\", \"7=3.5\"] means 1-3 kinds x1, 4-6 kinds x2, 7 or more x3.5. Default: [\"1=1\"]")
                .defineListAllowEmpty(List.of("costTable"), () -> List.of("1=1"), () -> "1=1", o -> o instanceof String step && FlexCost.isStep(step));
        builder.pop();

        builder.comment("Preview features that are not yet stable or ready for use.")
                .push("preview_features");

        previewBlueprintScreen = builder.comment("Blueprint screen (JEI structure view with more fancy buttons). Default: false")
                .define("previewBlueprintScreen", false);

        builder.pop();
    }
}
