package io.ticticboom.mods.mm.item;

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
        if (level.isClientSide) {
            return InteractionResultHolder.pass(stack);
        }

        // if player is sneaking and clicks in air, clear stored corners from the item
        if (player.isShiftKeyDown()) {
            if (stack.hasTag()) {
                stack.getOrCreateTag().remove(NBT_POS1);
                stack.getOrCreateTag().remove(NBT_POS2);
            }
            player.displayClientMessage(Component.literal("Cleared marked corners."), true);
            return InteractionResultHolder.success(stack);
        }

        // check if both corners exist
        var tag = stack.getTag();
        if (tag == null || !tag.contains(NBT_POS1) || !tag.contains(NBT_POS2)) {
            player.displayClientMessage(Component.literal("Mark two corners first by right-clicking on blocks."), true);
            return InteractionResultHolder.pass(stack);
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.fail(stack);
        }

        long p1l = stack.getOrCreateTag().getLong(NBT_POS1);
        long p2l = stack.getOrCreateTag().getLong(NBT_POS2);
        BlockPos p1 = BlockPos.of(p1l);
        BlockPos p2 = BlockPos.of(p2l);

        try {
            var result = StructureCaptureUtil.captureAndSave(level, p1, p2, serverPlayer.getName().getString());
            if (result.success) {
                // clear positions
                stack.getOrCreateTag().remove(NBT_POS1);
                stack.getOrCreateTag().remove(NBT_POS2);
                serverPlayer.displayClientMessage(Component.literal("Saved structure: " + result.baseName).withStyle(net.minecraft.ChatFormatting.GREEN), false);
                serverPlayer.displayClientMessage(Component.literal("Files: " + result.jsonPath + " , " + result.jsPath), false);
                return InteractionResultHolder.success(stack);
            } else {
                serverPlayer.displayClientMessage(Component.literal("Failed to save: " + result.error).withStyle(net.minecraft.ChatFormatting.RED), false);
                return InteractionResultHolder.fail(stack);
            }
        } catch (Exception e) {
            Ref.LOG.error("Error during multiblock save", e);
            serverPlayer.displayClientMessage(Component.literal("Error during save: " + e.getMessage()).withStyle(net.minecraft.ChatFormatting.RED), false);
            return InteractionResultHolder.fail(stack);
        }
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) return InteractionResult.PASS;

        if (!(context.getPlayer() instanceof ServerPlayer)) return InteractionResult.FAIL;

        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos();

        var tag = stack.getOrCreateTag();
        // If sneaking while clicking a block, explicitly set this block as Corner 1
        if (context.getPlayer().isShiftKeyDown()) {
            tag.putLong(NBT_POS1, pos.asLong());
            context.getPlayer().displayClientMessage(Component.literal("Corner 1 set: " + pos.getX() + "," + pos.getY() + "," + pos.getZ()), true);
            return InteractionResult.SUCCESS;
        }

        // Normal click behavior: if no Corner1 set, set it; otherwise set Corner2
        if (!tag.contains(NBT_POS1)) {
            tag.putLong(NBT_POS1, pos.asLong());
            context.getPlayer().displayClientMessage(Component.literal("Corner 1 set: " + pos.getX() + "," + pos.getY() + "," + pos.getZ()), true);
        } else {
            tag.putLong(NBT_POS2, pos.asLong());
            context.getPlayer().displayClientMessage(Component.literal("Corner 2 set: " + pos.getX() + "," + pos.getY() + "," + pos.getZ()), true);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, @NotNull List<Component> pTooltipComponents, @NotNull TooltipFlag pIsAdvanced) {
        var tag = pStack.getTag();
        if (tag != null) {
            if (tag.contains(NBT_POS1)) {
                long l = tag.getLong(NBT_POS1);
                BlockPos pos = BlockPos.of(l);
                pTooltipComponents.add(Component.literal("Corner 1: " + pos.getX() + "," + pos.getY() + "," + pos.getZ()));
            }
            if (tag.contains(NBT_POS2)) {
                long l = tag.getLong(NBT_POS2);
                BlockPos pos = BlockPos.of(l);
                pTooltipComponents.add(Component.literal("Corner 2: " + pos.getX() + "," + pos.getY() + "," + pos.getZ()));
            }
        }
        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
    }
}





