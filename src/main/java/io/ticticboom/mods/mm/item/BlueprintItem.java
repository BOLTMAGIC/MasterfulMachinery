package io.ticticboom.mods.mm.item;

import io.ticticboom.mods.mm.structure.StructureManager;
import io.ticticboom.mods.mm.structure.StructureModel;
import io.ticticboom.mods.mm.util.StructurePasteUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
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
        texts.add(Component.literal("Creative: Sneak to preview, Sneak-Right-Click to paste"));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        if (player == null || level == null) {
            return;
        }

        if (!player.isShiftKeyDown() || !player.getAbilities().instabuild) {
            return InteractionResult.PASS;
        }

        StructureModel structure = getStructure(context.getItemInHand());
        if (structure == null) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("Blueprint has no valid structure id."), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        Rotation rotation = rotationFromPlayer(player);
        StructurePasteUtil.PasteResult result = StructurePasteUtil.paste((ServerLevel) level, player, structure, context.getClickedPos(), context.getClickedFace(), rotation, player.getDirection());
        player.displayClientMessage(result.message(), true);
        return result.success() ? InteractionResult.CONSUME : InteractionResult.FAIL;
    }

    private static Rotation rotationFromPlayer(Player player) {
        return switch (player.getDirection()) {
            case EAST -> Rotation.CLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    public ItemStack getStructureInstance(ResourceLocation structureId) {
        ItemStack defaultInstance = getDefaultInstance();
        defaultInstance.getOrCreateTag().putString(TAG_STRUCTURE, structureId.toString());
        return defaultInstance;
    }

    public static StructureModel getStructure(ItemStack stack) {
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
