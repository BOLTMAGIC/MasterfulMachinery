package io.ticticboom.mods.mm.controller;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.controller.machine.MachineControllerType;
import io.ticticboom.mods.mm.controller.single.SingleMachineControllerType;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.*;

public class MMControllerRegistry {
    private static Map<Identifier, ControllerType> CONTROLLER_TYPES = new HashMap<>();
    public static List<RegistryGroupHolder> CONTROLLERS = new ArrayList<>();

    public static void init() {
        register(Ref.Controller.MACHINE, new MachineControllerType());
        register(Ref.Controller.SINGLE, new SingleMachineControllerType());
    }

    public static ControllerType get(Identifier id) {
        return CONTROLLER_TYPES.get(id);
    }

    public static void register(Identifier id, ControllerType type) {
        CONTROLLER_TYPES.put(id, type);
    }

    public static Collection<ControllerType> getControllers() {
        return CONTROLLER_TYPES.values();
    }
    public static Item getControllerItem(Identifier id) {
        for (RegistryGroupHolder holder : CONTROLLERS) {
            if (!holder.getItem().isBound()) continue;
            Item item = holder.getItem().get();
            if (item instanceof IControllerPart part) {
                if (part.getModel().id().equals(id.getPath())) {
                    return item;
                }
            }
        }
        return null;
    }
    public static Block getControllerBlock(Identifier id) {
        for (RegistryGroupHolder holder : CONTROLLERS) {
            if (!holder.getBlock().isBound()) continue;
            var block = holder.getBlock().get();
            if (block instanceof IControllerPart part) {
                if (part.getModel().id().equals(id.getPath())) {
                    return block;
                }
            }
        }
        return null;
    }
}
