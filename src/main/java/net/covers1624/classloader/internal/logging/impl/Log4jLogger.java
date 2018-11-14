package net.covers1624.classloader.internal.logging.impl;

import net.covers1624.classloader.internal.logging.ILogger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A logger that pipes to Log4j.
 *
 * Created by covers1624 on 13/11/18.
 */
public class Log4jLogger implements ILogger {

    private final Logger logger;

    Log4jLogger(String name) {
        this.logger = LogManager.getLogger(name);
    }

    //@formatter:off
    @Override public boolean isNoop() { return false; }
    @Override public boolean isDebugEnabled() { return logger.isDebugEnabled(); }
    @Override public boolean isTraceEnabled() { return logger.isTraceEnabled(); }
    @Override public void info(Object message) { logger.info(message); }
    @Override public void info(Object message, Throwable t) { logger.info(message, t); }
    @Override public void info(String message) { logger.info(message); }
    @Override public void info(String message, Throwable t) { logger.info(message, t); }
    @Override public void info(String message, Object... params) { logger.info(message, params); }
    @Override public void warn(Object message) { logger.warn(message); }
    @Override public void warn(Object message, Throwable t) { logger.warn(message, t); }
    @Override public void warn(String message) { logger.warn(message); }
    @Override public void warn(String message, Throwable t) { logger.warn(message, t); }
    @Override public void warn(String message, Object... params) { logger.warn(message, params); }
    @Override public void error(Object message) { logger.error(message); }
    @Override public void error(Object message, Throwable t) { logger.error(message, t); }
    @Override public void error(String message) { logger.error(message); }
    @Override public void error(String message, Throwable t) { logger.error(message, t); }
    @Override public void error(String message, Object... params) { logger.error(message, params); }
    @Override public void debug(Object message) { logger.debug(message); }
    @Override public void debug(Object message, Throwable t) { logger.debug(message, t); }
    @Override public void debug(String message) { logger.debug(message); }
    @Override public void debug(String message, Throwable t) { logger.debug(message, t); }
    @Override public void debug(String message, Object... params) { logger.debug(message, params); }
    @Override public void trace(Object message) { logger.trace(message); }
    @Override public void trace(Object message, Throwable t) { logger.trace(message, t); }
    @Override public void trace(String message) { logger.trace(message); }
    @Override public void trace(String message, Throwable t) { logger.trace(message, t); }
    @Override public void trace(String message, Object... params) { logger.trace(message, params); }
    //@formatter:on
}
