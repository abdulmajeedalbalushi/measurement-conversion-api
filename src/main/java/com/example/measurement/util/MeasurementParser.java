package com.example.measurement.util;

import com.example.measurement.exception.InvalidInputException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * State-based decoder for the package-measurement encoding.
 *
 * <h2>Encoding</h2>
 * <p>The parser alternates between two modes:</p>
 * <ul>
 *   <li><b>Package mode</b> — reads a header that determines the package size.
 *       A letter maps alphabetically (a=1 .. z=26). <b>z</b> is special: it
 *       activates <i>continuation</i>, so the parser keeps consuming z's,
 *       accumulating 26 per character, until it reaches the first non-z
 *       character — which terminates the header and is included in the total.
 *       Examples: {@code zd = 26 + 4 = 30}, {@code zza = 26 + 26 + 1 = 53}.</li>
 *
 *   <li><b>Value mode</b> — reads exactly N characters, converts each one to
 *       its value (a=1 .. z=26, '_'=0) and sums them to produce the package
 *       result.</li>
 * </ul>
 *
 * <p>After a package is produced the parser returns to package mode and repeats
 * until the input is exhausted.</p>
 *
 * <p>This class is stateless and thread-safe; Spring manages it as a singleton.</p>
 */
@Component
public class MeasurementParser {

    /**
     * Parses the supplied measurement string and returns the list of per-package totals.
     *
     * @param input non-null measurement string consisting of letters and underscores
     * @return list of summed values, one per package, in input order
     * @throws InvalidInputException if the input is malformed
     */
    public List<Integer> parse(String input) {
        if (input == null) {
            throw new InvalidInputException("Input must not be null");
        }
        if (input.isEmpty()) {
            return List.of();
        }

        String normalised = input.toLowerCase();
        int n = normalised.length();
        List<Integer> packages = new ArrayList<>();

        int i = 0;
        while (i < n) {
            // ----- Package mode: decode the header -----
            int packageSize = 0;
            char c = normalised.charAt(i);

            // Header characters must be letters only (no underscore in the header).
            while (c == 'z') {
                packageSize += 26;
                i++;
                if (i >= n) {
                    throw new InvalidInputException(
                            "Unexpected end of input while decoding package header (dangling 'z' chain)");
                }
                c = normalised.charAt(i);
            }

            packageSize += letterValueForHeader(c);
            i++; // consume the terminating header character

            // ----- Value mode: consume exactly `packageSize` characters -----
            if (i + packageSize > n) {
                throw new InvalidInputException(
                        "Package declares size " + packageSize
                                + " but only " + (n - i) + " character(s) remain");
            }
            int sum = 0;
            for (int j = 0; j < packageSize; j++) {
                sum += valueCharValue(normalised.charAt(i + j));
            }
            i += packageSize;
            packages.add(sum);
        }
        return packages;
    }

    /**
     * Maps a header character to its numeric value. The header alphabet is a-z only.
     */
    private int letterValueForHeader(char c) {
        if (c >= 'a' && c <= 'z') {
            return c - 'a' + 1;
        }
        throw new InvalidInputException(
                "Invalid header character '" + c + "' (only letters a-z are allowed in package headers)");
    }

    /**
     * Maps a value-mode character to its numeric value. Letters map alphabetically,
     * underscore is zero, anything else is invalid.
     */
    private int valueCharValue(char c) {
        if (c == '_') return 0;
        if (c >= 'a' && c <= 'z') return c - 'a' + 1;
        throw new InvalidInputException(
                "Invalid value character '" + c + "' (allowed: a-z and '_')");
    }
}
