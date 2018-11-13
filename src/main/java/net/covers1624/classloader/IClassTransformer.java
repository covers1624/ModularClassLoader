package net.covers1624.classloader;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provides the ability to modify the bytes of a class
 * before its defined by ModularClassLoader.
 * See {@link ModularClassLoader} for more information.
 *
 * Created by covers1624 on 5/11/18.
 */
public interface IClassTransformer {

    /**
     * Called to transform the named classes bytes.
     * If no transformation is required, simply return the bytes
     * you receive.
     *
     * @param name  The classes name. Example: 'java.lang.String'
     * @param bytes The classes bytes.
     * @return The transformed bytes..
     */
    @Nullable byte[] transform(@NotNull String name, @Nullable byte[] bytes);

}
