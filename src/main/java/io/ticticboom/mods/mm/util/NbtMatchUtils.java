package io.ticticboom.mods.mm.util;

import com.google.gson.*;
import net.minecraft.nbt.*;

public class NbtMatchUtils {

    public static CompoundTag parseFromJson(JsonElement el) {
        if (el == null || el.isJsonNull()) return null;
        if (el.isJsonObject()) {
            return parseCompound(el.getAsJsonObject());
        }
        if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()) {
            // try SNBT parsing
            try {
                Tag parsed = TagParser.parseTag(el.getAsString());
                if (parsed instanceof CompoundTag ct) return ct;
                // if parsed is not a compound, wrap it into one under key "value"
                CompoundTag wrapper = new CompoundTag();
                wrapper.put("value", parsed);
                return wrapper;
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse SNBT: " + e.getMessage(), e);
            }
        }
        throw new RuntimeException("Unsupported NBT json format");
    }

    private static CompoundTag parseCompound(JsonObject obj) {
        CompoundTag ct = new CompoundTag();
        for (var entry : obj.entrySet()) {
            String key = entry.getKey();
            JsonElement v = entry.getValue();
            if (v.isJsonObject()) {
                ct.put(key, parseCompound(v.getAsJsonObject()));
            } else if (v.isJsonArray()) {
                var arr = v.getAsJsonArray();
                ListTag lt = new ListTag();
                for (JsonElement e : arr) {
                    if (e.isJsonObject()) {
                        lt.add(parseCompound(e.getAsJsonObject()));
                    } else if (e.isJsonPrimitive()) {
                        var prim = e.getAsJsonPrimitive();
                        if (prim.isNumber()) {
                            if (prim.getAsString().contains(".")) {
                                lt.add(DoubleTag.valueOf(prim.getAsDouble()));
                            } else {
                                lt.add(LongTag.valueOf(prim.getAsLong()));
                            }
                        } else if (prim.isBoolean()) {
                            lt.add(ByteTag.valueOf((byte) (prim.getAsBoolean() ? 1 : 0)));
                        } else {
                            lt.add(StringTag.valueOf(prim.getAsString()));
                        }
                    } else {
                        lt.add(StringTag.valueOf(e.toString()));
                    }
                }
                ct.put(key, lt);
            } else if (v.isJsonPrimitive()) {
                var prim = v.getAsJsonPrimitive();
                if (prim.isNumber()) {
                    if (prim.getAsString().contains(".")) {
                        ct.putDouble(key, prim.getAsDouble());
                    } else {
                        try {
                            ct.putInt(key, prim.getAsInt());
                        } catch (Exception ex) {
                            ct.putLong(key, prim.getAsLong());
                        }
                    }
                } else if (prim.isBoolean()) {
                    ct.putBoolean(key, prim.getAsBoolean());
                } else if (prim.isString()) {
                    ct.putString(key, prim.getAsString());
                }
            } else {
                ct.putString(key, v.toString());
            }
        }
        return ct;
    }

    public static JsonElement toJson(CompoundTag tag) {
        if (tag == null) return JsonNull.INSTANCE;
        JsonObject obj = new JsonObject();
        for (String key : tag.getAllKeys()) {
            Tag t = tag.get(key);
            obj.add(key, tagToJson(t));
        }
        return obj;
    }

    private static JsonElement tagToJson(Tag t) {
        if (t instanceof CompoundTag ct) {
            return toJson(ct);
        }
        if (t instanceof ListTag lt) {
            JsonArray arr = new JsonArray();
            for (Tag e : lt) arr.add(tagToJson(e));
            return arr;
        }
        if (t instanceof StringTag st) {
            return new JsonPrimitive(st.getAsString());
        }
        if (t instanceof ByteTag bt) {
            return new JsonPrimitive(bt.getAsByte());
        }
        if (t instanceof ShortTag sht) {
            return new JsonPrimitive(sht.getAsShort());
        }
        if (t instanceof IntTag it) {
            return new JsonPrimitive(it.getAsInt());
        }
        if (t instanceof LongTag ltg) {
            return new JsonPrimitive(ltg.getAsLong());
        }
        if (t instanceof FloatTag ft) {
            return new JsonPrimitive(ft.getAsFloat());
        }
        if (t instanceof DoubleTag dt) {
            return new JsonPrimitive(dt.getAsDouble());
        }
        // fallback
        return new JsonPrimitive(t.getAsString());
    }

    public static boolean matchesWeak(CompoundTag subset, CompoundTag superset) {
        if (subset == null) return true;
        if (superset == null) return false;
        for (String key : subset.getAllKeys()) {
            if (!superset.contains(key)) return false;
            Tag a = subset.get(key);
            Tag b = superset.get(key);
            if (!tagMatchesWeak(a, b)) return false;
        }
        return true;
    }

    private static boolean tagMatchesWeak(Tag a, Tag b) {
        if (a == null) return true;
        if (b == null) return false;
        if (a.getId() != b.getId()) {
            // allow numeric coercion
            if (isNumberTag(a) && isNumberTag(b)) {
                return numberTagEquals(a, b);
            }
            return false;
        }
        if (a instanceof CompoundTag ac && b instanceof CompoundTag bc) {
            return matchesWeak(ac, bc);
        }
        if (a instanceof ListTag al && b instanceof ListTag bl) {
            // subset: every element in al must match some element in bl
            for (Tag elemA : al) {
                boolean matched = false;
                for (Tag elemB : bl) {
                    if (tagMatchesWeak(elemA, elemB)) { matched = true; break; }
                }
                if (!matched) return false;
            }
            return true;
        }
        if (isNumberTag(a) && isNumberTag(b)) {
            return numberTagEquals(a, b);
        }
        return a.equals(b);
    }

    private static boolean isNumberTag(Tag t) {
        return t instanceof NumericTag;
    }

    private static boolean numberTagEquals(Tag a, Tag b) {
        double va = ((NumericTag) a).getAsNumber().doubleValue();
        double vb = ((NumericTag) b).getAsNumber().doubleValue();
        return Double.compare(va, vb) == 0;
    }
}
