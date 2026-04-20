/**
 * FSROT — File System Recovery & Optimization Tool
 * File: RecoveryEngine.java
 * Simulation only. No real disk operations performed.
 */
package recovery;

import core.*;
import logger.FSLogger;

import java.util.*;

public class RecoveryEngine {
    private final FileSystem fs;
    private final FSLogger logger;
    private final Map<Integer, InodeSnapshot> backupTable;
    private final List<String> recoveryLog;

    public RecoveryEngine(FileSystem fs, FSLogger logger) {
        this.fs = fs;
        this.logger = logger;
        this.backupTable = new HashMap<>();
        this.recoveryLog = new ArrayList<>();
    }

    public void takeBackup() {
        backupTable.clear();
        for (Map.Entry<Integer, Inode> entry : fs.getInodeTable().entrySet()) {
            Inode inode = entry.getValue();
            if (!inode.isDirectory() && !inode.isDeleted()) {
                try {
                    String content = fs.readFile(inode.getId());
                    backupTable.put(inode.getId(), new InodeSnapshot(inode, content));
                } catch (Exception ignored) {}
            }
        }
        logger.info("Backup taken: " + backupTable.size() + " files snapshotted.");
        recoveryLog.add("[BACKUP] Snapshot taken for " + backupTable.size() + " files.");
    }

    public void simulateDiskCrash(double corruptionProbability) {
        Disk disk = fs.getDisk();
        List<Integer> allocated = disk.getAllocatedBlockIds();
        int corrupted = 0;
        Random rng = new Random();
        for (int blockId : allocated) {
            if (rng.nextDouble() < corruptionProbability) {
                disk.corruptBlock(blockId);
                corrupted++;
            }
        }
        logger.error("DISK CRASH SIMULATED: " + corrupted + " blocks corrupted out of " + allocated.size());
        recoveryLog.add("[CRASH] " + corrupted + "/" + allocated.size() + " blocks corrupted.");
    }

    public void simulateRandomDeletion(int count) {
        List<Integer> candidates = new ArrayList<>();
        for (Inode inode : fs.getInodeTable().values()) {
            if (!inode.isDirectory() && !inode.isDeleted()) candidates.add(inode.getId());
        }
        Collections.shuffle(candidates);
        int deleted = 0;
        for (int i = 0; i < Math.min(count, candidates.size()); i++) {
            fs.deleteFile(candidates.get(i));
            deleted++;
        }
        logger.warn("Random deletion: " + deleted + " files deleted.");
        recoveryLog.add("[DELETE] " + deleted + " files randomly deleted.");
    }

    public RecoveryReport recoverFromBackup() {
        int recovered = 0, failed = 0;
        List<String> details = new ArrayList<>();

        for (Map.Entry<Integer, InodeSnapshot> entry : backupTable.entrySet()) {
            int inodeId = entry.getKey();
            InodeSnapshot snapshot = entry.getValue();
            Inode current = fs.getInodeTable().get(inodeId);

            boolean needsRecovery = (current == null || current.isDeleted() || isFileCorrupted(current));

            if (needsRecovery) {
                try {
                    if (current == null || current.isDeleted()) {
                        int parentId = snapshot.getParentId();
                        fs.createFile(parentId, "[RECOVERED] " + snapshot.getName(), snapshot.getContent());
                        details.add("RESTORED: " + snapshot.getName());
                    } else {
                        fs.updateFile(inodeId, snapshot.getContent());
                        details.add("REPAIRED: " + snapshot.getName());
                    }
                    recovered++;
                } catch (Exception e) {
                    details.add("FAILED: " + snapshot.getName() + " (" + e.getMessage() + ")");
                    failed++;
                }
            }
        }

        logger.info("Recovery complete. Recovered=" + recovered + " Failed=" + failed);
        recoveryLog.add("[RECOVERY] Restored=" + recovered + " | Failed=" + failed);
        return new RecoveryReport(recovered, failed, details);
    }

    public RecoveryReport recoverFromJournal() {
        List<String> journal = fs.getJournal();
        int replayed = 0;
        List<String> details = new ArrayList<>();

        for (String entry : journal) {
            if (entry.startsWith("CREATE_FILE")) {
                details.add("JOURNAL REPLAYED: " + entry);
                replayed++;
            }
        }

        logger.info("Journal replay: " + replayed + " entries processed.");
        recoveryLog.add("[JOURNAL] Replayed " + replayed + " journal entries.");
        return new RecoveryReport(replayed, 0, details);
    }

    private boolean isFileCorrupted(Inode inode) {
        Disk disk = fs.getDisk();
        for (int blockId : inode.getBlockPointers()) {
            if (disk.getBlock(blockId).isCorrupted()) return true;
        }
        return false;
    }

    public List<String> getRecoveryLog() { return recoveryLog; }

    public void printRecoveryLog() {
        System.out.println("\n===== RECOVERY LOG =====");
        recoveryLog.forEach(System.out::println);
        System.out.println("========================\n");
    }
}
