/**
 * FSROT — File System Recovery & Optimization Tool
 * File: Block.java
 * Simulation only. No real disk operations performed.
 */
package core;

public class Block {
    public static final int BLOCK_SIZE = 512;

    private final int id;
    private byte[] data;
    private boolean corrupted;
    private boolean allocated;

    public Block(int id) {
        this.id = id;
        this.data = new byte[BLOCK_SIZE];
        this.corrupted = false;
        this.allocated = false;
    }

    public int getId() { return id; }
    public byte[] getData() { return data; }
    public boolean isCorrupted() { return corrupted; }
    public boolean isAllocated() { return allocated; }

    public void write(byte[] content) {
        this.data = new byte[BLOCK_SIZE];
        int len = Math.min(content.length, BLOCK_SIZE);
        System.arraycopy(content, 0, this.data, 0, len);
        this.allocated = true;
        this.corrupted = false;
    }

    public void corrupt() {
        this.corrupted = true;
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (Math.random() * 256 - 128);
        }
    }

    public void free() {
        this.data = new byte[BLOCK_SIZE];
        this.allocated = false;
        this.corrupted = false;
    }

    @Override
    public String toString() {
        return String.format("Block[%d | allocated=%b | corrupted=%b]", id, allocated, corrupted);
    }
}
