/**
 * FSROT — File System Recovery & Optimization Tool
 * File: RecoveryReport.java
 * Simulation only. No real disk operations performed.
 */
package recovery;

import java.util.List;

public class RecoveryReport {
    private final int recovered;
    private final int failed;
    private final List<String> details;

    public RecoveryReport(int recovered, int failed, List<String> details) {
        this.recovered = recovered;
        this.failed = failed;
        this.details = details;
    }

    public int getRecovered() { return recovered; }
    public int getFailed() { return failed; }
    public List<String> getDetails() { return details; }

    public double getSuccessRate() {
        int total = recovered + failed;
        return total == 0 ? 0.0 : (double) recovered / total * 100;
    }

    public void print() {
        System.out.println("\n===== RECOVERY REPORT =====");
        System.out.printf("  Recovered : %d%n", recovered);
        System.out.printf("  Failed    : %d%n", failed);
        System.out.printf("  Success   : %.1f%%%n", getSuccessRate());
        System.out.println("  Details:");
        details.forEach(d -> System.out.println("    - " + d));
        System.out.println("===========================\n");
    }
}
