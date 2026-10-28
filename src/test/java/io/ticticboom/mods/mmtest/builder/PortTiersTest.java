package io.ticticboom.mods.mmtest.builder;

import io.ticticboom.mods.mm.builder.PortTiers;
import io.ticticboom.mods.mm.builder.PortTiers.RankedPort;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PortTiersTest {

    @Test
    void inputWinsOverOutputOfTheSameTier() {
        var byRank = PortTiers.byRank(List.of(
                new RankedPort<>(1, false, "s_out"),
                new RankedPort<>(1, true, "s_in"),
                new RankedPort<>(2, true, "m_in")));
        assertEquals(Map.of(1, "s_in", 2, "m_in"), byRank);
    }

    @Test
    void outputOnlyTiersStillGetAPort() {
        var byRank = PortTiers.byRank(List.of(
                new RankedPort<>(1, true, "s_in"),
                new RankedPort<>(2, false, "m_out"),
                new RankedPort<>(3, false, "l_out")));
        assertEquals(Map.of(1, "s_in", 2, "m_out", 3, "l_out"), byRank);
    }

    @Test
    void firstRegisteredPortOfATierWins() {
        var byRank = PortTiers.byRank(List.of(
                new RankedPort<>(2, true, "first"),
                new RankedPort<>(2, true, "second")));
        assertEquals("first", byRank.get(2));
    }
}
