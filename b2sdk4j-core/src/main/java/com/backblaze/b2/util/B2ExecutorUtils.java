/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class B2ExecutorUtils {

    // This tries to cleanly terminate an executorService.
    // from http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ExecutorService.html
    public static void shutdownAndAwaitTermination(ExecutorService executor, int gracefulSecs, int otherSecs) {
        // log.info("stop accepting new tasks in executorService");
        executor.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            // log.info("awaiting termination of tasks executorService");
            if (!executor.awaitTermination(gracefulSecs, TimeUnit.SECONDS)) {
                // log.info("cancelling currently executing tasks in executorService");
                executor.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                // log.info("awaiting termination of cancelled tasks in executorService");
                if (!executor.awaitTermination(otherSecs, TimeUnit.SECONDS)) {
                    // log.warn("executorService did not terminate.");
                }
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            // log.info("interrupted while trying to shutdown.");
            executor.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

    /**
     * @param nameFormat the format for the thread names, should contain a single %d.
     * @return a new ThreadFactory that takes a string format to help name threads
     */
    public static ThreadFactory createThreadFactory(String nameFormat) {
        final AtomicInteger count = new AtomicInteger(0);

        return (runnable) -> {
            final Thread thread = Executors.defaultThreadFactory().newThread(runnable);
            thread.setName(String.format(nameFormat, count.getAndIncrement()));
            return thread;
        };
    }
}
