/**
 * FSROT — File System Recovery & Optimization Tool
 * File: FileSystem.java
 * Simulation only. No real disk operations performed.
 */
package core;

import logger.FSLogger;
import metrics.PerformanceMetrics;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

public class FileSystem {
    private final Disk disk;
    private final Map<Integer, Inode> inodeTable;
    private final Map<Integer, List<Integer>> directoryChildren;
    private int nextInodeId;
    private final int rootInodeId;
    private final FSLogger logger;
    private final PerformanceMetrics metrics;
    private final List<String> journal;

    public FileSystem(Disk disk, FSLogger logger, PerformanceMetrics metrics) {
        this.disk = disk;
        this.inodeTable = new HashMap<>();
        this.directoryChildren = new HashMap<>();
        this.nextInodeId = 0;
        this.logger = logger;
        this.metrics = metrics;
        this.journal = new ArrayList<>();
        this.rootInodeId = createRootDirectory();
    }

    private int createRootDirectory() {
        Inode root = new Inode(nextInodeId++, "/", true, -1);
        inodeTable.put(root.getId(), root);
        directoryChildren.put(root.getId(), new ArrayList<>());
        return root.getId();
    }

    public int getRootInodeId() { return rootInodeId; }
    public Disk getDisk() { return disk; }
    public Map<Integer, Inode> getInodeTable() { return inodeTable; }
    public Map<Integer, List<Integer>> getDirectoryChildren() { return directoryChildren; }
    public List<String> getJournal() { return journal; }

    public int createDirectory(int parentId, String name) {
        validateParent(parentId);
        int id = nextInodeId++;
        Inode dir = new Inode(id, name, true, parentId);
        inodeTable.put(id, dir);
        directoryChildren.put(id, new ArrayList<>());
        directoryChildren.get(parentId).add(id);
        journal.add("CREATE_DIR | id=" + id + " | name=" + name + " | parent=" + parentId);
        logger.info("Created directory: " + name + " (inode " + id + ")");
        return id;
    }

    public int createFile(int parentId, String name, String content) {
        validateParent(parentId);
        long start = System.nanoTime();
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        int blocksNeeded = (int) Math.ceil((double) bytes.length / Block.BLOCK_SIZE);
        if (blocksNeeded == 0) blocksNeeded = 1;

        List<Integer> blockIds = disk.allocateBlocks(blocksNeeded);
        for (int i = 0; i < blockIds.size(); i++) {
            int from = i * Block.BLOCK_SIZE;
            int to = Math.min(from + Block.BLOCK_SIZE, bytes.length);
            byte[] chunk = (from < bytes.length)
                    ? Arrays.copyOfRange(bytes, from, to)
                    : new byte[0];
            disk.writeBlock(blockIds.get(i), chunk);
        }

        int id = nextInodeId++;
        Inode file = new Inode(id, name, false, parentId);
        file.setSize(bytes.length);
        blockIds.forEach(file::addBlockPointer);
        file.setChecksum(computeChecksum(content));
        inodeTable.put(id, file);
        directoryChildren.get(parentId).add(id);

        long elapsed = System.nanoTime() - start;
        metrics.recordWrite(elapsed);
        journal.add("CREATE_FILE | id=" + id + " | name=" + name + " | size=" + bytes.length + " | blocks=" + blockIds);
        logger.info("Created file: " + name + " (" + bytes.length + " bytes, inode " + id + ")");
        return id;
    }

    public String readFile(int inodeId) {
        Inode inode = getInode(inodeId);
        if (inode.isDirectory()) throw new RuntimeException("Cannot read a directory.");
        long start = System.nanoTime();
        StringBuilder sb = new StringBuilder();
        for (int blockId : inode.getBlockPointers()) {
            byte[] data = disk.readBlock(blockId);
            sb.append(new String(data, StandardCharsets.UTF_8).replace("\0", ""));
        }
        long elapsed = System.nanoTime() - start;
        metrics.recordRead(elapsed);
        logger.info("Read file: " + inode.getName() + " (inode " + inodeId + ")");
        return sb.toString();
    }

    public void updateFile(int inodeId, String newContent) {
        Inode inode = getInode(inodeId);
        if (inode.isDirectory()) throw new RuntimeException("Cannot write to a directory.");
        inode.getBlockPointers().forEach(disk::freeBlock);
        inode.clearBlockPointers();

        byte[] bytes = newContent.getBytes(StandardCharsets.UTF_8);
        int blocksNeeded = Math.max(1, (int) Math.ceil((double) bytes.length / Block.BLOCK_SIZE));
        List<Integer> blockIds = disk.allocateBlocks(blocksNeeded);
        for (int i = 0; i < blockIds.size(); i++) {
            int from = i * Block.BLOCK_SIZE;
            int to = Math.min(from + Block.BLOCK_SIZE, bytes.length);
            byte[] chunk = (from < bytes.length) ? Arrays.copyOfRange(bytes, from, to) : new byte[0];
            disk.writeBlock(blockIds.get(i), chunk);
        }
        blockIds.forEach(inode::addBlockPointer);
        inode.setSize(bytes.length);
        inode.setChecksum(computeChecksum(newContent));
        journal.add("UPDATE_FILE | id=" + inodeId + " | newSize=" + bytes.length);
        logger.info("Updated file: " + inode.getName());
    }

    public void deleteFile(int inodeId) {
        Inode inode = getInode(inodeId);
        inode.getBlockPointers().forEach(disk::freeBlock);
        inode.setDeleted(true);
        List<Integer> siblings = directoryChildren.get(inode.getParentInodeId());
        if (siblings != null) siblings.remove((Integer) inodeId);
        journal.add("DELETE | id=" + inodeId + " | name=" + inode.getName());
        logger.warn("Deleted: " + inode.getName() + " (inode " + inodeId + ")");
    }

    public void printTree(int inodeId, String indent) {
        Inode inode = inodeTable.get(inodeId);
        if (inode == null || inode.isDeleted()) return;
        String icon = inode.isDirectory() ? "📁" : "📄";
        System.out.printf("%s%s %s%n", indent, icon, inode.getName()
                + (inode.isDirectory() ? "/" : " [" + inode.getSize() + "B]"));
        List<Integer> children = directoryChildren.getOrDefault(inodeId, Collections.emptyList());
        for (int childId : children) {
            printTree(childId, indent + "   ");
        }
    }

    public Inode getInode(int id) {
        Inode inode = inodeTable.get(id);
        if (inode == null) throw new RuntimeException("Inode " + id + " not found.");
        return inode;
    }

    private void validateParent(int parentId) {
        Inode parent = inodeTable.get(parentId);
        if (parent == null || !parent.isDirectory())
            throw new RuntimeException("Invalid parent directory inode: " + parentId);
    }

    private String computeChecksum(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            return "checksum-error";
        }
    }
}
