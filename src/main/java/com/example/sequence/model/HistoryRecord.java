package com.example.sequence.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "history_record")
public class HistoryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "source_ip_address", length = 64)
    private String sourceIpAddress;

    @Column(name = "input", nullable = false, length = 1024)
    private String input;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "history_record_output",
        joinColumns = @JoinColumn(name = "history_record_id")
    )
    @OrderColumn(name = "position")
    @Column(name = "value")
    private List<Integer> output;

    public HistoryRecord() {
    }

    public HistoryRecord(Long id,
                         LocalDateTime timestamp,
                         String sourceIpAddress,
                         String input,
                         List<Integer> output) {
        this.id = id;
        this.timestamp = timestamp;
        this.sourceIpAddress = sourceIpAddress;
        this.input = input;
        this.output = output;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getSourceIpAddress() {
        return sourceIpAddress;
    }

    public void setSourceIpAddress(String sourceIpAddress) {
        this.sourceIpAddress = sourceIpAddress;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public List<Integer> getOutput() {
        return output;
    }

    public void setOutput(List<Integer> output) {
        this.output = output;
    }
}
