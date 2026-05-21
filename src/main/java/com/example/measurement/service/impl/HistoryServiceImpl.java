package com.example.measurement.service.impl;

import com.example.measurement.dto.HistoryDto;
import com.example.measurement.entity.ConversionHistory;
import com.example.measurement.exception.ResourceNotFoundException;
import com.example.measurement.repository.HistoryRepository;
import com.example.measurement.service.HistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class HistoryServiceImpl implements HistoryService {

    private static final Logger log = LoggerFactory.getLogger(HistoryServiceImpl.class);

    private final HistoryRepository repository;

    public HistoryServiceImpl(HistoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public HistoryDto create(HistoryDto dto) {
        ConversionHistory entity = toEntity(dto);
        ConversionHistory saved = repository.save(entity);
        log.debug("Persisted history id={} ip={}", saved.getId(), saved.getSourceIpAddress());
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistoryDto> findAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public HistoryDto findById(Long id) {
        return repository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "History record not found: id=" + id));
    }

    @Override
    public HistoryDto update(Long id, HistoryDto dto) {
        ConversionHistory entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "History record not found: id=" + id));
        entity.setInput(dto.getInput());
        entity.setOutput(dto.getOutput());
        if (dto.getSourceIpAddress() != null) {
            entity.setSourceIpAddress(dto.getSourceIpAddress());
        }
        // timestamp and id are intentionally immutable post-creation
        ConversionHistory updated = repository.save(entity);
        log.info("Updated history id={}", id);
        return toDto(updated);
    }

    @Override
    public void deleteById(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("History record not found: id=" + id);
        }
        repository.deleteById(id);
        log.info("Deleted history id={}", id);
    }

    @Override
    public void deleteAll() {
        long count = repository.count();
        repository.deleteAll();
        log.warn("Cleared {} history record(s)", count);
    }

    // ---------- mapping helpers ----------

    private HistoryDto toDto(ConversionHistory e) {
        return new HistoryDto(
                e.getId(),
                e.getTimestamp(),
                e.getSourceIpAddress(),
                e.getInput(),
                e.getOutput());
    }

    private ConversionHistory toEntity(HistoryDto d) {
        ConversionHistory e = new ConversionHistory();
        e.setId(d.getId());
        e.setTimestamp(d.getTimestamp());
        e.setSourceIpAddress(d.getSourceIpAddress());
        e.setInput(d.getInput());
        e.setOutput(d.getOutput());
        return e;
    }
}
