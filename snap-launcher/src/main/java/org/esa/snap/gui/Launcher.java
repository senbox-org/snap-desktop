package org.esa.snap.gui;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * A plain Java NetBeans Platform launcher.
 * <p/>
 * <i>IMPORTANT NOTE: This launcher may only be used with IDEs other than the NetBeans IDE. It only implements a
 * subset of the functionality the native NetBeans launcher provides. For example, you cannot update plugins
 * and then let the application restart itself.<br/>
 * The recommended way to run/debug applications build on the NetBeans platform is either using the NetBeans
 * Maven plugin (nbm) or using the NetBeans IDE.
 * </i>
 * <p/>
 * Usage:
 * <pre>
 *    Launcher [--branding &lt;app&gt;] [--userdir &lt;userdir&gt;] [--cachedir &lt;cachedir&gt;] &lt;args&gt;
 * </pre>
 * where the options are the same as for the native launcher.
 * The current working directory must be the target deployment directory, {@code $appmodule/target/$app}.
 *
 * @author Norman Fomferra
 */
public class Launcher {

    private final String[] args;
    private final Map<String, String> variables;

    public static void main(String[] args) {
        new Launcher(args).run();
    }

    private Launcher(String[] args) {
        this.args = args;
        this.variables = new HashMap<>();
    }

    private void run() {

        String deploymentDir = abspath("");
        String appName = basename(deploymentDir);

        LinkedList<String> argList = new LinkedList<>(Arrays.asList(args));
        String brandingToken = getArg(argList, "--branding");
        String userDir = getArg(argList, "--userdir");
        String cacheDir = getArg(argList, "--cachedir");

        variables.putAll(System.getenv());
        setVarIfNotSet("APPNAME", appName);
        setVarIfNotSet("HOME", System.getProperty("user.home"));

        String appDir = abspath(deploymentDir, appName);
        String platformDir = abspath(deploymentDir, "platform");
        if (!exists(appDir) || !exists(platformDir)) {
            throw new IllegalStateException("current working directory must be the '" + appName + "' deployment directory");
        }

        String confFile = path(deploymentDir, "etc", appName + ".conf");
        if (exists(confFile)) {
            loadConf(confFile);
        }

        // Parse "default_options"
        List<String> defaultOptionList = new LinkedList<>();
        String defaultOptions = getVar("default_options");
        String defaultBrandingToken = null;
        String defaultUserDir = null;
        String defaultCacheDir = null;
        if (defaultOptions != null) {
            defaultOptionList = parseOptions(defaultOptions);
            defaultBrandingToken = getArg(defaultOptionList, "--branding");
            defaultUserDir = getArg(defaultOptionList, "--userdir");
            defaultCacheDir = getArg(defaultOptionList, "--cachdir");
        }

        if (defaultUserDir == null) {
            /*
            if ("Darwin".equals(System.getProperty("os.name"))) {
                defaultUserDir = getVar("default_mac_userdir");
            } else {
                defaultUserDir = getVar("default_userdir");
            }
            */
            if (defaultUserDir == null) {
                defaultUserDir = path(deploymentDir, "..", "userdir");
            }
        }

        if (userDir == null) {
            userDir = defaultUserDir;
        }

        if (defaultCacheDir == null) {
            defaultCacheDir = path(userDir, "var", "cache");
        }

        if (brandingToken == null) {
            brandingToken = defaultBrandingToken;
        }

        if (cacheDir == null) {
            cacheDir = defaultCacheDir;
        }

        String clustersFile = path(deploymentDir, "etc", appName + ".clusters");
        List<String> clusterList;
        if (exists(clustersFile)) {
            clusterList = readLines(clustersFile);
            clusterList = toAbsolutePaths(clusterList);
        } else {
            clusterList = new ArrayList<>();
        }

        String extraClusterPaths = getVar("extra_clusters");
        if (extraClusterPaths != null) {
            clusterList.add(extraClusterPaths);
        }

        String clusterPaths = toPathsString(clusterList);

        List<URL> classPathList = new ArrayList<>();
        buildClasspath(userDir, classPathList);
        buildClasspath(platformDir, classPathList);

        if ("true".equals(getVar("KDE_FULL_SESSION"))) {
            setPropIfNotSet("netbeans.running.environment", "kde");
        } else if (getVar("GNOME_DESKTOP_SESSION_ID") != null) {
            setPropIfNotSet("netbeans.running.environment", "gnome");
        }

        // todo - address following warning:
        // WARNING [org.netbeans.modules.autoupdate.ui.actions.AutoupdateSettings]: The property "netbeans.default_userdir_root" was not set!

        if (getVar("DEFAULT_USERDIR_ROOT") != null) {
            setPropIfNotSet("netbeans.default_userdir_root", getVar("DEFAULT_USERDIR_ROOT"));
        }

        setPropIfNotSet("netbeans.home", platformDir);
        setPropIfNotSet("netbeans.dirs", clusterPaths);
        setPropIfNotSet("netbeans.logger.console", "true");
        setPropIfNotSet("com.apple.mrj.application.apple.menu.about.name", brandingToken);

        List<String> remainingArgs = extractJavaOptions(argList);
        List<String> remainingDefaultOptions = extractJavaOptions(defaultOptionList);

        setPatchModules(deploymentDir, appDir);

        List<String> newArgList = new ArrayList<>();
        newArgList.add("--branding");
        newArgList.add(brandingToken);
        newArgList.add("--userdir");
        newArgList.add(userDir);
        newArgList.add("--cachedir");
        newArgList.add(cacheDir);
        newArgList.addAll(remainingArgs);
        newArgList.addAll(remainingDefaultOptions);

        runMain(classPathList, newArgList);
    }

    private List<String> extractJavaOptions(List<String> defaultOptionList) {
        List<String> remainingDefaultOptions = new ArrayList<>();
        for (String option : defaultOptionList) {
            if (option.startsWith("-J")) {
                if (option.startsWith("-J-D")) {
                    String kv = option.substring(4);
                    int i = kv.indexOf("=");
                    if (i > 0) {
                        setPropIfNotSet(kv.substring(0, i), kv.substring(i + 1));
                    }
                } else {
                    System.err.printf("WARNING: %s: option '%s' will be ignored, because the JVM is already running%n", getClass(), option);
                }
            } else {
                remainingDefaultOptions.add(option);
            }
        }
        return remainingDefaultOptions;
    }

    /*
     * scan appDir for modules and set system property netbeans.patches.<module>=<module-classes-dir> for each module
     */
    private void setPatchModules(String deploymentDir, String appDir) {
        String appModulesDir = path(appDir, "modules");
        File[] moduleJars = new File(path(appModulesDir)).listFiles(file -> file.getName().toLowerCase().endsWith(".jar"));
        List<String> moduleNames = new ArrayList<>();
        if (moduleJars != null) {
            for (File moduleJar : moduleJars) {
                String moduleFileName = moduleJar.getName();
                String moduleName = moduleFileName.substring(0, moduleFileName.length() - 4);
                moduleNames.add(moduleName);
            }
        }

        File[] projectDirs = new File(path(deploymentDir, "..", "..", "..")).listFiles(file -> file.isDirectory() && !file.getName().startsWith("."));
        if (projectDirs != null) {
            for (File projectDir : projectDirs) {
                String classesDir = path(projectDir.getPath(), "target", "classes");
                if (exists(classesDir)) {
                    String artifactName = projectDir.getName();
                    moduleNames.stream().filter(moduleName -> moduleName.endsWith(artifactName)).forEach(moduleName -> {
                        String propertyName = "netbeans.patches." + moduleName.replace("-", ".");
                        setPropIfNotSet(propertyName, classesDir);
                    });
                }
            }
        }
    }

    private List<String> parseOptions(String defaultOptions) {
        LinkedList<String> defaultOptionList = new LinkedList<>();

        StreamTokenizer st = new StreamTokenizer(new StringReader(defaultOptions));
        st.resetSyntax();
        st.wordChars(' ' + 1, 255);
        st.whitespaceChars(0, ' ');
        st.quoteChar('"');
        st.quoteChar('\'');

        boolean firstArgQuoted = false;
        try {
            int tt = st.nextToken();
            firstArgQuoted = tt == '\'' || tt == '"';
            if (tt != StreamTokenizer.TT_EOF) {
                do {
                    if (st.sval != null) {
                        defaultOptionList.add(st.sval);
                    }
                    tt = st.nextToken();
                } while (tt != StreamTokenizer.TT_EOF);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        if (defaultOptionList.size() == 1 && firstArgQuoted) {
            return parseOptions(defaultOptionList.get(0));
        }

        return defaultOptionList;
    }

    private static void runMain(List<URL> classPathList, List<String> argList) {
        URLClassLoader classLoader = new URLClassLoader(classPathList.toArray(new URL[classPathList.size()]));
        try {
            Class<?> nbMainClass = classLoader.loadClass("org.netbeans.Main");
            Method nbMainMethod = nbMainClass.getDeclaredMethod("main", String[].class);
            nbMainMethod.invoke(null, (Object) argList.toArray(new String[argList.size()]));
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    private void buildClasspath(String base, List<URL> classPathList) {
        appendToClasspath(path(base, "lib", "patches"), classPathList);
        appendToClasspath(path(base, "lib"), classPathList);
        appendToClasspath(path(base, "locale", "locale"), classPathList);
    }

    private void appendToClasspath(String path, List<URL> classPathList) {
        File[] files = new File(path).listFiles(file -> file.isDirectory()
                || file.getName().toLowerCase().endsWith(".jar")
                || file.getName().toLowerCase().endsWith(".zip"));
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    appendToClasspath(file.getPath(), classPathList);
                } else if (file.isFile()) {
                    try {
                        URL url = file.toURI().toURL();
                        classPathList.add(url);
                    } catch (MalformedURLException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
        }
    }

    private void setVarIfNotSet(String varName, String varValue) {
        if (!variables.containsKey(varName)) {
            variables.put(varName, varValue);
        }
    }

    private void setPropIfNotSet(String propertyName, String propertyValue) {
        if (System.getProperty(propertyName) == null) {
            System.setProperty(propertyName, propertyValue);
        }
    }

    private static String toPathsString(List<String> paths) {
        StringBuilder sb = new StringBuilder();
        for (String path : paths) {
            if (sb.length() > 0) {
                sb.append(File.pathSeparatorChar);
            }
            sb.append(path);
        }
        return sb.toString();
    }

    private static List<String> toAbsolutePaths(List<String> paths) {
        ArrayList<String> absPaths = new ArrayList<>();
        for (String path : paths) {
            absPaths.add(Paths.get(path).toAbsolutePath().toString());
        }
        return absPaths;
    }

    private static List<String> readLines(String path) {
        try {
            return Files.readAllLines(Paths.get(path));
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    private static boolean exists(String path) {
        return new File(path).exists();
    }

    private String getArg(List<String> argList, String name) {
        String value = null;
        int i = argList.indexOf(name);
        if (i >= 0 && i + 1 < argList.size()) {
            value = argList.get(i + 1);
            argList.remove(i);
            argList.remove(i);
        }
        return value;
    }

    private String getVar(String name) {
        String value = variables.get(name);
        if (value != null) {
            return resolveString(value, variables);
        }
        return null;
    }

    private void loadConf(String etc) {
        try {
            Properties properties = new Properties();
            try (FileReader reader = new FileReader(etc)) {
                properties.load(reader);
            }
            Set<String> propertyNames = properties.stringPropertyNames();
            for (String propertyName : propertyNames) {
                String propertyValue = properties.getProperty(propertyName);
                variables.put(propertyName, propertyValue);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String resolveString(String text, Map<String, String> variables) {
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            text = text.replace("$" + entry.getKey(), entry.getValue());
            text = text.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        return text;
    }

    private static String path(String first, String... more) {
        return Paths.get(first, more).toString();
    }

    private static String abspath(String first, String... more) {
        return Paths.get(first, more).toAbsolutePath().toString();
    }

    private static String basename(String deploymentDir) {
        return Paths.get(deploymentDir).getFileName().toString();
    }


}
