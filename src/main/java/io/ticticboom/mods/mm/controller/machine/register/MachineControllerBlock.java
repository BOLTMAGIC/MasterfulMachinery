package io.ticticboom.mods.mm.controller.machine.register;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.controller.IControllerBlock;
import io.ticticboom.mods.mm.controller.IControllerPart;
import io.ticticboom.mods.mm.datagen.provider.MMBlockstateProvider;
import io.ticticboom.mods.mm.model.ControllerModel;
import io.ticticboom.mods.mm.port.kinetic.register.CreateKineticGenPortBlockEntity;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import io.ticticboom.mods.mm.util.BlockUtils;
import io.ticticboom.mods.mm.util.WorldUtil;
import io.ticticboom.mods.mm.config.MMConfigSetup;
import net.minecraft.resources.ResourceLocation;
import io.ticticboom.mods.mm.config.WorkingEffectsConfig;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;

public class MachineControllerBlock extends HorizontalDirectionalBlock implements IControllerPart, IControllerBlock {
    private final ControllerModel model;
    private final RegistryGroupHolder groupHolder;

    public MachineControllerBlock(ControllerModel model, RegistryGroupHolder groupHolder) {
        super(BlockUtils.createBlockProperties());
        this.model = model;
        this.groupHolder = groupHolder;
        registerDefaultState(this.getStateDefinition().any()
                .setValue(FACING, Direction.NORTH)
                .setValue(ControllerState.PROPERTY, ControllerState.UNFORMED));
    }

    @Override
    public ControllerModel getModel() {
        return model;
    }

    @Override
    public void generateModel(MMBlockstateProvider provider) {
        // one model per state (<id>, <id>_idle, <id>_working) so resource packs can give each state its own look
        var id = groupHolder.getBlock().getId();
        var models = new EnumMap<ControllerState, ModelFile>(ControllerState.class);
        for (ControllerState state : ControllerState.values()) {
            var loc = state == ControllerState.UNFORMED ? id : id.withSuffix("_" + state.getSerializedName());
            models.put(state, provider.controllerModel(loc, Ref.Textures.BASE_BLOCK, Ref.Textures.CONTROLLER_FRAME,
                    Ref.Textures.controllerScreen(state.getSerializedName())));
        }
        provider.getVariantBuilder(groupHolder.getBlock().get())
                .forAllStates(state -> ConfiguredModel.builder()
                        .modelFile(models.get(state.getValue(ControllerState.PROPERTY)))
                        .rotationY((int) state.getValue(FACING).toYRot())
                        .build());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ControllerState.PROPERTY);
    }


    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        // a controller renamed in an anvil keeps its name
        if (stack.hasCustomHoverName() && level.getBlockEntity(pos) instanceof MachineControllerBlockEntity controller) {
            controller.setCustomName(stack.getHoverName().getString());
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        return BlockUtils.commonUse(state, level, pos, player, hand, hitResult, MachineControllerBlockEntity.class, null);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return groupHolder.getBe().get().create(blockPos, blockState);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type == groupHolder.getBe().get()) {
            return (l, pos, s, be) -> ((MachineControllerBlockEntity) be).tick();
        }
        return null;
    }

    /**
     * Called every client tick by the controller: while the machine works, plays the controller's workingSound every
     * workingSoundInterval ticks and puts its workingParticle on the screen side. Both are optional and off unless the
     * controller sets them. (animateTick is only called for a few random blocks per tick, far too rarely for a sound.)
     */
    public void tickWorkingEffects(BlockState state, Level level, BlockPos pos) {
        if (!state.hasProperty(ControllerState.PROPERTY) || state.getValue(ControllerState.PROPERTY) != ControllerState.WORKING
                || !MMConfigSetup.CLIENT.workingEffects.get()) {
            return;
        }
        resolveWorkingEffects();
        RandomSource random = level.getRandom();
        double x = pos.getX() + 0.5;
        double z = pos.getZ() + 0.5;
        // offset by position so machines started together don't all play at once
        if (workingSound != null && (level.getGameTime() + pos.hashCode()) % workingSoundInterval == 0) {
            level.playLocalSound(x, pos.getY() + 0.5, z, workingSound, SoundSource.BLOCKS, 1.0F, 1.0F, false);
        }
        if (workingParticle != null && random.nextInt(4) == 0) {
            Direction front = state.getValue(FACING).getOpposite();
            double along = random.nextDouble() * 0.6 - 0.3;
            double offsetX = front.getAxis() == Direction.Axis.X ? front.getStepX() * 0.52 : along;
            double offsetZ = front.getAxis() == Direction.Axis.Z ? front.getStepZ() * 0.52 : along;
            level.addParticle(workingParticle, x + offsetX, pos.getY() + 0.2 + random.nextDouble() * 0.6, z + offsetZ, 0, 0, 0);
        }
    }

    // resolved on first use (once the registries are filled) and again after config/mm/working_effects.json reloads
    private int workingEffectsGeneration = -1;
    @Nullable
    private SoundEvent workingSound;
    private int workingSoundInterval;
    @Nullable
    private SimpleParticleType workingParticle;

    private void resolveWorkingEffects() {
        int generation = WorkingEffectsConfig.generation();
        if (workingEffectsGeneration == generation) return;
        workingEffectsGeneration = generation;
        var blockId = groupHolder.getBlock().getId();
        // config/mm/working_effects.json overrides what the controller sets itself (KubeJS or JSON)
        var entry = WorkingEffectsConfig.get(blockId);
        ResourceLocation soundId = entry != null && entry.sound() != null ? idOrNull(entry.sound()) : model.workingSound();
        ResourceLocation particleId = entry != null && entry.particle() != null ? idOrNull(entry.particle()) : model.workingParticle();
        workingSoundInterval = entry != null && entry.interval() != null ? entry.interval() : model.workingSoundInterval();

        workingSound = null;
        if (soundId != null) {
            workingSound = ForgeRegistries.SOUND_EVENTS.getValue(soundId);
            if (workingSound == null) Ref.LOG.warn("Unknown working sound {} on controller {}", soundId, blockId);
        }
        workingParticle = null;
        if (particleId != null) {
            if (ForgeRegistries.PARTICLE_TYPES.getValue(particleId) instanceof SimpleParticleType simple) {
                workingParticle = simple;
            } else {
                Ref.LOG.warn("Working particle {} on controller {} is unknown or needs options, only simple particles are supported", particleId, blockId);
            }
        }
    }

    // "" in the config turns an effect off
    @Nullable
    private static ResourceLocation idOrNull(String id) {
        return id.isBlank() ? null : ResourceLocation.tryParse(id);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof MachineControllerBlockEntity be ? be.getComparatorSignal() : 0;
    }

    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (oldState.getBlock() != newState.getBlock()) {
            if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
                var be = WorldUtil.getBlockEntity(pos, serverLevel);
                if (be instanceof MachineControllerBlockEntity mbe) {
                    mbe.invalidateProgress();
                    mbe.resetPortStates();
                }
            }

            super.onRemove(oldState, level, pos, newState, isMoving);
        }
    }
}

