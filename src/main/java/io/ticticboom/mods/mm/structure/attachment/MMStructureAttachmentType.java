package io.ticticboom.mods.mm.structure.attachment;


import com.google.gson.JsonObject;

public abstract class MMStructureAttachmentType {
    public abstract boolean identify(JsonObject json);

    public abstract StructureAttachment parse(JsonObject json);
}
