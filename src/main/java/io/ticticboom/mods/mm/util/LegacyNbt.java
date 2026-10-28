package io.ticticboom.mods.mm.util;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.ticticboom.mods.mm.Ref;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.stream.Stream;

/**
 * Block entities save through ValueOutput / ValueInput since 1.21.6. MM's storages, ports and controllers still build
 * their data as a CompoundTag; this writes those entries at the top level of the block entity's data and reads them
 * back, so the saved layout stays the one 1.21.1 wrote (worlds, structure captures and blueprints keep working).
 */
public final class LegacyNbt {

    /** Every entry of a CompoundTag as top-level fields, converted to and from whatever format the ops use. */
    private static final MapCodec<CompoundTag> FLAT = new MapCodec<>() {
        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return Stream.empty();
        }

        @Override
        public <T> DataResult<CompoundTag> decode(DynamicOps<T> ops, MapLike<T> input) {
            var tag = new CompoundTag();
            input.entries().forEach(entry -> ops.getStringValue(entry.getFirst()).result()
                    .ifPresent(key -> tag.put(key, ops.convertTo(NbtOps.INSTANCE, entry.getSecond()))));
            return DataResult.success(tag);
        }

        @Override
        public <T> RecordBuilder<T> encode(CompoundTag tag, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            for (String key : tag.keySet()) {
                Tag value = tag.get(key);
                if (value != null) {
                    prefix.add(key, NbtOps.INSTANCE.convertTo(ops, value));
                }
            }
            return prefix;
        }
    };

    private LegacyNbt() {
    }

    public static void write(ValueOutput output, CompoundTag tag) {
        output.store(FLAT, tag);
    }

    public static CompoundTag read(ValueInput input) {
        return input.read(FLAT).orElseGet(CompoundTag::new);
    }

    /** What a ValueIO-saved object (an ItemStackHandler, say) writes, as a CompoundTag. */
    public static CompoundTag save(ValueIOSerializable serializable, HolderLookup.Provider registries) {
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        serializable.serialize(output);
        return output.buildResult();
    }

    public static void load(ValueIOSerializable serializable, HolderLookup.Provider registries, CompoundTag tag) {
        serializable.deserialize(TagValueInput.create(ProblemReporter.DISCARDING, registries, tag));
    }

    /** ItemStack.save / saveOptional, gone since 1.21.5: an empty stack saves as an empty compound. */
    public static Tag saveItem(ItemStack stack, HolderLookup.Provider registries) {
        if (stack.isEmpty()) {
            return new CompoundTag();
        }
        return ItemStack.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), stack).getOrThrow();
    }

    /** ItemStack.parseOptional: an empty stack for anything that does not read. */
    public static ItemStack parseItem(HolderLookup.Provider registries, Tag tag) {
        if (tag instanceof CompoundTag compound && compound.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return ItemStack.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag)
                .resultOrPartial(error -> Ref.LOG.error("Tried to load invalid item: {}", error))
                .orElse(ItemStack.EMPTY);
    }

    public static Tag saveFluid(FluidStack stack, HolderLookup.Provider registries) {
        if (stack.isEmpty()) {
            return new CompoundTag();
        }
        return FluidStack.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), stack).getOrThrow();
    }

    public static FluidStack parseFluid(HolderLookup.Provider registries, Tag tag) {
        if (tag instanceof CompoundTag compound && compound.isEmpty()) {
            return FluidStack.EMPTY;
        }
        return FluidStack.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag)
                .resultOrPartial(error -> Ref.LOG.error("Tried to load invalid fluid: {}", error))
                .orElse(FluidStack.EMPTY);
    }

    /** CompoundTag.putUUID, gone since 1.21.5; stored the same way (an int array). */
    public static void putUuid(CompoundTag tag, String key, UUID uuid) {
        tag.store(key, UUIDUtil.CODEC, uuid);
    }

    public static @Nullable UUID getUuid(CompoundTag tag, String key) {
        return tag.read(key, UUIDUtil.CODEC).orElse(null);
    }

    /** Component.Serializer.toJson / fromJson, gone since 1.21.6. */
    public static String componentToJson(Component component, HolderLookup.Provider registries) {
        return ComponentSerialization.CODEC.encodeStart(registries.createSerializationContext(JsonOps.INSTANCE), component)
                .getOrThrow().toString();
    }

    public static @Nullable Component componentFromJson(String json, HolderLookup.Provider registries) {
        try {
            return ComponentSerialization.CODEC.parse(registries.createSerializationContext(JsonOps.INSTANCE), JsonParser.parseString(json))
                    .result().orElse(null);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Entity.save(CompoundTag), which takes a ValueOutput since 1.21.6; null when the entity is not saved. */
    public static @Nullable CompoundTag saveEntity(Entity entity) {
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, entity.registryAccess());
        return entity.save(output) ? output.buildResult() : null;
    }

    /** The registries to save items and other registry-backed data with: the level's, else the running server's. */
    public static HolderLookup.Provider registries(@Nullable Level level) {
        if (level != null) {
            return level.registryAccess();
        }
        var server = ServerLifecycleHooks.getCurrentServer();
        return server != null ? server.registryAccess() : RegistryAccess.EMPTY;
    }
}
