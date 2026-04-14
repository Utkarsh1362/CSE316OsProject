#!/bin/bash
# FSROT — Run Script (Linux / macOS)

echo "================================================"
echo "  FSROT - File System Recovery & Optimization"
echo "================================================"

if ! command -v java &> /dev/null; then
    echo "ERROR: Java not found. Install Java 17+ from https://adoptium.net"
    exit 1
fi

JAVA_VER=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d. -f1)
if [ "$JAVA_VER" -lt 17 ] 2>/dev/null; then
    echo "WARNING: Java 17+ recommended. Found version $JAVA_VER"
fi

MODE=${1:-gui}

if [ "$MODE" = "cli" ]; then
    echo "Starting CLI mode..."
    cd src
    javac logger/FSLogger.java metrics/PerformanceMetrics.java \
          core/Block.java core/Inode.java core/Disk.java core/FileSystem.java \
          recovery/InodeSnapshot.java recovery/RecoveryReport.java recovery/RecoveryEngine.java \
          optimization/ScheduleResult.java optimization/DiskScheduler.java \
          optimization/DefragReport.java optimization/Defragmenter.java \
          Main.java
    java -cp . Main
else
    echo "Starting GUI mode..."
    java -jar out/FSROT_GUI.jar
fi
