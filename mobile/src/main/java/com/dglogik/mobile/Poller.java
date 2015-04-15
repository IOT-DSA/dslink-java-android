package com.dglogik.mobile;

import android.support.annotation.NonNull;

import java.util.concurrent.*;

/**
 * Polls devices at set intervals.
 */
public class Poller {

    private static final ScheduledThreadPool STPE = new ScheduledThreadPool(8, new ThreadFactory() {
        @Override
        public Thread newThread(@NonNull Runnable r) {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        }
    });

    private final Runnable pollHandler;
    private ScheduledFuture<?> future;

    /**
     * @param pollHandler Polling callback when the poll timer triggers.
     */
    public Poller(Runnable pollHandler) {
        if (pollHandler == null)
            throw new NullPointerException();
        this.pollHandler = pollHandler;
    }

    /**
     * @return Whether or not polling is active
     */
    public synchronized boolean running() {
        return future != null;
    }

    /**
     * Cancels the polling.
     */
    public synchronized void cancel() {
        if (future != null) {
            future.cancel(true);
            future = null;
        }
    }

    /**
     * Note that it will be instantly ran
     * @param unit The time unit for polling
     * @param interval Polling interval
     * @param delay Whether to do a first poll right away or wait the interval
     *              time.
     */
    public synchronized void poll(TimeUnit unit, int interval, boolean delay) {
        if (future != null) {
            throw new IllegalStateException("Poller already running");
        }

        int doDelay = 0;
        if (delay)
            doDelay = interval;

        future = STPE.scheduleWithFixedDelay(pollHandler, doDelay, interval, unit);
    }

    private static class ScheduledThreadPool extends ScheduledThreadPoolExecutor {

        public ScheduledThreadPool(int corePoolSize, ThreadFactory threadFactory) {
            super(corePoolSize, threadFactory);
        }

        @Override
        protected void afterExecute(Runnable runnable, Throwable t) {
            if (t == null && runnable instanceof Future<?>) {
                try {
                    ((Future<?>) runnable).get(0, TimeUnit.NANOSECONDS);
                } catch (CancellationException | TimeoutException ignored) {
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (ExecutionException e) {
                    throw new RuntimeException(e.getCause());
                }
            } else if (t != null) {
                throw new RuntimeException(t);
            }
        }
    }
}