package io.ticticboom.mods.mm.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class BlockUtils {
    public static <T extends MenuProvider> InteractionResult commonUse(BlockState state, Level level, BlockPos pos,
                                                                       Player player, BlockHitResult hitResult, Class<T> clz, @Nullable Supplier<Boolean> preScreenCheck) {
        if (preScreenCheck == null) {
            preScreenCheck = () -> true;
        }
        // useWithoutItem is only fired for the main hand, so the old hand check is redundant.
        if (!level.isClientSide()) {
            var be = WorldUtil.getBlockEntity(pos, (ServerLevel) level);
            if (be != null && clz.isAssignableFrom(be.getClass())) {
                if (preScreenCheck.get()) {
                    // NeoForge folded NetworkHooks.openScreen into an extension method on the player.
                    ((ServerPlayer) player).openMenu((T) be, pos);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    public static final int PLAYER_INVENTORY_TOP = 141;

    public static final int SLOT_AREA_TOP = 19;

    public static int slotGridOriginX(int columns) {
        return Math.max(((162 - (columns * 18)) / 2) + 8, 8);
    }

    public static int slotGridOriginY(int rows) {
        return Math.max(((108 - (rows * 18)) / 2) + 8, SLOT_AREA_TOP);
    }

    public static void setupPlayerInventory(AbstractContainerMenu container, Inventory inv, int xOffset, int yOffset) {
        int playerOffsetX = 8 + xOffset;
        int playerOffsetY = 141 + yOffset;
        for (int j = 0; j < 3; j++) {
            for (int i = 0; i < 9; i++) {
                container.addSlot(new Slot(inv, 9 + (j * 9 + i), i * 18 + playerOffsetX, j * 18 + playerOffsetY));
            }
        }

        for (int i = 0; i < 9; i++) {
            container.addSlot(new Slot(inv, i, 8 + (i * 18) + xOffset, 199 + yOffset));
        }
    }

    public static BlockBehaviour.Properties createBlockProperties() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().strength(5f, 5f)
                .sound(SoundType.METAL);
    }
}
