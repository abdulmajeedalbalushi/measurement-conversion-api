package com.example.sequence.controller;

import com.example.sequence.model.Sequence;
import com.example.sequence.service.SequenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST controller — the entry point that the browser / Postman talks to.
 *
 *  GET /api/sequence?input=...   -> process one input and return its values
 *  GET /api/sequence/history     -> return all processed sequences so far
 */
@RestController
@RequestMapping("/api/sequence")
public class SequenceController {

    private final SequenceService service;

    public SequenceController(SequenceService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<?> process(@RequestParam("input") String input) {
        try {
            Sequence sequence = service.process(input);
            return ResponseEntity.ok(sequence);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/history")
    public List<Sequence> history() {
        return service.history();
    }
}
