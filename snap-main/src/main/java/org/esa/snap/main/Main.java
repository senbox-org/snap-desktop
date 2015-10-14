package org.esa.snap.main;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;

/**
 * Custom SNAP main class for the NetBeans Platform to be called from a NetBeans command-line executable.
 * <p>
 * To activate it, add
 * {@code -J-Dnetbeans.mainclass=org.esa.snap.main.Main}
 * to {@code default_options} parameter provided in the file {@code $INSTALL_DIR/etc/snap.config}.
 * <p>
 * The intention of this class is to initialise {@code snap.home} which will be set to the value of the
 * NetBeans Platform system property {@code netbeans.home}, which is expected to be already set by the
 * NetBeans Platform command-line.
 * <p>
 * See
 * <ul>
 * <li><a href="http://wiki.netbeans.org/DevFaqPlatformAppAuthStrategies">DevFaqPlatformAppAuthStrategies</a></li>
 * <li><a href="http://wiki.netbeans.org/FaqNetbeansConf">FaqNetbeansConf</a></li>
 * <li><a href="http://wiki.netbeans.org/FaqStartupParameters">FaqStartupParameters</a> in the NetBeans wiki.</li>
 * </ul>
 *
 * @author Norman Fomferra
 * @since SNAP 2
 */
public class Main {

    private static final String NB_MAIN_CLASS = "org.netbeans.core.startup.Main";

    /**
     * A custom main entry point called from a NetBeans Platform command-line.
     *
     * @param args NetBeans Platform command-line arguments
     */
    public static void main(String[] args) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        System.setProperty("snap.home", Paths.get(getPropertySafe("netbeans.home")).getParent().toString());
        if (Boolean.getBoolean("snap.debug")) {
            dumpEnv(args);
        }
        runNetBeans(args, NB_MAIN_CLASS);
    }

    private static String getPropertySafe(String key) {
        String value = System.getProperty(key);
        if (value == null) {
            throw new IllegalStateException(String.format("Expecting system property '%s' to be set", key));
        }
        return value;
    }

    private static void dumpEnv(String[] args) {
        System.out.println();
        System.out.println("Class: " + Main.class.getName());
        System.out.println();
        System.out.println("Arguments:");
        for (int i = 0; i < args.length; i++) {
            System.out.printf("args[%d] = \"%s\"%n", i, args[i]);
        }
        System.out.println();
        System.out.println("System properties:");
        Properties properties = System.getProperties();
        ArrayList<String> propertyNameList = new ArrayList<>(properties.stringPropertyNames());
        Collections.sort(propertyNameList);
        for (String name : propertyNameList) {
            String value = properties.getProperty(name);
            System.out.println(name + " = " + value);
        }
        System.out.flush();
        System.out.println();
        System.out.println("Stack trace (this is no error!): ");
        new Exception().printStackTrace(System.out);
        System.out.println();
        System.out.flush();
    }

    private static void runNetBeans(String[] args, String mainClassName) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Class<?> nbMainClass = classLoader.loadClass(mainClassName);
        Method nbMainMethod = nbMainClass.getDeclaredMethod("main", String[].class);
        nbMainMethod.invoke(null, new Object[]{args});
    }
}
