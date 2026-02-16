package com.durable.app;

import com.durable.engine.DurableContext;
import com.durable.engine.StepStore;
import com.durable.examples.onboarding.OnboardingWorkflow;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Main {

    public static void main(String[] args) {
        Map<String, String> a = parseArgs(args);

        String workflowId = a.getOrDefault("workflowId", UUID.randomUUID().toString());
        String employee = a.getOrDefault("employee", "Alex");
        int crashAfter = Integer.parseInt(a.getOrDefault("crashAfter", "0"));

        // SQLite file in project folder
        String jdbcUrl = "jdbc:sqlite:durable.db";

        StepStore store = new StepStore(jdbcUrl);
        DurableContext ctx = new CrashableDurableContext(workflowId, store, crashAfter);

        System.out.println("=== Durable Onboarding ===");
        System.out.println("workflowId = " + workflowId);
        System.out.println("employee   = " + employee);
        System.out.println("crashAfter = " + crashAfter);
        System.out.println("==========================");

        OnboardingWorkflow.run(ctx, employee);

        System.out.println(" WORKFLOW COMPLETED");
    }

    // args like: --workflowId=abc --employee=Chetan --crashAfter=2
    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> m = new HashMap<>();
        for (String s : args) {
            if (s.startsWith("--") && s.contains("=")) {
                String[] p = s.substring(2).split("=", 2);
                m.put(p[0], p[1]);
            }
        }
        return m;
    }
}
