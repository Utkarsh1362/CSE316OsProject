# FSROT — File System Recovery & Optimization Tool

A production-level **Java simulation** of an OS file system with crash recovery, journaling, defragmentation, and disk scheduling — featuring a full **Java Swing dark-theme GUI**.

> ⚠️ This is a **simulation only**. No real disk operations are performed. Everything runs in memory. Your actual file system is never touched.

---

## Screenshots

```
┌─────────────────────────────────────────────────────────────────┐
│  ◈ FSROT    File System Recovery & Optimization Tool    v1.0   │
├──────────────┬──────────────────────────────┬───────────────────┤
│  [USED] [FREE│   DISK BLOCK MAP (128 blocks) │   SYSTEM LOG      │
│  [COR] [RECOV│   ████░░░░░░░░░░░░░░░░░░░░░  │   [INFO] Created  │
│              │                               │   [ERROR] Crash   │
│  FILE TREE   │   [+File][+Dir][Delete][Read] │   [INFO] Restored │
│  📁 documents│   [Backup][Crash][Recover][▶] │                   │
│    📄 report │                               │   JOURNAL         │
│    📄 readme │   DISK SCHEDULER PANEL        │                   │
└──────────────┴──────────────────────────────┴───────────────────┘
```

---

## Features

### Module 1 — File System Core
- 128 simulated disk blocks (512 bytes each)
- Inode-based metadata (name, size, block pointers, checksum, timestamps)
- Tree-based directory hierarchy
- File create / read / update / delete operations
- MD5 checksum integrity verification
- Write-ahead journal for all operations

### Module 2 — Recovery Engine
- Simulated disk crash (30% random block corruption)
- Backup-based file recovery (snapshot & restore)
- Journal replay for operation reconstruction
- Recovery success/failure reporting with logs

### Module 3 — Optimization Engine
- Disk scheduling algorithms: **FCFS**, **SSTF**, **SCAN**
- Side-by-side performance comparison (track movements)
- Defragmentation engine (contiguity analysis + block relocation)
- Before/after fragmentation percentage reporting

### GUI (Java Swing)
- Dark theme desktop application (1200×780)
- Live animated disk block map (colour-coded: blue/red/green/grey)
- Real-time file tree with icons (📁 📄 ↩ ✕)
- 4 live metric cards (Used / Free / Corrupted / Recovered)
- Disk usage and fragmentation progress bars
- 3 scrolling log panels (System Log / Journal / Recovery Log)
- Full demo mode (automated timed simulation)

---

## Quick Start

### Option 1 — Double-click launcher (Windows, no terminal popup)
Double-click `launch.vbs` — opens the GUI silently with no command prompt window.

### Option 2 — Run the prebuilt JAR
```bash
# Windows (no terminal popup)
javaw -jar out\FSROT_GUI.jar

# Mac / Linux
java -jar out/FSROT_GUI.jar
```

### Option 2 — Compile from source
```bash
cd src

javac logger/FSLogger.java \
      metrics/PerformanceMetrics.java \
      core/Block.java core/Inode.java core/Disk.java core/FileSystem.java \
      recovery/InodeSnapshot.java recovery/RecoveryReport.java recovery/RecoveryEngine.java \
      optimization/ScheduleResult.java optimization/DiskScheduler.java \
      optimization/DefragReport.java optimization/Defragmenter.java \
      Main.java FSROT_GUI.java

java -cp . FSROT_GUI
```

### Option 3 — CLI mode (no GUI)
```bash
cd src
# (compile as above, then:)
java -cp . Main
```

---

## Requirements

| Requirement | Version |
|-------------|---------|
| Java JDK    | 17+     |
| OS          | Windows / macOS / Linux |
| RAM         | ~64 MB  |
| Libraries   | None (pure Java + Swing) |

Check your Java version:
```bash
java -version
```
Download Java 17+: https://adoptium.net

---

## Project Structure

```
fsrot-file-system-tool/
├── src/
│   ├── Main.java                        ← CLI entry point
│   ├── FSROT_GUI.java                   ← Swing GUI (single file)
│   ├── core/
│   │   ├── Block.java                   ← 512-byte disk block
│   │   ├── Inode.java                   ← File metadata node
│   │   ├── Disk.java                    ← Block pool + allocation
│   │   └── FileSystem.java              ← FS operations + journal
│   ├── recovery/
│   │   ├── RecoveryEngine.java          ← Crash sim + recovery
│   │   ├── InodeSnapshot.java           ← Backup snapshot object
│   │   └── RecoveryReport.java          ← Recovery results
│   ├── optimization/
│   │   ├── DiskScheduler.java           ← FCFS / SSTF / SCAN
│   │   ├── ScheduleResult.java          ← Scheduling output
│   │   ├── Defragmenter.java            ← Block compaction
│   │   └── DefragReport.java            ← Defrag results
│   ├── logger/
│   │   └── FSLogger.java                ← INFO/WARN/ERROR logger
│   └── metrics/
│       └── PerformanceMetrics.java      ← Timing + stats
└── out/
    └── FSROT_GUI.jar                    ← Prebuilt runnable JAR
```

---

## How to Use the GUI

| Button | Action |
|--------|--------|
| `＋ Create File` | Adds a simulated file, allocates blue blocks on disk map |
| `＋ Create Dir` | Adds a directory to the file tree |
| `✕ Delete Random` | Randomly deletes a file, frees its blocks |
| `⟳ Read File` | Simulates a file read, increments read counter |
| `💾 Take Backup` | Snapshots all files — **do this before crashing** |
| `⚡ Crash Disk` | Corrupts ~30% of blocks, screen flashes red |
| `♻ Recover` | Restores files from backup, turns blocks green |
| `⚙ Compare Schedulers` | Runs FCFS/SSTF/SCAN benchmark |
| `◈ Defragment` | Reduces fragmentation, shows before/after |
| `▶ Full Demo` | Runs entire simulation automatically |

**Disk block map colours:**
- 🟦 Blue = in use
- 🟥 Red = corrupted
- 🟩 Green = recovered
- ⬛ Dark = free

---

## OS Concepts Demonstrated

| Concept | Implementation |
|---------|---------------|
| Inode table | `Inode.java` with block pointer lists |
| Block allocation | `Disk.java` with BitSet free-block map |
| Journaling | Write-ahead log in `FileSystem.java` |
| Disk scheduling | FCFS, SSTF, SCAN in `DiskScheduler.java` |
| Fragmentation | Contiguity check in `Defragmenter.java` |
| Crash recovery | Backup snapshot restore in `RecoveryEngine.java` |
| File integrity | MD5 checksum in `FileSystem.java` |

---

## Sample CLI Output

```
╔══════════════════════════════════════════════════╗
║   FILE SYSTEM RECOVERY AND OPTIMIZATION TOOL     ║
╚══════════════════════════════════════════════════╝

[INFO]  Created directory: documents (inode 1)
[INFO]  Created file: report.txt (128 bytes, inode 5)
[ERROR] DISK CRASH SIMULATED: 18 blocks corrupted
[INFO]  Recovery: restored=2 failed=0

  [FCFS] Total Movement = 640
  [SSTF] Total Movement = 236  ← Best
  [SCAN] Total Movement = 331
```

---

## Author

Built as an Operating Systems course project demonstrating file system internals, crash recovery, and I/O optimization algorithms.

---

## License

MIT License — free to use, modify, and distribute.
