package net.covers1624.classloader.internal.logging;

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

    /**
     * Used to find and auto assign a ILoggerFactory implementation.
     * Currently only Log4j is supported.
     *
     * @param classLoader The ClassLoader to check and load classes with.
     */
    public static void findLoggerImpl(ClassLoader classLoader) {
        try {
            testLog4j(classLoader);
            return;
        } catch (Exception ignored) {
        }
    }

    /**
     * Checks if Log4j is installed and sets the logging factory if it exists.
     */
    private static void testLog4j(ClassLoader classLoader) throws Exception {
        Class.forName("org.apache.logging.log4j.LogManager", false, classLoader);
        //Instantiate via reflection, because it needs to be loaded by our class loader.
        setLoggerFactory((ILoggerFactory) Class.forName("net.covers1624.classloader.internal.logging.impl.Log4jFactory", true, classLoader).newInstance());
        //Solves some ClassLoading issues with ModularClassLoader attempting to load classes whilst its logging.
        getLogger("LogHelper").info("ModularClassLoader will use Log4j!{}", "");
    }

}
