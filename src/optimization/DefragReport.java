/**
 * FSROT — File System Recovery & Optimization Tool
 * File: DefragReport.java
 * Simulation only. No real disk operations performed.
 */
package optimization;

public class DefragReport {
    private final int filesMoved;
    private final int blocksMoved;
    private final int beforeFragmentation;
    private final int afterFragmentation;
    private final long durationNs;

    public DefragReport(int filesMoved, int blocksMoved, int beforeFrag, int afterFrag, long durationNs) {
        this.filesMoved = filesMoved;
        this.blocksMoved = blocksMoved;
        this.beforeFragmentation = beforeFrag;
        this.afterFragmentation = afterFrag;
        this.durationNs = durationNs;
    }

    public int getFilesMoved() { return filesMoved; }
    public int getBlocksMoved() { return blocksMoved; }
    public int getBeforeFragmentation() { return beforeFragmentation; }
    public int getAfterFragmentation() { return afterFragmentation; }
    public long getDurationNs() { return durationNs; }

    public void print() {
        System.out.println("\n===== DEFRAG REPORT =====");
        System.out.printf("  Files defragmented : %d%n", filesMoved);
        System.out.printf("  Blocks relocated   : %d%n", blocksMoved);
        System.out.printf("  Fragmentation      : %d%% → %d%%%n", beforeFragmentation, afterFragmentation);
        System.out.printf("  Duration           : %.2f ms%n", durationNs / 1_000_000.0);
        System.out.println("=========================\n");
    }
}
