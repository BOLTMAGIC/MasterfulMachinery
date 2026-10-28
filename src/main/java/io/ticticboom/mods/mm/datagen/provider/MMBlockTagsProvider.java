package io.ticticboom.mods.mm.datagen.provider;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.controller.IControllerBlock;
import io.ticticboom.mods.mm.extra.IExtraBlock;
import io.ticticboom.mods.mm.port.IPortBlock;
import io.ticticboom.mods.mm.setup.MMRegisters;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.concurrent.CompletableFuture;

public class MMBlockTagsProvider extends BlockTagsProvider {

    public MMBlockTagsProvider(DataGenerator generator, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(generator.getPackOutput(), lookupProvider, Ref.ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        var pickaxeTool = this.tag(BlockTags.MINEABLE_WITH_PICKAXE);

        for (DeferredHolder<Block, ? extends Block> entry : MMRegisters.BLOCKS.getEntries()) {
            Block block = entry.get();
            if (block instanceof IControllerBlock || block instanceof IPortBlock || block instanceof IExtraBlock) {
                pickaxeTool.add(block);
            }
        }
    }
}
