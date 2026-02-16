package com.durable.examples.onboarding;

import com.durable.engine.DurableContext;

import java.util.concurrent.CompletableFuture;

public class OnboardingWorkflow {

    public static void run(DurableContext ctx, String employeeName) {
        // Step 1: Create employee record (sequential)
        String employeeId = ctx.step("CreateEmployeeRecord", () -> {
            sleep(800);
            return "EMP-" + employeeName.toUpperCase().replace(" ", "") + "-001";
        });

        // Step 2 & 3: Parallel steps
        CompletableFuture<String> laptop = CompletableFuture.supplyAsync(() ->
                ctx.step("ProvisionLaptop", () -> {
                    sleep(1200);
                    return "LAPTOP-ASSIGNED-" + employeeId;
                })
        );

        CompletableFuture<String> access = CompletableFuture.supplyAsync(() ->
                ctx.step("ProvisionAccess", () -> {
                    sleep(1500);
                    return "ACCESS-GRANTED-" + employeeId;
                })
        );

        String laptopResult = laptop.join();
        String accessResult = access.join();

        // Step 4: Send welcome email (sequential)
        ctx.step("SendWelcomeEmail", () -> {
            sleep(600);
            return "WELCOME-EMAIL-SENT to " + employeeName +
                    " [" + laptopResult + ", " + accessResult + "]";
        });
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
