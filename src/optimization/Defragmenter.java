/**
 * FSROT — File System Recovery & Optimization Tool
 * File: Defragmenter.java
 * Simulation only. No real disk operations performed.
 */
package optimization;

import core.*;
import logger.FSLogger;
import metrics.PerformanceMetrics;

import java.util.*;

public class Defragmenter {
    private final FileSystem fs;
    private final FSLogger logger;
    private final PerformanceMetrics metrics;

    public Defragmenter(FileSystem fs, FSLogger logger, PerformanceMetrics metrics) {
        this.fs = fs;
        this.logger = logger;
        this.metrics = metrics;
    }

    public DefragReport defragment() {
        long start = System.nanoTime();
        int filesMoved = 0;
        int blocksMoved = 0;
        int beforeFragmentation = computeFragmentation();

        Disk disk = fs.getDisk();
        Map<Integer, Inode> inodeTable = fs.getInodeTable();

        for (Inode inode : inodeTable.values()) {
            if (inode.isDirectory() || inode.isDeleted() || inode.getBlockPointers().size() <= 1) continue;

            List<Integer> oldBlocks = new ArrayList<>(inode.getBlockPointers());
            boolean isFragmented = !isContiguous(oldBlocks);

            if (isFragmented) {
                List<byte[]> dataChunks = new ArrayList<>();
                for (int blockId : oldBlocks) {
                    dataChunks.add(disk.getBlock(blockId).getData().clone());
                }
                oldBlocks.forEach(disk::freeBlock);
                inode.clearBlockPointers();
                List<Integer> newBlocks = disk.allocateBlocks(dataChunks.size());
                for (int i = 0; i < newBlocks.size(); i++) {
                    disk.writeBlock(newBlocks.get(i), dataChunks.get(i));
                    inode.addBlockPointer(newBlocks.get(i));
                }
                blocksMoved += oldBlocks.size();
                filesMoved++;
            }
        }

        int afterFragmentation = computeFragmentation();
        disk.setFragmentation(afterFragmentation);
        long elapsed = System.nanoTime() - start;
        metrics.recordDefrag(elapsed, beforeFragmentation, afterFragmentation);
        logger.info("Defragmentation complete. Files moved=" + filesMoved + ", Blocks moved=" + blocksMoved);
        return new DefragReport(filesMoved, blocksMoved, beforeFragmentation, afterFragmentation, elapsed);
    }

    private boolean isContiguous(List<Integer> blocks) {
        for (int i = 1; i < blocks.size(); i++) {
            if (blocks.get(i) != blocks.get(i - 1) + 1) return false;
        }
        return true;
    }

    private int computeFragmentation() {
        int fragmented = 0, total = 0;
        for (Inode inode : fs.getInodeTable().values()) {
            if (inode.isDirectory() || inode.isDeleted() || inode.getBlockPointers().size() <= 1) continue;
            total++;
            if (!isContiguous(inode.getBlockPointers())) fragmented++;
        }
        return total == 0 ? 0 : (int) ((double) fragmented / total * 100);
    }
}
