package io.ticticboom.mods.mm.tool;

import io.ticticboom.mods.mm.builder.AssemblyPlanner;
import io.ticticboom.mods.mm.builder.ChainedMaterialSource;
import io.ticticboom.mods.mm.builder.MaterialSource;
import io.ticticboom.mods.mm.builder.me.MeAccessFactory;
import io.ticticboom.mods.mm.config.MMConfigSetup;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.networklink.LinkData;
import io.ticticboom.mods.mm.networklink.Permissions;
import io.ticticboom.mods.mm.structure.StructureManager;
import io.ticticboom.mods.mm.structure.StructureModel;
import io.ticticboom.mods.mm.util.StructurePasteUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/** Decides what using the multiblock tool on a block builds: finish a matching controller, or a whole new machine. */
public final class ToolBuilds {
    private ToolBuilds() {
    }

    /** A build ready to hand to {@link io.ticticboom.mods.mm.builder.AssemblyJobs#start}. */
    public record Prepared(BlockPos controllerPos, AssemblyPlanner.Plan plan, MaterialSource source, int perBlockFe) {
    }

    /**
     * Exactly one of {@code prepared} and {@code error} is set. {@code notice}, when set, is worth telling the player
     * either way (the tool's ME network could not be reached, so only the store and inventory were used).
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

    public static Result prepare(Level level, Player player, ItemStack tool, BlockPos clickedPos, Direction clickedFace) {
        StructureModel structure = selectedStructure(tool);
        if (structure == null) {
            return Result.error(Component.translatable("message.mm.tool.no_structure"));
        }
        ChainedMaterialSource source = ToolBuildPlan.source(player, tool);
        return prepare(level, player, tool, clickedPos, clickedFace, structure, source).withNotice(meNotice(player, tool, source));
    }

    /**
     * A bound tool whose network is turned on but cannot be reached right now (chunk not loaded, block removed,
     * unpowered): the build goes on from the store and inventory, and the player should know why ME was not used.
     */
    private static @Nullable Component meNotice(Player player, ItemStack tool, ChainedMaterialSource source) {
        LinkData.NetworkPos network = ToolData.network(tool);
        if (network == null || source.me() != null || source.free() || !ToolData.useMe(tool)
                || !(player instanceof ServerPlayer) || !MeAccessFactory.available()) {
            return null;
        }
        return Component.translatable("message.mm.tool.me_unreachable",
                network.pos().toShortString(), network.dimension().location().getPath()).withStyle(ChatFormatting.YELLOW);
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
            return startable(tool, new Prepared(controller.getBlockPos(), plan, source, perBlockFe));
        }

        ToolBuildPlan build = ToolBuildPlan.create(level, player, tool, structure, clickedPos, clickedFace, source.snapshot());
        if (build == null) {
            return Result.error(Component.translatable("message.mm.tool.no_controller"));
        }
        if (!build.obstructed().isEmpty()) {
            // nothing is ever broken; the build does not start at all
            return Result.error(StructurePasteUtil.obstructionMessage("message.mm.tool.obstructed", build.obstructed()));
        }
        // without its controller nothing else is worth building
        AssemblyPlanner.Planned controllerStep = build.plan().steps().get(0);
        Block controllerBlock = controllerStep.state().getBlock();
        boolean controllerThere = level.getBlockState(build.controllerPos()).is(controllerBlock);
        if (!controllerThere && (!player.mayBuild() || !level.mayInteract(player, build.controllerPos()))) {
            return Result.error(Component.translatable("message.mm.tool.protected"));
        }
        if (!controllerThere && !source.has(controllerBlock)) {
            return Result.error(Component.translatable("message.mm.assemble.missing", controllerBlock.getName()));
        }
        return startable(tool, new Prepared(build.controllerPos(), build.plan(), source, perBlockFe));
    }

    /** Refuses a build the tool cannot pay even its first block for, so it never starts only to stop at once. */
    private static Result startable(ItemStack tool, Prepared prepared) {
        if (!prepared.source().free() && prepared.perBlockFe() > 0
                && new ToolEnergy(tool, MMConfigSetup.COMMON.toolEnergyCapacity.get()).getEnergyStored() < prepared.perBlockFe()) {
            return Result.error(Component.translatable("message.mm.assemble.out_of_energy", 0, prepared.plan().steps().size()));
        }
        return new Result(prepared, null, null);
    }
}
