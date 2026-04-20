/**
 * FSROT — File System Recovery & Optimization Tool
 * File: PerformanceMetrics.java
 * Simulation only. No real disk operations performed.
 */
package metrics;

import java.util.*;

public class PerformanceMetrics {
    private final List<Long> readTimes = new ArrayList<>();
    private final List<Long> writeTimes = new ArrayList<>();
    private final Map<String, Integer> schedulerMovements = new LinkedHashMap<>();
    private long defragDuration = 0;
    private int defragBefore = 0;
    private int defragAfter = 0;

    public void recordRead(long nanoseconds)  { readTimes.add(nanoseconds); }
    public void recordWrite(long nanoseconds) { writeTimes.add(nanoseconds); }

    public void recordSchedulerRun(String algo, int movement) {
        schedulerMovements.put(algo, movement);
    }

    public void recordDefrag(long duration, int before, int after) {
        this.defragDuration = duration;
        this.defragBefore = before;
        this.defragAfter = after;
    }

    public double avgReadMs() {
        return readTimes.isEmpty() ? 0 :
                readTimes.stream().mapToLong(Long::longValue).average().orElse(0) / 1_000_000.0;
    }

    public double avgWriteMs() {
        return writeTimes.isEmpty() ? 0 :
                writeTimes.stream().mapToLong(Long::longValue).average().orElse(0) / 1_000_000.0;
    }

    public void printReport() {
        System.out.println("\n====== PERFORMANCE METRICS ======");
        System.out.printf("  Read Operations  : %d (avg %.4f ms)%n", readTimes.size(), avgReadMs());
        System.out.printf("  Write Operations : %d (avg %.4f ms)%n", writeTimes.size(), avgWriteMs());
        System.out.println("  Disk Scheduler Comparison:");
        schedulerMovements.forEach((algo, mv) ->
                System.out.printf("    %-6s : %d track movements%n", algo, mv));
        if (defragDuration > 0) {
            System.out.printf("  Defrag: %d%% → %d%% (%.2f ms)%n",
                    defragBefore, defragAfter, defragDuration / 1_000_000.0);
        }
        System.out.println("=================================\n");
    }
}
