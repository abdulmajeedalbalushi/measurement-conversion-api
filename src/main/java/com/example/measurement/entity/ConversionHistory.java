package com.example.measurement.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;

/**
 * Database row representing a single conversion request/response cycle.
 *
 * <p>Entity is intentionally separated from the API-facing {@code HistoryDto} so that
 * persistence concerns (lazy loading, columns, JPA lifecycle) do not leak into the
 * REST contract.</p>
 */
@Entity
@Table(name = "CONVERSION_HISTORY")
public class ConversionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "TS", nullable = false, updatable = false)
    private Instant timestamp;

    @Column(name = "SOURCE_IP", length = 64)
    private String sourceIpAddress;

    @Lob
    @Column(name = "INPUT_VAL", nullable = false)
    private String input;

    @Lob
    @Column(name = "OUTPUT_VAL", nullable = false)
    private String output;

    public ConversionHistory() {
        // JPA
    }

    public ConversionHistory(Instant timestamp, String sourceIpAddress, String input, String output) {
        this.timestamp = timestamp;
        this.sourceIpAddress = sourceIpAddress;
        this.input = input;
        this.output = output;
    }

    @PrePersist
    void onCreate() {
        if (this.timestamp == null) {
            this.timestamp = Instant.now();
        }
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConversionHistory that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
