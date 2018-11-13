package net.covers1624.classloader;

import net.covers1624.classloader.internal.logging.ILogger;
import net.covers1624.classloader.internal.logging.LogHelper;
import net.covers1624.classloader.internal.logging.impl.NoopLogger;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

/**
 * ModularClassLoader!
 * ModularClassLoader is a special ClassLoader, where it allows registrable
 * 'resolvers' and 'transformers' to provide resources and modify classes.
 *
 * ClassLoading, ModularClassLoader _May_ violate some of the ClassLoading
 * contracts that other ClassLoader's use. Specifically it can share a class
 * from a parent ClassLoader only if it was already loaded by that ClassLoader
 * or it's not excluded. There are several reasons for this, notably transformers
 * don't need to be instantiated by the ClassLoader that instantiated ModularClassLoader,
 * as the IClassTransformer underlying 'Class' object is shared from the parent.
 *
 * Transformers, Transformers can modify any class that is not excluded or already loaded.
 * Transformers are run in a special way, if a transformer is active and and trigger a class
 * load, they will be called to transform the class, but if they trigger the same class to load
 * again, an internal abort exception is triggered bonking out of the first transform where the
 * class load was triggered, completely skipping all transformers and defining the standard
 * class.
 *
 * Optimizations.
 * ModularClassLoader needs to use reflection to call {@link ClassLoader#findLoadedClass(String)}
 * on the parent ClassLoader. ModularClassLoader provides an optimization for this, provided
 * you opt into using it, its entirely possible that future versions of java will break the
 * work around's used to make it possible. See {@link ProtectedAccessor} for a detailed breakdown.
 * Basically, it Uses the ObjectWeb ASM library to generate and register an inner class of ClassLoader
 * that has access to the needed method, this generated class also implements BiFunction meaning its
 * entirely optional and a drop in replacement for the existing reflection.
 *
 * ModularClassLoader is also parallel compatible.
 *
 * Several random utilities also exist here, because they can.
 *
 * Created by covers1624 on 30/10/2017.
 */
public final class ModularClassLoader extends ClassLoader {

    private static final List<String> loaderExclusions = Collections.unmodifiableList(Arrays.asList(//
            "java.",//
            "sun.", //
            "javax."//
    ));
    private static ILogger logger = new NoopLogger();
    private static final boolean ONE_TRY_ASM = Boolean.getBoolean("covers1624.classloader.one_try");
    private static final boolean DEBUG = Boolean.getBoolean("covers1624.classloader.debug");
    private static final boolean DUMP = Boolean.getBoolean("covers1624.classloader.dump");

    private final ClassLoader parent;

    //Transformer state information.
    private final ThreadLocal<Deque<IClassTransformer>> transformerStack = ThreadLocal.withInitial(ArrayDeque::new);
    private final ThreadLocal<Deque<String>> classTransformingStack = ThreadLocal.withInitial(ArrayDeque::new);

    private List<IClassTransformer> transformers = new ArrayList<>();
    private List<IResourceResolver> resolvers = new ArrayList<>();

    private Map<String, byte[]> definedClazzBytes = new ConcurrentHashMap<>();
    private Map<String, Class<?>> clazzCache = new ConcurrentHashMap<>();
    private BiFunction<ClassLoader, String, Class> parentLookup;
    private boolean injected = false;

    static {
        ClassLoader.registerAsParallelCapable();
        refreshLogger();
    }

    public ModularClassLoader() {
        this(getSystemClassLoader());
    }

    public ModularClassLoader(ClassLoader parent) {
        super(parent);
        this.parent = parent;
        if (parent != null) {
            addResolver(IResourceResolver.fromClassLoader(parent));
        }
        reflect();
    }

    private void reflect() {
        try {
            Method m = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
            m.setAccessible(true);
            parentLookup = (obj, args) -> {
                try {
                    return (Class) m.invoke(obj, args);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            };
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Call this if you have the ObjectWeb ASM library installed,
     * This can significantly improve the performance of classloading,
     * since calls to {@link ClassLoader#findLoadedClass(String)} are
     * performed via a generated class, instead of reflection.
     */
    @SuppressWarnings ("unchecked")
    public void useASMHacks() {
        logger.trace("Enabling ASM hacks for better performance.");
        if (!injected) {
            injected = true;
            try {
                //This is called via reflection, so we avoid yet more ClassLoader issues.
                //What can happen is this class loader can load some ASM classes, and the parent load others.
                //This causes transformers to fail down the line due to our 'if the parent has loded it use that'
                //stance, this forces this ClassLoader to load ProtectedAccessor and all the classes it will load.
                Class<ProtectedAccessor> clazz = (Class<ProtectedAccessor>) Class.forName("net.covers1624.classloader.ProtectedAccessor", true, this);
                Method m = clazz.getDeclaredMethod("inject", ModularClassLoader.class);
                m.setAccessible(true);
                m.invoke(null, this);
            } catch (Throwable t) {
                logger.error("Failed to enable ASM hacks.", t);
                if (ONE_TRY_ASM) {
                    throw new RuntimeException(t);
                }
                if (logger.isNoop()) {
                    System.err.println("Failed to enable ASM hacks.");
                    t.printStackTrace();
                }
            }
        }
    }

    /**
     * Called to refresh the internal logging implementation of ModularClassLoader.
     */
    public static void refreshLogger() {
        if(DEBUG) {
            logger = LogHelper.getLogger("ModularClassLoader");
        } else {
            logger = new NoopLogger();
        }
    }

    /**
     * Register a resolver to this ClassLoader.
     * These are used to resolve the resources this ClassTransformer can load.
     *
     * @param resolver The resolver.
     */
    public void addResolver(IResourceResolver resolver) {
        resolvers.add(resolver);
    }

    /**
     * Register a transformer to this ClassLoader.
     * These are passed over all classes loaded by this ClassLoader.
     *
     * @param transformer The transformer.
     */
    public void addTransformer(IClassTransformer transformer) {
        logger.trace("Adding transformer. {}", transformer.getClass());
        transformers.add(transformer);
    }

    //This allows us to share classes from our parent class loader.
    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        logger.trace("Attempting Load: {}", name);
        synchronized (getClassLoadingLock(name)) {
            //We have already loaded this class before, just return it.
            Class<?> c = clazzCache.get(name);
            if (c != null) {
                logger.trace(" Cache hit.");
                return c;
            }
            //Check if native says its loaded.
            c = findLoadedClass(name);
            if (c == null) {
                c = parentLookup.apply(parent, name);
                if (c != null) {
                    logger.trace(" Parent cache hit.");
                    clazzCache.put(name, c);
                    return c;
                }
            }

            if (c == null) {
                if (loaderExclusions.stream().noneMatch(name::startsWith)) {
                    try {
                        //Find the bytes and transform.
                        c = findClass(name);
                        logger.trace(" Loaded.");
                    } catch (ClassNotFoundException ignored) {
                    }
                } else {
                    logger.trace(" excluded from this ClassLoader.");
                }
            }
            //Finally check if the parent knows where it is.
            if (c == null && parent != null) {
                //Called with false so we are the one to resolve it.
                //This will also throw a CNFE if it cant be found, bonking out of the method.
                c = parent.loadClass(name);
                logger.trace(" Parent loaded.");
            }
            //we have found the class, resolve.
            if (resolve) {
                resolveClass(c);
            }
            //Add it to our cache lookup.
            clazzCache.put(name, c);
            return c;
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        //Check if our cache has it. (kinda pointless since above.)
        Class<?> c = clazzCache.get(name);
        if (c != null) {
            return c;
        }

        Deque<String> loadingClasses = classTransformingStack.get();
        boolean loadReentry = loadingClasses.contains(name);
        try {
            loadingClasses.push(name);
            if (loadReentry && !transformerStack.get().isEmpty()) {
                logger.trace(" ReEntry on existing class whilst transforming. Assuming loop. Aborted.");
                throw new AbortException();
            }
            byte[] bytes = getClassBytes(name);

            //Check again, perhaps it was loaded by one of the transformers whilst transforming.
            c = clazzCache.get(name);
            if (c != null) {
                return c;
            }

            if (bytes != null) {
                c = defineClass(name, bytes);
            }

            if (c == null) {
                throw new ClassNotFoundException(name);
            }
        } finally {
            loadingClasses.pop();
        }
        return c;
    }

    @Nullable
    @Override
    public URL getResource(String name) {
        return findResource(name);
    }

    @Nullable
    @Override
    protected URL findResource(String name) {
        URL url = null;
        try {
            for (IResourceResolver resolver : resolvers) {
                url = resolver.findResource(name);
                if (url != null) {
                    break;
                }
            }
        } catch (IOException e) {
            return null;
        }
        return url;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        return findResources(name);
    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        List<Iterable<URL>> iters = new ArrayList<>();
        for (IResourceResolver resolver : resolvers) {
            iters.add(resolver.findResources(name));
        }
        return Utils.toEnumeration(Utils.merge(iters));
    }

    /**
     * Returns the bytes for a resource.
     * Null if the resource doesnt exist.
     *
     * @param name The resource.
     * @return The bytes, null otherwise.
     */
    public byte[] getResourceAsBytes(String name) {
        InputStream is = getResourceAsStream(name);
        if (is != null) {
            try (InputStream is_ = is) {
                return Utils.toByteArray(is_);
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Simple wrapper for getting a classes bytes from disk.
     *
     * @param name The class name E.G: 'java.lang.Object'
     * @return The bytes, Null if it doesnt exist.
     */
    @Nullable
    public byte[] getClassBytes(String name) {
        byte[] bytes = definedClazzBytes.get(name);
        if (bytes != null) {
            return bytes;
        }
        String asmName = name.replace(".", "/");
        String resource = asmName + ".class";
        bytes = getResourceAsBytes(resource);
        try {
            bytes = transform(name, bytes);
        } catch (AbortException ignored) {
            logger.trace("  Caught abort, registering un transformed class.");
        }

        if (DEBUG && DUMP) {
            dumpClass(asmName, bytes);
        }
        return bytes;
    }

    /**
     * Runs the transformer stack over the bytes provided,
     * A transformer will never be called if it is the cause
     * for the class load. This prevents deadlocks due to a
     * transformer needing to transform a class that it needs
     * to transform classes.
     *
     * @param name  The name of the class 'java.lang.Object'.
     * @param bytes The bytes of the class.
     * @return The transformed bytes;
     */
    @Nullable
    private byte[] transform(String name, @Nullable byte[] bytes) {
        Deque<IClassTransformer> activeTransformers = transformerStack.get();
        for (IClassTransformer transformer : transformers) {
            //if (!activeTransformers.contains(transformer)) {
            //Make sure transformers always pop from the list.
            try {
                activeTransformers.push(transformer);
                bytes = transformer.transform(name, bytes);
            } finally {
                activeTransformers.pop();
            }
            // }
        }

        return bytes;
    }

    /**
     * Dumps the bytes provided as a class.
     *
     * @param name  The ASM name for the class.
     * @param bytes The bytes.
     */
    private void dumpClass(String name, byte[] bytes) {
        try {
            File file = new File("CL_CACHE", name + ".class");
            if (!file.exists()) {
                if (!file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }
                file.createNewFile();
            }
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(bytes);
                fos.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //Defines a class and caches it.
    private Class<?> defineClass(String name, byte[] bytes) {
        Class<?> clazz = defineClass(name, bytes, 0, bytes.length);
        definedClazzBytes.put(name, bytes);
        clazzCache.put(name, clazz);
        return clazz;
    }

    //Internal.
    public void setParentLookup(BiFunction<ClassLoader, String, Class> parentLookup) {
        this.parentLookup = parentLookup;
    }

    private static class AbortException extends RuntimeException {

    }

}
