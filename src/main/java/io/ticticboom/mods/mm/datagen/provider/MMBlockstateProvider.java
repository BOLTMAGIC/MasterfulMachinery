package io.ticticboom.mods.mm.datagen.provider;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.extra.IExtraBlock;
import io.ticticboom.mods.mm.controller.IControllerBlock;
import io.ticticboom.mods.mm.port.IPortBlock;
import io.ticticboom.mods.mm.setup.MMRegisters;
import net.minecraft.core.Direction;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.*;
import net.minecraftforge.client.model.generators.loaders.CompositeModelBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

public class MMBlockstateProvider extends BlockStateProvider {

    private final DataGenerator generator;

    public MMBlockstateProvider(DataGenerator generator, ExistingFileHelper exFileHelper) {
        super(generator.getPackOutput(), Ref.ID, exFileHelper);
        this.generator = generator;
    }

    @Override
    protected void registerStatesAndModels() {
        for (RegistryObject<Block> entry : MMRegisters.BLOCKS.getEntries()) {
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
    }

    public BlockModelBuilder dynamicBlockNorthOverlay(ResourceLocation loc, ResourceLocation baseTexture, ResourceLocation overlayTexture) {
        return models().getBuilder(loc.toString()).parent(new ModelFile.UncheckedModelFile(mcLoc("block/block")))
            .texture("particle", overlayTexture)
                .transforms()
                .transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND)
                .rotation(75F, 45F, 0F)
                .translation(0F, 2.5F, 0)
                .scale(0.375F, 0.375F, 0.375F)
                .end()
                .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
                .rotation(75F, 45F, 0F)
                .translation(0F, 2.5F, 0)
                .scale(0.375F, 0.375F, 0.375F)
                .end()
                .end()
                .customLoader(CompositeModelBuilder::begin)
                .child("base", this.models().nested().parent(new ModelFile.UncheckedModelFile(mcLoc("block/cube_all")))
                        .texture("all", baseTexture)
                        .renderType("solid")
                )
                .child("overlay", this.models().nested().parent(new ModelFile.UncheckedModelFile(mcLoc("block/block")))
                        .texture("overlay", overlayTexture)
                        .element()
                        .from(0, 0, 0)
                        .to(16, 16, 16)
                        .face(Direction.NORTH)
                        .uvs(0,0, 16, 16)
                        .texture("#overlay")
                        .end()
                        .end()
                        .renderType("translucent")
                )
                .end();
    }

    /**
     * Controller model: base block, the overlay frame on the north face, and the screen on top of it
     * with tintindex 0 so its color can follow the controller's state.
     */
    public BlockModelBuilder controllerModel(ResourceLocation loc, ResourceLocation baseTexture, ResourceLocation frameTexture, ResourceLocation screenTexture) {
        return models().getBuilder(loc.toString()).parent(new ModelFile.UncheckedModelFile(mcLoc("block/block")))
                .texture("particle", frameTexture)
                .transforms()
                .transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND)
                .rotation(75F, 45F, 0F)
                .translation(0F, 2.5F, 0)
                .scale(0.375F, 0.375F, 0.375F)
                .end()
                .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
                .rotation(75F, 45F, 0F)
                .translation(0F, 2.5F, 0)
                .scale(0.375F, 0.375F, 0.375F)
                .end()
                .end()
                .customLoader(CompositeModelBuilder::begin)
                .child("base", this.models().nested().parent(new ModelFile.UncheckedModelFile(mcLoc("block/cube_all")))
                        .texture("all", baseTexture)
                        .renderType("solid")
                )
                .child("overlay", this.models().nested().parent(new ModelFile.UncheckedModelFile(mcLoc("block/block")))
                        .texture("overlay", frameTexture)
                        .element()
                        .from(0, 0, 0)
                        .to(16, 16, 16)
                        .face(Direction.NORTH)
                        .uvs(0, 0, 16, 16)
                        .texture("#overlay")
                        .end()
                        .end()
                        .renderType("translucent")
                )
                .child("screen", this.models().nested().parent(new ModelFile.UncheckedModelFile(mcLoc("block/block")))
                        .texture("screen", screenTexture)
                        .element()
                        .from(0, 0, 0)
                        .to(16, 16, 16)
                        .face(Direction.NORTH)
                        .uvs(0, 0, 16, 16)
                        .texture("#screen")
                        .tintindex(0)
                        .end()
                        .end()
                        .renderType("translucent")
                )
                .end();
    }

    /**
     * Port model: the same as {@link #dynamicBlock} plus a status light on every face (tintindex 0).
     */
    public BlockModelBuilder portModel(ResourceLocation loc, ResourceLocation baseTexture, ResourceLocation overlayTexture, ResourceLocation lightTexture) {
        return models().getBuilder(loc.toString()).parent(new ModelFile.UncheckedModelFile(mcLoc("block/block")))
                .texture("particle", overlayTexture)
                .transforms()
                .transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND)
                .rotation(75F, 45F, 0F)
                .translation(0F, 2.5F, 0)
                .scale(0.375F, 0.375F, 0.375F)
                .end()
                .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
                .rotation(75F, 45F, 0F)
                .translation(0F, 2.5F, 0)
                .scale(0.375F, 0.375F, 0.375F)
                .end()
                .end()
                .customLoader(CompositeModelBuilder::begin)
                .child("base", this.models().nested().parent(new ModelFile.UncheckedModelFile(mcLoc("block/cube_all")))
                        .texture("all", baseTexture)
                        .renderType("solid")
                )
                .child("overlay", this.models().nested().parent(new ModelFile.UncheckedModelFile(mcLoc("block/block")))
                        .texture("overlay", overlayTexture)
                        .element()
                        .from(0, 0, 0)
                        .to(16, 16, 16)
                        .allFaces((dir, uv) -> uv.texture("#overlay").uvs(0, 0, 16, 16))
                        .end()
                        .renderType("translucent")
                )
                // children are drawn in JSON key order and the generator sorts keys alphabetically:
                // the light must sort after "overlay", or the overlay's transparent pixels cover it
                .child("status_light", this.models().nested().parent(new ModelFile.UncheckedModelFile(mcLoc("block/block")))
                        .texture("light", lightTexture)
                        .element()
                        .from(0, 0, 0)
                        .to(16, 16, 16)
                        .allFaces((dir, uv) -> uv.texture("#light").uvs(0, 0, 16, 16).tintindex(0))
                        .end()
                        .renderType("translucent")
                )
                .end();
    }

    public BlockModelBuilder dynamicBlock(ResourceLocation loc, ResourceLocation baseTexture, ResourceLocation overlayTexture) {
        return models().getBuilder(loc.toString()).parent(new ModelFile.UncheckedModelFile(mcLoc("block/block")))
                .texture("particle", overlayTexture)
                .transforms()
                .transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND)
                .rotation(75F, 45F, 0F)
                .translation(0F, 2.5F, 0)
                .scale(0.375F, 0.375F, 0.375F)
                .end()
                .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
                .rotation(75F, 45F, 0F)
                .translation(0F, 2.5F, 0)
                .scale(0.375F, 0.375F, 0.375F)
                .end()
                .end()
                .customLoader(CompositeModelBuilder::begin)
                .child("base", this.models().nested().parent(new ModelFile.UncheckedModelFile(mcLoc("block/cube_all")))
                        .texture("all", baseTexture)
                        .renderType("solid")
                )
                .child("overlay", this.models().nested().parent(new ModelFile.UncheckedModelFile(mcLoc("block/block")))
                        .texture("overlay", overlayTexture)
                        .element()
                        .from(0, 0, 0)
                        .to(16, 16, 16)
                        .allFaces((dir, uv) -> uv.texture("#overlay").uvs(0, 0, 16, 16))
                        .end()
                        .renderType("translucent")
                )
                .end();
    }

    public void directionalState(Block block, ModelFile modelFile) {
        this.getVariantBuilder(block) // Get variant builder
                .forAllStates(state -> // For all possible states
                        ConfiguredModel.builder() // Creates configured model builder
                                .modelFile(modelFile) // Can show 'modelFile'
                                .rotationY((int) state.getValue(HORIZONTAL_FACING).toYRot())
                                // Rotates 'modelFile' on the Y axis depending on the property
                                .build() // Creates the array of configured models
                );
    }
}
