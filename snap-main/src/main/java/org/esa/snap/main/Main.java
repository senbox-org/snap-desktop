package org.esa.snap.main;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Custom SNAP main class for the NetBeans Platform to be called from a NetBeans command-line executable.
 * <p>
 * To activate it, add
 * {@code -J-Dnetbeans.mainclass=org.esa.snap.main.Main}
 * to {@code default_options} parameter provided in the file {@code $INSTALL_DIR/etc/snap.config}.
 * <p>
 * See
 * <a href="http://wiki.netbeans.org/DevFaqPlatformAppAuthStrategies">DevFaqPlatformAppAuthStrategies</a>,
 * <a href="http://wiki.netbeans.org/FaqNetbeansConf">FaqNetbeansConf</a>,
 * <a href="http://wiki.netbeans.org/FaqStartupParameters">FaqStartupParameters</a>
 * in the NetBeans wiki.
 * <p>
 * The actual intention of this class is to adjust similar configuration settings used both by the NetBeans Platform
 * and the SNAP Engine.
 * <p>
 * The system property {@code snap.home} will be set to the value of the NetBeans Platform system property
 * {@code netbeans.home}, which is expected to be already set by the NetBeans Platform command-line.
 * <p>
 * For the SNAP Desktop application the user and cache directories can be provided through
 * <ul>
 *     <li>NetBeans Platform configuration settings {@code default_userdir} and {@code default_cachedir} in the {@code $INSTALL_DIR/etc/snap.config} file</li>
 *     <li>NetBeans Platform command-line options {@code --userdir} and {@code --cachedir}</li>
 * </ul>
 * The cache directory may not be set through NetBeans configuration at all. It defaults to {@code ${snap.userdir}/var/cache} then.
 * <p>
 * A special rule is applied on Windows OS: If
 * <ol>
 *     <li>the user directory has the special value {@code ${user.home}\AppData\Roaming\.snap} then it is redirected to {@code ${user.home}/.snap}</li>
 *     <li>the cache directory has the special value {@code ${user.home}\AppData\Roaming\.snap\var\cache} then it is redirected to {@code ${user.home}/.snap/var/cache}</li>
 * </ol>
 * <p>
 * In order to adjust similar SNAP Engine configuration properties the {@code snap.userdir} and  {@code snap.cachedir}
 * system properties are set accordingly to the NetBeans configuration.
 * This way the various methods in {@code org.esa.snap.util.SystemUtils} will make use of a common configuration.
 *
 * @author Norman Fomferra
 * @since SNAP 2
 */
public class Main {

    private static final String NB_MAIN_CLASS = "org.netbeans.core.startup.Main";
    private static final String NB_CACHEDIR_OPTION = "--cachedir";
    private static final String NB_USERDIR_OPTION = "--userdir";
    private static final String NB_USERDIR_PROPERTY = "netbeans.user";
    private static final String SNAP_USERDIR_PROPERTY = "snap.userdir";
    private static final String SNAP_CACHEDIR_PROPERTY = "snap.cachedir";

    /**
     * A custom main entry point called from a NetBeans Platform command-line.
     *
     * @param args NetBeans Platform command-line arguments
     */
    public static void main(String[] args) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        System.setProperty("snap.home", Paths.get(getPropertySafe("netbeans.home")).getParent().toString());

        List<String> argList = adjustUserDirArgs(args);
        String userDir = getArg(argList, NB_USERDIR_OPTION);
        System.setProperty(NB_USERDIR_PROPERTY, userDir);
        System.setProperty(SNAP_USERDIR_PROPERTY, userDir);
        System.setProperty(SNAP_CACHEDIR_PROPERTY, getArg(argList, NB_CACHEDIR_OPTION));

        if (Boolean.getBoolean("snap.debug")) {
            dumpEnv(argList);
        }

        runNetBeans(argList, NB_MAIN_CLASS);
    }

    static List<String> adjustUserDirArgs(String... args) {
        List<String> argList = new ArrayList<>(Arrays.asList(args));

        // User directory

        Path userHome = Paths.get(getPropertySafe("user.home"));
        String defaultUserDir = userHome.resolve(".snap").toString();
        String userDir;
        int userDirArgIndex = getArgIndex(argList, NB_USERDIR_OPTION);
        if (userDirArgIndex == -1) {
            userDir = System.getProperty(NB_USERDIR_PROPERTY, defaultUserDir);
            argList.add(NB_USERDIR_OPTION);
            argList.add(userDir);
            userDirArgIndex = argList.size() - 1;
        } else {
            userDir = argList.get(userDirArgIndex);
        }

        // Special rule on Windows OS
        Path nbDefaultWindowsUserDir = userHome.resolve("AppData\\Roaming\\.snap").normalize();
        boolean isWindowsDefaultUserDir = Paths.get(userDir).normalize().equals(nbDefaultWindowsUserDir);
        if (isWindowsDefaultUserDir) {
            userDir = defaultUserDir;
            argList.set(userDirArgIndex, userDir);
        }

        // Cache directory

        String defaultCacheDir = Paths.get(userDir).resolve("var").resolve("cache").toString();
        int cacheDirArgIndex = getArgIndex(argList, NB_CACHEDIR_OPTION);
        String cacheDir;
        if (cacheDirArgIndex == -1) {
            cacheDir = defaultCacheDir;
            argList.add(NB_CACHEDIR_OPTION);
            argList.add(cacheDir);
            cacheDirArgIndex = argList.size() - 1;
        } else {
            cacheDir = argList.get(cacheDirArgIndex);
        }

        // Special rule on Windows OS
        Path nbDefaultWindowsCacheDir = userHome.resolve("AppData\\Roaming\\.snap\\var\\cache").normalize();
        boolean isWindowsDefaultCacheDir = Paths.get(cacheDir).normalize().equals(nbDefaultWindowsCacheDir);
        if (isWindowsDefaultCacheDir) {
            cacheDir = defaultCacheDir;
            argList.set(cacheDirArgIndex, cacheDir);
        }

        return argList;
    }

    static String getArg(List<String> argList, String option) {
        return argList.get(getArgIndex(argList, option));
    }

    private static int getArgIndex(List<String> argList, String option) {
        int index = argList.indexOf(option);
        if (index >= 0 && index < argList.size() - 1) {
            return index + 1;
        }
        return -1;
    }

    private static String getPropertySafe(String key) {
        String value = System.getProperty(key);
        if (value == null) {
            throw new IllegalStateException(String.format("Expecting system property '%s' to be set", key));
        }
        return value;
    }

    private static void dumpEnv(List<String> argList) {
        System.out.println();
        System.out.println("Class: " + Main.class.getName());
        System.out.println();
        System.out.println("Arguments:");
        for (int i = 0; i < argList.size(); i++) {
            System.out.printf("args[%d] = \"%s\"%n", i, argList.get(i));
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

    private static void runNetBeans(List<String> argList, String mainClassName) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        String[] args = argList.toArray(new String[argList.size()]);
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Class<?> nbMainClass = classLoader.loadClass(mainClassName);
        Method nbMainMethod = nbMainClass.getDeclaredMethod("main", String[].class);
        nbMainMethod.invoke(null, new Object[]{args});
    }

}
