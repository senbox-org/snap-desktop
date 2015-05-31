package org.esa.snap.main;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;

/**
 * Must be called from a NetBeans native platform executable.
 */
public class Main {

    private static final String NB_MAIN_CLASS = "org.netbeans.core.startup.Main";

    /**
     * Called from a NetBeans native platform executable.
     * Adjusts SNAP Engine properties so that they match NetBeans settings.
     */
    public static void main(String[] args) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        System.setProperty("snap.home", Paths.get(System.getProperty("netbeans.home")).getParent().toString());
        System.setProperty("snap.userdir", Paths.get(System.getProperty("netbeans.user")).toString());
        if (Boolean.getBoolean("snap.debug")) {
            dumpEnv(args);
        }

        runNetBeans(args, NB_MAIN_CLASS);
    }

    private static void dumpEnv(String[] args) {
        System.out.println("");
        System.out.println("Class: " + Main.class.getName());
        System.out.println("Arguments:");
        for (int i = 0; i < args.length; i++) {
            System.out.println("args[" + i + "] = \"" + args[i] + "\"");
        }
        System.out.println("");
        System.out.println("System properties:");
        Properties properties = System.getProperties();
        ArrayList<String> propertyNameList = new ArrayList<>(properties.stringPropertyNames());
        Collections.sort(propertyNameList);
        for (String name : propertyNameList) {
            String value = properties.getProperty(name);
            System.out.println(name + " = " + value);
        }
    }

    private static void runNetBeans(String[] args, String mainClassName) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Class<?> nbMainClass = classLoader.loadClass(mainClassName);
        Method nbMainMethod = nbMainClass.getDeclaredMethod("main", String[].class);
        nbMainMethod.invoke(null, new Object[]{args});
    }

}
