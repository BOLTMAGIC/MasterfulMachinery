package io.ticticboom.mods.mm.item;

import io.ticticboom.mods.mm.util.ItemNbtUtil;

import io.ticticboom.mods.mm.util.StructureCaptureUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import io.ticticboom.mods.mm.Ref;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Item to mark two corners and save a multiblock selection to config/mm/structures
 */
public class MultiblockSaverItem extends Item {
    public static final String NBT_POS1 = "MMPos1";
    public static final String NBT_POS2 = "MMPos2";

    public MultiblockSaverItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.pass(stack);
        }

        // if player is sneaking and clicks in air, clear stored corners from the item
        if (player.isShiftKeyDown()) {
            if (ItemNbtUtil.hasTag(stack)) {
                ItemNbtUtil.mutate(stack, __t -> __t.remove(NBT_POS1));
                ItemNbtUtil.mutate(stack, __t -> __t.remove(NBT_POS2));
            }
            player.sendOverlayMessage(Component.translatable("item.mm.multiblock_saver.message.cleared"));
            return InteractionResultHolder.success(stack);
        }

        // check if both corners exist
        var tag = ItemNbtUtil.getTag(stack);
        if (tag == null || !tag.contains(NBT_POS1) || !tag.contains(NBT_POS2)) {
            player.sendOverlayMessage(Component.translatable("item.mm.multiblock_saver.message.mark_corners"));
            return InteractionResultHolder.pass(stack);
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.fail(stack);
        }

        long p1l = ItemNbtUtil.getTagOrEmpty(stack).getLongOr(NBT_POS1, 0L);
        long p2l = ItemNbtUtil.getTagOrEmpty(stack).getLongOr(NBT_POS2, 0L);
        BlockPos p1 = BlockPos.of(p1l);
        BlockPos p2 = BlockPos.of(p2l);

        try {
            var result = StructureCaptureUtil.captureAndSave(level, p1, p2, serverPlayer.getName().getString());
            if (result.success) {
                // clear positions
                ItemNbtUtil.mutate(stack, __t -> __t.remove(NBT_POS1));
                ItemNbtUtil.mutate(stack, __t -> __t.remove(NBT_POS2));
                serverPlayer.sendSystemMessage(Component.translatable("item.mm.multiblock_saver.message.saved", result.baseName).withStyle(net.minecraft.ChatFormatting.GREEN));
                serverPlayer.sendSystemMessage(Component.translatable("item.mm.multiblock_saver.message.files", result.jsonPath, result.jsPath));
                return InteractionResultHolder.success(stack);
            } else {
                serverPlayer.sendSystemMessage(Component.translatable("item.mm.multiblock_saver.message.failed", result.error).withStyle(net.minecraft.ChatFormatting.RED));
                return InteractionResultHolder.fail(stack);
            }
        } catch (Exception e) {
            Ref.LOG.error("Error during multiblock save", e);
            serverPlayer.sendSystemMessage(Component.translatable("item.mm.multiblock_saver.message.error", e.getMessage()).withStyle(net.minecraft.ChatFormatting.RED));
            return InteractionResultHolder.fail(stack);
        }
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) return InteractionResult.PASS;

        if (!(context.getPlayer() instanceof ServerPlayer)) return InteractionResult.FAIL;

        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos();

        // CustomData is immutable, so mutate a copy and write it back rather than editing in place.
        var tag = ItemNbtUtil.getTagOrEmpty(stack);
        // If sneaking while clicking a block, explicitly set this block as Corner 1
        if (context.getPlayer().isShiftKeyDown()) {
            tag.putLong(NBT_POS1, pos.asLong());
            ItemNbtUtil.setTag(stack, tag);
            context.getPlayer().sendOverlayMessage(Component.translatable("item.mm.multiblock_saver.message.corner1_set", pos.getX(), pos.getY(), pos.getZ()));
            return InteractionResult.SUCCESS;
        }

        // Normal click behavior: if no Corner1 set, set it; otherwise set Corner2
        if (!tag.contains(NBT_POS1)) {
            tag.putLong(NBT_POS1, pos.asLong());
            context.getPlayer().sendOverlayMessage(Component.translatable("item.mm.multiblock_saver.message.corner1_set", pos.getX(), pos.getY(), pos.getZ()));
        } else {
            tag.putLong(NBT_POS2, pos.asLong());
            context.getPlayer().sendOverlayMessage(Component.translatable("item.mm.multiblock_saver.message.corner2_set", pos.getX(), pos.getY(), pos.getZ()));
        }
        ItemNbtUtil.setTag(stack, tag);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack pStack, Item.TooltipContext pContext, @NotNull List<Component> pTooltipComponents, @NotNull TooltipFlag pIsAdvanced) {
        var tag = ItemNbtUtil.getTag(pStack);
        if (tag != null) {
            if (tag.contains(NBT_POS1)) {
                long l = tag.getLongOr(NBT_POS1, 0L);
                BlockPos pos = BlockPos.of(l);
                pTooltipComponents.add(Component.translatable("item.mm.multiblock_saver.tooltip.corner1", pos.getX(), pos.getY(), pos.getZ()));
            }
            if (tag.contains(NBT_POS2)) {
                long l = tag.getLongOr(NBT_POS2, 0L);
                BlockPos pos = BlockPos.of(l);
                pTooltipComponents.add(Component.translatable("item.mm.multiblock_saver.tooltip.corner2", pos.getX(), pos.getY(), pos.getZ()));
            }
        }
        super.appendHoverText(pStack, pContext, pTooltipComponents, pIsAdvanced);
    }
}





