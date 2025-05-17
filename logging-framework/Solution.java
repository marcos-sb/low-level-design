/*
Log Levels: Support for multiple log levels (INFO, DEBUG, ERROR, etc.).
Multiple Appenders: Ability to log to different destinations (console, file, etc.).
Custom Formatting: Support for custom log message formatting.
Configuration: Ability to configure loggers and appenders.
Thread Safety: Should be thread-safe for concurrent logging.
Extensibility: Easy to add new log levels, appenders, or formatters.
*/

import java.util.Collections;
import java.util.EnumMap;

public class Logger {

    private final EnumMap<LogLevel, List<Appender>> appendersByLevel;

    public Logger(Configuration configuration) {
        this.appendersByLevel =
            Collections.unmodifiableMap(configuration.getAppendersByLevel());
    }

    private void log(LogMessage message) {
        final var logLevel = message.getLogLevel();
        final var appenders = appendersByLevel.get(logLevel);
        for (var appender : appenders) {
            appender.append(message);
        }
    }

    public void trace(String message) {
        log(new LogMessage(LogLevel.TRACE, message));
    }

    public void debug()

    public void info()

    public void warn()

    public void error()
}

public interface LogAppender {
    void append(LogMessage logMessage);
}

abstract class Appender implements LogAppender {

    private final LogLevel logLevel;
    private final LogFormatter formatter;
    private final String name;

    public Appender(LogLevel logLevel, LogFormater formatter, String name) {
        this.logLevel = logLevel;
        this.formatter = formatter;
        this.name = name;
    }
}

public class ConsoleAppender extends Appender {

    public ConsoleAppender(LogLevel logLevel, LogFormatter formatter, String name) {
        super(logLevel, formatter, name);
    }

    @Override
    public void append(LogMessage logMessage) {
        System.out.println(getName() + " " + formatter.format(logMessage));
    }
}

public enum LogLevel {
    TRACE,
    DEBUG,
    INFO,
    WARN,
    ERROR,
}

public class LogMessage {

    private final LogLevel logLevel;
    private final String message;
    private final long timestamp;
    private final String threadName;

    public LogMessage(
        LogLevel logLevel,
        String message,
        long timestamp,
        String threadName
    ) {
        this.logLevel = logLevel;
        this.message = message;
        this.timestamp = timestamp;
        this.threadName = threadName;
    }

    public LogMessage(LogLevel logLevel, String message) {
        this(logLevel, message, timestamp, threadName);
    }
}

public class Configuration {
    private final Map<LogLevel, List<Appender>> appendersByLevel;

    public Configuration(Appender appenders...) {
        this.appendersByLevel = new EnumMap<>(LogLevel.class);
        for (var appender : appenders) {
            appendersByLevel
                .computeIfAbsent(appender.getLogLevel, k -> new ArrayList<>())
                .add(appender);
        }
    }
}

public interface LogFormatter {
    public String format(LogMessage message);
}

public class SimpleLogFormatter implements LogFormatter {
    @Override
    public String format(LogMessage message) {
        return String.format(
            "[%s] [%s] %s: %s",
            message.getTimestamp(),
            message.getThreadName(),
            message.getLogLevel(),
            message.getMessage()
        );
    }
}
