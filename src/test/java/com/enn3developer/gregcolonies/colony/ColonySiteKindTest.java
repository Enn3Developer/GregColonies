package com.enn3developer.gregcolonies.colony;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ColonySiteKindTest {

    @Test
    void flagKeyIsCapitalisedKey() {
        assertEquals("hasDropOff", ColonySiteKind.DROP_OFF.getFlagKey());
        assertEquals("hasPickUp", ColonySiteKind.PICK_UP.getFlagKey());
        assertEquals("hasMaterials", ColonySiteKind.MATERIALS.getFlagKey());
    }

    @Test
    void axisKeysAreDistinctPerKindAndAxis() {
        Set<String> keys = new HashSet<>();
        for (ColonySiteKind kind : ColonySiteKind.values()) {
            keys.add(kind.getAxisKey('X'));
            keys.add(kind.getAxisKey('Y'));
            keys.add(kind.getAxisKey('Z'));
            keys.add(kind.getFlagKey());
        }
        assertEquals(ColonySiteKind.values().length * 4, keys.size());
    }

    @Test
    void byIdIsOrdinalAndBoundsChecked() {
        assertEquals(ColonySiteKind.DROP_OFF, ColonySiteKind.byId(0));
        assertEquals(ColonySiteKind.MATERIALS, ColonySiteKind.byId(2));
        assertNull(ColonySiteKind.byId(-1));
        assertNull(ColonySiteKind.byId(ColonySiteKind.values().length));
    }

    @Test
    void labelsAreSet() {
        assertEquals("materials pick-up", ColonySiteKind.MATERIALS.getLabel());
        assertEquals("materials", ColonySiteKind.MATERIALS.getShortLabel());
        assertEquals("materials", ColonySiteKind.MATERIALS.getKey());
    }
}
