package io.ticticboom.mods.mm.controller.machine.register;

import net.minecraft.util.RandomSource;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.SimpleParticleType;
import io.ticticboom.mods.mm.config.WorkingEffectsConfig;
import io.ticticboom.mods.mm.config.MMConfigSetup;
import io.ticticboom.mods.mm.util.DisplayNameUtil;
import net.minecraft.network.chat.MutableComponent;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.controller.IControllerBlock;
import io.ticticboom.mods.mm.controller.IControllerPart;
import io.ticticboom.mods.mm.datagen.provider.MMBlockstateProvider;
import io.ticticboom.mods.mm.model.ControllerModel;
// [26.1-pending] import: import io.ticticboom.mods.mm.port.kinetic.register.CreateKineticGenPortBlockEntity;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import io.ticticboom.mods.mm.util.BlockUtils;
import io.ticticboom.mods.mm.util.WorldUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import org.jetbrains.annotations.Nullable;
import java.util.EnumMap;
import java.util.Objects;

public class MachineControllerBlock extends HorizontalDirectionalBlock implements IControllerPart, IControllerBlock {

    /**
     * HorizontalDirectionalBlock declares codec() abstract in 1.20.5+. MM controller blocks are
     * built from data and registered programmatically rather than decoded from a codec, so this
     * exists only to satisfy the contract and is never used to construct one.
     */
    @Override
    protected com.mojang.serialization.MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return com.mojang.serialization.MapCodec.unit(this);
    }
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
        var id = groupHolder.getBlock().getId();
        var models = new EnumMap<ControllerState, Identifier>(ControllerState.class);
        var base = Objects.requireNonNullElse(model.baseTexture(), Ref.Textures.BASE_BLOCK);
        for (ControllerState state : ControllerState.values()) {
            var name = state.getSerializedName();
            var loc = state == ControllerState.UNFORMED ? id : id.withSuffix("_" + name);
            var custom = model.customModel(name);
            var overlay = model.overlayTexture(name);
            if (custom != null) {
                models.put(state, provider.customBlock(loc, custom));
            } else if (overlay != null) {
                models.put(state, provider.dynamicBlockNorthOverlay(loc, base, overlay));
            } else {
                models.put(state, provider.controllerModel(loc, base, Ref.Textures.CONTROLLER_FRAME,
                        Ref.Textures.controllerScreen(name)));
            }
        }
        provider.variants(groupHolder.getBlock().get(), state -> new MMBlockstateProvider.Variant(
                models.get(state.getValue(ControllerState.PROPERTY)), (int) state.getValue(FACING).toYRot()));
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
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof MachineControllerBlockEntity controller) {
            serverPlayer.openMenu(controller, buf -> MachineControllerMenu.writeOpenData(buf, controller));
        }
        return InteractionResult.SUCCESS;
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

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof MachineControllerBlockEntity be ? be.getComparatorSignal() : 0;
    }

    public void tickWorkingEffects(BlockState state, Level level, BlockPos pos, boolean soundMuted) {
        if (state.getValue(ControllerState.PROPERTY) != ControllerState.WORKING || !MMConfigSetup.CLIENT.workingEffects.get()) {
            return;
        }
        resolveWorkingEffects();
        RandomSource random = level.getRandom();
        double x = pos.getX() + 0.5;
        double z = pos.getZ() + 0.5;
        if (workingSound != null && !soundMuted && (level.getGameTime() + pos.hashCode()) % workingSoundInterval == 0) {
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
        var entry = WorkingEffectsConfig.get(blockId);
        Identifier soundId = entry != null && entry.sound() != null ? idOrNull(entry.sound()) : model.workingSound();
        Identifier particleId = entry != null && entry.particle() != null ? idOrNull(entry.particle()) : model.workingParticle();
        workingSoundInterval = entry != null && entry.interval() != null ? entry.interval() : model.workingSoundInterval();

        workingSound = null;
        if (soundId != null) {
            workingSound = BuiltInRegistries.SOUND_EVENT.getOptional(soundId).orElse(null);
            if (workingSound == null) Ref.LOG.warn("Unknown working sound {} on controller {}", soundId, blockId);
        }
        workingParticle = null;
        if (particleId != null) {
            if (BuiltInRegistries.PARTICLE_TYPE.getOptional(particleId).orElse(null) instanceof SimpleParticleType simple) {
                workingParticle = simple;
            } else {
                Ref.LOG.warn("Working particle {} on controller {} is unknown or needs options, only simple particles are supported", particleId, blockId);
            }
        }
    }

    public boolean hasWorkingSound() {
        resolveWorkingEffects();
        return workingSound != null;
    }

    @Nullable
    private static Identifier idOrNull(String id) {
        return id.isBlank() ? null : Identifier.tryParse(id);
    }

    public boolean usesTintedScreen() {
        return model.customModel() == null && model.overlayTexture() == null;
    }

    /**
     * Matches the block item: plain string names keep using the generated block.mm.<id> entry so
     * resource packs can override them; only a pack-supplied translation key bypasses it.
     */
    @Override
    public MutableComponent getName() {
        var name = DisplayNameUtil.packSuppliedName(model.displayName());
        return name != null ? name.copy() : super.getName();
    }
}
