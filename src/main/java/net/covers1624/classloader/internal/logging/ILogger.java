package net.covers1624.classloader.internal.logging;

/**
 * Simple abstraction from Log4j, because this lib is minimalistic.
 *
 * Created by covers1624 on 13/11/18.
 */
public interface ILogger {

    //check if the logger is noop.
    //used to focibly print stack traces in some cases.
    boolean isNoop();
    boolean isDebugEnabled();
    boolean isTraceEnabled();

    void info(Object message);
    void info(Object message, Throwable t);
    void info(String message);
    void info(String message, Throwable t);
    void info(String message, Object... params);

    void warn(Object message);
    void warn(Object message, Throwable t);
    void warn(String message);
    void warn(String message, Throwable t);
    void warn(String message, Object... params);

    void error(Object message);
    void error(Object message, Throwable t);
    void error(String message);
    void error(String message, Throwable t);
    void error(String message, Object... params);

    void debug(Object message);
    void debug(Object message, Throwable t);
    void debug(String message);
    void debug(String message, Throwable t);
    void debug(String message, Object... params);

    void trace(Object message);
    void trace(Object message, Throwable t);
    void trace(String message);
    void trace(String message, Throwable t);
    void trace(String message, Object... params);
}
