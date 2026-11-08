package io.ticticboom.mods.mm.compat.jei;

import io.ticticboom.mods.mm.compat.jei.category.MMStructureCategory;
import io.ticticboom.mods.mm.net.MMNetwork;
import io.ticticboom.mods.mm.net.packet.ToolStoreFillPkt;
import io.ticticboom.mods.mm.setup.MMRegisters;
import io.ticticboom.mods.mm.structure.StructureModel;
import io.ticticboom.mods.mm.tool.MultiblockToolMenu;
import io.ticticboom.mods.mm.tool.ToolStoreFill;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * "+" on a structure page while the Structure Builder's store is open: moves the structure's blocks from the
 * inventory into the store ({@link ToolStoreFill}). Greyed out when nothing could be moved.
 */
public class ToolStoreTransferHandler implements IRecipeTransferHandler<MultiblockToolMenu, StructureModel> {
    private final IRecipeTransferHandlerHelper helper;

    public ToolStoreTransferHandler(IRecipeTransferHandlerHelper helper) {
        this.helper = helper;
    }

    @Override
    public Class<? extends MultiblockToolMenu> getContainerClass() {
        return MultiblockToolMenu.class;
    }

    @Override
    public Optional<MenuType<MultiblockToolMenu>> getMenuType() {
        return Optional.of(MMRegisters.MULTIBLOCK_TOOL_MENU.get());
    }

    @Override
    public RecipeType<StructureModel> getRecipeType() {
        return MMStructureCategory.RECIPE_TYPE;
    }

    @Override
    public @Nullable IRecipeTransferError transferRecipe(MultiblockToolMenu menu, StructureModel structure, IRecipeSlotsView slots,
                                                         Player player, boolean maxTransfer, boolean doTransfer) {
        List<ToolStoreFill.Need> needs = needs(slots);
        if (ToolStoreFill.movable(player, menu.store(), needs) == 0) {
            return helper.createUserErrorWithTooltip(Component.translatable("jei.mm.tool_store.nothing"));
        }
        if (doTransfer) {
            MMNetwork.INSTANCE.sendToServer(new ToolStoreFillPkt(needs));
        }
        return null;
    }

    /** The page's block slots: the blocks each accepts (port tiers...) and how many the structure takes. */
    static List<ToolStoreFill.Need> needs(IRecipeSlotsView slots) {
        var needs = new ArrayList<ToolStoreFill.Need>();
        for (IRecipeSlotView slot : slots.getSlotViews(RecipeIngredientRole.INPUT)) {
            List<ItemStack> stacks = slot.getIngredients(VanillaTypes.ITEM_STACK).filter(stack -> !stack.isEmpty()).toList();
            if (stacks.isEmpty() || needs.size() >= ToolStoreFill.MAX_NEEDS) {
                continue;
            }
            List<Item> options = stacks.stream().map(ItemStack::getItem).distinct().limit(ToolStoreFill.MAX_OPTIONS).toList();
            needs.add(new ToolStoreFill.Need(options, stacks.get(0).getCount()));
        }
        return needs;
    }
}
