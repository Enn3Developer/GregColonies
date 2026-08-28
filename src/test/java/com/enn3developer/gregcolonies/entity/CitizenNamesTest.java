package com.enn3developer.gregcolonies.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

class CitizenNamesTest {

    @Test
    void namesAreCapitalisedAndNonEmpty() {
        Random random = new Random(7L);
        for (int i = 0; i < 200; i++) {
            String name = CitizenNames.generate(random, CitizenGender.MALE);
            assertFalse(name.isEmpty());
            assertTrue(Character.isUpperCase(name.charAt(0)), name + " should start with a capital");
        }
    }

    @Test
    void namesAreLettersOnly() {
        Random random = new Random(11L);
        for (int i = 0; i < 200; i++) {
            String name = CitizenNames.generate(random, CitizenGender.FEMALE);
            assertTrue(name.matches("[A-Za-z]+"), name + " should be letters only");
        }
    }

    @Test
    void namesFitTheBlueprintNameBudget() {
        Random random = new Random(13L);
        for (int i = 0; i < 500; i++) {
            String name = CitizenNames.generate(random, CitizenGender.MALE);
            assertTrue(name.length() <= 24, name + " is too long at " + name.length());
        }
    }

    @Test
    void theSameSeedGivesTheSameName() {
        assertEquals(
            CitizenNames.generate(new Random(42L), CitizenGender.MALE),
            CitizenNames.generate(new Random(42L), CitizenGender.MALE));
    }

    @Test
    void thereIsPlentyOfVariety() {
        Random random = new Random(99L);
        Set<String> names = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            names.add(CitizenNames.generate(random, CitizenGender.FEMALE));
        }
        assertTrue(names.size() > 400, "expected varied names, got " + names.size() + " distinct out of 500");
    }

    @Test
    void femaleNamesMostlyReadBackAsFemale() {
        Random random = new Random(5L);
        int female = 0;
        for (int i = 0; i < 300; i++) {
            if (CitizenNames.genderOf(CitizenNames.generate(random, CitizenGender.FEMALE)) == CitizenGender.FEMALE) {
                female++;
            }
        }
        assertTrue(female > 200, "most female names should read back as female, got " + female + " of 300");
    }

    @Test
    void theOnlyFemaleNamesThatReadBackAsMaleUseTheUnisexEndings() {
        Random random = new Random(8L);
        for (int i = 0; i < 1000; i++) {
            String name = CitizenNames.generate(random, CitizenGender.FEMALE);
            if (CitizenNames.genderOf(name) == CitizenGender.FEMALE) {
                continue;
            }
            assertTrue(
                name.endsWith("is") || name.endsWith("ex") || name.endsWith("ix"),
                name + " reads as male but is not one of the unisex endings");
        }
    }

    @Test
    void maleNamesDoNotEndInAOrE() {
        Random random = new Random(6L);
        for (int i = 0; i < 300; i++) {
            String name = CitizenNames.generate(random, CitizenGender.MALE);
            assertEquals(CitizenGender.MALE, CitizenNames.genderOf(name), name + " should read as male");
        }
    }

    @Test
    void genderIsReadFromTheLastLetter() {
        assertEquals(CitizenGender.FEMALE, CitizenNames.genderOf("Julia"));
        assertEquals(CitizenGender.FEMALE, CitizenNames.genderOf("Chloe"));
        assertEquals(CitizenGender.FEMALE, CitizenNames.genderOf("JULIA"));
        assertEquals(CitizenGender.MALE, CitizenNames.genderOf("Marcus"));
        assertEquals(CitizenGender.MALE, CitizenNames.genderOf("Victor"));
    }

    @Test
    void anEmptyNameHasNoGender() {
        assertNull(CitizenNames.genderOf(null));
        assertNull(CitizenNames.genderOf(""));
    }
}
