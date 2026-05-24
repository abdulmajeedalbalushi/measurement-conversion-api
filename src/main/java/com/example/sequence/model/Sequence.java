package com.example.sequence.model;

import java.util.List;

public class Sequence {

    private final String input;
    private final List<Integer> values;

    public Sequence(String input, List<Integer> values) {
        if (!isValid(input)) {
            throw new IllegalArgumentException(
                "Sequence input must be non-empty and contain only letters and underscores");
        }
        this.input = input;
        this.values = values;
    }

    /** Validate that the format is non-empty letters/underscores only. */
    public static boolean isValid(String input) {
        return input != null && !input.isBlank() && input.matches("[A-Za-z_]+");
    }

    public String getInput() {
        return input;
    }

    public List<Integer> getValues() {
        return values;
    }

    @Override
    public String toString() {
        return input + " => " + values;
    }
}
