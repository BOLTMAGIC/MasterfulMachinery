package io.ticticboom.mods.mm.structure;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.client.structure.GuiBlockRenderer;
import io.ticticboom.mods.mm.client.structure.GuiCountedItemStack;
import io.ticticboom.mods.mm.client.structure.GuiStructureRenderer;
import io.ticticboom.mods.mm.client.structure.PositionedCyclingBlockRenderer;
import io.ticticboom.mods.mm.compat.jei.SlotGrid;
import io.ticticboom.mods.mm.controller.MMControllerRegistry;
import io.ticticboom.mods.mm.model.IdList;
import io.ticticboom.mods.mm.piece.StructurePieceSetupMetadata;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import io.ticticboom.mods.mm.structure.attachment.StructureAttachments;
import io.ticticboom.mods.mm.structure.layout.PositionedLayoutPiece;
import io.ticticboom.mods.mm.structure.layout.StructureLayout;
import io.ticticboom.mods.mm.util.ParserUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class StructureModel {

    private final Identifier id;
    private final String name;
    private final Component displayName;
    private final IdList controllerIds;
    private final StructureLayout layout;

    @Getter
    private final StructureAttachments attachments;

    @Getter
    private final JsonObject config;

    @OnlyIn(Dist.CLIENT)
    private GuiStructureRenderer renderer;

    @OnlyIn(Dist.CLIENT)
    private List<GuiCountedItemStack> countedPartItems;

    @Getter
    @Setter
    @OnlyIn(Dist.CLIENT)
    private SlotGrid grid;

    public StructureModel(
            Identifier id,
            String name,
            IdList controllerIds,
            StructureLayout layout,
            StructureAttachments attachments,
            JsonObject config
    ) {
        this(id, name, Component.literal(name), controllerIds, layout, attachments, config);
    }

    public StructureModel(
            Identifier id,
            String name,
            Component displayName,
            IdList controllerIds,
            StructureLayout layout,
            StructureAttachments attachments,
            JsonObject config
    ) {
        this.id = id;
        this.name = name;
        this.displayName = displayName;
        this.controllerIds = controllerIds;
        this.layout = layout;
        this.attachments = attachments;
        this.config = config;
        if (FMLEnvironment.dist == Dist.CLIENT) {
            renderer = new GuiStructureRenderer(this);
//            countedPartItems = getCountedItemStacks();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public PositionedCyclingBlockRenderer controllerUiRenderer() {
        BlockPos pos = new BlockPos(0, 0, 0);
        List<Identifier> ids = controllerIds.getIds();
        var res = new ArrayList<GuiBlockRenderer>();
        for (Identifier id : ids) {
            Block controllerBlock = MMControllerRegistry.getControllerBlock(id);
            GuiBlockRenderer gbr = new GuiBlockRenderer(controllerBlock, List.of());
            gbr.setupAt(pos);
            res.add(gbr);
        }
        return new PositionedCyclingBlockRenderer(res, pos);
    }

    public static StructureModel parse(JsonObject json, Identifier structureId) {
        // "name" accepts a plain string or a { "translation": "key" } object.
        var name = ParserUtils.parseComponentKey(json.get("name"));
        var displayName = ParserUtils.parseComponent(json.get("name"));
        var layout = StructureLayout.parse(json, structureId);
        var ids = IdList.parse(json.get("controllerIds"));
        var attachments = StructureAttachments.parse(json);
        return new StructureModel(structureId, name, displayName, ids, layout, attachments, json);
    }

    public static JsonObject paramsToJson(Identifier id, String name, IdList controllerIds, StructureLayout layout) {
        return paramsToJson(id, name, controllerIds, layout, -1);
    }

    public static JsonObject paramsToJson(Identifier id, String name, IdList controllerIds, StructureLayout layout, int maxParallelRecipes) {
        var json = new JsonObject();
        json.addProperty("id", id.toString());
        json.addProperty("name", name);
        json.add("controllerIds", controllerIds.serialize());
        layout.serialize(json);
        // only include maxParallelRecipes if explicitly specified (>=0)
        if (maxParallelRecipes >= 0) {
            int v = maxParallelRecipes;
            if (v > 100) v = 100;
            json.addProperty("maxParallelRecipes", v);
        }
        return json;
    }

    public void validate() {
        var meta = new StructurePieceSetupMetadata(id, this);
        layout.validate(meta);
        attachments.validate(meta);
    }

    public boolean formed(Level level, BlockPos controllerPos) {
        return layout.formed(level, controllerPos, this);
    }

    public RecipeStorages getStorages(Level level, BlockPos controllerPos) {
        return layout.getRecipeStorages(level, controllerPos, this);
    }

    public @Nullable Rotation formedRotation(Level level, BlockPos controllerPos, @Nullable Rotation preferred) {
        return layout.formedRotation(level, controllerPos, this, preferred);
    }

    public RecipeStorages getStorages(Level level, BlockPos controllerPos, Rotation rotation) {
        return layout.getRecipeStorages(level, controllerPos, rotation);
    }

    public List<BlockPos> getPositions(Level level, BlockPos controllerPos) {
        return layout.getPositions(level, controllerPos, this);
    }

    public JsonObject getConfig() {
        return config;
    }

    public JsonObject debugFormed(Level level, BlockPos controllerPos) {
        var json = new JsonObject();
        var debugLayout = layout.debugFormed(level, controllerPos, this);
        json.addProperty("structureId", id.toString());
        json.addProperty("name", name);
        json.add("controllerIds", controllerIds.serialize());
        json.add("layout", debugLayout);
        addMissingPorts(json);
        return json;
    }

    public JsonObject debugSerialize() {
        var json = new JsonObject();
        json.addProperty("structureId", id.toString());
        json.addProperty("name", name);
        json.add("controllerIds", controllerIds.serialize());
        addMissingPorts(json);
        return json;
    }

    private void addMissingPorts(JsonObject json) {
        var missing = StructureProblems.missingPorts(id);
        if (missing.isEmpty()) {
            return;
        }
        var array = new com.google.gson.JsonArray();
        for (var portId : missing) {
            array.add(portId.toString());
        }
        json.add("missingPorts", array);
    }

    /**
     * Optional per-structure override for max parallel recipes.
     * Returns -1 when not specified (use controller/global defaults), or 0..100 when set.
     */
    public int maxParallelRecipes() {
        if (config == null) return -1;
        try {
            if (config.has("maxParallelRecipes")) {
                int v = config.get("maxParallelRecipes").getAsInt();
                if (v == -1) return -1;
                if (v < 0) return -1;
                return Math.min(v, 100);
            }
        } catch (RuntimeException ignored) { }
        return -1;
    }

    @OnlyIn(Dist.CLIENT)
    public GuiStructureRenderer getGuiRenderer() {
        return renderer;
    }

    public Identifier id() {
        return id;
    }

    public String debugPath() {
        return id.getNamespace() + "/" + id.getPath() + ".json";
    }

    public String name() {
        return name;
    }

    /** Display form of the name, resolving a datapack-supplied translation key where one was given. */
    public Component displayName() {
        return displayName;
    }

    public IdList controllerIds() {
        return controllerIds;
    }

    public StructureLayout layout() {
        return layout;
    }

    public List<GuiCountedItemStack> getCountedItemStacks() {
        if (countedPartItems != null) {
            return countedPartItems;
        }
        var result = new HashMap<String, GuiCountedItemStack>();
        for (PositionedLayoutPiece positionedPiece : layout.getPositionedPieces()) {
            var guiPiece = positionedPiece.piece().guiPiece();
            var blocks = guiPiece.getBlocks().stream().map(x -> x.asItem().getDefaultInstance()).toList();
            String valueId = positionedPiece.piece().valueId();
            if (!result.containsKey(valueId)) {
                result.put(valueId, new GuiCountedItemStack(1, blocks, guiPiece.getDisplay(),
                        valueId));
            } else {
                result.get(valueId).addCount(1);
            }
        }
        countedPartItems = new ArrayList<>(result.values());
        var controllerList = new ArrayList<ItemStack>();
        for (Identifier controller : controllerIds.getIds()) {
            Block controllerBlock = MMControllerRegistry.getControllerBlock(controller);
            assert controllerBlock != null;
            controllerList.add(controllerBlock.asItem().getDefaultInstance());
        }
        countedPartItems.add(new GuiCountedItemStack(1, controllerList, Component.translatable("gui.mm.structure.controller").withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD), "C"));
        return countedPartItems;
    }

    @Override
    public String toString() {
        return "StructureModel{id=" + id + ", name=" + name + "}";
    }
}