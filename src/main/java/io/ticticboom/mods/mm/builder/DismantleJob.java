package io.ticticboom.mods.mm.builder;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * One running dismantle: breaks a machine's blocks a few per tick like a player would (claims and protection may
 * cancel), putting what the blocks drop into a sink. Block entities empty themselves as usual when removed
 * (a port's contents fall to the ground).
 */
public final class DismantleJob {
    final ServerLevel level;
    final BlockPos controllerPos;
    private final Deque<DismantlePlanner.Target> queue;
    private final int total;
    private final int perBlockFe;
    /** Loot tables see this: the tool with Silk Touch, so blocks like glass come back as themselves. */
    private final ItemStack lootTool;
    int removed;
    int blocked;
    private boolean outOfEnergy;
    private boolean sinkGone;

    private DismantleJob(ServerLevel level, BlockPos controllerPos, List<DismantlePlanner.Target> positions, int perBlockFe, ItemStack tool) {
        this.level = level;
        this.controllerPos = controllerPos;
        this.queue = new ArrayDeque<>(positions);
        this.total = positions.size();
        this.perBlockFe = perBlockFe;
        this.lootTool = tool.copy();
        this.lootTool.enchant(Enchantments.SILK_TOUCH, 1);
    }

    /** @param positions what to break, in order (the controller last); a position holding another block is skipped */
    public static DismantleJob create(ServerLevel level, BlockPos controllerPos, List<DismantlePlanner.Target> positions, int perBlockFe, ItemStack tool) {
        return new DismantleJob(level, controllerPos, positions, perBlockFe, tool);
    }

    public int removed() {
        return removed;
    }

    public int blocked() {
        return blocked;
    }

    public int total() {
        return total;
    }

    /** True once the job stopped early because the sink could not pay for the next block. */
    public boolean outOfEnergy() {
        return outOfEnergy;
    }

    /** True once the job stopped because its sink went away (the multiblock tool is no longer carried). */
    public boolean sinkGone() {
        return sinkGone;
    }

    /** @return true when every position has been handled (or the job stopped early) */
    public boolean tick(Player player, ItemSink sink, int budget) {
        while (budget > 0 && !queue.isEmpty()) {
            if (sink.gone()) {
                sinkGone = true;
                queue.clear();
                break;
            }
            DismantlePlanner.Target target = queue.poll();
            BlockPos pos = target.pos();
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                continue; // already gone, costs nothing
            }
            if (!state.is(target.block())) {
                blocked++;
                continue; // replaced since planning: not part of the machine any more
            }
            if (!player.mayBuild() || !level.mayInteract(player, pos) || state.getDestroySpeed(level, pos) < 0) {
                blocked++;
                continue; // adventure mode, spawn protection, unbreakable blocks
            }
            budget--;
            // paid first, so no break event is posted for a block that is then not broken for lack of energy
            if (!sink.payEnergy(perBlockFe)) {
                outOfEnergy = true;
                queue.clear();
                break; // remaining blocks stay
            }
            // broken like a player would: claim and protection mods (and linked machines) may cancel it
            var event = new BlockEvent.BreakEvent(level, pos, state, player);
            if (MinecraftForge.EVENT_BUS.post(event)) {
                sink.refundEnergy(perBlockFe);
                blocked++;
                continue;
            }
            BlockEntity be = level.getBlockEntity(pos);
            List<ItemStack> drops = Block.getDrops(state, level, pos, be, player, lootTool);
            // no loot drops here (collected above); block entities still empty themselves in onRemove
            if (!level.destroyBlock(pos, false, player)) {
                sink.refundEnergy(perBlockFe);
                blocked++;
                continue;
            }
            removed++;
            for (ItemStack drop : drops) {
                sink.insert(drop);
            }
        }
        return queue.isEmpty();
    }
}
