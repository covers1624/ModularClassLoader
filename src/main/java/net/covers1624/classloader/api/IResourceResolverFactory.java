package net.covers1624.classloader.api;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Used for Dynamic registration through LaunchBouncer.
 * Recursively loaded until no more are found.
 *
 * Created by covers1624 on 5/11/18.
 */
public interface IResourceResolverFactory {

    /**
     * Setup and create an IResourceResolver.
     *
     * @return The IResourceResolver, null if disabled.
     */
    @Nullable
    IResourceResolver create() throws IOException;
}
