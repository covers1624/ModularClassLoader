package net.covers1624.classloader;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotate your IClassTransformer with this to sort it during
 * dynamic registration.
 *
 * Created by covers1624 on 5/1 1/18.
 */
@Target (TYPE)
@Retention (RUNTIME)
public @interface Sort {

    /**
     * The priority for the annotated class.
     * Higher numbers mean later in the cycle.
     * Negative numbers are supported.
     *
     * @return The priority, Default is NORMAL.
     */
    Priority value() default Priority.NORMAL;
}
