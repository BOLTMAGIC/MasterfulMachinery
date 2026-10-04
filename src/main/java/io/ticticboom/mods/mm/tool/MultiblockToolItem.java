package io.ticticboom.mods.mm.tool;

import io.ticticboom.mods.mm.builder.AssemblyJobs;
import io.ticticboom.mods.mm.builder.DismantlePlanner;
import io.ticticboom.mods.mm.config.MMConfigSetup;
import io.ticticboom.mods.mm.networklink.NetworkLink;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Carries a chosen Masterful Machinery structure, a 54-slot block store and FE; right-click builds it
 * in the world or completes a matching controller, Shift+right-click on a machine (or holding V) dismantles it,
 * Shift+use elsewhere opens the store/gallery/settings screen.
 */
public class MultiblockToolItem extends Item {
    private static final int BAR_COLOR = 0x3399FF;
    /** The "ME network not used" chat notice is repeated at most this often per player (10 s). */
    private static final int NOTICE_TICKS = 200;
    /** Server tick each player was last told the notice (server thread only). */
    private static final Map<UUID, Integer> LAST_NOTICE = new HashMap<>();
    /** The dismantle key's name for the tooltip; the client points this at the bound key (common code cannot). */
    private static Supplier<Component> dismantleKeyName = () -> Component.literal("V");
    private static Supplier<Component> anchorKeyName = () -> Component.literal("G");

    public MultiblockToolItem() {
        super(new Item.Properties().stacksTo(1));
    }

    /** Client setup: names the key actually bound to dismantling in the tooltip. */
    public static void setDismantleKeyName(Supplier<Component> name) {
        dismantleKeyName = name;
    }

    public static void setAnchorKeyName(Supplier<Component> name) { anchorKeyName = name; }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new ICapabilityProvider() {
            private final LazyOptional<IEnergyStorage> energy = LazyOptional.of(() ->
                    new ToolEnergy(stack, MMConfigSetup.COMMON.toolEnergyCapacity.get()));

            @Override
            public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
                return cap == ForgeCapabilities.ENERGY ? energy.cast() : LazyOptional.empty();
            }
        };
    }

    /**
     * Shift+use on air opens the block store menu (Task 3). Right-click on a block is handled
     * separately ({@link #useOn}); this only fires when there's nothing to interact with.
     */
    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide()) {
            openStore((ServerPlayer) player, stack, hand);
        }
        // sidedSuccess (not pass) on both sides: a client-side pass here would also fire use() on the
        // other hand's item (e.g. eating/placing from the offhand) since vanilla falls through on PASS
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private static void openStore(ServerPlayer player, ItemStack stack, InteractionHand hand) {
        NetworkHooks.openScreen(player, new MenuProvider() {
            @Override
            public @NotNull Component getDisplayName() {
                return stack.getHoverName();
            }

            @Override
            public @NotNull AbstractContainerMenu createMenu(int windowId, @NotNull Inventory inv, @NotNull Player p) {
                return new MultiblockToolMenu(windowId, inv, hand);
            }
        }, buf -> buf.writeEnum(hand));
    }

    /**
     * Right-click on a controller that can assemble the selected structure completes it. Runs before the block's own
     * use, which would otherwise open the controller's screen.
     */
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || player.isShiftKeyDown()
                || ToolBuilds.acceptingController(context.getLevel(), context.getClickedPos(), stack) == null) {
            return InteractionResult.PASS;
        }
        return build(context);
    }

    /**
     * Right-click on any other block face builds the selected structure in front of it, controller included.
     * Shift+right-click on an AE2 network block binds the tool to that network; on a machine it dismantles it after a
     * second click; on any other block it opens the store.
     */
    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (!player.isShiftKeyDown()) {
            return build(context);
        }
        // the server decides machine or not; the client only must not also fire use()
        if (player instanceof ServerPlayer serverPlayer) {
            if (NetworkLink.bindTool(serverPlayer, context)) {
                // an AE2 network block: bound to its network, never dismantled or opened through
                return InteractionResult.SUCCESS;
            }
            if (DismantlePlanner.resolve(context.getLevel(), context.getClickedPos()) != null
                    || DismantlePlanner.matchBuilder(context.getLevel(), context.getClickedPos(), context.getItemInHand()) != null) {
                ToolDismantles.shiftClick(serverPlayer, context.getItemInHand(), context.getClickedPos());
            } else {
                openStore(serverPlayer, context.getItemInHand(), context.getHand());
            }
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
    }

    private static InteractionResult build(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (player instanceof ServerPlayer serverPlayer) return buildTarget(serverPlayer, stack);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    public static InteractionResult buildTarget(ServerPlayer player, ItemStack stack) {
        Level level = player.level();
        if (!player.isAlive() || player.isSpectator() || !(stack.getItem() instanceof MultiblockToolItem)) return InteractionResult.FAIL;
        if (player.getCooldowns().isOnCooldown(stack.getItem())) return InteractionResult.FAIL;
        player.getCooldowns().addCooldown(stack.getItem(), 4);
        // an MM structure or another mod's; the server checks it still exists
        if (ToolData.structure(stack) == null && ToolData.builderStructure(stack) == null) {
            player.displayClientMessage(Component.translatable("message.mm.tool.no_structure"), true);
            return InteractionResult.FAIL;
        }
        ToolTarget target = ToolTarget.resolve(player, stack, MMConfigSetup.COMMON.toolBuildRange.get());
        if (target == null) {
            player.displayClientMessage(Component.translatable("message.mm.tool.target_missing"), true);
            return InteractionResult.FAIL;
        }
        // before any planning: a player runs one job at a time
        Component busy = AssemblyJobs.busyMessage(player);
        if (busy != null) {
            player.displayClientMessage(busy, true);
            return InteractionResult.FAIL;
        }
        ToolBuilds.Result result = ToolBuilds.prepare(level, player, stack, target.pos(), target.face());
        if (result.notice() != null && noticeDue(player)) {
            // in chat: the action bar is soon taken by the result or the job's summary
            player.displayClientMessage(result.notice(), false);
        }
        if (result.prepared() == null) {
            player.displayClientMessage(result.error(), true);
            return InteractionResult.FAIL;
        }
        ToolBuilds.Prepared build = result.prepared();
        if (!AssemblyJobs.startTool(player, build.controllerPos(), build.plan(), build.source(), build.perBlockFe(),
                build.requiresController(), target.pos(), ToolData.instantBuild(stack))) {
            player.displayClientMessage(Component.translatable("message.mm.assemble.busy"), true);
            return InteractionResult.FAIL;
        }
        return InteractionResult.CONSUME;
    }

    /** Whether player may be told the ME notice again: once per {@value #NOTICE_TICKS} ticks, not on every right-click. */
    private static boolean noticeDue(ServerPlayer player) {
        int now = player.server.getTickCount();
        Integer last = LAST_NOTICE.get(player.getUUID());
        if (last != null && now - last >= 0 && now - last < NOTICE_TICKS) {
            return false;
        }
        LAST_NOTICE.put(player.getUUID(), now);
        return true;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int capacity = MMConfigSetup.COMMON.toolEnergyCapacity.get();
        int energy = new ToolEnergy(stack, capacity).getEnergyStored();
        return Math.round(13.0F * energy / capacity);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return BAR_COLOR;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        ResourceLocation structure = ToolData.structure(stack);
        if (structure == null) {
            structure = ToolData.builderStructure(stack);
        }
        if (structure != null) {
            tooltip.add(Component.translatable("tooltip.mm.structure_builder.structure", structure.toString()).withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.mm.structure_builder.no_structure").withStyle(ChatFormatting.DARK_GRAY));
        }
        int capacity = MMConfigSetup.COMMON.toolEnergyCapacity.get();
        int energy = new ToolEnergy(stack, capacity).getEnergyStored();
        tooltip.add(Component.translatable("tooltip.mm.structure_builder.energy", energy, capacity).withStyle(ChatFormatting.GRAY));
        // ME networks only exist with AE2
        var network = NetworkLink.AVAILABLE ? ToolData.network(stack) : null;
        if (network != null) {
            tooltip.add(Component.translatable("tooltip.mm.structure_builder.network",
                    network.pos().toShortString(), network.dimension().location().getPath()).withStyle(ChatFormatting.AQUA));
        } else if (NetworkLink.AVAILABLE) {
            tooltip.add(Component.translatable("tooltip.mm.structure_builder.no_network").withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.add(Component.translatable("tooltip.mm.structure_builder.usage", dismantleKeyName.get()).withStyle(ChatFormatting.DARK_GRAY));
        ToolData.Anchor anchor = ToolData.anchor(stack);
        tooltip.add(Component.translatable("tooltip.mm.structure_builder.anchor_key", anchorKeyName.get()).withStyle(ChatFormatting.DARK_GRAY));
        if (anchor != null) {
            tooltip.add(Component.translatable("message.mm.tool.anchor.set", anchor.pos().toShortString()).withStyle(ChatFormatting.AQUA));
        }
    }
}
