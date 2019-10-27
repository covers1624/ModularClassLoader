package net.covers1624.classloader.internal.logging.impl;

import net.covers1624.classloader.api.logging.ILogger;
import net.covers1624.classloader.api.logging.ILoggerFactory;

/**
 * A log4j factory implementation.
 * Created by covers1624 on 13/11/18.
 */
public class Log4jFactory implements ILoggerFactory {

    @Override
    public ILogger getLogger(String name) {
        return new Log4jLogger(name);
    }
}
