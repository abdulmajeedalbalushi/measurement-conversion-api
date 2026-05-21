package com.example.measurement.repository;

import com.example.measurement.entity.ConversionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HistoryRepository extends JpaRepository<ConversionHistory, Long> {
    // Inherits findAll, findById, save, deleteById, deleteAll, count, ...
}
