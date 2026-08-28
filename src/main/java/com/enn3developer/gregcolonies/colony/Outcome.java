package com.enn3developer.gregcolonies.colony;

public final class Outcome {

    private static final Outcome SILENT = new Outcome(false, "", -1);

    private final boolean ok;
    private final String message;
    private final int value;

    private Outcome(boolean ok, String message, int value) {
        this.ok = ok;
        this.message = message;
        this.value = value;
    }

    public static Outcome ok(String message) {
        return new Outcome(true, message, -1);
    }

    public static Outcome ok(String message, int value) {
        return new Outcome(true, message, value);
    }

    public static Outcome fail(String message) {
        return new Outcome(false, message, -1);
    }

    public static Outcome silent() {
        return SILENT;
    }

    public boolean isOk() {
        return ok;
    }

    public boolean hasMessage() {
        return !message.isEmpty();
    }

    public String getMessage() {
        return message;
    }

    public int getValue() {
        return value;
    }
}
