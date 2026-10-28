package io.ticticboom.mods.mm.port.replication.link;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.port.IPortBlock;
import io.ticticboom.mods.mm.port.MMPortRegistry;
import io.ticticboom.mods.mm.port.item.SingleItemPortIngredient;
import io.ticticboom.mods.mm.recipe.MachineRecipeManager;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.output.simple.SimpleRecipeOutputEntry;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import io.ticticboom.mods.mm.structure.StructureManager;
import io.ticticboom.mods.mm.structure.StructureModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ReplicationLinkRecipes {

    private static List<ItemStack> craftable = List.of();
    private static int craftableVersion = -1;

    public static List<ItemStack> outputs(Identifier structureId) {
        var result = new ArrayList<ItemStack>();
        for (RecipeModel recipe : MachineRecipeManager.getRecipesByStrucutreId(structureId)) {
            for (ItemStack stack : itemOutputs(recipe)) {
                addUnique(result, stack);
            }
        }
        return result;
    }

    public static @Nullable RecipeModel recipeFor(@Nullable StructureModel structure, ItemStack wanted) {
        if (structure == null || wanted.isEmpty()) {
            return null;
        }
        for (RecipeModel recipe : MachineRecipeManager.getRecipesByStrucutreId(structure.id())) {
            for (ItemStack stack : itemOutputs(recipe)) {
                if (ItemStack.isSameItemSameComponents(stack, wanted)) {
                    return recipe;
                }
            }
        }
        return null;
    }

    public static boolean isCraftable(ItemStack stack) {
        if (craftableVersion != MachineRecipeManager.RECIPE_VERSION) {
            craftableVersion = MachineRecipeManager.RECIPE_VERSION;
            craftable = collectCraftable();
        }
        for (ItemStack candidate : craftable) {
            if (ItemStack.isSameItemSameComponents(candidate, stack)) {
                return true;
            }
        }
        return false;
    }

    private static List<ItemStack> collectCraftable() {
        var result = new ArrayList<ItemStack>();
        for (RegistryGroupHolder holder : MMPortRegistry.PORTS) {
            if (!(holder.getBlock().get() instanceof IPortBlock block) || !Ref.Ports.REPLICATION_LINK.equals(block.getModel().type())) {
                continue;
            }
            for (Identifier controllerId : block.getModel().controllerIds().getIds()) {
                for (StructureModel structure : StructureManager.getStructuresForController(controllerId)) {
                    for (ItemStack stack : outputs(structure.id())) {
                        addUnique(result, stack);
                    }
                }
            }
        }
        return List.copyOf(result);
    }

    private static List<ItemStack> itemOutputs(RecipeModel recipe) {
        var result = new ArrayList<ItemStack>();
        for (var entry : recipe.outputs().outputs()) {
            if (entry instanceof SimpleRecipeOutputEntry simple && simple.getIngredient() instanceof SingleItemPortIngredient item) {
                result.add(item.outputStack());
            }
        }
        return result;
    }

    private static void addUnique(List<ItemStack> list, ItemStack stack) {
        for (ItemStack existing : list) {
            if (ItemStack.isSameItemSameComponents(existing, stack)) {
                return;
            }
        }
        list.add(stack);
    }
}
