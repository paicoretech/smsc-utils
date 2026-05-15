package com.paicbd.smsc.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Generated
public class Watcher {
    private final String name;
    private final AtomicInteger counter;
    private final int time;
    private final ScheduledExecutorService executorService;

    public Watcher(String name, AtomicInteger counter, int time) {
        log.warn("Watcher Name: {} was created successfully", name);
        this.name = name;
        this.counter = counter;
        this.time = time;
        this.executorService = Executors.newSingleThreadScheduledExecutor(
                Thread.ofPlatform().name("Watcher-" + name, 0).factory());
        this.startWatching();
        Runtime.getRuntime().addShutdownHook(new Thread(this::stopWatching));
    }

    public void startWatching() {
        Runnable task = () -> {
            if (counter.get() > 0) {
                log.warn("Watcher Name: {} | Current Value: {}", name, counter.getAndSet(0));
            }
        };

        executorService.scheduleAtFixedRate(task, time, time, TimeUnit.SECONDS);
    }

    public void stopWatching() {
        executorService.shutdown();
    }
}
