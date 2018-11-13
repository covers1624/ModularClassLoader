package net.covers1624.classloader;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Created by covers1624 on 10/11/18.
 */
public class Utils {

    private static final Class<ServiceLoader> c_ServiceLoader;
    private static final Class c_LazyIterator;
    private static final Constructor cs_LazyIterator;
    private static final Field f_lookupIterator;
    private static final Field f_service;
    private static final Field f_loader;

    static {
        try {
            c_ServiceLoader = ServiceLoader.class;
            c_LazyIterator = Class.forName(Utils.c_ServiceLoader.getName() + "$LazyIterator");
            cs_LazyIterator = Utils.c_LazyIterator.getDeclaredConstructors()[0];
            f_lookupIterator = Utils.c_ServiceLoader.getDeclaredField("lookupIterator");
            f_service = Utils.c_ServiceLoader.getDeclaredField("service");
            f_loader = Utils.c_ServiceLoader.getDeclaredField("loader");

            cs_LazyIterator.setAccessible(true);
            f_lookupIterator.setAccessible(true);
            f_service.setAccessible(true);
            f_loader.setAccessible(true);

        } catch (ClassNotFoundException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads an InputStream into a byte array.
     * Will NOT close the InputStream.
     *
     * @param is The InputStream.
     * @return The bytes.
     * @throws IOException Write or Read error.
     */
    public static byte[] toByteArray(InputStream is) throws IOException {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            copy(is, output);
            return output.toByteArray();
        }
    }

    /**
     * Copies the entire InputStream to an OutputStream.
     * Will NOT close the streams.
     *
     * @param is The input.
     * @param os The output.
     * @throws IOException Write or Read error.
     */
    public static void copy(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        while (-1 != (len = is.read(buffer))) {
            os.write(buffer, 0, len);
        }
    }

    /**
     * Merge's an Iterable of Iterables together into one Iterable.
     *
     * @param iterables The Iterables to merge together.
     * @param <E>       The Type.
     * @return The merged Iterable.
     */
    @NotNull
    @Contract (pure = true)
    public static <E> Iterable<E> merge(@NotNull Iterable<Iterable<E>> iterables) {
        return () -> new Iterator<E>() {
            private Iterator<Iterable<E>> master = iterables.iterator();
            private Iterator<E> iter;

            @Override
            public boolean hasNext() {
                while (iter == null || !iter.hasNext()) {
                    if (!master.hasNext()) {
                        return false;
                    }
                    iter = master.next().iterator();
                }
                return iter.hasNext();
            }

            @Override
            public E next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return iter.next();
            }
        };
    }

    /**
     * Represent this Iterable as an Enumeration.
     *
     * @param iterable The Iterable.
     * @param <E>      The Type.
     * @return The Enumeration.
     */
    @NotNull
    public static <E> Enumeration<E> toEnumeration(@NotNull Iterable<E> iterable) {
        Iterator<E> iter = iterable.iterator();
        return new Enumeration<E>() {
            //@formatter:off
            @Override public boolean hasMoreElements() { return iter.hasNext(); }
            @Override public E nextElement() { return iter.next(); }

        };
    }

    /**
     * Represents this Enumeration as an Iterable.
     *
     * @param enumeration The Enumeration.
     * @param <E> The Type.
     * @return The Iterable.
     */
    @NotNull
    @Contract(pure = true)
    public static <E> Iterable<E> toIterable(@NotNull Enumeration<E> enumeration) {
        return () -> new Iterator<E>() {
            //@formatter:off
            @Override public boolean hasNext() { return enumeration.hasMoreElements(); }
            @Override public E next() { return enumeration.nextElement(); }
            //@formatter:on
        };
    }

    /**
     * Simply shifts the args one to the right, Just a tail operation.
     *
     * @param s The array.
     * @return The tail of the array.
     */
    @NotNull
    @Contract ("_ -> new")
    public static String[] shiftArgs(@Nullable String[] s) {
        if (s == null || s.length == 0) {
            return new String[0];
        }

        String[] s1 = new String[s.length - 1];
        System.arraycopy(s, 1, s1, 0, s1.length);
        return s1;
    }

    /**
     * Performs a 'soft' Reload on a ServiceLoader. See {@link ServiceLoader#reload()}.
     * Reload specifically clears the already constructed list, meaning if we are checking
     * for changes, we would end up constructing classes multiple times for no reason.
     * Whilst this is still 'Hacky' its cleaner implementation wise.
     *
     * This just replaces the underlying LazyIterator instance inside ServiceLoader with a
     * new one, exactly the same way {@link ServiceLoader#reload()} does.
     *
     * @param serviceLoader The service loader to reload.
     * @throws Throwable If something went wrong.
     */
    public static void softReload(ServiceLoader<?> serviceLoader) throws Throwable {
        Class service = (Class) f_service.get(serviceLoader);
        ClassLoader loader = (ClassLoader) f_loader.get(serviceLoader);
        Object lazyIter = cs_LazyIterator.newInstance(serviceLoader, service, loader, null);
        f_lookupIterator.set(serviceLoader, lazyIter);
    }

    /**
     * Simply collects all files in the directory to a list of URLs.
     *
     * @param dir    The directory.
     * @param filter The filter.
     * @return The List of URLs.
     */
    public static List<URL> dirToURLs(File dir, FilenameFilter filter) {
        File[] files = dir.listFiles(filter);
        List<URL> urls = new ArrayList<>();
        if (dir.exists() && files != null) {
            for (File file : files) {
                try {
                    urls.add(file.toURI().toURL());
                } catch (MalformedURLException ignored) {

                }
            }
        }
        return urls;
    }
}
