package net.covers1624.classloader;

import net.covers1624.classloader.api.*;
import net.covers1624.classloader.api.logging.ILoggerFactory;
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
        EnvVar.class.getName();
        EnvVarList.class.getName();
        ClassLoaderLoggerImpl.class.getName();
        ILoggerFactory.class.getName();

        boolean newStuff = false;
        SimpleServiceLoader<IResourceResolverFactory> factories = new SimpleServiceLoader<>(IResourceResolverFactory.class, classLoader);
        do {
            factories.poll();
            newStuff = false;
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
        Map<String, BounceState> bounceStates = new HashMap<>();
        bounceLoader.poll();
        for (Class<IBounceClass> bounceClazz : bounceLoader.getAllServices()) {
            String id = bounceClazz.getName();
            if (bounceClazz.isAnnotationPresent(BounceId.class)) {
                BounceId bounceId = bounceClazz.getAnnotation(BounceId.class);
                id = bounceId.value();
            }
            {
                BounceState existing = bounceStates.get(id);
                if (existing != null) {
                    StringBuilder builder = new StringBuilder();
                    builder.append("Duplicate IBounceClass id. ").append(id).append("\n");
                    builder.append(" A: ").append(existing.clazz.getName()).append("\n");
                    builder.append(" B:").append(bounceClazz.getName()).append("\n");
                    throw new RuntimeException(builder.toString());
                }
            }
            BounceState state = new BounceState();
            bounceStates.put(id, state);
            state.id = id;
            state.clazz = bounceClazz;
            state.useASM = bounceClazz.isAnnotationPresent(UseClassLoaderASM.class);

            if (bounceClazz.isAnnotationPresent(EnvVarList.class)) {
                EnvVarList list = bounceClazz.getAnnotation(EnvVarList.class);
                for (EnvVar var : list.value()) {
                    state.env.put(var.key(), var.value());
                }
            } else if (bounceClazz.isAnnotationPresent(EnvVar.class)) {
                EnvVar var = bounceClazz.getAnnotation(EnvVar.class);
                state.env.put(var.key(), var.value());
            }

            if (bounceClazz.isAnnotationPresent(ClassLoaderLoggerImpl.class)) {
                state.loggerImpl = bounceClazz.getAnnotation(ClassLoaderLoggerImpl.class).value();
            }

        }
        if (bounceStates.isEmpty()) {
            throw new RuntimeException("No Bounce classes found.");
        }
        BounceState state = null;
        if (bounceStates.size() == 1) {
            if (args.length > 0) {
                String first = args[0];
                if (bounceStates.containsKey(first)) {
                    args = Utils.shiftArgs(args);
                }
            }
            state = bounceStates.values().iterator().next();
        }
        if (state == null) {
            if (args.length >= 1) {
                String first = args[0];
                args = Utils.shiftArgs(args);
                state = bounceStates.get(first);
                if (state == null) {
                    throw new RuntimeException("Bounce Class id '" + first + "' Not found.");
                }
            }
        }

        if (state == null) {
            StringBuilder builder = new StringBuilder("Available IBounceClasses: \n");
            for (String id : bounceStates.keySet()) {
                builder.append("  ").append(id);
            }
            System.out.println(builder.toString());

        } else {
            invoke(state, args);
        }
    }

    private static void invoke(BounceState state, String[] args) throws Throwable {
        if (state.useASM) {
            classLoader.useASMHacks();
        }
        state.env.forEach(System::setProperty);

        if (state.loggerImpl != null) {
            Class factoryClass = Class.forName(state.loggerImpl, false, classLoader);
            if (!ILoggerFactory.class.isAssignableFrom(factoryClass)) {
                throw new IllegalArgumentException("Class provided via @ClassLoaderLoggerImpl is not an instance of ILoggerFactory.");
            }
            LogHelper.setLoggerFactory((ILoggerFactory) factoryClass.newInstance());
        }
        ModularClassLoader.refreshLogger();
        state.clazz.newInstance().main(args);
    }

    private static class BounceState {

        public String id;
        public Class<IBounceClass> clazz;
        public boolean useASM;
        public Map<String, String> env = new HashMap<>();
        public String loggerImpl;
    }

}
