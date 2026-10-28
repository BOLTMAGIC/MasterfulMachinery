package io.ticticboom.mods.mm.event;

import com.google.gson.JsonElement;
import io.ticticboom.mods.mm.recipe.MachineRecipeManager;
import io.ticticboom.mods.mm.structure.StructureManager;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

import java.util.Map;

/**
 * Since 26.1 items get their data components only after every server reload listener has run, and MM's structures
 * and recipes build item stacks (and read their components) while parsing. The reload listeners therefore just keep
 * the JSON, and it is parsed here, once the components are bound: before the server starts, and on /reload when the
 * new data is synced to the players (before MM validates its structures).
 */
@EventBusSubscriber
public final class DeferredDataLoad {
    private static Map<Identifier, JsonElement> structures;
    private static Map<Identifier, JsonElement> recipes;

    private DeferredDataLoad() {
    }

    public static void structures(Map<Identifier, JsonElement> jsons) {
        structures = jsons;
    }

    public static void recipes(Map<Identifier, JsonElement> jsons) {
        recipes = jsons;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        finish();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        finish();
    }

    /** Parses what the last reload left; structures first, as recipes refer to them. */
    public static synchronized void finish() {
        if (structures != null) {
            var pending = structures;
            structures = null;
            StructureManager.receiveStructures(pending);
        }
        if (recipes != null) {
            var pending = recipes;
            recipes = null;
            MachineRecipeManager.recieveRecipes(pending);
        }
    }
}
