package net.covers1624.classloader.internal.logging.impl;

import net.covers1624.classloader.api.logging.ILogger;

/**
 * A noop logger.
 * Does literally nothing.
 * Created by covers1624 on 13/11/18.
 */
public class NoopLogger implements ILogger {


    //@formatter:off
    @Override public boolean isNoop() { return true; }
    @Override public boolean isDebugEnabled() { return false; }
    @Override public boolean isTraceEnabled() { return false; }
    @Override public void info(Object message) { }
    @Override public void info(Object message, Throwable t) { }
    @Override public void info(String message) { }
    @Override public void info(String message, Throwable t) { }
    @Override public void info(String message, Object... params) { }
    @Override public void warn(Object message) { }
    @Override public void warn(Object message, Throwable t) { }
    @Override public void warn(String message) { }
    @Override public void warn(String message, Throwable t) { }
    @Override public void warn(String message, Object... params) { }
    @Override public void error(Object message) { }
    @Override public void error(Object message, Throwable t) { }
    @Override public void error(String message) { }
    @Override public void error(String message, Throwable t) { }
    @Override public void error(String message, Object... params) { }
    @Override public void debug(Object message) { }
    @Override public void debug(Object message, Throwable t) { }
    @Override public void debug(String message) { }
    @Override public void debug(String message, Throwable t) { }
    @Override public void debug(String message, Object... params) { }
    @Override public void trace(Object message) { }
    @Override public void trace(Object message, Throwable t) { }
    @Override public void trace(String message) { }
    @Override public void trace(String message, Throwable t) { }
    @Override public void trace(String message, Object... params) { }
    //@formatter:on
}
