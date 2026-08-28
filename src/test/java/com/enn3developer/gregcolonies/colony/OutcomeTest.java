package com.enn3developer.gregcolonies.colony;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OutcomeTest {

    @Test
    void okCarriesAMessage() {
        Outcome outcome = Outcome.ok("done");
        assertTrue(outcome.isOk());
        assertTrue(outcome.hasMessage());
        assertEquals("done", outcome.getMessage());
        assertEquals(-1, outcome.getValue());
    }

    @Test
    void okCanCarryAValue() {
        Outcome outcome = Outcome.ok("saved", 3);
        assertTrue(outcome.isOk());
        assertEquals(3, outcome.getValue());
    }

    @Test
    void failIsNotOkButStillSpeaks() {
        Outcome outcome = Outcome.fail("nope");
        assertFalse(outcome.isOk());
        assertTrue(outcome.hasMessage());
        assertEquals("nope", outcome.getMessage());
    }

    @Test
    void silentSaysNothing() {
        Outcome outcome = Outcome.silent();
        assertFalse(outcome.isOk());
        assertFalse(outcome.hasMessage());
        assertEquals("", outcome.getMessage());
    }

    @Test
    void emptyMessageCountsAsNoMessage() {
        assertFalse(
            Outcome.ok("")
                .hasMessage());
    }
}
