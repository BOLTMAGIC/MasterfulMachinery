package io.ticticboom.mods.mm.pattern;

/** Why a pattern run did or didn't start; AE2 is told no for everything but STARTED and tries again later. */
public enum PatternStartResult {
    STARTED,
    NOT_FORMED,
    PARALLEL_FULL,
    CONDITIONS,
    MISSING_INPUTS,
    OUTPUTS_BLOCKED
}
