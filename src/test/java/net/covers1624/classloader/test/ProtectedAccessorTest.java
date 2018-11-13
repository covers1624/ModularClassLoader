package net.covers1624.classloader.test;

import net.covers1624.classloader.ModularClassLoader;
import org.junit.Test;

/**
 * Created by covers1624 on 9/11/18.
 */
public class ProtectedAccessorTest {

    @Test
    public void doTest() {
        System.setProperty("covers1624.classloader.one_try", "true");
        System.setProperty("covers1624.classloader.potected_acc.debug", "true");
        System.setProperty("covers1624.classloader.debug", "true");
        ModularClassLoader cl = new ModularClassLoader();
        cl.useASMHacks();
    }


}
