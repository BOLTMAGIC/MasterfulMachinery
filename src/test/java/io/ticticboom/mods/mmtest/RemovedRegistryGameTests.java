package io.ticticboom.mods.mmtest;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.core.registries.Registries;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.controller.MMControllerRegistry;
import io.ticticboom.mods.mm.setup.MMRegisters;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;

import java.util.ArrayList;

@GameTestHolder
public class RemovedRegistryGameTests {
    @GameTest(template = "empty")
    public static void creativeItemsSkipRemovedController(GameTestHelper helper) {
        DeferredHolder<Item, Item> missing = DeferredHolder.create(Registries.ITEM, Ref.id("removed_test_controller"));
        check(helper, !missing.isBound(), "fixture must refer to an absent item");
        var entries = new ArrayList<>(MMRegisters.ITEMS.getEntries());
        entries.add(0, missing);
        var stacks = MMRegisters.registeredItemStacks(entries);
        check(helper, stacks.size() == entries.size() - 1, "only the missing entry should be skipped");
        check(helper, stacks.stream().anyMatch(s -> s.is(MMRegisters.BLUEPRINT.get())), "registered items must stay visible");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void controllerLookupSkipsRemovedEntries(GameTestHelper helper) {
        var missing = new RegistryGroupHolder();
        missing.setItem(DeferredHolder.create(Registries.ITEM, Ref.id("removed_test_controller")));
        missing.setBlock(DeferredHolder.create(Registries.BLOCK, Ref.id("removed_test_controller")));
        MMControllerRegistry.CONTROLLERS.add(0, missing);
        try {
            check(helper, MMControllerRegistry.getControllerItem(Ref.id("flex_test")) != null, "live controller item should still be found");
            check(helper, MMControllerRegistry.getControllerBlock(Ref.id("flex_test")) != null, "live controller block should still be found");
            check(helper, MMControllerRegistry.getControllerItem(Ref.id("removed_test_controller")) == null, "removed item should not be found");
            check(helper, MMControllerRegistry.getControllerBlock(Ref.id("removed_test_controller")) == null, "removed block should not be found");
        } finally {
            MMControllerRegistry.CONTROLLERS.remove(missing);
        }
        helper.succeed();
    }
    private static void check(GameTestHelper helper, boolean ok, String message) {
        if (!ok) helper.fail(message);
    }
}
