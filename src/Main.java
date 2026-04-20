/**
 * FSROT — File System Recovery & Optimization Tool
 * File: Main.java
 * Simulation only. No real disk operations performed.
 */
import core.*;
import logger.FSLogger;
import metrics.PerformanceMetrics;
import optimization.*;
import recovery.*;

import java.util.*;

public class Main {

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║   FILE SYSTEM RECOVERY AND OPTIMIZATION TOOL     ║");
        System.out.println("║   Production Simulation — v1.0                   ║");
        System.out.println("╚══════════════════════════════════════════════════╝\n");

        FSLogger logger = new FSLogger(true);
        PerformanceMetrics metrics = new PerformanceMetrics();
        Disk disk = new Disk(256);
        FileSystem fs = new FileSystem(disk, logger, metrics);

        System.out.println("\n─────────────────────────────────────────────────");
        System.out.println("  MODULE 1: FILE SYSTEM CORE — Build Structure");
        System.out.println("─────────────────────────────────────────────────\n");

        int root = fs.getRootInodeId();
        int docsDir = fs.createDirectory(root, "documents");
        int imagesDir = fs.createDirectory(root, "images");
        int logsDir = fs.createDirectory(root, "logs");
        int srcDir = fs.createDirectory(docsDir, "source_code");

        int file1 = fs.createFile(docsDir, "report.txt",
                "Q4 Financial Report. Revenue increased by 23% compared to last year. " +
                "Total assets: $4.2M. Liabilities: $1.1M. Net profit: $3.1M.");
        int file2 = fs.createFile(docsDir, "readme.md",
                "# Project Documentation\nThis project simulates a complete file system with " +
                "recovery and optimization capabilities. Built using Java OOP principles.");
        int file3 = fs.createFile(imagesDir, "logo.png",
                "PNG_BINARY_DATA:89504E470D0A1A0A0000000D49484452...[simulated binary content]");
        int file4 = fs.createFile(logsDir, "system.log",
                "2024-01-15 08:00:01 [INFO] System booted successfully.\n" +
                "2024-01-15 08:00:05 [INFO] File system mounted.\n" +
                "2024-01-15 08:01:32 [WARN] High memory usage detected.\n" +
                "2024-01-15 08:05:11 [ERROR] Disk write latency spike: 200ms.");
        int file5 = fs.createFile(srcDir, "Main.java",
                "public class Main { public static void main(String[] args) { System.out.println(\"Hello\"); } }");
        int file6 = fs.createFile(srcDir, "FileSystem.java",
                "package core; public class FileSystem { /* File system implementation */ }");

        System.out.println("\n📂 Directory Tree:\n");
        fs.printTree(root, "");

        System.out.println("\n📖 Reading report.txt:");
        System.out.println("  > " + fs.readFile(file1));

        System.out.println("\n✏️  Updating readme.md...");
        fs.updateFile(file2, "# Project Documentation v2\nUpdated content with new sections.");
        System.out.println("  > " + fs.readFile(file2));

        System.out.println("\n🗑️  Deleting logo.png...");
        fs.deleteFile(file3);

        System.out.println("\n📊 Disk Health:");
        disk.getHealthReport().forEach((k, v) -> System.out.printf("  %-18s: %s%n", k, v));

        System.out.println("\n─────────────────────────────────────────────────");
        System.out.println("  MODULE 2: RECOVERY ENGINE");
        System.out.println("─────────────────────────────────────────────────\n");

        RecoveryEngine engine = new RecoveryEngine(fs, logger);

        System.out.println("💾 Taking full backup snapshot...");
        engine.takeBackup();

        System.out.println("\n⚡ Simulating disk crash (30% block corruption)...");
        engine.simulateDiskCrash(0.30);

        System.out.println("\n🗑️  Simulating random file deletions (2 files)...");
        engine.simulateRandomDeletion(2);

        System.out.println("\n📂 File tree AFTER crash:\n");
        fs.printTree(root, "");

        System.out.println("\n🔧 Recovering from backup...");
        RecoveryReport report = engine.recoverFromBackup();
        report.print();

        System.out.println("📋 Recovering from journal replay...");
        RecoveryReport journalReport = engine.recoverFromJournal();
        journalReport.print();

        engine.printRecoveryLog();

        System.out.println("📂 File tree AFTER recovery:\n");
        fs.printTree(root, "");

        System.out.println("\n─────────────────────────────────────────────────");
        System.out.println("  MODULE 3: OPTIMIZATION ENGINE");
        System.out.println("─────────────────────────────────────────────────\n");

        System.out.println("📦 Defragmenting file system...");
        Defragmenter defrag = new Defragmenter(fs, logger, metrics);
        DefragReport defragReport = defrag.defragment();
        defragReport.print();

        System.out.println("⚙️  Disk Scheduling Algorithm Comparison:");
        DiskScheduler scheduler = new DiskScheduler(200, logger, metrics);
        List<Integer> requests = Arrays.asList(98, 183, 37, 122, 14, 124, 65, 67);
        int headStart = 53;
        System.out.println("\n  Requests: " + requests + " | Initial head: " + headStart + "\n");

        ScheduleResult fcfsResult  = scheduler.schedule(DiskScheduler.Algorithm.FCFS,  headStart, requests);
        ScheduleResult sstfResult  = scheduler.schedule(DiskScheduler.Algorithm.SSTF,  headStart, requests);
        ScheduleResult scanResult  = scheduler.schedule(DiskScheduler.Algorithm.SCAN,  headStart, requests);

        fcfsResult.print();
        sstfResult.print();
        scanResult.print();

        String best = Collections.min(
                List.of("FCFS=" + fcfsResult.getTotalMovement(),
                        "SSTF=" + sstfResult.getTotalMovement(),
                        "SCAN=" + scanResult.getTotalMovement()),
                Comparator.comparingInt(s -> Integer.parseInt(s.split("=")[1])));
        System.out.println("\n  ✅ Best Algorithm: " + best.split("=")[0] +
                " (" + best.split("=")[1] + " track movements)\n");

        System.out.println("─────────────────────────────────────────────────");
        System.out.println("  FINAL: PERFORMANCE METRICS SUMMARY");
        System.out.println("─────────────────────────────────────────────────");
        metrics.printReport();

        System.out.println("─────────────────────────────────────────────────");
        System.out.println("  JOURNAL (Last 5 Entries)");
        System.out.println("─────────────────────────────────────────────────");
        List<String> journal = fs.getJournal();
        journal.stream().skip(Math.max(0, journal.size() - 5)).forEach(e -> System.out.println("  " + e));

        System.out.println("\n╔══════════════════════════════════════════════════╗");
        System.out.println("║          SIMULATION COMPLETE                     ║");
        System.out.println("╚══════════════════════════════════════════════════╝");
    }
}
