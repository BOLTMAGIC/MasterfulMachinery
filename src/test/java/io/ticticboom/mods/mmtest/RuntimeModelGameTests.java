package io.ticticboom.mods.mmtest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.controller.IControllerBlock;
import io.ticticboom.mods.mm.controller.IControllerPart;
import io.ticticboom.mods.mm.datagen.provider.MMBlockstateProvider;
import io.ticticboom.mods.mm.extra.IExtraBlock;
import io.ticticboom.mods.mm.extra.IExtraBlockPart;
import io.ticticboom.mods.mm.port.IPortBlock;
import io.ticticboom.mods.mm.port.IPortPart;
import io.ticticboom.mods.mm.setup.MMRegisters;
import net.minecraft.DetectedVersion;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelDispatcher;
import net.minecraft.data.DataGenerator;
import net.minecraft.gametest.framework.GameTestHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The client builds MM's block states, models and item definitions at runtime (MMBlockstateProvider writes them into
 * MM's generated pack). This runs that generator headless and reads what it wrote with the game's own block state
 * codec, so a broken variant key or a missing file shows up without starting a client.
 */
@GameTestHolder
public class RuntimeModelGameTests {

    @GameTest
    public static void generatedBlockStatesCoverEveryStateAndReferToWrittenModels(GameTestHelper helper) throws IOException {
        Path out = Files.createTempDirectory("mm-runtime-models");
        var generator = new DataGenerator.Cached(out, DetectedVersion.tryDetectVersion(), true);
        generator.addProvider(true, new MMBlockstateProvider(generator));
        generator.run();
        Path assets = out.resolve("assets").resolve(Ref.ID);

        int checked = 0;
        for (var entry : MMRegisters.BLOCKS.getEntries()) {
            if (!entry.isBound()) continue;
            var block = entry.get();
            if (!(block instanceof IPortBlock || block instanceof IControllerBlock || block instanceof IExtraBlock)) continue;
            String path = entry.getId().getPath();
            Path stateFile = assets.resolve("blockstates/" + path + ".json");
            helper.assertTrue(Files.exists(stateFile), "no block state written for " + path);
            JsonObject json = JsonParser.parseString(Files.readString(stateFile)).getAsJsonObject();
            var variants = json.getAsJsonObject("variants");
            helper.assertTrue(variants != null && !variants.isEmpty(), "no variants for " + path);
            if (variants.has("") && variants.getAsJsonObject("").has("type")) {
                // the connected-texture model is a custom block state model, registered on the client only
                helper.assertTrue(Files.exists(assets.resolve("models/block/" + path + ".json")), "no item-side model for connected " + path);
                checked++;
                continue;
            }
            var dispatcher = BlockStateModelDispatcher.VANILLA_CODEC.codec().parse(JsonOps.INSTANCE, json)
                    .getOrThrow(error -> new IllegalStateException(path + ": " + error));
            int states = block.getStateDefinition().getPossibleStates().size();
            int mapped = dispatcher.instantiate(block.getStateDefinition(), () -> path).size();
            helper.assertTrue(mapped == states, path + " maps " + mapped + " of its " + states + " block states");
            for (var variant : variants.entrySet()) {
                var model = variant.getValue().getAsJsonObject().get("model").getAsString();
                var id = net.minecraft.resources.Identifier.parse(model);
                if (id.getNamespace().equals(Ref.ID)) {
                    helper.assertTrue(Files.exists(assets.resolve("models/" + id.getPath() + ".json")), path + " refers to a missing model " + model);
                }
            }
            checked++;
        }
        helper.assertTrue(checked > 0, "no data-driven MM blocks to check");

        for (var entry : MMRegisters.ITEMS.getEntries()) {
            if (!entry.isBound()) continue;
            var item = entry.get();
            if (!(item instanceof IControllerPart || item instanceof IPortPart || item instanceof IExtraBlockPart)) continue;
            String path = entry.getId().getPath();
            helper.assertTrue(Files.exists(assets.resolve("items/" + path + ".json")), "no item definition for " + path);
            helper.assertTrue(Files.exists(assets.resolve("models/item/" + path + ".json")), "no item model for " + path);
        }
        helper.succeed();
    }
}
