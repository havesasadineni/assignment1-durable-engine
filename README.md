# Assignment 1 - Native Durable Execution Engine (Java)

## What this is
A minimal Durable Execution Engine. Wrap side-effecting code inside `ctx.step(stepId, fn)` and the engine:
- persists step status + output in SQLite
- memoizes completed steps (no re-execution after crash)
- supports loops/conditionals via a logical clock (sequence counter)
- supports parallel steps (CompletableFuture) with thread-safe persistence

## How durability works
Each workflow run has a `workflowId`.
Each `step()` call generates a unique `step_key = stepId:sequenceNumber`.

Before executing a step, we check SQLite:
- If `COMPLETED` -> return cached output (skip execution)
- Else -> write `RUNNING` -> execute -> write `COMPLETED` with JSON output
- If execution throws -> write `FAILED` and propagate error

## SQLite schema
Table: `steps(workflow_id, step_key, status, output_json, error_msg, started_at, updated_at)`  
Primary key: `(workflow_id, step_key)`

PRAGMAs used:
- `journal_mode=WAL`
- `busy_timeout=5000`

## Sequence / logical clock (loops + conditionals)
This engine keeps a monotonic sequence counter (logical clock) per workflow run.
Each call to `step()` increments the sequence, ensuring uniqueness even when step IDs repeat inside loops/conditionals.

## Parallel execution thread-safety
Parallel steps (CompletableFuture) can write to SQLite concurrently.
To avoid `SQLITE_BUSY` errors, this implementation uses a single global DB lock around DB reads/writes.
(Alternative design: a single-writer queue executor.)

## Zombie step handling (RUNNING at crash time)
Zombie step: crash occurs after a side effect executes but before the step is marked `COMPLETED`.

This engine records `RUNNING` first. On resume:
- if a step is found as `RUNNING`, it is **retried** (treat as at-least-once execution).

Note: exactly-once side effects generally require **idempotent operations** or **external dedupe keys**.
## Screenshots

![Run Output](Screenshot%20(147).png)

![Run Output](Screenshot%20(148).png)


## Build
Requires Java 17+ and Maven.

```bash
mvn clean install
