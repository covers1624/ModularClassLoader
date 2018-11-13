package net.covers1624.classloader;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import sun.misc.URLClassPath;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;

/**
 * Created by covers1624 on 30/10/2017.
 */
public interface IResourceResolver {

    IResourceResolver EMPTY = new IResourceResolver() {
        @Override
        public URL findResource(String name) throws IOException {
            return null;
        }

        @Override
        public Iterable<URL> findResources(String name) throws IOException {
            return Collections::emptyIterator;
        }
    };

    /**
     * Returns the URL for the requested resource.
     * Mirror of {@link ClassLoader#findResource(String)}
     *
     * @param name The name of the resource E.G: 'java/lang/Object.class'
     * @return The bytes of the resource.
     */
    URL findResource(String name) throws IOException;

    /**
     * Returns an Iterable of URL's representing all resources with the requested name.
     * Mirror of {@link ClassLoader#findResources(String)}
     *
     * @param name The name of the resource E.G: 'java/lang/Object.class'
     * @return The URL's
     */
    Iterable<URL> findResources(String name) throws IOException;

    /**
     * Creates an IResourceResolver from a ClassLoader.
     *
     * @param cl The ClassLoader.
     * @return The IResourceResolver.
     */
    @NotNull
    @Contract (value = "_->new", pure = true)
    static IResourceResolver fromClassLoader(ClassLoader cl) {
        return new IResourceResolver() {
            @Override
            public URL findResource(String name) throws IOException {
                return cl.getResource(name);
            }

            @Override
            public Iterable<URL> findResources(String name) throws IOException {
                return Utils.toIterable(cl.getResources(name));
            }
        };
    }

    /**
     * Creates an IResourceResolver from a collection of URL's.
     * This is equivalent to how a URLClassLoader finds resources.
     *
     * @param urls The URL's
     * @return The IResourceResolver.
     */
    @NotNull
    @Contract (value = "_ -> new", pure = true)
    static IResourceResolver fromURLs(Collection<URL> urls) {
        URLClassPath ucp = new URLClassPath(urls.toArray(new URL[0]));
        return new IResourceResolver() {
            @Override
            public URL findResource(String name) throws IOException {
                URL url = ucp.findResource(name, true);
                return url != null ? ucp.checkURL(url) : null;
            }

            @Override
            public Iterable<URL> findResources(String name) throws IOException {
                return Utils.toIterable(ucp.findResources(name, true));
            }
        };
    }
}
