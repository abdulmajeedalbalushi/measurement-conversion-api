package com.example.measurement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request payload carrying the raw measurement string supplied by the client.
 *
 * <p>Validation constraints enforce the API contract:
 * <ul>
 *   <li>Non-blank and length-bounded to prevent DoS via huge payloads.</li>
 *   <li>Pattern restricts characters to the alphabet plus underscore — invalid
 *       characters are rejected before the parser is even invoked.</li>
 * </ul>
 */
public class ConversionRequest {

    @NotBlank(message = "input must not be blank")
    @Size(max = 10_000, message = "input must be at most 10000 characters")
    @Pattern(regexp = "^[A-Za-z_]+$",
             message = "input may only contain letters (A-Z, a-z) and underscores")
    private String input;

    public ConversionRequest() { }
    public ConversionRequest(String input) { this.input = input; }

    public String getInput() { return input; }
    public void setInput(String input) { this.input = input; }
}
