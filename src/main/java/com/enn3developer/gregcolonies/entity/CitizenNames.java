package com.enn3developer.gregcolonies.entity;

import java.util.Random;

public final class CitizenNames {

    private static final String[] ONSETS = { "b", "c", "c", "d", "d", "f", "g", "l", "l", "m", "m", "n", "n", "p", "p",
        "qu", "r", "r", "s", "s", "t", "t", "v", "v", "h", "br", "cl", "cr", "dr", "fl", "fr", "gr", "pl", "pr", "tr",
        "st", "sc" };

    private static final String[] VOWELS = { "a", "a", "a", "e", "e", "e", "i", "i", "i", "o", "o", "u" };

    private static final String[] DIPHTHONGS = { "ae", "au", "oe", "ei" };

    private static final String[] MEDIALS = { "b", "c", "c", "d", "d", "f", "g", "l", "l", "m", "m", "n", "n", "p", "r",
        "r", "s", "s", "t", "t", "v", "v", "qu", "br", "cl", "cr", "dr", "fl", "fr", "gr", "pl", "pr", "tr", "ll", "mm",
        "nn", "rr", "ss", "tt", "ct", "nt", "nd", "mp", "mb", "nc", "rc", "rn", "rt", "rd", "rm", "rv", "ld", "lv",
        "st", "sc", "x" };

    private static final String[] FINAL_CODAS = { "c", "d", "l", "l", "m", "n", "n", "r", "r", "s", "s", "t", "t", "x",
        "ll", "nn", "rr", "ct", "nt", "nd", "rc", "rn", "rt", "st", "mp", "lv", "rv" };

    private static final String[] MALE_SHORT_ENDINGS = { "us", "us", "us", "ius", "is", "or", "ax", "ex", "ix", "o" };

    private static final String[] FEMALE_SHORT_ENDINGS = { "a", "a", "a", "ia", "is", "ex", "ix", "e" };

    private static final String[] MALE_LONG_ENDINGS = { "inus", "anus", "ianus", "icus", "ellus", "illus", "onius",
        "ulus", "imus" };

    private static final String[] FEMALE_LONG_ENDINGS = { "ina", "ana", "iana", "ica", "ella", "illa", "onia", "ula",
        "ima" };

    private static final int SINGLE_SYLLABLE_CHANCE = 55;

    private static final int LONG_ENDING_CHANCE = 45;

    private static final int DIPHTHONG_CHANCE = 12;

    private CitizenNames() {}

    public static String generate(Random random, CitizenGender gender) {
        StringBuilder builder = new StringBuilder();
        boolean single = random.nextInt(100) < SINGLE_SYLLABLE_CHANCE;
        String nucleus = nucleus(random, true);
        builder.append(pick(random, ONSETS))
            .append(nucleus);
        if (!single) {
            builder.append(pick(random, MEDIALS))
                .append(nucleus(random, nucleus.length() == 1));
        }
        builder.append(pick(random, FINAL_CODAS));
        boolean female = gender == CitizenGender.FEMALE;
        builder.append(
            single && random.nextInt(100) < LONG_ENDING_CHANCE
                ? pick(random, female ? FEMALE_LONG_ENDINGS : MALE_LONG_ENDINGS)
                : pick(random, female ? FEMALE_SHORT_ENDINGS : MALE_SHORT_ENDINGS));
        builder.setCharAt(0, Character.toUpperCase(builder.charAt(0)));
        return builder.toString();
    }

    public static CitizenGender genderOf(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        char last = Character.toLowerCase(name.charAt(name.length() - 1));
        return last == 'a' || last == 'e' ? CitizenGender.FEMALE : CitizenGender.MALE;
    }

    private static String nucleus(Random random, boolean diphthongs) {
        return diphthongs && random.nextInt(100) < DIPHTHONG_CHANCE ? pick(random, DIPHTHONGS) : pick(random, VOWELS);
    }

    private static String pick(Random random, String[] values) {
        return values[random.nextInt(values.length)];
    }
}
