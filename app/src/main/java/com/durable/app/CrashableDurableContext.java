package com.durable.app;

import com.durable.engine.DurableContext;
import com.durable.engine.StepStore;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Helper to simulate a crash after N step executions (COMPLETED).
 */
public class CrashableDurableContext extends DurableContext {
    private final int crashAfter;
    private final AtomicInteger completedCount = new AtomicInteger(0);

    public CrashableDurableContext(String workflowId, StepStore store, int crashAfter) {
        super(workflowId, store);
        this.crashAfter = crashAfter;
    }

    @Override
    public <T> T step(String stepId, Supplier<T> fn) {
        T out = super.step(stepId, fn);

        int c = completedCount.incrementAndGet();
        if (crashAfter > 0 && c >= crashAfter) {
            System.out.println(" SIMULATED CRASH after completing " + c + " steps.");
            System.exit(1);
        }
        return out;
    }
}
