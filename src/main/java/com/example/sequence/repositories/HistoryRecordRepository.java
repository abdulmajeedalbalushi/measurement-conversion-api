package com.example.sequence.repositories;

import com.example.sequence.model.HistoryRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link HistoryRecord}.
 *
 * Beginner notes:
 *  - We declare an interface; Spring Data writes the implementation at runtime.
 *  - Extending JpaRepository gives us save, findAll, findById, deleteAll,
 *    count, existsById, etc. for free.
 *  - The two type parameters are <Entity, PrimaryKeyType> — here, our entity
 *    is HistoryRecord and its primary key is Long.
 */
@Repository
public interface HistoryRecordRepository extends JpaRepository<HistoryRecord, Long> {
}
