package net.covers1624.classloader.api.logging;

import net.covers1624.classloader.internal.logging.LogHelper;

/**
 * Simple logger factory.
 * See {@link LogHelper}.
 * Created by covers1624 on 13/11/18.
 */
public interface ILoggerFactory {

    public static final String NOOP_FACTORY = "net.covers1624.classloader.internal.logging.impl.NoopFactory";
    public static final String LOG4J_FACTORY = "net.covers1624.classloader.internal.logging.impl.Log4jFactory";

    /**
     * Get or create a new ILogger with the specified name.
     *
     * @param name The name.
     * @return The instance.
     */
    ILogger getLogger(String name);

}
