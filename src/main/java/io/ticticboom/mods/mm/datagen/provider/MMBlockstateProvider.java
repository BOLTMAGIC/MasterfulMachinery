package io.ticticboom.mods.mm.datagen.provider;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.controller.IControllerBlock;
import io.ticticboom.mods.mm.controller.IControllerPart;
import io.ticticboom.mods.mm.extra.IExtraBlock;
import io.ticticboom.mods.mm.extra.IExtraBlockPart;
import io.ticticboom.mods.mm.port.IPortBlock;
import io.ticticboom.mods.mm.port.IPortPart;
import io.ticticboom.mods.mm.setup.MMRegisters;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Block states, block and item models, and item definitions for MM's pack-defined blocks, written into MM's generated
 * resource pack. NeoForge's BlockStateProvider/ItemModelProvider are gone since 1.21.4 (vanilla's ModelProvider replaces
 * them, and insists on covering every block of the namespace), so the JSON is written directly. Composite models
 * use NeoForge's "neoforge:composite" loader; overlays are forced translucent (render types left model JSON in 1.21.5).
 */
public class MMBlockstateProvider implements DataProvider {
    public record Variant(Identifier model, int yRot) {
    }

    private final PackOutput output;
    private final Map<String, JsonObject> blockstates = new LinkedHashMap<>();
    private final Map<String, JsonObject> models = new LinkedHashMap<>();
    private final Map<String, JsonObject> items = new LinkedHashMap<>();

    public MMBlockstateProvider(DataGenerator generator) {
        this.output = generator.getPackOutput();
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        blockstates.clear();
        models.clear();
        items.clear();
        for (DeferredHolder<Block, ? extends Block> entry : MMRegisters.BLOCKS.getEntries()) {
            if (!entry.isBound()) {
                continue;
            }
            Block block = entry.get();
            if (block instanceof IControllerBlock controllerPart) {
                controllerPart.generateModel(this);
            }
            if (block instanceof IPortBlock portBlock) {
                portBlock.generateModel(this);
            }
            if (block instanceof IExtraBlock eb) {
                eb.generateModel(this);
            }
        }
        for (DeferredHolder<Item, ? extends Item> entry : MMRegisters.ITEMS.getEntries()) {
            if (!entry.isBound()) {
                continue;
            }
            String id = null;
            boolean tinted = false;
            if (entry.get() instanceof IControllerPart controllerPart) {
                id = controllerPart.getModel().id();
                tinted = true;
            } else if (entry.get() instanceof IPortPart portPart) {
                id = portPart.getModel().id();
            } else if (entry.get() instanceof IExtraBlockPart ebp) {
                id = ebp.getModel().id();
            }
            if (id != null) {
                itemModel(id, tinted);
            }
        }
        List<CompletableFuture<?>> writes = new ArrayList<>();
        var root = output.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve(Ref.ID);
        blockstates.forEach((path, json) -> writes.add(DataProvider.saveStable(cache, json, root.resolve("blockstates/" + path + ".json"))));
        models.forEach((path, json) -> writes.add(DataProvider.saveStable(cache, json, root.resolve("models/" + path + ".json"))));
        items.forEach((path, json) -> writes.add(DataProvider.saveStable(cache, json, root.resolve("items/" + path + ".json"))));
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "MM block states and models";
    }

    private void itemModel(String id, boolean controllerTint) {
        var model = new JsonObject();
        model.addProperty("parent", Ref.id("block/" + id).toString());
        models.put("item/" + id, model);
        var definition = new JsonObject();
        var inner = new JsonObject();
        inner.addProperty("type", "minecraft:model");
        inner.addProperty("model", Ref.id("item/" + id).toString());
        if (controllerTint) {
            var tints = new JsonArray();
            var tint = new JsonObject();
            tint.addProperty("type", Ref.id("controller_screen").toString());
            tints.add(tint);
            inner.add("tints", tints);
        }
        definition.add("model", inner);
        items.put(id, definition);
    }

    // --- models ---

    private static Identifier blockModel(Identifier loc) {
        return Identifier.fromNamespaceAndPath(loc.getNamespace(), loc.getPath().contains("/") ? loc.getPath() : "block/" + loc.getPath());
    }

    private Identifier putModel(Identifier loc, JsonObject json) {
        var id = blockModel(loc);
        models.put(id.getPath(), json);
        return id;
    }

    private static JsonObject display() {
        var display = new JsonObject();
        for (String hand : new String[]{"thirdperson_lefthand", "thirdperson_righthand"}) {
            var t = new JsonObject();
            t.add("rotation", array(75, 45, 0));
            t.add("translation", array(0, 2.5, 0));
            t.add("scale", array(0.375, 0.375, 0.375));
            display.add(hand, t);
        }
        return display;
    }

    private static JsonArray array(Number... values) {
        var a = new JsonArray();
        for (Number v : values) {
            a.add(v);
        }
        return a;
    }

    private static JsonObject translucent(Identifier texture) {
        var material = new JsonObject();
        material.addProperty("sprite", texture.toString());
        material.addProperty("force_translucent", true);
        return material;
    }

    private static JsonObject cubeAll(Identifier texture) {
        var child = new JsonObject();
        child.addProperty("parent", "minecraft:block/cube_all");
        var textures = new JsonObject();
        textures.addProperty("all", texture.toString());
        child.add("textures", textures);
        return child;
    }

    /** A full-block element showing texture #key on the given faces. */
    private static JsonObject layer(String key, Identifier texture, boolean translucent, List<Direction> faces, int tintIndex) {
        var child = new JsonObject();
        child.addProperty("parent", "minecraft:block/block");
        var textures = new JsonObject();
        if (translucent) {
            textures.add(key, translucent(texture));
        } else {
            textures.addProperty(key, texture.toString());
        }
        child.add("textures", textures);
        var element = new JsonObject();
        element.add("from", array(0, 0, 0));
        element.add("to", array(16, 16, 16));
        var faceJson = new JsonObject();
        for (Direction face : faces) {
            var f = new JsonObject();
            f.add("uv", array(0, 0, 16, 16));
            f.addProperty("texture", "#" + key);
            if (tintIndex >= 0) {
                f.addProperty("tintindex", tintIndex);
            }
            faceJson.add(face.getSerializedName(), f);
        }
        element.add("faces", faceJson);
        var elements = new JsonArray();
        elements.add(element);
        child.add("elements", elements);
        return child;
    }

    private static JsonObject composite(Identifier particle, Map<String, JsonObject> children) {
        var json = new JsonObject();
        json.addProperty("parent", "minecraft:block/block");
        json.addProperty("loader", "neoforge:composite");
        var textures = new JsonObject();
        textures.addProperty("particle", particle.toString());
        json.add("textures", textures);
        json.add("display", display());
        var childJson = new JsonObject();
        children.forEach(childJson::add);
        json.add("children", childJson);
        return json;
    }

    public Identifier dynamicBlockNorthOverlay(Identifier loc, Identifier baseTexture, Identifier overlayTexture) {
        var children = new LinkedHashMap<String, JsonObject>();
        children.put("base", cubeAll(baseTexture));
        children.put("overlay", layer("overlay", overlayTexture, true, List.of(Direction.NORTH), -1));
        return putModel(loc, composite(overlayTexture, children));
    }

    public Identifier controllerModel(Identifier loc, Identifier baseTexture, Identifier frameTexture, Identifier screenTexture) {
        var children = new LinkedHashMap<String, JsonObject>();
        children.put("base", cubeAll(baseTexture));
        children.put("overlay", layer("overlay", frameTexture, true, List.of(Direction.NORTH), -1));
        children.put("screen", layer("screen", screenTexture, true, List.of(Direction.NORTH), 0));
        return putModel(loc, composite(frameTexture, children));
    }

    public Identifier dynamicBlock(Identifier loc, Identifier baseTexture, Identifier overlayTexture) {
        var children = new LinkedHashMap<String, JsonObject>();
        children.put("base", cubeAll(baseTexture));
        children.put("overlay", layer("overlay", overlayTexture, true, List.of(Direction.values()), -1));
        return putModel(loc, composite(overlayTexture, children));
    }

    /**
     * A connected-texture block: the block state uses the mm:connected block state model, and the item a plain model
     * with the lone tile and the overlay. Returns the item's model.
     */
    public Identifier connectedBlock(Identifier loc, Identifier overlayTexture) {
        return dynamicBlock(loc, Ref.id("block/base_block_ctm0"), overlayTexture);
    }

    public void connectedState(Block block, Identifier overlayTexture) {
        var variant = new JsonObject();
        variant.addProperty("type", Ref.id("connected").toString());
        variant.addProperty("overlay", overlayTexture.toString());
        var variants = new JsonObject();
        variants.add("", variant);
        var json = new JsonObject();
        json.add("variants", variants);
        blockstates.put(BuiltInRegistries.BLOCK.getKey(block).getPath(), json);
    }

    public Identifier customBlock(Identifier loc, Identifier model) {
        var json = new JsonObject();
        json.addProperty("parent", model.toString());
        return putModel(loc, json);
    }

    // --- block states ---

    public void simpleBlock(Block block) {
        simpleBlock(block, blockModel(BuiltInRegistries.BLOCK.getKey(block)));
    }

    public void simpleBlock(Block block, Identifier model) {
        variants(block, state -> new Variant(model, 0));
    }

    public void directionalState(Block block, Identifier model, Property<Direction> facing) {
        variants(block, state -> new Variant(model, (int) state.getValue(facing).toYRot()));
    }

    public void variants(Block block, Function<BlockState, Variant> variantFor) {
        var variants = new JsonObject();
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            Variant variant = variantFor.apply(state);
            var json = new JsonObject();
            json.addProperty("model", variant.model().toString());
            if (variant.yRot() != 0) {
                json.addProperty("y", variant.yRot());
            }
            variants.add(key(state), json);
        }
        var json = new JsonObject();
        json.add("variants", variants);
        blockstates.put(BuiltInRegistries.BLOCK.getKey(block).getPath(), json);
    }

    private static String key(BlockState state) {
        return state.getValues()
                .map(value -> value.property().getName() + "=" + value.valueName())
                .collect(Collectors.joining(","));
    }
}
