package io.ticticboom.mods.mm.structure.attachment;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.piece.StructurePieceSetupMetadata;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StructureAttachments {
    public final Map<ResourceLocation, StructureAttachment> attachments = new HashMap<>();

    public static StructureAttachments parse(JsonObject json) {
        var attachments = MMStructureAttachmentRegistry.parseStructureAttachments(json);
        return new StructureAttachments(attachments);
    }

    public StructureAttachments(List<StructureAttachment> attachments) {
        this.attachments.putAll(attachments.stream().collect(Collectors.toMap(StructureAttachment::getId, a -> a)));
    }

    public boolean has(ResourceLocation id) {
        return attachments.containsKey(id);
    }

    public <T extends StructureAttachment> T get(ResourceLocation id, Class<T> clazz) {
        return (T)attachments.get(id);
    }

    public void validate(StructurePieceSetupMetadata meta) {
        for (var entry : attachments.entrySet()) {
            entry.getValue().validate(meta);
        }
    }
}
