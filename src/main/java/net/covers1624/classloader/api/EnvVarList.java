package net.covers1624.classloader.api;

import java.lang.annotation.*;

/**
 * Created by covers1624 on 22/11/18.
 */
@Target (ElementType.TYPE)
@Retention (RetentionPolicy.RUNTIME)
public @interface EnvVarList {

    EnvVar[] value();

}
