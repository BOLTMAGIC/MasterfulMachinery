package io.ticticboom.mods.mm;

import com.google.gson.Gson;
import io.ticticboom.mods.mm.log.LogContextStack;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Ref {
    public static final String ID = "mm";
    public static final Logger LOG = LogManager.getLogger("MasterfulMachinery");
    public static final LogContextStack LCTX = new LogContextStack();
    public static final Gson GSON = new Gson();

    public static Identifier id(String path) {
        return Identifier.tryBuild(ID, path);
    }

    public static final Identifier SCANNER_CAP = id("scanner_selection");
    public static String NBT_STORAGE_KEY = "MMStorage";

    public static final class Registry {
        public static final Identifier STRUCTURES = id("structures");
        public static final Identifier PORCESSES = id("processes");
    }


    public static class Ports {
        public static final Identifier ITEM = id("item");
        public static final Identifier FLUID = id("fluid");
        public static final Identifier ENERGY = id("energy");
        public static final Identifier ENTITY = id("entity");
        public static final Identifier MEK_CHEMICAL = id("mekanism/chemical");
        public static final Identifier MEK_HEAT = id("mekanism/heat");
        public static final Identifier CREATE_KINETIC = id("create/kinetic");
        public static final Identifier BOTANIA_MANA = id("botania/mana");
        public static final Identifier ARS_SOURCE = id("ars_nouveau/source");
        public static final Identifier PROJECTE_EMC = id("projecte/emc");
        public static final Identifier AE2_PATTERN = id("ae2/pattern");
        public static final Identifier NUCLEAR_RADIATION = id("nuclear_radiation/radiation");

        public static final Identifier REPLICATION_MATTER = id("replication/matter");
        public static final Identifier REPLICATION_LINK = id("replication/link");

        public static final Identifier PNEUMATIC_AIR = id("pneumaticcraft/air");
        public static final Identifier PNEUMATIC_TEMPERATURE = id("pneumaticcraft/temperature");
    }

    public static class Controller {
        public static final Identifier MACHINE = id("machine");
        public static final Identifier SINGLE = id("single");
    }

    public static class RecipeEntries {
        public static final Identifier CONSUME_INPUT = id("input/consume");
        public static final Identifier SIMPLE_OUTPUT = id("output/simple");
        public static final Identifier WEIGHTED_OUTPUT = id("output/weighted");
        public static final Identifier FLEX_INPUT = id("input/flex");
        public static final Identifier FLEX_OUTPUT = id("output/flex");
        public static final Identifier COUNT_INPUT = id("input/count");
        public static final Identifier FLEX_MATCH_INPUT = id("input/flex_match");
    }

    public static class RecipeConditions {
        public static final Identifier DIMENSION = id("dimension");
        public static final Identifier WEATHER = id("weather");
        public static final Identifier BIOME = id("biome");
        public static final Identifier TIME = id("time");
        public static final Identifier HEIGHT = id("height");
        public static final Identifier REDSTONE = id("redstone");
        public static final Identifier TIER = id("tier");
    }

    public static class StructureAttachments {
        public static final Identifier STATE_LISTS = id("state_lists");
        public static final Identifier VISUALS = id("visuals");
    }

    public static class ExtraBlocks {
        public static final Identifier CIRCUIT = id("circuit");
        public static final Identifier GEARBOX = id("gearbox");
        public static final Identifier VENT = id("vent");
    }

    public static class Textures {
        public static final Identifier BASE_BLOCK = id("block/base_block");
        public static final Identifier CONTROLLER_OVERLAY = id("block/controller_cutout");
        public static final Identifier CONTROLLER_FRAME = id("block/controller_frame");

        public static Identifier controllerScreen(String state) {
            return id("block/controller_screen_" + state);
        }

        public static final Identifier INPUT_ITEM_PORT_OVERLAY = id("block/base_ports/item_input_cutout");
        public static final Identifier OUTPUT_ITEM_PORT_OVERLAY = id("block/base_ports/item_output_cutout");

        public static final Identifier INPUT_FLUID_PORT_OVERLAY = id("block/base_ports/fluid_input_cutout");
        public static final Identifier OUTPUT_FLUID_PORT_OVERLAY = id("block/base_ports/fluid_output_cutout");

        public static final Identifier INPUT_ENERGY_PORT_OVERLAY = id("block/base_ports/energy_input_cutout");
        public static final Identifier OUTPUT_ENERGY_PORT_OVERLAY = id("block/base_ports/energy_output_cutout");

        public static final Identifier INPUT_ENTITY_PORT_OVERLAY = id("block/base_ports/entity_input_cutout");
        public static final Identifier OUTPUT_ENTITY_PORT_OVERLAY = id("block/base_ports/entity_output_cutout");

        public static final Identifier CIRCUIT_OVERLAY = id("block/circuit_cutout");
        public static final Identifier GEARBOX_OVERLAY = id("block/gearbox_cutout");
        public static final Identifier VENT_OVERLAY = id("block/vent_cutout");





        public static final Identifier INPUT_CHEMICAL_PORT_OVERLAY = id("block/compat_ports/mekanism_chemical_input_cutout");
        public static final Identifier OUTPUT_CHEMICAL_PORT_OVERLAY = id("block/compat_ports/mekanism_chemical_output_cutout");

        public static final Identifier INPUT_MEK_HEAT_PORT_OVERLAY = id("block/compat_ports/mekanism_heat_input_cutout");
        public static final Identifier OUTPUT_MEK_HEAT_PORT_OVERLAY = id("block/compat_ports/mekanism_heat_output_cutout");

        public static final Identifier INPUT_KINETIC_PORT_OVERLAY = id("block/compat_ports/create_rotation_input_cutout");
        public static final Identifier OUTPUT_KINETIC_PORT_OVERLAY = id("block/compat_ports/create_rotation_output_cutout");

        public static final Identifier INPUT_PNCR_AIR_PORT_OVERLAY = id("block/compat_ports/pncr_pressure_input_cutout");
        public static final Identifier OUTPUT_PNCR_AIR_PORT_OVERLAY = id("block/compat_ports/pncr_pressure_output_cutout");

        public static final Identifier INPUT_REPLICATION_MATTER_PORT_OVERLAY = id("block/compat_ports/replication_matter_input_cutout");
        public static final Identifier OUTPUT_REPLICATION_MATTER_PORT_OVERLAY = id("block/compat_ports/replication_matter_output_cutout");

        public static final Identifier INPUT_REPLICATION_LINK_PORT_OVERLAY = id("block/compat_ports/replication_link_input_cutout");
        public static final Identifier OUTPUT_REPLICATION_LINK_PORT_OVERLAY = id("block/compat_ports/replication_link_output_cutout");

        public static final Identifier INPUT_BOTANIA_MANA_PORT_OVERLAY = id("block/compat_ports/botania_mana_input_cutout");
        public static final Identifier OUTPUT_BOTANIA_MANA_PORT_OVERLAY = id("block/compat_ports/botania_mana_output_cutout");

        public static final Identifier INPUT_AE2_PATTERN_PORT_OVERLAY = id("block/compat_ports/ae2_pattern_input_cutout");
        public static final Identifier OUTPUT_AE2_PATTERN_PORT_OVERLAY = id("block/compat_ports/ae2_pattern_output_cutout");

        public static final Identifier INPUT_ARS_SOURCE_PORT_OVERLAY = id("block/compat_ports/ars_source_input_cutout");
        public static final Identifier OUTPUT_ARS_SOURCE_PORT_OVERLAY = id("block/compat_ports/ars_source_output_cutout");

        public static final Identifier INPUT_PROJECTE_EMC_PORT_OVERLAY = id("block/compat_ports/projecte_emc_input_cutout");
        public static final Identifier OUTPUT_PROJECTE_EMC_PORT_OVERLAY = id("block/compat_ports/projecte_emc_output_cutout");

        public static final Identifier INPUT_NUCLEAR_RADIATION_PORT_OVERLAY = id("block/compat_ports/nuclear_radiation_input_cutout");
        public static final Identifier OUTPUT_NUCLEAR_RADIATION_PORT_OVERLAY = id("block/compat_ports/nuclear_radiation_output_cutout");
    }

    public static class UiTextures {
        public static final Identifier GUI_LARGE_JEI = id("textures/gui/gui_large_jei.png");
        public static final Identifier GUI_LARGE = id("textures/gui/gui_large.png");
        public static final Identifier PORT_GUI = id("textures/gui/port_gui.png");
        public static final Identifier SCANNER_GUI = id("textures/gui/scanner_gui.png");
        public static final Identifier SLOT_PARTS = id("textures/gui/slot_parts.png");
        public static final Identifier CREATIVE_TAB_BG = id("textures/gui/tab_item_search.png");
        public static final Identifier BUTTON_ACTIVE = id("textures/gui/parts/button_active.png");
        public static final Identifier BUTTON_PRESSED = id("textures/gui/parts/button_pressed.png");
        public static final Identifier ARROW_LEFT = id("textures/gui/parts/arrow_left.png");
        public static final Identifier ARROW_RIGHT = id("textures/gui/parts/arrow_right.png");
        public static final Identifier TILING_GUI = id("textures/gui/parts/gui_bg_tile_borders.png");
    }

}
