package com.habit.service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service to handle background threads and reminders.
 */
public class ReminderService {
    // Creates a thread pool with a single thread
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public void startReminders() {
        // Run this task every 10 seconds
        scheduler.scheduleAtFixedRate(() -> {
            // This runs in a background thread
            System.out.println("[Reminder] Don't forget to complete your daily habits!");
        }, 0, 10, TimeUnit.SECONDS);
    }

    public void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }
}