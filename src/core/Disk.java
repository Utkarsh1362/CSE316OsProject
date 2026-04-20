/**
 * FSROT — File System Recovery & Optimization Tool
 * File: Disk.java
 * Simulation only. No real disk operations performed.
 */
package core;

import java.util.*;

public class Disk {
    private final int totalBlocks;
    private final Block[] blocks;
    private final BitSet freeBlockMap;
    private int fragmentation;

    public Disk(int totalBlocks) {
        this.totalBlocks = totalBlocks;
        this.blocks = new Block[totalBlocks];
        this.freeBlockMap = new BitSet(totalBlocks);
        this.fragmentation = 0;
        for (int i = 0; i < totalBlocks; i++) {
            blocks[i] = new Block(i);
            freeBlockMap.set(i);
        }
    }

    public int allocateBlock() {
        int idx = freeBlockMap.nextSetBit(0);
        if (idx == -1) throw new RuntimeException("Disk full: no free blocks available.");
        freeBlockMap.clear(idx);
        return idx;
    }

    public List<Integer> allocateBlocks(int count) {
        List<Integer> allocated = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            allocated.add(allocateBlock());
        }
        return allocated;
    }

    public void freeBlock(int blockId) {
        blocks[blockId].free();
        freeBlockMap.set(blockId);
    }

    public void writeBlock(int blockId, byte[] data) {
        blocks[blockId].write(data);
    }

    public byte[] readBlock(int blockId) {
        if (blocks[blockId].isCorrupted()) {
            throw new RuntimeException("Block " + blockId + " is corrupted.");
        }
        return blocks[blockId].getData();
    }

    public void corruptBlock(int blockId) {
        blocks[blockId].corrupt();
    }

    public Block getBlock(int blockId) { return blocks[blockId]; }
    public int getTotalBlocks() { return totalBlocks; }
    public int getFreeBlocks() { return freeBlockMap.cardinality(); }
    public int getUsedBlocks() { return totalBlocks - getFreeBlocks(); }
    public int getFragmentation() { return fragmentation; }
    public void setFragmentation(int f) { this.fragmentation = f; }

    public List<Integer> getAllocatedBlockIds() {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < totalBlocks; i++) {
            if (!freeBlockMap.get(i)) result.add(i);
        }
        return result;
    }

    public double getUsagePercent() {
        return (double) getUsedBlocks() / totalBlocks * 100;
    }

    public Map<String, Object> getHealthReport() {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("totalBlocks", totalBlocks);
        report.put("usedBlocks", getUsedBlocks());
        report.put("freeBlocks", getFreeBlocks());
        report.put("usagePercent", String.format("%.1f%%", getUsagePercent()));
        report.put("fragmentation", fragmentation + "%");
        long corruptCount = Arrays.stream(blocks).filter(Block::isCorrupted).count();
        report.put("corruptedBlocks", corruptCount);
        return report;
    }
}
