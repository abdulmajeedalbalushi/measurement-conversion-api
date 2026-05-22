package com.example.sequence.service;

import com.example.sequence.model.Sequence;
import com.example.sequence.model.SequenceHistory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Business logic for the Sequence API.
 *
 * Responsibilities:
 *  - Validate the raw input.
 *  - Convert the sequence into a list of numbers.
 *  - Save the processed sequence into the in-memory history.
 *
 * Encoding (z-chain):
 *  - A "z-chain" is zero or more 'z' characters (each worth 26) followed by ONE
 *    terminator character (a=1, b=2, ..., z=26, '_'=0, anything else=0).
 *  - The first z-chain in the input is the HEADER and tells us how many values
 *    are in the package. Each value that follows is itself a z-chain.
 *  - After one package is read we start over with a new header. We repeat until
 *    the input is exhausted.
 *
 * Examples:
 *  - "ab"          -> header 'a' = size 1, value 'b' = 2          -> [2]
 *  - "dz_a_aazzaaa" -> [28, 53, 1]
 *  - "za_a_a_a_a_a_a_a_a_a_a_a_a_azaaa" -> [40, 1]
 */
@Service
public class SequenceService {

    private final SequenceHistory history;

    public SequenceService(SequenceHistory history) {
        this.history = history;
    }

    /** Process the raw input: validate, parse, and save into history. */
    public Sequence process(String input) {
        if (!Sequence.isValid(input)) {
            throw new IllegalArgumentException(
                "input must be non-empty and contain only letters and underscores");
        }
        List<Integer> values = parse(input.toLowerCase());
        Sequence sequence = new Sequence(input, values);
        history.save(sequence);
        return sequence;
    }

    /** Return the full processing history. */
    public List<Sequence> history() {
        return history.list();
    }

    // ---------------- parsing helpers ----------------

    private List<Integer> parse(String s) {
        List<Integer> packages = new ArrayList<>();
        int[] cursor = {0};
        int n = s.length();
        while (cursor[0] < n) {
            int packageSize = readZChain(s, cursor);
            int sum = 0;
            for (int v = 0; v < packageSize && cursor[0] < n; v++) {
                sum += readZChain(s, cursor);
            }
            packages.add(sum);
        }
        return packages;
    }

    private int readZChain(String s, int[] cursor) {
        int n = s.length();
        int value = 0;
        while (cursor[0] < n && s.charAt(cursor[0]) == 'z') {
            value += 26;
            cursor[0]++;
        }
        if (cursor[0] < n) {
            char c = s.charAt(cursor[0]);
            if (c >= 'a' && c <= 'z') {
                value += c - 'a' + 1;
            }
            cursor[0]++;
        }
        return value;
    }
}
