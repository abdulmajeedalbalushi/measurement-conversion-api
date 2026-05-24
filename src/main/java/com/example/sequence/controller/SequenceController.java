package com.example.sequence.controller;

import com.example.sequence.model.HistoryRecord;
import com.example.sequence.service.SequenceService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
public class SequenceController {

    private static final Logger log = LoggerFactory.getLogger(SequenceController.class);

    private final SequenceService service;

    public SequenceController(SequenceService service) {
        this.service = service;
    }

    // Process a sequence and store the result in history.
    @GetMapping("/sequence")
    public ResponseEntity<?> processSequence(@RequestParam("input") String input,
                                             HttpServletRequest request) {
        String ip = clientIp(request);
        log.info("API request received: GET /sequence input='{}' from {}", input, ip);
        try {
            HistoryRecord saved = service.process(input, ip);
            log.info("Sequence processed successfully: id={} output={}", saved.getId(), saved.getOutput());
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid input detected on GET /sequence: '{}' — {}", input, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Return all history records.
    @GetMapping("/history")
    public List<HistoryRecord> allHistory() {
        log.info("API request received: GET /history");
        List<HistoryRecord> all = service.getAllHistory();
        log.info("Returning {} history record(s)", all.size());
        return all;
    }

    // Remove every history record.
    @DeleteMapping("/history")
    public ResponseEntity<?> clearHistory() {
        log.info("API request received: DELETE /history");
        service.clearHistory();
        log.info("History cleared");
        return ResponseEntity.ok(Map.of("message", "History cleared successfully"));
    }

    // Return one history record by id, or 404 if it doesn't exist.
    @GetMapping("/history/{id}")
    public ResponseEntity<?> historyById(@PathVariable Long id) {
        log.info("API request received: GET /history/{}", id);
        Optional<HistoryRecord> found = service.getHistoryById(id);
        if (found.isEmpty()) {
            log.warn("History record not found: id={}", id);
            return ResponseEntity.status(404)
                .body(Map.of("error", "History record not found: id=" + id));
        }
        return ResponseEntity.ok(found.get());
    }

    //Update an existing history record.
    @PutMapping("/history/{id}")
    public ResponseEntity<?> updateHistory(@PathVariable Long id,
                                           @RequestBody UpdateHistoryRequest body) {
        log.info("API request received: PUT /history/{} body input='{}' output={} ip='{}'",
            id, body.getInput(), body.getOutput(), body.getSourceIpAddress());
        try {
            Optional<HistoryRecord> updated = service.updateHistory(
                id,
                body.getInput(),
                body.getOutput(),
                body.getSourceIpAddress()
            );
            if (updated.isEmpty()) {
                log.warn("History record not found for update: id={}", id);
                return ResponseEntity.status(404)
                    .body(Map.of("error", "History record not found: id=" + id));
            }
            log.info("History updated: id={}", id);
            return ResponseEntity.ok(updated.get());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid input detected on PUT /history/{}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    //Read the client IP, honouring the X-Forwarded-For header when present.
    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma >= 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return request.getRemoteAddr();
    }

    //Request body for PUT /history/{id}. All fields are optional.
    public static class UpdateHistoryRequest {
        private String input;
        private List<Integer> output;
        private String sourceIpAddress;

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

        public String getSourceIpAddress() {
            return sourceIpAddress;
        }

        public void setSourceIpAddress(String sourceIpAddress) {
            this.sourceIpAddress = sourceIpAddress;
        }
    }
}
