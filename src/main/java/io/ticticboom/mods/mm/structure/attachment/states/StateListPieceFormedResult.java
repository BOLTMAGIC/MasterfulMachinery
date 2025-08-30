package io.ticticboom.mods.mm.structure.attachment.states;

public record StateListPieceFormedResult(boolean formed, String stateName) {
    public static final StateListPieceFormedResult NOT_FORMED = new StateListPieceFormedResult(false, "");
}
