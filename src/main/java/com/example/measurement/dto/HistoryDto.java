package com.example.measurement.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

/**
 * API-facing view of a {@link com.example.measurement.entity.ConversionHistory} row.
 *
 * <p>Used both as a response body (list/get) and as a request body for {@code PUT}.
 * Decoupling this from the JPA entity prevents accidental field exposure and lets
 * the persistence schema evolve without breaking the API contract.</p>
 */
public class HistoryDto {

    private Long id;
    private Instant timestamp;
    private String sourceIpAddress;

    @NotBlank(message = "input must not be blank")
    private String input;

    @NotBlank(message = "output must not be blank")
    private String output;

    public HistoryDto() { }

    public HistoryDto(Long id, Instant timestamp, String sourceIpAddress, String input, String output) {
        this.id = id;
        this.timestamp = timestamp;
        this.sourceIpAddress = sourceIpAddress;
        this.input = input;
        this.output = output;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getSourceIpAddress() { return sourceIpAddress; }
    public void setSourceIpAddress(String sourceIpAddress) { this.sourceIpAddress = sourceIpAddress; }

    public String getInput() { return input; }
    public void setInput(String input) { this.input = input; }

    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }
}
