package io.ticticboom.mods.mm.port.entity;

import io.ticticboom.mods.mm.util.LegacyNbt;
import net.minecraft.world.entity.EntitySpawnReason;
import io.ticticboom.mods.mm.port.PortContent;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.port.IPortBlockEntity;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import io.ticticboom.mods.mm.port.common.INotifyChangeFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

public class EntityPortStorage implements IPortStorage {

    private final EntityPortStorageModel model;
    private final INotifyChangeFunction changed;
    private final List<PortEntity> entities = new ArrayList<>();
    private final UUID uid = UUID.randomUUID();

    private BlockEntity owner;

    public EntityPortStorage(EntityPortStorageModel model, INotifyChangeFunction changed) {
        this.model = model;
        this.changed = changed;
    }

    public void setOwner(BlockEntity owner) {
        this.owner = owner;
    }

    public EntityPortStorageModel model() {
        return model;
    }

    public List<PortEntity> entities() {
        return List.copyOf(entities);
    }

    @Override
    public List<PortContent> contents() {
        return List.of(PortContent.gauge(net.minecraft.network.chat.Component.translatable("port.mm.entity.name"), 0x7A9A5A, entities.size(), model.capacity(), ""));
    }

    @Override
    public List<net.minecraft.network.chat.Component> describeContents() {
        var lines = new ArrayList<net.minecraft.network.chat.Component>();
        lines.add(net.minecraft.network.chat.Component.translatable("jade.mm.port.mobs", entities.size(), model.capacity()));
        var counts = new java.util.LinkedHashMap<Identifier, Integer>();
        for (var entry : entities) {
            counts.merge(entry.type(), 1, Integer::sum);
        }
        counts.forEach((type, count) -> lines.add(net.minecraft.network.chat.Component.translatable(
                "gui.mm.port.entity.entry", count, EntityTypes.displayName(type))));
        return lines;
    }

    public boolean isFull() {
        return entities.size() >= model.capacity();
    }

    public int count(Predicate<PortEntity> match) {
        int total = 0;
        for (var entry : entities) {
            if (match.test(entry)) {
                total++;
            }
        }
        return total;
    }

    public boolean absorb(LivingEntity entity) {
        if (isFull() || !model.accepts(entity.getType())) {
            return false;
        }
        var id = EntityTypes.idOf(entity.getType());
        if (id == null) {
            return false;
        }
        if (model.mode() == EntityPortMode.STORED) {
            var data = LegacyNbt.saveEntity(entity);
            if (data == null) {
                return false;
            }
            entities.add(PortEntity.stored(id, entity.getUUID(), data));
            entity.discard();
        } else {
            if (isPinned(entity.getUUID())) {
                return false;
            }
            boolean hadAi = !(entity instanceof Mob mob) || !mob.isNoAi();
            boolean wasInvulnerable = entity.isInvulnerable();
            boolean wasSilent = entity.isSilent();
            applyPin(entity);
            entities.add(PortEntity.pinned(id, entity.getUUID(), hadAi, wasInvulnerable, wasSilent));
        }
        changed.call();
        return true;
    }

    public void refresh() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        boolean dirty = false;
        var iterator = entities.iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.isStored() || entry.uuid() == null) {
                continue;
            }
            var entity = serverLevel.getEntity(entry.uuid());
            if (entity == null || !entity.isAlive() || !zone().inflate(0.5).contains(entity.position())) {
                if (entity instanceof LivingEntity living) {
                    releasePin(living, entry);
                }
                iterator.remove();
                dirty = true;
                continue;
            }
            if (entity instanceof LivingEntity living) {
                applyPin(living);
            }
        }
        if (dirty) {
            changed.call();
        }
    }

    public int extract(Predicate<PortEntity> match, int amount, boolean simulate) {
        if (!model.consume()) {
            return Math.min(amount, count(match));
        }
        return consume(match, amount, simulate);
    }

    public int consume(Predicate<PortEntity> match, int amount, boolean simulate) {
        if (amount <= 0) {
            return 0;
        }
        int taken = 0;
        var level = level();
        var iterator = entities.iterator();
        while (iterator.hasNext() && taken < amount) {
            var entry = iterator.next();
            if (!match.test(entry)) {
                continue;
            }
            taken++;
            if (simulate) {
                continue;
            }
            if (!entry.isStored() && entry.uuid() != null && level instanceof ServerLevel serverLevel) {
                var entity = serverLevel.getEntity(entry.uuid());
                if (entity != null) {
                    entity.discard();
                }
            }
            iterator.remove();
        }
        if (taken > 0 && !simulate) {
            changed.call();
        }
        return taken;
    }

    public int produce(EntityType<?> type, int amount, boolean simulate) {
        if (type == null || amount <= 0 || !model.accepts(type)) {
            return 0;
        }
        var id = EntityTypes.idOf(type);
        if (id == null) {
            return 0;
        }
        int room = Math.max(0, model.capacity() - entities.size());
        int made = Math.min(room, amount);
        if (made <= 0 || simulate) {
            return made;
        }
        if (!(level() instanceof ServerLevel serverLevel)) {
            return 0;
        }
        int created = 0;
        for (int i = 0; i < made; i++) {
            var entity = type.create(serverLevel, EntitySpawnReason.SPAWNER);
            if (entity == null) {
                break;
            }
            var target = center();
            entity.snapTo(target.x, target.y, target.z, entity.getYRot(), entity.getXRot());
            if (model.mode() == EntityPortMode.STORED) {
                var data = LegacyNbt.saveEntity(entity);
                if (data == null) {
                    break;
                }
                entities.add(PortEntity.stored(id, entity.getUUID(), data));
            } else {
                if (!serverLevel.addFreshEntity(entity)) {
                    break;
                }
                boolean hadAi = !(entity instanceof Mob mob) || !mob.isNoAi();
                boolean wasInvulnerable = entity.isInvulnerable();
                boolean wasSilent = entity.isSilent();
                if (entity instanceof LivingEntity living) {
                    applyPin(living);
                }
                entities.add(PortEntity.pinned(id, entity.getUUID(), hadAi, wasInvulnerable, wasSilent));
            }
            created++;
        }
        if (created > 0) {
            changed.call();
        }
        return created;
    }

    public void releaseAll() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            entities.clear();
            return;
        }
        for (var entry : entities) {
            if (entry.isStored()) {
                spawnStored(serverLevel, entry);
            } else if (entry.uuid() != null && serverLevel.getEntity(entry.uuid()) instanceof LivingEntity living) {
                releasePin(living, entry);
            }
        }
        entities.clear();
        changed.call();
    }

    private void spawnStored(ServerLevel level, PortEntity entry) {
        var target = center();
        var spawned = EntityType.loadEntityRecursive(entry.data().copy(), level, EntitySpawnReason.LOAD, entity -> {
            entity.snapTo(target.x, target.y, target.z, entity.getYRot(), entity.getXRot());
            return entity;
        });
        if (spawned != null) {
            level.addFreshEntity(spawned);
        }
    }

    private boolean holdsForMachine() {
        return owner instanceof IPortBlockEntity portBlockEntity && portBlockEntity.isInput();
    }

    private boolean keepsAlive() {
        return model.persistent() != null ? model.persistent() : holdsForMachine();
    }

    private void applyPin(LivingEntity entity) {
        if (entity instanceof Mob held && keepsAlive()) {
            held.setPersistenceRequired();
        }
        if (model.immobile()) {
            if (entity instanceof Mob mob) {
                mob.setNoAi(true);
            }
            if (model.singleBlockZone()) {
                var target = center();
                entity.snapTo(target.x, target.y, target.z, entity.getYRot(), entity.getXRot());
            }
            entity.setDeltaMovement(Vec3.ZERO);
        }
        if (model.invulnerable()) {
            entity.setInvulnerable(true);
        }
        if (model.silent()) {
            entity.setSilent(true);
        }
    }

    private void releasePin(LivingEntity entity, PortEntity entry) {
        if (entity instanceof Mob mob && entry.hadAi()) {
            mob.setNoAi(false);
        }
        if (model.invulnerable() && !entry.wasInvulnerable()) {
            entity.setInvulnerable(false);
        }
        if (model.silent() && !entry.wasSilent()) {
            entity.setSilent(false);
        }
    }

    private boolean isPinned(UUID uuid) {
        for (var entry : entities) {
            if (uuid.equals(entry.uuid())) {
                return true;
            }
        }
        return false;
    }

    public double speedBonus() {
        if (model.speedPerEntity() <= 0) {
            return 0;
        }
        return Math.max(0, entities.size() - 1) * model.speedPerEntity();
    }

    public AABB zone() {
        var pos = owner == null ? BlockPos.ZERO : owner.getBlockPos();
        double centerX = pos.getX() + 0.5;
        double centerZ = pos.getZ() + 0.5;
        double bottom = pos.getY() + 1;
        double halfWidth = model.zoneWidth() / 2.0;
        double halfDepth = model.zoneDepth() / 2.0;
        return new AABB(centerX - halfWidth, bottom, centerZ - halfDepth,
                centerX + halfWidth, bottom + model.zoneHeight(), centerZ + halfDepth);
    }

    private Vec3 center() {
        var pos = owner == null ? BlockPos.ZERO : owner.getBlockPos().above();
        return new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
    }

    private Level level() {
        return owner == null ? null : owner.getLevel();
    }

    @Override
    public <T> @Nullable T getCapability(BlockCapability<T, ?> capability) {
        return null;
    }

    @Override
    public <T> boolean hasCapability(BlockCapability<T, ?> capability) {
        return false;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        var list = new ListTag();
        for (var entry : entities) {
            list.add(entry.save());
        }
        tag.put("Entities", list);
        return tag;
    }

    public CompoundTag saveForClient(CompoundTag tag) {
        var list = new ListTag();
        for (var entry : entities) {
            var light = new CompoundTag();
            light.putString("Type", entry.type().toString());
            if (entry.isStored()) {
                light.put("Data", new CompoundTag());
            }
            list.add(light);
        }
        tag.put("Entities", list);
        return tag;
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        entities.clear();
        var list = tag.getListOrEmpty("Entities");
        for (int i = 0; i < list.size(); i++) {
            var entry = PortEntity.load(list.getCompoundOrEmpty(i));
            if (entry != null) {
                entities.add(entry);
            }
        }
    }

    @Override
    public IPortStorageModel getStorageModel() {
        return model;
    }

    @Override
    public UUID getStorageUid() {
        return uid;
    }

    @Override
    public JsonObject debugDump() {
        var dump = new JsonObject();
        dump.addProperty("uid", uid.toString());
        dump.addProperty("capacity", model.capacity());
        dump.addProperty("mode", model.mode().serialize());
        dump.addProperty("invulnerable", model.invulnerable());
        dump.addProperty("immobile", model.immobile());
        dump.addProperty("silent", model.silent());
        dump.addProperty("persistent", keepsAlive());
        dump.addProperty("speedPerEntity", model.speedPerEntity());
        dump.addProperty("zone", model.zoneWidth() + "x" + model.zoneHeight() + "x" + model.zoneDepth());
        dump.addProperty("consume", model.consume());
        var held = new JsonArray();
        for (var entry : entities) {
            var json = new JsonObject();
            json.addProperty("type", entry.type().toString());
            json.addProperty("stored", entry.isStored());
            json.addProperty("uuid", entry.uuid() == null ? null : entry.uuid().toString());
            held.add(json);
        }
        dump.add("entities", held);
        return dump;
    }
}
