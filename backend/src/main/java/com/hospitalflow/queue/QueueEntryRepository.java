package com.hospitalflow.queue;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QueueEntryRepository extends JpaRepository<QueueEntry, Long> {
    List<QueueEntry> findByStatusOrderByPriorityScoreDesc(QueueEntry.Status status);
}
