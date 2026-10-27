package io.ticticboom.mods.mm.compat.waila;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.pattern.PatternMismatch;
import io.ticticboom.mods.mm.pattern.PatternPortBlockEntity;
import io.ticticboom.mods.mm.pattern.PatternRun;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.Locale;

/** Jade tooltip of a Pattern Port: its machine, the patterns running, outputs waiting and why the last push was refused. */
public class PatternPortDataProvider implements IServerDataProvider<BlockAccessor>, IBlockComponentProvider {
    public static final ResourceLocation UID = Ref.id("pattern_port");
    private static final String MACHINE_KEY = "PatternMachine";
    private static final String RUNS_KEY = "PatternRuns";
    private static final String RETURNING_KEY = "PatternReturning";
    private static final String LAST_KEY = "PatternLast";
    private static final String WHY_KEY = "PatternWhy";

    public static final PatternPortDataProvider INSTANCE = new PatternPortDataProvider();

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof PatternPortBlockEntity port)) {
            return;
        }
        var machine = port.machine();
        if (machine != null) {
            data.putString(MACHINE_KEY, machine.getModel().name());
            int running = 0;
            int returning = 0;
            for (PatternRun run : machine.getPatternRuns()) {
                if (run.isReturning()) returning++;
                else running++;
            }
            data.putInt(RUNS_KEY, running);
            data.putInt(RETURNING_KEY, returning);
        }
        if (port.lastHadNoRecipe()) {
            data.putString(LAST_KEY, "no_recipe");
            if (port.noRecipeWhy() != null) {
                data.put(WHY_KEY, port.noRecipeWhy().save());
            }
        } else if (port.lastResult() != null) {
            data.putString(LAST_KEY, port.lastResult().name().toLowerCase(Locale.ROOT));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (data.contains(MACHINE_KEY)) {
            tooltip.add(Component.translatable("jade.mm.port.machine", data.getString(MACHINE_KEY)));
            tooltip.add(Component.translatable("jade.mm.pattern_port.runs", data.getInt(RUNS_KEY)));
            if (data.getInt(RETURNING_KEY) > 0) {
                tooltip.add(Component.translatable("jade.mm.pattern_port.returning", data.getInt(RETURNING_KEY)).withStyle(ChatFormatting.GOLD));
            }
        } else {
            tooltip.add(Component.translatable("jade.mm.port.no_machine").withStyle(ChatFormatting.GRAY));
        }
        String last = data.getString(LAST_KEY);
        if (!last.isEmpty() && !last.equals("started")) {
            tooltip.add(Component.translatable("jade.mm.pattern_port.last." + last).withStyle(ChatFormatting.GRAY));
        }
        PatternMismatch why = data.contains(WHY_KEY) ? PatternMismatch.load(data.getCompound(WHY_KEY)) : null;
        if (why != null) {
            tooltip.add(Component.translatable("jade.mm.pattern_port.why." + why.reason().name().toLowerCase(Locale.ROOT),
                    why.what().toString(), why.patternAmount(), why.recipeAmount()).withStyle(ChatFormatting.GRAY));
        }
    }
}
