package io.ticticboom.mods.mm.item;

import io.ticticboom.mods.mm.util.ItemNbtUtil;

import io.ticticboom.mods.mm.port.IPortBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PrioritySetterItem extends Item {
    public static final String NBT_KEY = "mm:priority";
    public static final int MAX_PRIORITY = 10;

    public PrioritySetterItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.pass(stack);
        }

        // If sneaking and used in air, reset priority. Use a raytrace to verify the player isn't targeting a block.
        if (player.isShiftKeyDown()) {
            HitResult trace = player.pick(10.0D, 0.0F, false);
            if (trace == null || trace.getType() == HitResult.Type.MISS) {
                setPriorityInItem(stack, 0);
                player.sendOverlayMessage(Component.translatable("item.mm.priority_setter.message.reset"));
                return InteractionResultHolder.success(stack);
            }
            // if targeting a block, don't reset here; useOn will handle applying to a port
            return InteractionResultHolder.pass(stack);
        }

        int cur = getPriorityFromItem(stack);
        int next = cur + 1;
        if (next > MAX_PRIORITY) next = 0;
        setPriorityInItem(stack, next);
        player.sendOverlayMessage(Component.translatable("item.mm.priority_setter.message.set", next));
        return InteractionResultHolder.success(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) return InteractionResult.PASS;

        Player player = context.getPlayer();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.FAIL;
        }

        // Require sneaking to apply to port
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;

        BlockPos pos = context.getClickedPos();
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof IPortBlockEntity portBe)) {
            return InteractionResult.PASS;
        }

        // must be an output (not input)
        if (portBe.isInput()) return InteractionResult.FAIL;

        int prio = getPriorityFromItem(context.getItemInHand());
        // set in storage
        var storage = portBe.getStorage();
        storage.setPriority(prio);
        // the priority is saved with the port: without this it's lost when the chunk unloads (or the server stops)
        // unless something else in that chunk changed
        be.setChanged();

        serverPlayer.sendOverlayMessage(Component.translatable("item.mm.priority_setter.message.apply_success", prio));
        return InteractionResult.SUCCESS;
    }

    public static int getPriorityFromItem(ItemStack stack) {
        var tag = ItemNbtUtil.getTag(stack);
        if (tag != null && tag.contains(NBT_KEY)) {
            return Math.max(0, Math.min(MAX_PRIORITY, tag.getIntOr(NBT_KEY, 0)));
        }
        return 0;
    }

    public static void setPriorityInItem(ItemStack stack, int prio) {
        ItemNbtUtil.mutate(stack, tag -> tag.putInt(NBT_KEY, Math.max(0, Math.min(MAX_PRIORITY, prio))));
    }

    @Override
    public void appendHoverText(ItemStack pStack, Item.TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        int prio = getPriorityFromItem(pStack);
        pTooltipComponents.add(Component.translatable("item.mm.priority_setter.tooltip.line1", prio));
        pTooltipComponents.add(Component.translatable("item.mm.priority_setter.tooltip.line2"));
        pTooltipComponents.add(Component.translatable("item.mm.priority_setter.tooltip.line3"));
        pTooltipComponents.add(Component.translatable("item.mm.priority_setter.tooltip.line4"));
        super.appendHoverText(pStack, pContext, pTooltipComponents, pIsAdvanced);
    }
}
