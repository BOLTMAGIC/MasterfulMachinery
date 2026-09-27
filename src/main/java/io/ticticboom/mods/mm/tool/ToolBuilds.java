package io.ticticboom.mods.mm.tool;

import io.ticticboom.mods.mm.builder.AssemblyPlanner;
import io.ticticboom.mods.mm.builder.ChainedMaterialSource;
import io.ticticboom.mods.mm.builder.MaterialSource;
import io.ticticboom.mods.mm.builder.me.CraftTracker;
import io.ticticboom.mods.mm.builder.me.MeAccessFactory;
import io.ticticboom.mods.mm.builder.structure.BuildableStructure;
import io.ticticboom.mods.mm.builder.structure.BuildableStructureRegistry;
import io.ticticboom.mods.mm.config.MMConfigSetup;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.networklink.Permissions;
import io.ticticboom.mods.mm.structure.StructureManager;
import io.ticticboom.mods.mm.structure.StructureModel;
import io.ticticboom.mods.mm.util.StructurePasteUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/** Decides what using the multiblock tool on a block builds: finish a matching controller, or a whole new machine. */
public final class ToolBuilds {
    private ToolBuilds() {
    }

    /**
     * A build ready to hand to {@link io.ticticboom.mods.mm.builder.AssemblyJobs#start}.
     *
     * @param requiresController false for another mod's structure: controllerPos is then only its center
     */
    public record Prepared(BlockPos controllerPos, AssemblyPlanner.Plan plan, MaterialSource source, int perBlockFe,
                           boolean requiresController) {
        public Prepared(BlockPos controllerPos, AssemblyPlanner.Plan plan, MaterialSource source, int perBlockFe) {
            this(controllerPos, plan, source, perBlockFe, true);
        }
    }

    /**
     * Exactly one of {@code prepared} and {@code error} is set. {@code notice}, when set, is worth telling the player
     * either way (the tool's ME network could not be reached or used, so only the store and inventory were).
     */
    public record Result(@Nullable Prepared prepared, @Nullable Component error, @Nullable Component notice) {
        static Result error(Component error) {
            return new Result(null, error, null);
        }

        Result withNotice(@Nullable Component notice) {
            return new Result(prepared, error, notice);
        }
    }

    public static @Nullable StructureModel selectedStructure(ItemStack tool) {
        ResourceLocation id = ToolData.structure(tool);
        return id == null ? null : StructureManager.STRUCTURES.get(id);
    }

    /** The controller at pos when it can assemble the structure the tool has selected; else null. */
    public static @Nullable MachineControllerBlockEntity acceptingController(Level level, BlockPos pos, ItemStack tool) {
        ResourceLocation id = ToolData.structure(tool);
        if (id != null && level.getBlockEntity(pos) instanceof MachineControllerBlockEntity controller
                && controller.findAssemblyCandidate(id) != null) {
            return controller;
        }
        return null;
    }

    /** The selected non-MM structure, from the server's registry, or null. */
    public static @Nullable BuildableStructure selectedBuilderStructure(ItemStack tool) {
        ResourceLocation id = ToolData.builderStructure(tool);
        return id == null ? null : BuildableStructureRegistry.SERVER.get(id);
    }

    public static Result prepare(Level level, Player player, ItemStack tool, BlockPos clickedPos, Direction clickedFace) {
        ChainedMaterialSource source = ToolBuildPlan.source(player, tool);
        return prepare(level, player, tool, clickedPos, clickedFace, source).withNotice(meNotice(player, tool, source));
    }

    /** As above, drawing from the given source (its network, if any, is the one crafts are requested from). */
    public static Result prepare(Level level, Player player, ItemStack tool, BlockPos clickedPos, Direction clickedFace, ChainedMaterialSource source) {
        BuildableStructure builder = selectedBuilderStructure(tool);
        if (builder != null) {
            return prepareFixed(level, player, tool, clickedPos, clickedFace, builder, source);
        }
        StructureModel structure = selectedStructure(tool);
        if (structure == null) {
            return Result.error(Component.translatable("message.mm.tool.no_structure"));
        }
        return prepare(level, player, tool, clickedPos, clickedFace, structure, source);
    }

    /** Another mod's structure: fixed blocks, no controller; refused when anything is in the way or it can't be built. */
    private static Result prepareFixed(Level level, Player player, ItemStack tool, BlockPos clickedPos, Direction clickedFace,
                                       BuildableStructure structure, ChainedMaterialSource source) {
        if (!structure.buildable()) {
            //noinspection DataFlowIssue - not buildable means there is an unbuildable block
            return Result.error(Component.translatable("message.mm.tool.unbuildable", structure.unbuildableBlock().getName()));
        }
        FixedBuildPlan build = FixedBuildPlan.create(level, structure, clickedPos, clickedFace, player.getDirection(), ToolData.extraTurns(tool));
        if (!build.obstructed().isEmpty()) {
            return Result.error(StructurePasteUtil.obstructionMessage("message.mm.tool.obstructed", build.obstructed()));
        }
        int perBlockFe = MMConfigSetup.COMMON.toolEnergyPerPlacedBlock.get();
        return startable(level, player, tool, source, new Prepared(build.center(), build.plan(), source, perBlockFe, false));
    }

    /**
     * A bound tool whose network is turned on but cannot be used right now (chunk not loaded, block removed, unpowered,
     * or no access to it): the build goes on from the store and inventory, and the player should know why ME was not
     * used.
     */
    private static @Nullable Component meNotice(Player player, ItemStack tool, ChainedMaterialSource source) {
        if (source.me() != null || source.free() || !(player instanceof ServerPlayer serverPlayer)) {
            return null;
        }
        return MeAccessFactory.problem(serverPlayer, tool);
    }

    private static Result prepare(Level level, Player player, ItemStack tool, BlockPos clickedPos, Direction clickedFace,
                                  StructureModel structure, ChainedMaterialSource source) {
        int perBlockFe = MMConfigSetup.COMMON.toolEnergyPerPlacedBlock.get();
        MachineControllerBlockEntity controller = acceptingController(level, clickedPos, tool);
        if (controller != null) {
            var link = controller.getNetworkLink();
            if (link != null && !Permissions.canAccess(player, link.owner())) {
                return Result.error(Component.translatable("message.mm.tool.no_access"));
            }
            // same as the controller's own Assemble, with the tool's tiers and the tool as source
            var plan = AssemblyPlanner.planCompletion(level, structure, controller.getBlockPos(), ToolData.tiers(tool), source.snapshot());
            if (plan == null) {
                return Result.error(Component.translatable("message.mm.assemble.already"));
            }
            return startable(level, player, tool, source, new Prepared(controller.getBlockPos(), plan, source, perBlockFe));
        }

        ToolBuildPlan build = ToolBuildPlan.create(level, player, tool, structure, clickedPos, clickedFace, source.snapshot());
        if (build == null) {
            return Result.error(Component.translatable("message.mm.tool.no_controller"));
        }
        if (!build.obstructed().isEmpty()) {
            // nothing is ever broken; the build does not start at all
            return Result.error(StructurePasteUtil.obstructionMessage("message.mm.tool.obstructed", build.obstructed()));
        }
        // without its controller nothing else is worth building, so it must be placeable here
        AssemblyPlanner.Planned controllerStep = build.plan().steps().get(0);
        Block controllerBlock = controllerStep.state().getBlock();
        boolean controllerThere = level.getBlockState(build.controllerPos()).is(controllerBlock);
        if (!controllerThere && (!player.mayBuild() || !level.mayInteract(player, build.controllerPos()))) {
            return Result.error(Component.translatable("message.mm.tool.protected"));
        }
        return startable(level, player, tool, source, new Prepared(build.controllerPos(), build.plan(), source, perBlockFe));
    }

    /**
     * All or nothing: a build starts only with every block it still needs on hand (controller included), else missing
     * ones are crafted where possible ({@link ToolCrafts}). It is refused first when the tool cannot pay even its first
     * block, so it never starts only to stop at once. A build that starts forgets the player's tracked crafts.
     */
    private static Result startable(Level level, Player player, ItemStack tool, ChainedMaterialSource source, Prepared prepared) {
        // energy first: no crafts are requested for a build that could not start anyway
        if (!source.free() && prepared.perBlockFe() > 0
                && new ToolEnergy(tool, MMConfigSetup.COMMON.toolEnergyCapacity.get()).getEnergyStored() < prepared.perBlockFe()) {
            return Result.error(Component.translatable("message.mm.assemble.out_of_energy", 0, prepared.plan().steps().size()));
        }
        if (!source.free()) {
            Map<Item, Integer> missing = new LinkedHashMap<>();
            needed(level, player, prepared.plan()).forEach((item, count) -> {
                long have = source.available(item);
                if (have < count) {
                    missing.put(item, (int) (count - have));
                } else {
                    CraftTracker.seen(player, item);
                }
            });
            if (!missing.isEmpty()) {
                return Result.error(ToolCrafts.request(player, tool, source.me(), missing));
            }
        }
        CraftTracker.clear(player);
        return new Result(prepared, null, null);
    }

    /**
     * Items the plan still has to place, by count: positions already right, in the way (never broken) or protected
     * are left out, like the job itself skips them. Blocks without an item cannot be supplied by anything; the job
     * reports them missing as before.
     */
    private static Map<Item, Integer> needed(Level level, Player player, AssemblyPlanner.Plan plan) {
        Map<Item, Integer> needed = new LinkedHashMap<>();
        for (AssemblyPlanner.Planned step : plan.steps()) {
            BlockState existing = level.getBlockState(step.pos());
            Block wanted = step.state().getBlock();
            if (existing.is(wanted) || step.accepted().contains(existing.getBlock())) {
                continue;
            }
            if (!existing.isAir() && !existing.canBeReplaced()) {
                continue;
            }
            if (!player.mayBuild() || !level.mayInteract(player, step.pos())) {
                continue;
            }
            Item item = wanted.asItem();
            if (item != Items.AIR) {
                needed.merge(item, 1, Integer::sum);
            }
        }
        return needed;
    }
}
