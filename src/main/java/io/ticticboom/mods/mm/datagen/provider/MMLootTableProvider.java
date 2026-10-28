package io.ticticboom.mods.mm.datagen.provider;

import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.controller.IControllerPart;
import io.ticticboom.mods.mm.extra.IExtraBlockPart;
import io.ticticboom.mods.mm.port.IPortPart;
import io.ticticboom.mods.mm.setup.MMRegisters;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class MMLootTableProvider extends LootTableProvider {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    // 1.21 added a registry-lookup future to the constructor, and SubProviderEntry now takes a
    // factory from that lookup rather than a bare supplier.
    public MMLootTableProvider(DataGenerator generator, CompletableFuture<HolderLookup.Provider> registries) {
        super(generator.getPackOutput(), ImmutableSet.of(),
                ImmutableList.of(new SubProviderEntry(lookup -> new MMLootTableSubProvider(), LootContextParamSets.BLOCK)),
                registries);
        this.generator = generator;
    }

    private final DataGenerator generator;

    public static class MMLootTableSubProvider implements LootTableSubProvider {

        @Override
        public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> consumer) {
            for (var blockEntry : MMRegisters.BLOCKS.getEntries()) {
                var block = blockEntry.get();
                if (block instanceof IControllerPart controllerPart) {
                    consumer.accept(lootTableKey(controllerPart.getModel().id()), createNamedBlockLootTable(block));
                }
                if (block instanceof IPortPart portPart) {
                    consumer.accept(lootTableKey(portPart.getModel().id()), createBlockLootTable(block));
                }
                if (block instanceof IExtraBlockPart extra) {
                    consumer.accept(lootTableKey(extra.getModel().id()), createBlockLootTable(block));
                }
            }
        }

        private static ResourceKey<LootTable> lootTableKey(String id) {
            return ResourceKey.create(Registries.LOOT_TABLE, Ref.id("blocks/" + id));
        }

        protected LootTable.Builder createNamedBlockLootTable(Block block) {
            LootPool.Builder builder = LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1f))
                    .add(LootItem.lootTableItem(block)
                            .apply(CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY)
                                    .include(DataComponents.CUSTOM_NAME)));
            return LootTable.lootTable().withPool(builder);
        }

        protected LootTable.Builder createBlockLootTable(Block block) {
            LootPool.Builder builder = LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1f))
                    .add(LootItem.lootTableItem(block));
            return LootTable.lootTable().withPool(builder);
        }
    }
}
