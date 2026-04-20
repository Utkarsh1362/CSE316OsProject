/**
 * FSROT — File System Recovery & Optimization Tool
 * File: ScheduleResult.java
 * Simulation only. No real disk operations performed.
 */
package optimization;

import java.util.List;

public class ScheduleResult {
    private final DiskScheduler.Algorithm algorithm;
    private final List<Integer> order;
    private final int totalMovement;
    private final int initialHead;

    public ScheduleResult(DiskScheduler.Algorithm algorithm, List<Integer> order,
                          int totalMovement, int initialHead) {
        this.algorithm = algorithm;
        this.order = order;
        this.totalMovement = totalMovement;
        this.initialHead = initialHead;
    }

    public DiskScheduler.Algorithm getAlgorithm() { return algorithm; }
    public List<Integer> getOrder() { return order; }
    public int getTotalMovement() { return totalMovement; }
    public int getInitialHead() { return initialHead; }

    public void print() {
        System.out.printf("  [%s] Head=%d | Service Order=%s | Total Movement=%d%n",
                algorithm, initialHead, order, totalMovement);
    }
}
