package io.ticticboom.mods.mm.structure.attachment;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StructureAttachments {
    public final Map<ResourceLocation, MMStructureAttachment> attachments = new HashMap<>();

    public static StructureAttachments parse(JsonObject json) {
        var attachments = MMStructureAttachmentRegistry.parseStructureAttachments(json);
        return new StructureAttachments(attachments);
    }

    public StructureAttachments(List<MMStructureAttachment> attachments) {
        this.attachments.putAll(attachments.stream().collect(Collectors.toMap(MMStructureAttachment::getId, a -> a)));
    }

    public boolean has(ResourceLocation id) {
        return attachments.containsKey(id);
    }

    public MMStructureAttachment get(ResourceLocation id) {
        return attachments.get(id);
    }
}
