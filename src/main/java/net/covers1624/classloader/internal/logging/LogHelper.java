package net.covers1624.classloader.internal.logging;

import net.covers1624.classloader.api.logging.ILogger;
import net.covers1624.classloader.api.logging.ILoggerFactory;
import net.covers1624.classloader.internal.logging.impl.NoopFactory;

/**
 * Simple helpers for logging.
 * Created by covers1624 on 13/11/18.
 */
public class LogHelper {

    //The current Logger factory.
    private static ILoggerFactory factory = new NoopFactory();

    /**
     * Sets the ILoggerFactory to a specific instance.
     *
     * @param factory The instance.
     */
    public static void setLoggerFactory(ILoggerFactory factory) {
        LogHelper.factory = factory;
    }

    /**
     * Gets an ILogger with the specified name.
     *
     * @param name The name.
     * @return The logger.
     */
    public static ILogger getLogger(String name) {
        return factory.getLogger(name);
    }

}
