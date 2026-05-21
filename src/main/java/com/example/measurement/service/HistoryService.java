package com.example.measurement.service;

import com.example.measurement.dto.HistoryDto;

import java.util.List;

/**
 * Application-layer contract for CRUD operations on conversion history records.
 */
public interface HistoryService {

    HistoryDto create(HistoryDto dto);

    List<HistoryDto> findAll();

    HistoryDto findById(Long id);

    HistoryDto update(Long id, HistoryDto dto);

    void deleteById(Long id);

    void deleteAll();
}
