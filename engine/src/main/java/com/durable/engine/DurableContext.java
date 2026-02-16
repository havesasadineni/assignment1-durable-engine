package com.durable.engine;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class DurableContext {
    private final String workflowId;
    private final StepStore store;

    // sequence/logical clock
    private final AtomicLong seq = new AtomicLong(0);

    // Simple safety: serialize DB operations to avoid SQLITE_BUSY in parallel steps
    private final ReentrantLock dbLock = new ReentrantLock(true);

    // zombie step retry threshold
    private final Duration runningStaleAfter = Duration.ofSeconds(20);

    public DurableContext(String workflowId, StepStore store) {
        this.workflowId = workflowId;
        this.store = store;
    }

    public String workflowId() {
        return workflowId;
    }

    private String nextStepKey(String stepId) {
        long n = seq.incrementAndGet();
        return stepId + ":" + n;
    }

    /** Durable step primitive: memoize completed results in SQLite */
    public <T> T step(String stepId, Supplier<T> fn) {
        String stepKey = nextStepKey(stepId);

        try {
            dbLock.lock();

            StepRecord existing = store.get(workflowId, stepKey);
            if (existing != null) {
                if (existing.status() == StepStatus.COMPLETED) {
                    System.out.println("[CACHE HIT] " + stepKey);
                    return JsonUtil.fromJson(existing.outputJson());
                }
                if (existing.status() == StepStatus.RUNNING) {
                    System.out.println("[RETRY RUNNING] " + stepKey + " (previous run crashed)");
                    // Just retry it. (In real systems you'd use heartbeats/leases.)
                }

                // FAILED -> retry by executing again in this run
            }

            store.tryInsertRunning(workflowId, stepKey);
        } catch (SQLException e) {
            throw new RuntimeException("DB error before executing step " + stepKey, e);
        } finally {
            dbLock.unlock();
        }

        // Execute outside the lock (so parallel steps can compute)
        try {
            System.out.println("[EXEC] " + stepKey);
            T out = fn.get();

            String json = JsonUtil.toJson(out);

            dbLock.lock();
            try {
                store.markCompleted(workflowId, stepKey, json);
            } finally {
                dbLock.unlock();
            }
            return out;
        } catch (Exception ex) {
            dbLock.lock();
            try {
                store.markFailed(workflowId, stepKey, ex.getMessage());
            } catch (SQLException e) {
                // if markFailed also fails, throw the original
            } finally {
                dbLock.unlock();
            }
            throw new RuntimeException("Step failed: " + stepKey + " => " + ex.getMessage(), ex);
        }
    }
}
