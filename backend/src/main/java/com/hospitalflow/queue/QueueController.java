package com.hospitalflow.queue;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/queue")
public class QueueController {

    private final QueueEntryRepository queueEntryRepository;

    public QueueController(QueueEntryRepository queueEntryRepository) {
        this.queueEntryRepository = queueEntryRepository;
    }

    // TODO (Week 2+): check-in endpoint, priority scoring logic, WebSocket broadcast on change
    @GetMapping
    public List<QueueEntry> getWaitingQueue() {
        return queueEntryRepository.findByStatusOrderByPriorityScoreDesc(QueueEntry.Status.WAITING);
    }
}
