package io.ticticboom.mods.mmtest;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.builder.PortTiers;
import io.ticticboom.mods.mm.builder.TierResolver;
import io.ticticboom.mods.mm.net.packet.ToolSettingsPkt;
import io.ticticboom.mods.mm.net.packet.ToolSettingsPkt.Action;
import io.ticticboom.mods.mm.networklink.LinkData;
import io.ticticboom.mods.mm.setup.MMRegisters;
import io.ticticboom.mods.mm.structure.StructureManager;
import io.ticticboom.mods.mm.tool.ToolData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.Map;

/**
 * Server-side checks of the tool screen's requests ({@link ToolSettingsPkt#apply}), against the real structures and
 * the registered test item ports (tiers 1-3).
 */
@GameTestHolder("mmtest")
@PrefixGameTestTemplate(false)
public class ToolSettingsGameTests {
    private static final String TEMPLATE = "empty";
    private static final ResourceLocation STRUCTURE = ResourceLocation.tryBuild("mmtest", "assembly_test");
    private static final String ITEM_INPUT = PortTiers.key(Ref.Ports.ITEM, true);

    @GameTest(template = TEMPLATE)
    public static void selectStructureNeedsKnownId(GameTestHelper helper) {
        ItemStack tool = MMRegisters.MULTIBLOCK_TOOL.get().getDefaultInstance();
        Map<String, Integer> tiers = PortTiers.registeredMaxTiers();

        check(helper, apply(tool, Action.SELECT_STRUCTURE, STRUCTURE.toString(), 0, tiers), "a known structure should be selected");
        check(helper, STRUCTURE.equals(ToolData.structure(tool)), "tool should hold " + STRUCTURE + ", got " + ToolData.structure(tool));

        check(helper, !apply(tool, Action.SELECT_STRUCTURE, "mmtest:no_such_structure", 0, tiers), "an unknown id should be refused");
        check(helper, !apply(tool, Action.SELECT_STRUCTURE, "Not An Id!", 0, tiers), "an invalid id should be refused");
        check(helper, STRUCTURE.equals(ToolData.structure(tool)), "a refused id must keep the selection, got " + ToolData.structure(tool));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void setTierIsClampedToRegisteredTiers(GameTestHelper helper) {
        ItemStack tool = MMRegisters.MULTIBLOCK_TOOL.get().getDefaultInstance();
        Map<String, Integer> tiers = PortTiers.registeredMaxTiers();
        check(helper, tiers.getOrDefault(ITEM_INPUT, 0) == 3, "test item input ports should reach tier 3, got " + tiers);

        check(helper, apply(tool, Action.SET_TIER, ITEM_INPUT, 2, tiers), "tier 2 should be taken");
        check(helper, ToolData.tiers(tool).get(ITEM_INPUT) == 2, "expected tier 2, got " + ToolData.tiers(tool).get(ITEM_INPUT));

        check(helper, apply(tool, Action.SET_TIER, ITEM_INPUT, 99, tiers), "a too high tier should be clamped, not refused");
        check(helper, ToolData.tiers(tool).get(ITEM_INPUT) == 3, "expected clamping to 3, got " + ToolData.tiers(tool).get(ITEM_INPUT));

        check(helper, apply(tool, Action.SET_TIER, ITEM_INPUT, -7, tiers), "a negative tier should mean lowest");
        check(helper, ToolData.tiers(tool).get(ITEM_INPUT) == TierResolver.LOWEST, "expected lowest, got " + ToolData.tiers(tool).get(ITEM_INPUT));

        check(helper, !apply(tool, Action.SET_TIER, "mm:no_such_port/input", 2, tiers), "an unknown port type should be refused");
        check(helper, !apply(tool, Action.SET_TIER, PortTiers.key(Ref.Ports.FLUID, true), 2, tiers), "a port type without tiers should be refused");
        check(helper, ToolData.tiers(tool).asMap().isEmpty(), "refused keys must not be stored, got " + ToolData.tiers(tool).asMap());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void meTogglesAndForgetAreValidated(GameTestHelper helper) {
        ItemStack tool = MMRegisters.MULTIBLOCK_TOOL.get().getDefaultInstance();
        Map<String, Integer> tiers = PortTiers.registeredMaxTiers();
        check(helper, ToolData.useMe(tool), "useMe should default to true");
        check(helper, ToolData.autoCraft(tool), "autoCraft should default to true");

        check(helper, apply(tool, Action.SET_USE_ME, "", 0, tiers), "turning useMe off should be accepted");
        check(helper, !ToolData.useMe(tool), "useMe should now be false");
        check(helper, apply(tool, Action.SET_USE_ME, "", 1, tiers), "turning useMe on should be accepted");
        check(helper, ToolData.useMe(tool), "useMe should now be true");

        check(helper, apply(tool, Action.SET_AUTOCRAFT, "", 0, tiers), "turning autoCraft off should be accepted");
        check(helper, !ToolData.autoCraft(tool), "autoCraft should now be false");
        check(helper, apply(tool, Action.SET_AUTOCRAFT, "", 1, tiers), "turning autoCraft on should be accepted");
        check(helper, ToolData.autoCraft(tool), "autoCraft should now be true");

        LinkData.NetworkPos network = new LinkData.NetworkPos(helper.getLevel().dimension(), BlockPos.ZERO, Direction.NORTH);
        ToolData.setNetwork(tool, network);
        check(helper, ToolData.network(tool) != null, "network should be bound before forgetting it");
        check(helper, apply(tool, Action.FORGET_NETWORK, "", 0, tiers), "forgetting the network should be accepted");
        check(helper, ToolData.network(tool) == null, "network should be cleared after FORGET_NETWORK");

        ItemStack other = new ItemStack(Items.STICK);
        check(helper, !apply(other, Action.SET_USE_ME, "", 1, tiers), "another item: refused");
        check(helper, !apply(other, Action.FORGET_NETWORK, "", 0, tiers), "another item: refused");
        check(helper, !other.hasTag(), "another item must stay untouched, got " + other.getTag());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void toolDataNetworkAndTogglesRoundTrip(GameTestHelper helper) {
        ItemStack tool = MMRegisters.MULTIBLOCK_TOOL.get().getDefaultInstance();
        check(helper, ToolData.network(tool) == null, "a fresh tool should not be bound");
        check(helper, ToolData.useMe(tool), "a fresh tool should default useMe to true");
        check(helper, ToolData.autoCraft(tool), "a fresh tool should default autoCraft to true");

        Level level = helper.getLevel();
        LinkData.NetworkPos network = new LinkData.NetworkPos(level.dimension(), helper.absolutePos(new BlockPos(1, 2, 3)), Direction.EAST);
        ToolData.setNetwork(tool, network);
        ToolData.setUseMe(tool, false);
        ToolData.setAutoCraft(tool, false);

        LinkData.NetworkPos loaded = ToolData.network(tool);
        check(helper, network.equals(loaded), "network should round-trip through the tool's NBT, got " + loaded);
        check(helper, !ToolData.useMe(tool), "useMe should round-trip as false");
        check(helper, !ToolData.autoCraft(tool), "autoCraft should round-trip as false");

        ToolData.setNetwork(tool, null);
        ToolData.setUseMe(tool, true);
        ToolData.setAutoCraft(tool, true);
        check(helper, ToolData.network(tool) == null, "network should be clearable again");
        check(helper, ToolData.useMe(tool), "useMe should round-trip back to true");
        check(helper, ToolData.autoCraft(tool), "autoCraft should round-trip back to true");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void settingsNeedTheTool(GameTestHelper helper) {
        Map<String, Integer> tiers = PortTiers.registeredMaxTiers();
        ItemStack other = new ItemStack(Items.STICK);

        check(helper, !apply(null, Action.SELECT_STRUCTURE, STRUCTURE.toString(), 0, tiers), "no held tool: refused");
        check(helper, !apply(other, Action.SELECT_STRUCTURE, STRUCTURE.toString(), 0, tiers), "another item: refused");
        check(helper, !apply(other, Action.SET_TIER, ITEM_INPUT, 2, tiers), "another item: refused");
        check(helper, !other.hasTag(), "another item must stay untouched, got " + other.getTag());
        helper.succeed();
    }

    private static boolean apply(ItemStack tool, Action action, String key, int value, Map<String, Integer> tiers) {
        return ToolSettingsPkt.apply(tool, action, key, value, StructureManager.STRUCTURES::containsKey, tiers);
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }
}
