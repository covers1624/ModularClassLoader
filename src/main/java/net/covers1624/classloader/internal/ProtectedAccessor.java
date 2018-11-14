package net.covers1624.classloader.internal;

import net.covers1624.classloader.ModularClassLoader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import static org.objectweb.asm.Opcodes.*;

/**
 * This looks simple, it exists as a work around for {@link ClassLoader#findLoadedClass(String)} being
 * protected for _some_ reason. This uses the ObjectWeb ASM library to generate an Inner class of
 * ClassLoader called 'java.lang.ClassLoader$$ProtectedAccessor$$1', the number at the end is incremented
 * each time just in case this is called multiple times you wont end up with a weird crash. The Generated
 * class implements an interface, BiFunction, this is generated very similar to how a lambda class is
 * generated, The class 'overrides' the apply method of BiFunction, does some casts and calls the required
 * method on ClassLoader returning the result.
 *
 * Its unclear if this will work on Java versions after 8.
 *
 * Created by covers1624 on 6/11/18.
 */
public class ProtectedAccessor {

    private static boolean DEBUG = Boolean.getBoolean("covers1624.classloader.potected_acc.debug");

    //Just in case we are called multiple times.
    private static AtomicInteger inc = new AtomicInteger();

    /**
     * Generates and injects a 'lambda' class for ClassLoader.findLoadedClass callback.
     * This is called via reflection to avoid ClassLoader issues.
     *
     * @param classLoader the ModularClassLoader to inject to.
     */
    @SuppressWarnings ("unchecked")
    private static void inject(ModularClassLoader classLoader) throws Exception {
        byte[] bytes = spinClass();
        List<Throwable> throwables = new ArrayList<>();
        Class<?> clazz = null;

        try {
            clazz = tryRegisterDirect(bytes);
        } catch (Throwable t) {
            if (DEBUG) {
                t.printStackTrace(System.out);
            }
            throwables.add(t);
        }
        if (clazz == null) {
            try {
                clazz = tryRegisterReflection1(bytes);
            } catch (Throwable t) {
                if (DEBUG) {
                    t.printStackTrace(System.out);
                }
                throwables.add(t);
            }
        }
        if (clazz == null) {
            try {
                clazz = tryRegisterReflection2(bytes);
            } catch (Throwable t) {
                if (DEBUG) {
                    t.printStackTrace(System.out);
                }
                throwables.add(t);
            }
        }
        if (clazz != null) {
            classLoader.setParentLookup((BiFunction<ClassLoader, String, Class>) clazz.newInstance());
            return;
        }

        RuntimeException e = new RuntimeException("Failed to register ProtectedAccessor generated class.");
        throwables.forEach(e::addSuppressed);
        throw e;
    }

    private static byte[] spinClass() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        MethodVisitor mv;
        String clsName = "java/lang/ClassLoader$$ProtectedAccessor$$" + inc.getAndIncrement();
        String iFace = "java/util/function/BiFunction";
        cw.visit(V1_8, ACC_PUBLIC | ACC_SUPER | ACC_FINAL | ACC_SYNTHETIC, clsName, null, "java/lang/Object", new String[] { iFace });

        mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(-1, -1);
        mv.visitEnd();

        mv = cw.visitMethod(ACC_PUBLIC, "apply", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", null, null);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitTypeInsn(CHECKCAST, "java/lang/ClassLoader");
        mv.visitVarInsn(ALOAD, 2);
        mv.visitTypeInsn(CHECKCAST, "java/lang/String");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/ClassLoader", "findLoadedClass", "(Ljava/lang/String;)Ljava/lang/Class;", false);
        mv.visitTypeInsn(CHECKCAST, "java/lang/Object");
        mv.visitInsn(ARETURN);
        mv.visitMaxs(-1, -1);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }

    //Try with this first. Maybe we can?
    private static Class<?> tryRegisterDirect(byte[] bytes) {
        if(DEBUG) {
            System.out.println("Trying registerDirect.");
        }
        return Unsafe.getUnsafe().defineAnonymousClass(ClassLoader.class, bytes, null);
    }

    //Ok, try and get the field and use it that way.
    private static Class<?> tryRegisterReflection1(byte[] bytes) throws Exception {
        if(DEBUG) {
            System.out.println("Trying registerReflection1.");
        }
        Method f_getDeclaredFields0 = Class.class.getDeclaredMethod("getDeclaredFields0", boolean.class);
        f_getDeclaredFields0.setAccessible(true);

        Field[] fields = (Field[]) f_getDeclaredFields0.invoke(Unsafe.class, false);
        Field f_Unsafe = null;
        for (Field field : fields) {
            if (field.getName().equals("theUnsafe")) {
                f_Unsafe = field;
                break;
            }
        }
        f_Unsafe.setAccessible(true);
        Unsafe unsafe = (Unsafe) f_Unsafe.get(null);
        return unsafe.defineAnonymousClass(ClassLoader.class, bytes, null);
    }

    //Wow don't even have access to the class, well too bad, work around.
    private static Class<?> tryRegisterReflection2(byte[] bytes) throws Exception {
        if(DEBUG) {
            System.out.println("Trying tryRegisterReflection2.");
        }
        Class<?> c_metaFactory = Class.forName("java.lang.invoke.InnerClassLambdaMetafactory", false, ClassLoader.getSystemClassLoader());
        Field f_unsafe = c_metaFactory.getDeclaredField("UNSAFE");
        Method m_define = f_unsafe.getType().getMethod("defineAnonymousClass", Class.class, byte[].class, Object[].class);
        f_unsafe.setAccessible(true);

        Object unsafe = f_unsafe.get(null);
        return (Class<?>) m_define.invoke(unsafe, ClassLoader.class, bytes, null);
    }
}
