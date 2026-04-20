/**
 * FSROT — File System Recovery & Optimization Tool
 * File: InodeSnapshot.java
 * Simulation only. No real disk operations performed.
 */
package recovery;

import core.Inode;

public class InodeSnapshot {
    private final int inodeId;
    private final String name;
    private final int parentId;
    private final String content;
    private final String checksum;
    private final long size;

    public InodeSnapshot(Inode inode, String content) {
        this.inodeId = inode.getId();
        this.name = inode.getName();
        this.parentId = inode.getParentInodeId();
        this.content = content;
        this.checksum = inode.getChecksum();
        this.size = inode.getSize();
    }

    public int getInodeId() { return inodeId; }
    public String getName() { return name; }
    public int getParentId() { return parentId; }
    public String getContent() { return content; }
    public String getChecksum() { return checksum; }
    public long getSize() { return size; }
}
