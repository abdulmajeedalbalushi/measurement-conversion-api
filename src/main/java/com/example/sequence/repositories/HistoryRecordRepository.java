package com.example.sequence.repositories;

import com.example.sequence.model.HistoryRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HistoryRecordRepository extends JpaRepository<HistoryRecord, Long> {}
