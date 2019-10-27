package net.covers1624.classloader.api;

import java.lang.annotation.*;

/**
 * EnvVars injected before your bouncer has initialized its class.
 * Place this on your ILaunchBouncer implementation.
 *
 * Created by covers1624 on 22/11/18.
 */
@Target (ElementType.TYPE)
@Repeatable (EnvVarList.class)
@Retention (RetentionPolicy.RUNTIME)
public @interface EnvVar {

    String key();

    String value();

}
