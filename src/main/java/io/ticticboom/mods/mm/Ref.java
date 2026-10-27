package io.ticticboom.mods.mm;

import com.google.gson.Gson;
import io.ticticboom.mods.mm.log.LogContextStack;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Ref {
    public static final String ID = "mm";
    public static final Logger LOG = LogManager.getLogger("MasterfulMachinery");
    public static final LogContextStack LCTX = new LogContextStack();
    public static final Gson GSON = new Gson();

    public static ResourceLocation id(String path) {
        return ResourceLocation.tryBuild(ID, path);
    }

    public static final ResourceLocation SCANNER_CAP = id("scanner_selection");
    public static String NBT_STORAGE_KEY = "MMStorage";

    public static final class Registry {
        public static final ResourceLocation STRUCTURES = id("structures");
        public static final ResourceLocation PORCESSES = id("processes");
    }


    public static class Ports {
        public static final ResourceLocation ITEM = id("item");
        public static final ResourceLocation FLUID = id("fluid");
        public static final ResourceLocation ENERGY = id("energy");
        public static final ResourceLocation ENTITY = id("entity");
        public static final ResourceLocation MEK_CHEMICAL = id("mekanism/chemical");
        public static final ResourceLocation MEK_HEAT = id("mekanism/heat");
        public static final ResourceLocation CREATE_KINETIC = id("create/kinetic");
        public static final ResourceLocation BOTANIA_MANA = id("botania/mana");
        public static final ResourceLocation ARS_SOURCE = id("ars_nouveau/source");
        public static final ResourceLocation PROJECTE_EMC = id("projecte/emc");
        public static final ResourceLocation AE2_PATTERN = id("ae2/pattern");
        public static final ResourceLocation NUCLEAR_RADIATION = id("nuclear_radiation/radiation");

        public static final ResourceLocation REPLICATION_MATTER = id("replication/matter");
        public static final ResourceLocation REPLICATION_LINK = id("replication/link");

        public static final ResourceLocation PNEUMATIC_AIR = id("pneumaticcraft/air");
        public static final ResourceLocation PNEUMATIC_TEMPERATURE = id("pneumaticcraft/temperature");
    }

    public static class Controller {
        public static final ResourceLocation MACHINE = id("machine");
        public static final ResourceLocation SINGLE = id("single");
    }

    public static class RecipeEntries {
        public static final ResourceLocation CONSUME_INPUT = id("input/consume");
        public static final ResourceLocation SIMPLE_OUTPUT = id("output/simple");
        public static final ResourceLocation WEIGHTED_OUTPUT = id("output/weighted");
        public static final ResourceLocation FLEX_INPUT = id("input/flex");
        public static final ResourceLocation FLEX_OUTPUT = id("output/flex");
        public static final ResourceLocation COUNT_INPUT = id("input/count");
        public static final ResourceLocation FLEX_MATCH_INPUT = id("input/flex_match");
    }

    public static class RecipeConditions {
        public static final ResourceLocation DIMENSION = id("dimension");
        public static final ResourceLocation WEATHER = id("weather");
        public static final ResourceLocation BIOME = id("biome");
        public static final ResourceLocation TIME = id("time");
        public static final ResourceLocation HEIGHT = id("height");
        public static final ResourceLocation REDSTONE = id("redstone");
        public static final ResourceLocation TIER = id("tier");
    }

    public static class StructureAttachments {
        public static final ResourceLocation STATE_LISTS = id("state_lists");
        public static final ResourceLocation VISUALS = id("visuals");
    }

    public static class ExtraBlocks {
        public static final ResourceLocation CIRCUIT = id("circuit");
        public static final ResourceLocation GEARBOX = id("gearbox");
        public static final ResourceLocation VENT = id("vent");
    }

    public static class Textures {
        public static final ResourceLocation BASE_BLOCK = id("block/base_block");
        public static final ResourceLocation CONTROLLER_OVERLAY = id("block/controller_cutout");
        public static final ResourceLocation CONTROLLER_FRAME = id("block/controller_frame");

        public static ResourceLocation controllerScreen(String state) {
            return id("block/controller_screen_" + state);
        }

        public static final ResourceLocation INPUT_ITEM_PORT_OVERLAY = id("block/base_ports/item_input_cutout");
        public static final ResourceLocation OUTPUT_ITEM_PORT_OVERLAY = id("block/base_ports/item_output_cutout");

        public static final ResourceLocation INPUT_FLUID_PORT_OVERLAY = id("block/base_ports/fluid_input_cutout");
        public static final ResourceLocation OUTPUT_FLUID_PORT_OVERLAY = id("block/base_ports/fluid_output_cutout");

        public static final ResourceLocation INPUT_ENERGY_PORT_OVERLAY = id("block/base_ports/energy_input_cutout");
        public static final ResourceLocation OUTPUT_ENERGY_PORT_OVERLAY = id("block/base_ports/energy_output_cutout");

        public static final ResourceLocation INPUT_ENTITY_PORT_OVERLAY = id("block/base_ports/entity_input_cutout");
        public static final ResourceLocation OUTPUT_ENTITY_PORT_OVERLAY = id("block/base_ports/entity_output_cutout");

        public static final ResourceLocation CIRCUIT_OVERLAY = id("block/circuit_cutout");
        public static final ResourceLocation GEARBOX_OVERLAY = id("block/gearbox_cutout");
        public static final ResourceLocation VENT_OVERLAY = id("block/vent_cutout");





        public static final ResourceLocation INPUT_CHEMICAL_PORT_OVERLAY = id("block/compat_ports/mekanism_chemical_input_cutout");
        public static final ResourceLocation OUTPUT_CHEMICAL_PORT_OVERLAY = id("block/compat_ports/mekanism_chemical_output_cutout");

        public static final ResourceLocation INPUT_MEK_HEAT_PORT_OVERLAY = id("block/compat_ports/mekanism_heat_input_cutout");
        public static final ResourceLocation OUTPUT_MEK_HEAT_PORT_OVERLAY = id("block/compat_ports/mekanism_heat_output_cutout");

        public static final ResourceLocation INPUT_KINETIC_PORT_OVERLAY = id("block/compat_ports/create_rotation_input_cutout");
        public static final ResourceLocation OUTPUT_KINETIC_PORT_OVERLAY = id("block/compat_ports/create_rotation_output_cutout");

        public static final ResourceLocation INPUT_PNCR_AIR_PORT_OVERLAY = id("block/compat_ports/pncr_pressure_input_cutout");
        public static final ResourceLocation OUTPUT_PNCR_AIR_PORT_OVERLAY = id("block/compat_ports/pncr_pressure_output_cutout");

        public static final ResourceLocation INPUT_REPLICATION_MATTER_PORT_OVERLAY = id("block/compat_ports/replication_matter_input_cutout");
        public static final ResourceLocation OUTPUT_REPLICATION_MATTER_PORT_OVERLAY = id("block/compat_ports/replication_matter_output_cutout");

        public static final ResourceLocation INPUT_REPLICATION_LINK_PORT_OVERLAY = id("block/compat_ports/replication_link_input_cutout");
        public static final ResourceLocation OUTPUT_REPLICATION_LINK_PORT_OVERLAY = id("block/compat_ports/replication_link_output_cutout");

        public static final ResourceLocation INPUT_BOTANIA_MANA_PORT_OVERLAY = id("block/compat_ports/botania_mana_input_cutout");
        public static final ResourceLocation OUTPUT_BOTANIA_MANA_PORT_OVERLAY = id("block/compat_ports/botania_mana_output_cutout");

        public static final ResourceLocation INPUT_AE2_PATTERN_PORT_OVERLAY = id("block/compat_ports/ae2_pattern_input_cutout");
        public static final ResourceLocation OUTPUT_AE2_PATTERN_PORT_OVERLAY = id("block/compat_ports/ae2_pattern_output_cutout");

        public static final ResourceLocation INPUT_ARS_SOURCE_PORT_OVERLAY = id("block/compat_ports/ars_source_input_cutout");
        public static final ResourceLocation OUTPUT_ARS_SOURCE_PORT_OVERLAY = id("block/compat_ports/ars_source_output_cutout");

        public static final ResourceLocation INPUT_PROJECTE_EMC_PORT_OVERLAY = id("block/compat_ports/projecte_emc_input_cutout");
        public static final ResourceLocation OUTPUT_PROJECTE_EMC_PORT_OVERLAY = id("block/compat_ports/projecte_emc_output_cutout");

        public static final ResourceLocation INPUT_NUCLEAR_RADIATION_PORT_OVERLAY = id("block/compat_ports/nuclear_radiation_input_cutout");
        public static final ResourceLocation OUTPUT_NUCLEAR_RADIATION_PORT_OVERLAY = id("block/compat_ports/nuclear_radiation_output_cutout");
    }

    public static class UiTextures {
        public static final ResourceLocation GUI_LARGE_JEI = id("textures/gui/gui_large_jei.png");
        public static final ResourceLocation GUI_LARGE = id("textures/gui/gui_large.png");
        public static final ResourceLocation PORT_GUI = id("textures/gui/port_gui.png");
        public static final ResourceLocation SCANNER_GUI = id("textures/gui/scanner_gui.png");
        public static final ResourceLocation SLOT_PARTS = id("textures/gui/slot_parts.png");
        public static final ResourceLocation CREATIVE_TAB_BG = id("textures/gui/tab_item_search.png");
        public static final ResourceLocation BUTTON_ACTIVE = id("textures/gui/parts/button_active.png");
        public static final ResourceLocation BUTTON_PRESSED = id("textures/gui/parts/button_pressed.png");
        public static final ResourceLocation ARROW_LEFT = id("textures/gui/parts/arrow_left.png");
        public static final ResourceLocation ARROW_RIGHT = id("textures/gui/parts/arrow_right.png");
        public static final ResourceLocation TILING_GUI = id("textures/gui/parts/gui_bg_tile_borders.png");
    }

}
