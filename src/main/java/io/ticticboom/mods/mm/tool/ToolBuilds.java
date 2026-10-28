package io.ticticboom.mods.mm.tool;

import io.ticticboom.mods.mm.config.MMConfig;
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
import net.minecraft.resources.Identifier;
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

public final class ToolBuilds {
    private ToolBuilds() {
    }

    public record Prepared(BlockPos controllerPos, AssemblyPlanner.Plan plan, MaterialSource source, int perBlockFe,
                           boolean requiresController) {
        public Prepared(BlockPos controllerPos, AssemblyPlanner.Plan plan, MaterialSource source, int perBlockFe) {
            this(controllerPos, plan, source, perBlockFe, true);
        }
    }

    public record Result(@Nullable Prepared prepared, @Nullable Component error, @Nullable Component notice) {
        static Result error(Component error) {
            return new Result(null, error, null);
        }

        Result withNotice(@Nullable Component notice) {
            return new Result(prepared, error, notice);
        }
    }

    public static @Nullable StructureModel selectedStructure(ItemStack tool) {
        Identifier id = ToolData.structure(tool);
        return id == null ? null : StructureManager.STRUCTURES.get(id);
    }

    public static @Nullable MachineControllerBlockEntity acceptingController(Level level, BlockPos pos, ItemStack tool) {
        Identifier id = ToolData.structure(tool);
        if (id != null && level.getBlockEntity(pos) instanceof MachineControllerBlockEntity controller) {
            StructureModel model = selectedStructure(tool);
            if (model == null) return null;
            for (var pieces : model.layout().getRotatedPositionedPieces().values()) {
                for (var piece : pieces) {
                    if (!ToolTarget.usablePosition(level, piece.findAbsolutePos(pos))) return null;
                }
            }
            if (controller.findAssemblyCandidate(id) != null) return controller;
        }
        return null;
    }

    public static @Nullable BuildableStructure selectedBuilderStructure(ItemStack tool) {
        Identifier id = ToolData.builderStructure(tool);
        return id == null ? null : BuildableStructureRegistry.SERVER.get(id);
    }

    public static Result prepare(Level level, Player player, ItemStack tool, BlockPos clickedPos, Direction clickedFace) {
        ChainedMaterialSource source = ToolBuildPlan.source(player, tool);
        return prepare(level, player, tool, clickedPos, clickedFace, source).withNotice(meNotice(player, tool, source));
    }

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

    private static Result prepareFixed(Level level, Player player, ItemStack tool, BlockPos clickedPos, Direction clickedFace,
                                       BuildableStructure structure, ChainedMaterialSource source) {
        if (!structure.buildable()) {
            return Result.error(Component.translatable("message.mm.tool.unbuildable", structure.unbuildableBlock().getName()));
        }
        FixedBuildPlan build = FixedBuildPlan.create(level, structure, clickedPos, clickedFace, ToolData.placementFacing(tool, player), ToolData.extraTurns(tool));
        if (!build.obstructed().isEmpty()) {
            return Result.error(StructurePasteUtil.obstructionMessage("message.mm.tool.obstructed", build.obstructed()));
        }
        int perBlockFe = MMConfig.TOOL_ENERGY_PER_PLACED_BLOCK;
        return startable(level, player, tool, source, new Prepared(build.center(), build.plan(), source, perBlockFe, false));
    }

    private static @Nullable Component meNotice(Player player, ItemStack tool, ChainedMaterialSource source) {
        if (source.me() != null || source.free() || !(player instanceof ServerPlayer serverPlayer)) {
            return null;
        }
        return MeAccessFactory.problem(serverPlayer, tool);
    }

    private static Result prepare(Level level, Player player, ItemStack tool, BlockPos clickedPos, Direction clickedFace,
                                  StructureModel structure, ChainedMaterialSource source) {
        int perBlockFe = MMConfig.TOOL_ENERGY_PER_PLACED_BLOCK;
        MachineControllerBlockEntity controller = acceptingController(level, clickedPos, tool);
        if (controller != null) {
            var link = controller.getNetworkLink();
            if (link != null && !Permissions.canAccess(player, link.owner())) {
                return Result.error(Component.translatable("message.mm.tool.no_access"));
            }
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
            return Result.error(StructurePasteUtil.obstructionMessage("message.mm.tool.obstructed", build.obstructed()));
        }
        AssemblyPlanner.Planned controllerStep = build.plan().steps().get(0);
        Block controllerBlock = controllerStep.state().getBlock();
        boolean controllerThere = level.getBlockState(build.controllerPos()).is(controllerBlock);
        if (!controllerThere && (!player.mayBuild() || !level.mayInteract(player, build.controllerPos()))) {
            return Result.error(Component.translatable("message.mm.tool.protected"));
        }
        return startable(level, player, tool, source, new Prepared(build.controllerPos(), build.plan(), source, perBlockFe));
    }

    private static Result startable(Level level, Player player, ItemStack tool, ChainedMaterialSource source, Prepared prepared) {
        if (prepared.plan().steps().stream().anyMatch(step -> !ToolTarget.usablePosition(level, step.pos()))) {
            return Result.error(Component.translatable("message.mm.tool.target_missing"));
        }
        if (!source.free() && prepared.perBlockFe() > 0
                && ToolEnergy.of(tool).getEnergyStored() < prepared.perBlockFe()) {
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
