/**
 * FSROT — File System Recovery & Optimization Tool
 * File: FSLogger.java
 * Simulation only. No real disk operations performed.
 */
package logger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class FSLogger {
    public enum Level { INFO, WARN, ERROR }

    private final List<LogEntry> entries;
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private boolean consoleEnabled;

    public FSLogger(boolean consoleEnabled) {
        this.entries = new ArrayList<>();
        this.consoleEnabled = consoleEnabled;
    }

    public void info(String message)  { log(Level.INFO,  message); }
    public void warn(String message)  { log(Level.WARN,  message); }
    public void error(String message) { log(Level.ERROR, message); }

    private void log(Level level, String message) {
        LogEntry entry = new LogEntry(level, message, LocalDateTime.now());
        entries.add(entry);
        if (consoleEnabled) {
            String prefix = switch (level) {
                case INFO  -> "\u001B[32m[INFO] \u001B[0m";
                case WARN  -> "\u001B[33m[WARN] \u001B[0m";
                case ERROR -> "\u001B[31m[ERROR]\u001B[0m";
            };
            System.out.printf("%s %s %s%n", entry.timestamp().format(fmt), prefix, message);
        }
    }

    public List<LogEntry> getEntries() { return entries; }

    public void printAll() {
        System.out.println("\n===== FULL LOG =====");
        for (LogEntry e : entries) {
            System.out.printf("[%s] [%s] %s%n", e.timestamp().format(fmt), e.level(), e.message());
        }
        System.out.println("====================\n");
    }

    public record LogEntry(Level level, String message, LocalDateTime timestamp) {}
}
