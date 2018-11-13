package net.covers1624.classloader.internal.logging.impl;

import net.covers1624.classloader.internal.logging.ILogger;
import net.covers1624.classloader.internal.logging.ILoggerFactory;

/**
 * A noop logger factory.
 * Created by covers1624 on 13/11/18.
 */
public class NoopFactory implements ILoggerFactory {

    @Override
    public ILogger getLogger(String name) {
        return new NoopLogger();
    }
}
