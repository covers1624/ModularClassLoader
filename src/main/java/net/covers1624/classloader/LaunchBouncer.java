package net.covers1624.classloader;

import net.covers1624.classloader.api.*;
import net.covers1624.classloader.internal.logging.LogHelper;

import java.io.IOException;
import java.util.*;

/**
 * LaunchBouncer!
 * A concept through the use of java's ServiceLoader system can identify various
 * 'bouncers', 'transformers' and 'resolvers'. Providing a platform for simple
 * programs that require some neat wrapping.
 *
 * ClassLoading:
 * LaunchBouncer uses a custom ClassLoader, {@link ModularClassLoader}.
 * This class loader is somewhat special, it provides registrable 'transformers'
 * and 'resolvers'.
 *
 * A Resolver is used to 'resolve' resources for the ClassLoader, by default
 * ModularClassLoader is setup to resolve resources from it's parent.
 * See ModularClassLoader for an in-depth explanation on how it loads classes
 * and how 'transformers' can be used, it does some neat things!
 *
 * A Transformer is a way of modifying the Java class structure before the class
 * is defined, Several Libraries exist for Java that facilitate this, ObjectWeb's ASM
 * library being the recommended way. In addition to the standard Transformer system
 * builtin to the ClassLoader, LaunchBouncer provides the ability to register them via
 * a ServiceLoader, Transformers are always loaded _After_ Resolvers. LaunchBouncer also
 * gives you the ability to Sort the Transformers loaded via the ServiceLoader as the order
 * can be sometimes random and platform dependant. Transformers are run in registration order,
 * simply add the {@link Sort} annotation to your transformer, using that you can specify a
 * priority to register the transformer in, The only guarantee made by the Sorting system is
 * a transformer with a higher priority will be registered before a Transformer with a lower
 * priority. The Sorting system makes no guarantee to the order the transformers will be in,
 * inside a priority, Example, 2 Transformers in the 'HIGH' group could be in any order.
 *
 * A Bouncer is a special kind of 'service', sort of. They are never run in parallel, but
 * selected via the first launch argument when using LaunchBouncer, or if only one is found,
 * perhaps in some environment where this will always be the case, you do not need to provide
 * the Bouncer's id. ID's by default are the class name, use {@link BounceId} to customize.
 *
 * Created by covers1624 on 10/11/2017.
 */
public class LaunchBouncer {

    public static ModularClassLoader classLoader;

    @SuppressWarnings ("unchecked")
    public static void main(String[] args) throws Throwable {
        classLoader = new ModularClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);
        //Force the system classloader to load these.
        Sort.class.getName();
        BounceId.class.getName();
        UseClassLoaderASM.class.getName();

        boolean newStuff = false;
        SimpleServiceLoader<IResourceResolverFactory> factories = new SimpleServiceLoader<>(IResourceResolverFactory.class, classLoader);
        do {
            factories.poll();
            for (Class<IResourceResolverFactory> clazz : factories.getNewServices()) {
                newStuff = true;
                IResourceResolverFactory factory = clazz.newInstance();
                try {
                    IResourceResolver resolver = factory.create();
                    if (resolver != null) {
                        classLoader.addResolver(resolver);
                    }
                } catch (IOException ignored) {
                }

            }

        }
        while (newStuff);

        LogHelper.findLoggerImpl(classLoader);
        ModularClassLoader.refreshLogger();

        SimpleServiceLoader<IClassTransformer> transformerLoader = new SimpleServiceLoader<>(IClassTransformer.class, classLoader);
        Map<Priority, List<IClassTransformer>> priorityMap = new HashMap<>();
        transformerLoader.poll();
        for (Class<IClassTransformer> transformerClazz : transformerLoader.getAllServices()) {
            Sort ann = transformerClazz.getAnnotation(Sort.class);
            Priority priority = ann != null ? ann.value() : Priority.NORMAL;
            priorityMap.computeIfAbsent(priority, e -> new ArrayList<>()).add(transformerClazz.newInstance());
        }

        for (Priority priority : Priority.values()) {
            for (IClassTransformer transformer : priorityMap.getOrDefault(priority, Collections.emptyList())) {
                classLoader.addTransformer(transformer);
            }
        }

        SimpleServiceLoader<IBounceClass> bounceLoader = new SimpleServiceLoader(IBounceClass.class, classLoader);
        Map<String, K2BPair<Class<IBounceClass>>> bounceClasses = new HashMap<>();
        bounceLoader.poll();
        for (Class<IBounceClass> bounceClazz : bounceLoader.getAllServices()) {
            String id = bounceClazz.getName();
            if (bounceClazz.isAnnotationPresent(BounceId.class)) {
                BounceId bounceId = bounceClazz.getAnnotation(BounceId.class);
                id = bounceId.value();
            }
            boolean useCLASM = bounceClazz.isAnnotationPresent(UseClassLoaderASM.class);

            K2BPair<Class<IBounceClass>> existing = bounceClasses.get(id);
            if (existing != null) {
                Class<IBounceClass> cls = existing.k;
                StringBuilder builder = new StringBuilder();
                builder.append("Duplicate IBounceClass id. ").append(id).append("\n");
                builder.append(" A: ").append(cls.getName()).append("\n");
                builder.append(" B:").append(bounceClazz.getName()).append("\n");
                throw new RuntimeException(builder.toString());
            }
            bounceClasses.put(id, K2BPair.of(bounceClazz, useCLASM));
        }
        if (bounceClasses.isEmpty()) {
            throw new RuntimeException("No Bounce classes found.");
        }
        K2BPair<Class<IBounceClass>> pair = null;
        if (bounceClasses.size() == 1) {
            if (args.length > 0) {
                String first = args[0];
                if (bounceClasses.containsKey(first)) {
                    args = Utils.shiftArgs(args);
                }
            }
            pair = bounceClasses.values().iterator().next();
        }
        if (pair == null) {
            if (args.length >= 1) {
                String first = args[0];
                args = Utils.shiftArgs(args);
                pair = bounceClasses.get(first);
                if (pair == null) {
                    throw new RuntimeException("Bounce Class id '" + first + "' Not found.");
                }
            }
        }

        if (pair == null) {
            StringBuilder builder = new StringBuilder("Available IBounceClasses: \n");
            for (String id : bounceClasses.keySet()) {
                builder.append("  ").append(id);
            }
            System.out.println(builder.toString());

        } else {
            invoke(pair, args);
        }
    }

    private static void invoke(K2BPair<Class<IBounceClass>> pair, String[] args) throws Throwable {
        if (pair.v) {
            classLoader.useASMHacks();
        }
        pair.k.newInstance().main(args);
    }

}
