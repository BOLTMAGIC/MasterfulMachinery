package io.ticticboom.mods.mm.item;

import io.ticticboom.mods.mm.structure.StructureManager;
import io.ticticboom.mods.mm.structure.StructureModel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BlueprintItem extends Item {
    public static final String TAG_STRUCTURE = "MMStructure";

    public BlueprintItem() {
        super(new Properties());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> texts, TooltipFlag tipFlag) {
        super.appendHoverText(stack, level, texts, tipFlag);
        var structure = getStructure(stack);
        if (structure == null) {
            return;
        }

        texts.add(Component.literal("Structure: " + structure.name()));
    }

    public ItemStack getStructureInstance(StructureModel structure) {
        ItemStack defaultInstance = getDefaultInstance();
        defaultInstance.getOrCreateTag().putString(TAG_STRUCTURE, structure.id().toString());
        return defaultInstance;
    }

    private StructureModel getStructure(ItemStack stack) {
        if (!stack.hasTag()) {
            return null;
        }

        var id = ResourceLocation.tryParse(stack.getTag().getString(TAG_STRUCTURE));
        if (id == null) {
            return null;
        }

        return StructureManager.STRUCTURES.get(id);
    }
}
