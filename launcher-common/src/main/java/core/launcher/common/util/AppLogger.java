package core.launcher.common.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public final class AppLogger {

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    private static volatile boolean initialized = false;

    private AppLogger() {
    }

    public static synchronized void configure(String levelName) {
        Level targetLevel = parseLevel(levelName);
        Logger root = Logger.getLogger("");
        root.setLevel(targetLevel);

        for (Handler handler : root.getHandlers()) {
            root.removeHandler(handler);
        }

        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(targetLevel);
        consoleHandler.setFormatter(new SingleLineFormatter());
        root.addHandler(consoleHandler);
        initialized = true;
    }

    public static Logger getLogger(Class<?> clazz) {
        if (!initialized) {
            configure("INFO");
        }
        return Logger.getLogger(clazz.getSimpleName());
    }

    public static Level parseLevel(String levelName) {
        if (levelName == null || levelName.isBlank()) {
            return Level.INFO;
        }
        return switch (levelName.trim().toUpperCase()) {
            case "DEBUG", "FINE", "FINER", "FINEST" -> Level.FINE;
            case "WARN", "WARNING" -> Level.WARNING;
            case "ERROR", "SEVERE" -> Level.SEVERE;
            default -> Level.INFO;
        };
    }

    private static final class SingleLineFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            String timestamp = TIME_FMT.format(Instant.ofEpochMilli(record.getMillis()));
            String level = formatLevel(record.getLevel());
            String threadName = Thread.currentThread().getName();
            String loggerName = record.getLoggerName() != null ? record.getLoggerName() : "root";
            String message = formatMessage(record);

            StringBuilder sb = new StringBuilder(128);
            sb.append('[').append(timestamp).append("] ")
              .append(String.format("[%-5s] ", level))
              .append('[').append(threadName).append("] ")
              .append('[').append(loggerName).append("] ")
              .append(message)
              .append(System.lineSeparator());

            if (record.getThrown() != null) {
                StringWriter sw = new StringWriter();
                try (PrintWriter pw = new PrintWriter(sw)) {
                    record.getThrown().printStackTrace(pw);
                }
                sb.append(sw);
            }
            return sb.toString();
        }

        private String formatLevel(Level level) {
            if (level == Level.SEVERE) {
                return "ERROR";
            }
            if (level == Level.WARNING) {
                return "WARN";
            }
            if (level == Level.FINE || level == Level.FINER || level == Level.FINEST) {
                return "DEBUG";
            }
            return "INFO";
        }
    }
}
