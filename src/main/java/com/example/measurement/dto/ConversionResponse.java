package com.example.measurement.dto;

import java.util.Collections;
import java.util.List;

/**
 * Response payload returned by {@code GET /api/conversion}.
 *
 * <p>Encapsulates the parsed result so that the wire contract is stable even if
 * additional metadata is added later (e.g. per-package character ranges).</p>
 */
public class ConversionResponse {

    private final String input;
    private final List<Integer> packages;
    private final int packageCount;

    public ConversionResponse(String input, List<Integer> packages) {
        this.input = input;
        this.packages = Collections.unmodifiableList(packages);
        this.packageCount = packages.size();
    }

    public String getInput() { return input; }
    public List<Integer> getPackages() { return packages; }
    public int getPackageCount() { return packageCount; }
}
