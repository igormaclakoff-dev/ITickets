package com.auracode.itickets.util;

public final class TextSanitizer {
    private TextSanitizer() {
    }

    public static String clean(String input) {
        if (input == null) {
            return "";
        }
        String stripped = input.replaceAll("[\\p{Cntrl}&&[^\t]]", "");
        return stripped.replace("\\", "\\\\").replace("<", "\\<");
    }
}
