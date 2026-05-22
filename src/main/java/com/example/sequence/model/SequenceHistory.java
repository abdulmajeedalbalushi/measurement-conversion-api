package com.example.sequence.model;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * In-memory store for all processed Sequences.
 *
 * Beginner notes:
 *  - @Component tells Spring to create exactly one shared instance (a "bean").
 *  - Because there is one shared instance, every request sees the same list.
 *  - Data lives only in RAM — restarting the app clears the history.
 */
@Component
public class SequenceHistory {

    private final List<Sequence> sequences = new ArrayList<>();

    /** Save the current sequence into history. */
    public void save(Sequence sequence) {
        sequences.add(sequence);
    }

    /** Return the full history list (read-only view). */
    public List<Sequence> list() {
        return Collections.unmodifiableList(sequences);
    }
}
