package com.example.employee_management_system.service;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class SchedulingService {

    private static final Logger log = LoggerFactory.getLogger(SchedulingService.class);
    private final AtomicInteger counter = new AtomicInteger(0);

    // Runs at a fixed rate; configurable via property demo.scheduled.fixedRate (ms)
    @Scheduled(fixedRateString = "${demo.scheduled.fixedRate:5000}")
    public void scheduledTask() {
        int c = counter.incrementAndGet();
        log.info("Scheduled task executed count={}", c);
    }

    public int getCount() {
        return counter.get();
    }
}

