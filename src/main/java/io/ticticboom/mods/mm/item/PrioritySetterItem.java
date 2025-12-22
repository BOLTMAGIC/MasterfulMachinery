package io.ticticboom.mods.mm.item;

import io.ticticboom.mods.mm.compat.claim.ClaimPermissionManager;
import io.ticticboom.mods.mm.port.IPortBlockEntity;
import io.ticticboom.mods.mm.Ref;
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
        if (level.isClientSide) {
            return InteractionResultHolder.pass(stack);
        }

        // If sneaking and used in air, reset priority. Use a raytrace to verify the player isn't targeting a block.
        if (player.isShiftKeyDown()) {
            HitResult trace = player.pick(10.0D, 0.0F, false);
            if (trace == null || trace.getType() == HitResult.Type.MISS) {
                setPriorityInItem(stack, 0);
                player.displayClientMessage(Component.translatable("item.mm.priority_setter.message.reset"), true);
                return InteractionResultHolder.success(stack);
            }
            // if targeting a block, don't reset here; useOn will handle applying to a port
            return InteractionResultHolder.pass(stack);
        }

        int cur = getPriorityFromItem(stack);
        int next = cur + 1;
        if (next > MAX_PRIORITY) next = 0;
        setPriorityInItem(stack, next);
        player.displayClientMessage(Component.translatable("item.mm.priority_setter.message.set", next), true);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) return InteractionResult.PASS;

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

        // permission check
        if (!ClaimPermissionManager.canPlayerModify(serverPlayer, pos)) {
            serverPlayer.displayClientMessage(Component.translatable("message.mm.no_permission"), true);
            return InteractionResult.FAIL;
        }

        int prio = getPriorityFromItem(context.getItemInHand());
        // set in storage
        var storage = portBe.getStorage();
        int oldPrio = storage.getPriority();
        storage.setPriority(prio);
        // logging suppressed

        serverPlayer.displayClientMessage(Component.translatable("item.mm.priority_setter.message.apply_success", prio), true);
        return InteractionResult.SUCCESS;
    }

    public static int getPriorityFromItem(ItemStack stack) {
        var tag = stack.getTag();
        if (tag != null && tag.contains(NBT_KEY)) {
            return Math.max(0, Math.min(MAX_PRIORITY, tag.getInt(NBT_KEY)));
        }
        return 0;
    }

    public static void setPriorityInItem(ItemStack stack, int prio) {
        var tag = stack.getOrCreateTag();
        tag.putInt(NBT_KEY, Math.max(0, Math.min(MAX_PRIORITY, prio)));
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        int prio = getPriorityFromItem(pStack);
        pTooltipComponents.add(Component.translatable("item.mm.priority_setter.tooltip.line1", prio));
        pTooltipComponents.add(Component.translatable("item.mm.priority_setter.tooltip.line2"));
        pTooltipComponents.add(Component.translatable("item.mm.priority_setter.tooltip.line3"));
        pTooltipComponents.add(Component.translatable("item.mm.priority_setter.tooltip.line4"));
        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
    }
}
