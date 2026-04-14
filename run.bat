@echo off
REM FSROT — Run Script (Windows)

echo ================================================
echo   FSROT - File System Recovery ^& Optimization
echo ================================================

java -version >nul 2>&1
IF ERRORLEVEL 1 (
    echo ERROR: Java not found. Install Java 17+ from https://adoptium.net
    pause
    exit /b 1
)

IF "%1"=="cli" (
    echo Starting CLI mode...
    cd src
    javac logger\FSLogger.java metrics\PerformanceMetrics.java ^
          core\Block.java core\Inode.java core\Disk.java core\FileSystem.java ^
          recovery\InodeSnapshot.java recovery\RecoveryReport.java recovery\RecoveryEngine.java ^
          optimization\ScheduleResult.java optimization\DiskScheduler.java ^
          optimization\DefragReport.java optimization\Defragmenter.java ^
          Main.java
    java -cp . Main
) ELSE (
    echo Starting GUI mode...
    start javaw -jar out\FSROT_GUI.jar
)

pause
