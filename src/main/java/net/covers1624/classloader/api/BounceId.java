package net.covers1624.classloader.api;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Use this to specify a custom name for your IBounceClass.
 *
 * Created by covers1624 on 15/11/18.
 */
@Target (TYPE)
@Retention (RUNTIME)
public @interface BounceId {

    String value();
}
