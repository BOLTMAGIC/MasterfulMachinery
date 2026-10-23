package io.ticticboom.mods.mm.setup;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.debug.tool.DebugToolItem;
import io.ticticboom.mods.mm.gateway.InputGatewayBlock;
import io.ticticboom.mods.mm.gateway.InputGatewayBlockEntity;
import io.ticticboom.mods.mm.pattern.PatternPortBlock;
import io.ticticboom.mods.mm.pattern.PatternPortBlockEntity;
import io.ticticboom.mods.mm.item.BlueprintItem;
import io.ticticboom.mods.mm.item.MultiblockSaverItem;
import io.ticticboom.mods.mm.item.PrioritySetterItem;
import io.ticticboom.mods.mm.item.WrenchItem;
import io.ticticboom.mods.mm.structure.StructureManager;
import io.ticticboom.mods.mm.tool.MultiblockToolItem;
import io.ticticboom.mods.mm.tool.MultiblockToolMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.*;

import java.util.Collection;
import java.util.List;

@SuppressWarnings("unused")
public class MMRegisters {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Ref.ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Ref.ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Ref.ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, Ref.ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Ref.ID);

    public static final RegistryObject<BlueprintItem> BLUEPRINT = ITEMS.register("blueprint", BlueprintItem::new);
    public static final RegistryObject<Item> DEBUG_TOOL = ITEMS.register("debug_tool", DebugToolItem::new);
    public static final RegistryObject<Item> PRIORITY_SETTER = ITEMS.register("priority_setter", PrioritySetterItem::new);
    public static final RegistryObject<WrenchItem> WRENCH = ITEMS.register("wrench", WrenchItem::new);
    public static final RegistryObject<MultiblockSaverItem> MULTIBLOCK_SAVER = ITEMS.register("multiblock_saver", MultiblockSaverItem::new);
    public static final RegistryObject<MultiblockToolItem> MULTIBLOCK_TOOL = ITEMS.register("structure_builder", MultiblockToolItem::new);

    public static final RegistryObject<MenuType<MultiblockToolMenu>> MULTIBLOCK_TOOL_MENU = MENUS.register("structure_builder",
            () -> IForgeMenuType.create((windowId, inv, buf) -> new MultiblockToolMenu(windowId, inv, buf)));

    public static final RegistryObject<InputGatewayBlock> INPUT_GATEWAY = BLOCKS.register("input_gateway", InputGatewayBlock::new);
    public static final RegistryObject<BlockItem> INPUT_GATEWAY_ITEM = ITEMS.register("input_gateway", () -> new BlockItem(INPUT_GATEWAY.get(), new Item.Properties()));
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<InputGatewayBlockEntity>> INPUT_GATEWAY_BE = BLOCK_ENTITIES.register("input_gateway",
            () -> BlockEntityType.Builder.of(InputGatewayBlockEntity::new, INPUT_GATEWAY.get()).build(null));

    public static final RegistryObject<PatternPortBlock> PATTERN_PORT = BLOCKS.register("pattern_port", PatternPortBlock::new);
    public static final RegistryObject<BlockItem> PATTERN_PORT_ITEM = ITEMS.register("pattern_port", () -> new BlockItem(PATTERN_PORT.get(), new Item.Properties()));
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<PatternPortBlockEntity>> PATTERN_PORT_BE = BLOCK_ENTITIES.register("pattern_port",
            () -> BlockEntityType.Builder.of(PatternPortBlockEntity::new, PATTERN_PORT.get()).build(null));

    public static final RegistryObject<CreativeModeTab> MM_TAB = TABS.register("mm", () -> CreativeModeTab.builder().title(Component.translatable("tab.mm.main"))
            .icon(() -> BLUEPRINT.get().getDefaultInstance())
            .withBackgroundLocation(Ref.UiTextures.CREATIVE_TAB_BG)
            .withSearchBar(55)
            .displayItems((p, o) -> o.acceptAll(registeredItemStacks(ITEMS.getEntries())))
            .build());

    public static final RegistryObject<CreativeModeTab> MM_STRUCTURE_TAB = TABS.register("mm_structures", () -> CreativeModeTab.builder().title(Component.translatable("tab.mm.structures"))
            .icon(() -> BLUEPRINT.get().getDefaultInstance())
            .withBackgroundLocation(Ref.UiTextures.CREATIVE_TAB_BG)
            .withSearchBar(55)
            .displayItems((p, o) -> o.acceptAll(StructureManager.STRUCTURE_BLUEPRINTS.values()))
            .build());

    static List<ItemStack> registeredItemStacks(Collection<? extends RegistryObject<? extends Item>> entries) {
        // Joining a world/server can remap away a controller removed from the pack.
        return entries.stream().filter(RegistryObject::isPresent).map(x -> x.get().getDefaultInstance()).toList();
    }

    public static void register() {
        @SuppressWarnings("removal") var bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        MENUS.register(bus);
        TABS.register(bus);
    }
}
