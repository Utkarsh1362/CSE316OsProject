/**
 * FSROT — File System Recovery & Optimization Tool
 * File: DiskScheduler.java
 * Simulation only. No real disk operations performed.
 */
package optimization;

import logger.FSLogger;
import metrics.PerformanceMetrics;

import java.util.*;

public class DiskScheduler {
    public enum Algorithm { FCFS, SSTF, SCAN }

    private final int totalTracks;
    private final FSLogger logger;
    private final PerformanceMetrics metrics;

    public DiskScheduler(int totalTracks, FSLogger logger, PerformanceMetrics metrics) {
        this.totalTracks = totalTracks;
        this.logger = logger;
        this.metrics = metrics;
    }

    public ScheduleResult schedule(Algorithm algo, int headPosition, List<Integer> requests) {
        return switch (algo) {
            case FCFS -> fcfs(headPosition, new ArrayList<>(requests));
            case SSTF -> sstf(headPosition, new ArrayList<>(requests));
            case SCAN -> scan(headPosition, new ArrayList<>(requests));
        };
    }

    private ScheduleResult fcfs(int head, List<Integer> requests) {
        List<Integer> order = new ArrayList<>(requests);
        int totalMovement = 0;
        int current = head;
        for (int r : order) {
            totalMovement += Math.abs(current - r);
            current = r;
        }
        logger.info("FCFS: total head movement = " + totalMovement);
        metrics.recordSchedulerRun("FCFS", totalMovement);
        return new ScheduleResult(Algorithm.FCFS, order, totalMovement, head);
    }

    private ScheduleResult sstf(int head, List<Integer> requests) {
        List<Integer> remaining = new ArrayList<>(requests);
        List<Integer> order = new ArrayList<>();
        int totalMovement = 0;
        int current = head;
        while (!remaining.isEmpty()) {
            final int cur = current;
            int closest = remaining.stream()
                    .min(Comparator.comparingInt(r -> Math.abs(r - cur)))
                    .orElseThrow();
            totalMovement += Math.abs(current - closest);
            current = closest;
            order.add(closest);
            remaining.remove((Integer) closest);
        }
        logger.info("SSTF: total head movement = " + totalMovement);
        metrics.recordSchedulerRun("SSTF", totalMovement);
        return new ScheduleResult(Algorithm.SSTF, order, totalMovement, head);
    }

    private ScheduleResult scan(int head, List<Integer> requests) {
        List<Integer> sorted = new ArrayList<>(requests);
        Collections.sort(sorted);
        List<Integer> order = new ArrayList<>();
        int totalMovement = 0;
        int current = head;

        List<Integer> right = new ArrayList<>();
        List<Integer> left = new ArrayList<>();
        for (int r : sorted) {
            if (r >= current) right.add(r);
            else left.add(r);
        }

        for (int r : right) {
            totalMovement += Math.abs(current - r);
            current = r;
            order.add(r);
        }
        if (current < totalTracks - 1) {
            totalMovement += (totalTracks - 1 - current);
            current = totalTracks - 1;
        }
        Collections.reverse(left);
        for (int r : left) {
            totalMovement += Math.abs(current - r);
            current = r;
            order.add(r);
        }
        logger.info("SCAN: total head movement = " + totalMovement);
        metrics.recordSchedulerRun("SCAN", totalMovement);
        return new ScheduleResult(Algorithm.SCAN, order, totalMovement, head);
    }
}
