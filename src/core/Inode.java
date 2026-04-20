/**
 * FSROT — File System Recovery & Optimization Tool
 * File: Inode.java
 * Simulation only. No real disk operations performed.
 */
package core;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Inode {
    private final int id;
    private String name;
    private long size;
    private boolean isDirectory;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
    private List<Integer> blockPointers;
    private int parentInodeId;
    private boolean deleted;
    private String checksum;

    public Inode(int id, String name, boolean isDirectory, int parentInodeId) {
        this.id = id;
        this.name = name;
        this.isDirectory = isDirectory;
        this.parentInodeId = parentInodeId;
        this.blockPointers = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.modifiedAt = LocalDateTime.now();
        this.size = 0;
        this.deleted = false;
        this.checksum = "";
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public long getSize() { return size; }
    public boolean isDirectory() { return isDirectory; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getModifiedAt() { return modifiedAt; }
    public List<Integer> getBlockPointers() { return blockPointers; }
    public int getParentInodeId() { return parentInodeId; }
    public boolean isDeleted() { return deleted; }
    public String getChecksum() { return checksum; }

    public void setName(String name) { this.name = name; }
    public void setSize(long size) { this.size = size; this.modifiedAt = LocalDateTime.now(); }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public void setChecksum(String checksum) { this.checksum = checksum; }
    public void setModifiedAt(LocalDateTime t) { this.modifiedAt = t; }

    public void addBlockPointer(int blockId) { blockPointers.add(blockId); }
    public void clearBlockPointers() { blockPointers.clear(); }

    @Override
    public String toString() {
        return String.format("Inode[%d | name=%s | dir=%b | size=%d | blocks=%s]",
                id, name, isDirectory, size, blockPointers);
    }
}
