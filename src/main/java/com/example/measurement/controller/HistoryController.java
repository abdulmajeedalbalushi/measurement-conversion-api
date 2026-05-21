package com.example.measurement.controller;

import com.example.measurement.dto.HistoryDto;
import com.example.measurement.service.HistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/history")
@Tag(name = "History", description = "CRUD operations on conversion history records")
public class HistoryController {

    private final HistoryService historyService;

    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping
    @Operation(summary = "Get all history records")
    public ResponseEntity<List<HistoryDto>> findAll() {
        return ResponseEntity.ok(historyService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a history record by id")
    public ResponseEntity<HistoryDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(historyService.findById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a history record")
    public ResponseEntity<HistoryDto> update(@PathVariable Long id,
                                             @Valid @RequestBody HistoryDto dto) {
        return ResponseEntity.ok(historyService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a single history record by id")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        historyService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @Operation(summary = "Clear all history records")
    public ResponseEntity<Void> deleteAll() {
        historyService.deleteAll();
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
