package io.ticticboom.mods.mm.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The existing Forge settings exposed by the in-game MM config screen. */
public final class MMConfigOptions {
    public enum Scope { SERVER, CLIENT }
    public enum Kind { BOOLEAN, INTEGER, COLOR, BUILD_MODE }

    public record Option(String key, String label, Scope scope, Kind kind, int min, int max,
                         ForgeConfigSpec.ConfigValue<?> value) {
        public String current() { return String.valueOf(value.get()); }

        public boolean valid(String text) {
            try {
                return switch (kind) {
                    case BOOLEAN -> text.equals("true") || text.equals("false");
                    case INTEGER -> {
                        int parsed = Integer.parseInt(text);
                        yield parsed >= min && parsed <= max;
                    }
                    case COLOR -> MMClientConfig.parseColor(text) != null;
                    case BUILD_MODE -> {
                        try {
                            io.ticticboom.mods.mm.tool.ToolBuildMode.valueOf(text);
                            yield true;
                        } catch (IllegalArgumentException e) { yield false; }
                    }
                };
            } catch (NumberFormatException e) {
                return false;
            }
        }

        public boolean set(String text) {
            if (!valid(text)) return false;
            try {
                switch (kind) {
                    case BOOLEAN -> {
                        ((ForgeConfigSpec.BooleanValue) value).set(Boolean.parseBoolean(text));
                    }
                    case INTEGER -> {
                        int parsed = Integer.parseInt(text);
                        ((ForgeConfigSpec.IntValue) value).set(parsed);
                    }
                    case COLOR -> {
                        @SuppressWarnings("unchecked") ForgeConfigSpec.ConfigValue<String> color =
                                (ForgeConfigSpec.ConfigValue<String>) value;
                        color.set(text.startsWith("#") ? text : "#" + text);
                    }
                    case BUILD_MODE -> {
                        @SuppressWarnings("unchecked") ForgeConfigSpec.EnumValue<io.ticticboom.mods.mm.tool.ToolBuildMode> mode =
                                (ForgeConfigSpec.EnumValue<io.ticticboom.mods.mm.tool.ToolBuildMode>) value;
                        mode.set(io.ticticboom.mods.mm.tool.ToolBuildMode.valueOf(text));
                    }
                }
                value.save();
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    }

    private static final MMCommonConfig S = MMConfigSetup.COMMON;
    private static final MMClientConfig C = MMConfigSetup.CLIENT;

    public static final List<Option> SERVER = List.of(
            bool("asyncValidation", "Async structure validation", Scope.SERVER, S.asyncStructureValidation),
            integer("structureValidationRate", "Validation interval (ticks)", Scope.SERVER, S.structureValidationRate, 1, 100),
            bool("debugTool", "Debug tool enabled", Scope.SERVER, S.debugTool),
            bool("splitRecipesJei", "Split JEI recipes by structure", Scope.SERVER, S.splitRecipesJei),
            bool("portsAutoExtractByDefault", "Port auto I/O default", Scope.SERVER, S.portsAutoExtractByDefault),
            integer("portAutoIOInterval", "Port auto I/O interval (ticks)", Scope.SERVER, S.portAutoIOInterval, 1, 200),
            integer("assemblyBlocksPerTick", "Assembly blocks per tick", Scope.SERVER, S.assemblyBlocksPerTick, 1, 64),
            bool("parallelProcessingDefault", "Parallel processing default", Scope.SERVER, S.parallelProcessingDefault),
            integer("maxParallelRecipes", "Maximum parallel recipes", Scope.SERVER, S.maxParallelRecipes, 1, 100),
            bool("showJeiMaxParallel", "Show parallel limit in JEI", Scope.SERVER, S.showJeiMaxParallel),
            integer("network_link.outputInterval", "Network output interval (ticks)", Scope.SERVER, S.networkLinkOutputInterval, 1, 1200),
            bool("network_link.opBypass", "Operator network bypass", Scope.SERVER, S.networkLinkOpBypass),
            bool("network_link.sendContentsOnRemove", "Send port contents on removal", Scope.SERVER, S.networkLinkSendOnRemove),
            integer("network_link.controllerSearchRadius", "Controller search radius", Scope.SERVER, S.networkLinkSearchRadius, 4, 64),
            integer("tool.energyCapacity", "Structure Builder FE capacity", Scope.SERVER, S.toolEnergyCapacity, 1, Integer.MAX_VALUE),
            integer("tool.energyPerPlacedBlock", "FE per placed block", Scope.SERVER, S.toolEnergyPerPlacedBlock, 0, Integer.MAX_VALUE),
            integer("tool.energyPerDismantledBlock", "FE per dismantled block", Scope.SERVER, S.toolEnergyPerDismantledBlock, 0, Integer.MAX_VALUE),
            integer("tool.energyReceiveRate", "FE receive rate", Scope.SERVER, S.toolEnergyReceiveRate, 1, Integer.MAX_VALUE),
            integer("tool.buildRange", "Structure Builder distance (blocks)", Scope.SERVER, S.toolBuildRange, 5, 128),
            integer("tool.buildBlocksPerTick", "Structure Builder blocks per tick", Scope.SERVER, S.toolBuildBlocksPerTick, 1, 1024),
            new Option("tool.buildMode", "Structure Builder placement mode", Scope.SERVER, Kind.BUILD_MODE, 0, 0, S.toolBuildMode),
            bool("preview_features.previewBlueprintScreen", "Preview blueprint screen", Scope.SERVER, S.previewBlueprintScreen)
    );

    public static final List<Option> CLIENT = List.of(
            bool("controller.tintControllerScreen", "Tint controller screen", Scope.CLIENT, C.tintControllerScreen),
            color("controller.unformedColor", "Unformed color", C.controllerUnformedColor),
            color("controller.idleColor", "Idle color", C.controllerIdleColor),
            color("controller.workingColor", "Working color", C.controllerWorkingColor),
            bool("controller.workingEffects", "Working sounds and particles", Scope.CLIENT, C.workingEffects),
            bool("controller.bigScreen", "Large controller screen", Scope.CLIENT, C.bigControllerScreen),
            bool("ports.statusLight", "Port status light", Scope.CLIENT, C.portStatusLight)
    );

    private static final Map<String, Option> SERVER_BY_KEY = byKey(SERVER);

    private MMConfigOptions() {}

    private static Option bool(String key, String label, Scope scope, ForgeConfigSpec.BooleanValue value) {
        return new Option(key, label, scope, Kind.BOOLEAN, 0, 0, value);
    }

    private static Option integer(String key, String label, Scope scope, ForgeConfigSpec.IntValue value, int min, int max) {
        return new Option(key, label, scope, Kind.INTEGER, min, max, value);
    }

    private static Option color(String key, String label, ForgeConfigSpec.ConfigValue<String> value) {
        return new Option(key, label, Scope.CLIENT, Kind.COLOR, 0, 0, value);
    }

    private static Map<String, Option> byKey(List<Option> options) {
        Map<String, Option> result = new LinkedHashMap<>();
        for (Option option : options) result.put(option.key(), option);
        return Map.copyOf(result);
    }

    public static Option server(String key) { return SERVER_BY_KEY.get(key); }

    public static Map<String, String> serverSnapshot() {
        Map<String, String> values = new LinkedHashMap<>();
        for (Option option : SERVER) values.put(option.key(), option.current());
        return values;
    }
}
