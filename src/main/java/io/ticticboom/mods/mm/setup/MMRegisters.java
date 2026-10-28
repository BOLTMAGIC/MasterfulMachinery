package io.ticticboom.mods.mm.setup;

import io.ticticboom.mods.mm.util.BlockUtils;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import io.ticticboom.mods.mm.tool.ToolComponents;
import io.ticticboom.mods.mm.tool.MultiblockToolMenu;
import io.ticticboom.mods.mm.controller.single.register.SingleMachineMenu;
import io.ticticboom.mods.mm.tool.MultiblockToolItem;
import io.ticticboom.mods.mm.item.WrenchItem;
import net.minecraft.world.item.BlockItem;
import io.ticticboom.mods.mm.gateway.InputGatewayBlockEntity;
import io.ticticboom.mods.mm.gateway.InputGatewayBlock;
import io.ticticboom.mods.mm.pattern.PatternPortBlock;
import io.ticticboom.mods.mm.pattern.PatternPortBlockEntity;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.debug.tool.DebugToolItem;
import io.ticticboom.mods.mm.item.BlueprintItem;
import io.ticticboom.mods.mm.item.MultiblockSaverItem;
import io.ticticboom.mods.mm.item.PrioritySetterItem;
import io.ticticboom.mods.mm.item.TooltipBlockItem;
import io.ticticboom.mods.mm.structure.StructureManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collection;
import java.util.List;

@SuppressWarnings("unused")
public class MMRegisters {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Ref.ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Ref.ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Ref.ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(BuiltInRegistries.MENU, Ref.ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Ref.ID);

    public static final DeferredHolder<Item, BlueprintItem> BLUEPRINT = ITEMS.registerItem("blueprint", BlueprintItem::new);
    public static final DeferredHolder<Item, Item> DEBUG_TOOL = ITEMS.registerItem("debug_tool", DebugToolItem::new);
    public static final DeferredHolder<Item, Item> PRIORITY_SETTER = ITEMS.registerItem("priority_setter", PrioritySetterItem::new);
    public static final DeferredHolder<Item, MultiblockSaverItem> MULTIBLOCK_SAVER = ITEMS.registerItem("multiblock_saver", MultiblockSaverItem::new);

    public static final DeferredHolder<Item, MultiblockToolItem> MULTIBLOCK_TOOL = ITEMS.registerItem("structure_builder", MultiblockToolItem::new);
    public static final DeferredHolder<MenuType<?>, MenuType<MultiblockToolMenu>> MULTIBLOCK_TOOL_MENU = MENUS.register("structure_builder",
            () -> IMenuTypeExtension.create(MultiblockToolMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<SingleMachineMenu>> SINGLE_MACHINE_MENU = MENUS.register("single_machine",
            () -> IMenuTypeExtension.create(SingleMachineMenu::new));
    public static final DeferredHolder<Item, WrenchItem> WRENCH = ITEMS.registerItem("wrench", WrenchItem::new);
    public static final DeferredHolder<Block, InputGatewayBlock> INPUT_GATEWAY = BLOCKS.registerBlock("input_gateway", InputGatewayBlock::new, () -> BlockUtils.createBlockProperties());
    public static final DeferredHolder<Item, BlockItem> INPUT_GATEWAY_ITEM = ITEMS.registerItem("input_gateway", properties -> new TooltipBlockItem(INPUT_GATEWAY.get(), properties.useBlockDescriptionPrefix(), 4));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InputGatewayBlockEntity>> INPUT_GATEWAY_BE = BLOCK_ENTITIES.register("input_gateway",
            () -> new BlockEntityType<>(InputGatewayBlockEntity::new, INPUT_GATEWAY.get()));

    public static final DeferredHolder<Block, PatternPortBlock> PATTERN_PORT = BLOCKS.registerBlock("pattern_port", PatternPortBlock::new, () -> BlockUtils.createBlockProperties());
    public static final DeferredHolder<Item, BlockItem> PATTERN_PORT_ITEM = ITEMS.registerItem("pattern_port", properties -> new TooltipBlockItem(PATTERN_PORT.get(), properties.useBlockDescriptionPrefix(), 4));
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PatternPortBlockEntity>> PATTERN_PORT_BE = BLOCK_ENTITIES.register("pattern_port",
            () -> new BlockEntityType<>(PatternPortBlockEntity::new, PATTERN_PORT.get()));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MM_TAB = TABS.register("mm", () -> CreativeModeTab.builder().title(Component.translatable("tab.mm.main"))
            .icon(() -> BLUEPRINT.get().getDefaultInstance())
            .backgroundTexture(Ref.UiTextures.CREATIVE_TAB_BG)
            .withSearchBar(55)
            .displayItems((p, o) -> o.acceptAll(registeredItemStacks(ITEMS.getEntries())))
            .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MM_STRUCTURE_TAB = TABS.register("mm_structures", () -> CreativeModeTab.builder().title(Component.translatable("tab.mm.structures"))
            .icon(() -> BLUEPRINT.get().getDefaultInstance())
            .backgroundTexture(Ref.UiTextures.CREATIVE_TAB_BG)
            .withSearchBar(55)
            .displayItems((p, o) -> o.acceptAll(StructureManager.STRUCTURE_BLUEPRINTS.values()))
            .build());

    static List<ItemStack> registeredItemStacks(Collection<? extends DeferredHolder<Item, ? extends Item>> entries) {
        // Joining a world/server can leave a controller removed from the pack unbound.
        return entries.stream().filter(DeferredHolder::isBound).map(x -> x.get().getDefaultInstance()).toList();
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        MENUS.register(bus);
        ToolComponents.register(bus);
        TABS.register(bus);
    }
}
