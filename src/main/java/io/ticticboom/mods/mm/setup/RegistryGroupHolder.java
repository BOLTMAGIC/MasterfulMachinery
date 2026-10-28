package io.ticticboom.mods.mm.setup;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.registries.DeferredHolder;

@NoArgsConstructor
@Getter
@Setter
public class RegistryGroupHolder {
    private Identifier registryId;
    private DeferredHolder<Block, Block> block;
    private DeferredHolder<Item, Item> item;
    private DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> be;
    private DeferredHolder<MenuType<?>, MenuType<?>> menu;
    @OnlyIn(Dist.CLIENT)
    private MenuScreens.ScreenConstructor<?, AbstractContainerScreen<?>> screen;
}
