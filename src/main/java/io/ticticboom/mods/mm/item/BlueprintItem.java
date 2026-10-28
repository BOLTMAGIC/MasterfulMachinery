package io.ticticboom.mods.mm.item;

import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import io.ticticboom.mods.mm.util.ItemNbtUtil;

import io.ticticboom.mods.mm.structure.StructureManager;
import io.ticticboom.mods.mm.structure.StructureModel;
import io.ticticboom.mods.mm.util.StructurePasteUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BlueprintItem extends Item {
    public static final String TAG_STRUCTURE = "MMStructure";
    public static final String TAG_STRUCTURE_NAME = "MMStructureName";

    public BlueprintItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        Component structureName = getStructureDisplayName(stack);
        if (structureName == null || structureName.getString().isBlank()) {
            return super.getName(stack);
        }

        return Component.translatable("item.mm.blueprint.name_format", structureName, super.getName(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> texts, TooltipFlag tipFlag) {
        super.appendHoverText(stack, context, display, texts, tipFlag);
        var structure = getStructure(stack);
        if (structure == null) {
            return;
        }

        texts.accept(Component.translatable("item.mm.blueprint.tooltip.structure", structure.displayName()));
        texts.accept(Component.translatable("item.mm.blueprint.tooltip.creative"));
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        if (player == null) {
            return InteractionResult.PASS;
        }

        if (!player.isShiftKeyDown() || !player.getAbilities().instabuild) {
            return InteractionResult.PASS;
        }

        StructureModel structure = getStructure(context.getItemInHand());
        if (structure == null) {
            if (!level.isClientSide()) {
                player.sendOverlayMessage(Component.translatable("item.mm.blueprint.message.no_structure"));
            }
            return InteractionResult.SUCCESS;
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        Rotation rotation = rotationFromPlayer(player);
        StructurePasteUtil.PasteResult result = StructurePasteUtil.paste((ServerLevel) level, player, structure, context.getClickedPos(), context.getClickedFace(), rotation, player.getDirection());
        player.sendOverlayMessage(result.message());
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

    public ItemStack getStructureInstance(Identifier structureId) {
        ItemStack defaultInstance = getDefaultInstance();
        ItemNbtUtil.mutate(defaultInstance, __t -> __t.putString(TAG_STRUCTURE, structureId.toString()));

        StructureModel structure = StructureManager.STRUCTURES.get(structureId);
        if (structure != null && structure.name() != null && !structure.name().isBlank()) {
            ItemNbtUtil.mutate(defaultInstance, __t -> __t.putString(TAG_STRUCTURE_NAME, structure.name()));
        }

        return defaultInstance;
    }

    @Nullable
    public static Identifier getStructureId(ItemStack stack) {
        if (!ItemNbtUtil.hasTag(stack)) {
            return null;
        }

        assert ItemNbtUtil.getTag(stack) != null;
        return Identifier.tryParse(ItemNbtUtil.getTag(stack).getStringOr(TAG_STRUCTURE, ""));
    }

    public static StructureModel getStructure(ItemStack stack) {
        Identifier id = getStructureId(stack);
        if (id == null) {
            return null;
        }

        return StructureManager.STRUCTURES.get(id);
    }

    @Nullable
    private static Component getStructureDisplayName(ItemStack stack) {
        StructureModel structure = getStructure(stack);
        if (structure != null && structure.name() != null && !structure.name().isBlank()) {
            return structure.displayName();
        }

        // Fallbacks only apply once the structure no longer resolves (removed from the pack), so the
        // stored value is shown literally rather than treated as a translation key.
        if (ItemNbtUtil.hasTag(stack)) {
            assert ItemNbtUtil.getTag(stack) != null;
            String storedName = ItemNbtUtil.getTag(stack).getStringOr(TAG_STRUCTURE_NAME, "");
            if (!storedName.isBlank()) {
                return Component.literal(storedName);
            }
        }

        Identifier id = getStructureId(stack);
        return id == null ? null : Component.literal(id.toString());
    }
}
