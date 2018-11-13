package net.covers1624.classloader;

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
 * selected via the first launch argument when using LaunchBouncer. Each 'bouncer' specifies
 * and 'id', this is what is uses to find the bouncer and run it. It should be noted, that
 * ALL 'bouncers' are instantiated before launching, it is recommended to not have any dangling
 * bouncers.
 *
 * Created by covers1624 on 10/11/2017.
 */
public class LaunchBouncer {

    public static ModularClassLoader classLoader;

    @SuppressWarnings ("unchecked")
    public static void main(String[] args) throws Throwable {
        classLoader = new ModularClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);

        Set<IResourceResolverFactory> loaded = Collections.newSetFromMap(new IdentityHashMap<>());
        ServiceLoader<IResourceResolverFactory> factories = ServiceLoader.load(IResourceResolverFactory.class, classLoader);
        //Loop forever, 'soft' Resetting the ServiceLoader each time.
        //meaning we can use an IdentitySet to identify Factories we have already processed.
        //Each subsequent loop will intern load any factories inside jars that may have been
        //made available through a previous factory.
        //Essentially this makes recursive jar extraction possible in a super neat way.
        while (true) {
            boolean newThings = false;
            Utils.softReload(factories);
            for (IResourceResolverFactory factory : factories) {
                if (!loaded.contains(factory)) {
                    newThings = true;
                    loaded.add(factory);
                    try {
                        IResourceResolver resolver = factory.create();
                        if (resolver != null) {
                            classLoader.addResolver(resolver);
                        }
                    } catch (IOException ignored) {
                    }
                }
            }
            if (!newThings) {
                break;
            }
        }
        LogHelper.findLoggerImpl(classLoader);
        ModularClassLoader.refreshLogger();

        ServiceLoader<IClassTransformer> transformerLoader = ServiceLoader.load(IClassTransformer.class, classLoader);
        Map<Priority, List<IClassTransformer>> priorityMap = new HashMap<>();
        for (IClassTransformer transformer : transformerLoader) {
            Class<IClassTransformer> cls = (Class<IClassTransformer>) transformer.getClass();
            Sort ann = cls.getAnnotation(Sort.class);
            Priority priority = ann != null ? ann.value() : Priority.NORMAL;
            priorityMap.computeIfAbsent(priority, e -> new ArrayList<>()).add(transformer);
        }

        for (Priority priority : Priority.values()) {
            for (IClassTransformer transformer : priorityMap.getOrDefault(priority, Collections.emptyList())) {
                classLoader.addTransformer(transformer);
            }
        }

        ServiceLoader<IBounceClass> bounceLoader = ServiceLoader.load(IBounceClass.class, classLoader);
        Map<String, IBounceClass> bounceClasses = new HashMap<>();
        for (IBounceClass bounce : bounceLoader) {
            String id = bounce.getId();
            IBounceClass existing = bounceClasses.get(id);
            if (existing != null) {
                StringBuilder builder = new StringBuilder();
                builder.append("Duplicate IBounceClass id. ").append(id).append("\n");
                builder.append(" A: ").append(existing.getClass().getName()).append("\n");
                builder.append(" B:").append(bounce.getClass().getName()).append("\n");
                throw new RuntimeException(builder.toString());
            }
            bounceClasses.put(id, bounce);
        }
        if (bounceClasses.isEmpty()) {
            throw new RuntimeException("No Bounce classes found.");
        }
        if (bounceClasses.size() == 1) {
            if (args.length > 0) {
                String first = args[0];
                if (bounceClasses.containsKey(first)) {
                    args = Utils.shiftArgs(args);
                }
            }
            bounceClasses.values().iterator().next().main(args);
            return;
        }
        if (args.length >= 1) {
            String first = args[0];
            args = Utils.shiftArgs(args);
            IBounceClass bounceClass = bounceClasses.get(first);
            if (bounceClass != null) {
                bounceClass.main(args);
                return;
            } else {
                System.out.println("Bounce Class id '" + first + "' Not found.");
            }
        }

        StringBuilder builder = new StringBuilder("Available IBounceClasses: \n");
        for (String id : bounceClasses.keySet()) {
            builder.append("  ").append(id);
        }
        System.out.println(builder.toString());
    }

}
