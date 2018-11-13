package net.covers1624.classloader.internal.logging;

/**
 * Simple logger factory.
 * See {@link LogHelper}.
 * Created by covers1624 on 13/11/18.
 */
public interface ILoggerFactory {

    /**
     * Get or create a new ILogger with the specified name.
     *
     * @param name The name.
     * @return The instance.
     */
    ILogger getLogger(String name);

}
