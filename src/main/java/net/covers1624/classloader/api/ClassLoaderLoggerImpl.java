package net.covers1624.classloader.api;

import net.covers1624.classloader.api.logging.ILoggerFactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to change the Logger implementation of ModularClassLoader.
 * Must implement {@link ILoggerFactory}
 *
 * Created by covers1624 on 22/11/18.
 */
@Target (ElementType.TYPE)
@Retention (RetentionPolicy.RUNTIME)
public @interface ClassLoaderLoggerImpl {

    String value();

}
