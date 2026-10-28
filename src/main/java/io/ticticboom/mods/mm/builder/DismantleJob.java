package io.ticticboom.mods.mm.builder;

import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public final class DismantleJob {
    final ServerLevel level;
    final BlockPos controllerPos;
    private final Deque<DismantlePlanner.Target> queue;
    private final int total;
    private final int perBlockFe;
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
        this.lootTool.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
    }

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

    public boolean outOfEnergy() {
        return outOfEnergy;
    }

    public boolean sinkGone() {
        return sinkGone;
    }

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
                continue;
            }
            if (!state.is(target.block())) {
                blocked++;
                continue;
            }
            if (!player.mayBuild() || !level.mayInteract(player, pos) || state.getDestroySpeed(level, pos) < 0) {
                blocked++;
                continue;
            }
            budget--;
            if (!sink.payEnergy(perBlockFe)) {
                outOfEnergy = true;
                queue.clear();
                break;
            }
            if (NeoForge.EVENT_BUS.post(new BreakBlockEvent(level, pos, state, player)).isCanceled()) {
                sink.refundEnergy(perBlockFe);
                blocked++;
                continue;
            }
            BlockEntity be = level.getBlockEntity(pos);
            List<ItemStack> drops = Block.getDrops(state, level, pos, be, player, lootTool);
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
