package net.covers1624.classloader.api;

import net.covers1624.classloader.ModularClassLoader;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Simple marker annotation, Use this on your IBounceClass if you
 * use ObjectWebs' ASM Library. Basically an early load alias for
 * calling {@link ModularClassLoader#useASMHacks()}.
 *
 * With this {@link ModularClassLoader#useASMHacks()} is called
 * directly before instantiating your IBounceClass and invoking
 * main.
 *
 * Created by covers1624 on 15/11/18.
 */
@Target (TYPE)
@Retention (RUNTIME)
public @interface UseClassLoaderASM {

}
