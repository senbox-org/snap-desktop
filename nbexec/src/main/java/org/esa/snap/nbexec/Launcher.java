package org.esa.snap.nbexec;

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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * A plain Java NetBeans Platform launcher which mimics the core functionality of the NB's native launcher
 * {@code nbexec}. Can be used for easy debugging of NB Platform (Maven) applications when using the NB IDE is not
 * an option.
 * <p>
 * <i>IMPORTANT NOTE: This launcher only implements a subset of the functionality the native NetBeans
 * launcher {@code nbexec} provides. For example, you cannot update plugins and then let the application
 * restart itself.<br/>
 * The recommended way to run/debug applications build on the NetBeans platform is either using the NetBeans
 * Maven plugin (via {@code nbm:run-platform}) or directly using the NetBeans IDE.
 * </i>
 * <p>
 * Usage:
 * <pre>
 *    Launcher {--root &lt;root&gt;} [--branding &lt;app&gt;] [--userdir &lt;userdir&gt;] [--cachedir &lt;cachedir&gt;] &lt;args&gt;
 * </pre>
 * where the {@code branding}, {@code userdir}, and {@code cachedir} options are the same as for the native launcher.
 * The current working directory must be the target deployment directory, {@code $appmodule/target/$app}.
 * <p>
 * The Launcher takes care of any changed code in all modules contained in a module <i>root</i> indicated by the given
 * {@code root} options. Every root must contain a single wildcard ($). The module root which
 * includes the application module, namely {@code $appmodule/../../../$/target/classes}<br/> is always included.
 * <p/>
 * So, In IntelliJ IDEA we can hit CTRL+F9
 * and then run/debug the Launcher.
 * <p>
 * This is enabled for all modules which are
 * <ul>
 * <li>(a) found in the applications target cluster (e.g. modules with {@code nbm} packaging) and </li>
 * <li>(b) have a valid target/classes output directory.</li>
 * </ul>
 * We may later want to be able to further configure this default behaviour. See code for how the current
 * strategy is implemented.
 *
 * @author Norman Fomferra
 * @version 1.0
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
        String brandingToken = parseArg(argList, "--branding");
        String userDir = parseArg(argList, "--userdir");
        String cacheDir = parseArg(argList, "--cachedir");

        // Collect project dirs.
        // Default is "../../.." which refers to a Maven specific directory layout.
        Set<Root> rootDirs = parseRoots(argList);

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
            defaultBrandingToken = parseArg(defaultOptionList, "--branding");
            defaultUserDir = parseArg(defaultOptionList, "--userdir");
            defaultCacheDir = parseArg(defaultOptionList, "--cachedir");
        }

        if (defaultUserDir == null) {
            // From nbexec:
            /*
            if ("Darwin".equals(System.getProperty("os.name"))) {
                defaultUserDir = getVar("default_mac_userdir");
            } else {
                defaultUserDir = getVar("default_userdir");
            }
            */
            // .. but not used here because our default is the nbm standard location
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

        List<String> remainingDefaultOptions = parseJavaOptions(defaultOptionList, false);
        List<String> remainingArgs = parseJavaOptions(argList, true);

        setPatchModules(appDir, rootDirs);

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

    private Set<Root> parseRoots(LinkedList<String> argList) {
        try {
            Set<Root> rootDirs = new LinkedHashSet<>();
            // Add Maven-specific output directory for application module
            processRoot("../../../$/target/classes", rootDirs);
            while (true) {
                String rootDirPath = parseArg(argList, "--root");
                if (rootDirPath != null) {
                    processRoot(rootDirPath, rootDirs);
                } else {
                    break;
                }
            }
            return rootDirs;
        } catch (IOException e) {
            System.err.println("ERROR: " + e.getMessage());
            System.exit(1);
            return null;
        }
    }

    private void processRoot(String rootDirPath, Set<Root> rootDirs) throws IOException {
        int wcPos = rootDirPath.indexOf("$");
        if (wcPos >= 0) {
            String subPath = rootDirPath.substring(wcPos + 1);
            subPath = subPath.replace('/', File.separatorChar);
            if (subPath.startsWith(File.separator)) {
                subPath = subPath.substring(1);
            }
            rootDirs.add(new Root(new File(rootDirPath.substring(0, wcPos)).getCanonicalFile(), subPath));
        } else {
            throw new IllegalArgumentException("module root must contain a single wildcard '$'");
        }
    }

    private List<String> parseJavaOptions(List<String> defaultOptionList, boolean fail) {
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
                    String msg = String.format("configured option '%s' will be ignored, because the JVM is already running", option);
                    if (fail) {
                        throw new IllegalArgumentException(msg);
                    }
                    warn(msg);
                }
            } else {
                remainingDefaultOptions.add(option);
            }
        }
        return remainingDefaultOptions;
    }

    int patchCount = 0;

    /*
     * scan appDir for modules and set system property netbeans.patches.<module>=<module-classes-dir> for each module
     */
    private void setPatchModules(String appDir, Set<Root> roots) {
        String modulesDir = path(appDir, "modules");
        File[] moduleJars = new File(path(modulesDir)).listFiles(file -> file.getName().toLowerCase().endsWith(".jar"));
        List<String> moduleNames = new ArrayList<>();
        if (moduleJars != null) {
            for (File moduleJar : moduleJars) {
                String moduleFileName = moduleJar.getName();
                String moduleName = moduleFileName.substring(0, moduleFileName.length() - 4);
                info("candidate patch-providing module in development: " + moduleName);
                moduleNames.add(moduleName);
            }
        }

        for (Root root : roots) {
            patchCount = 0;
            File[] projectDirs = root.dir.listFiles(file -> file.isDirectory() && !file.getName().startsWith("."));
            if (projectDirs != null) {
                for (File projectDir : projectDirs) {
                    //info("checking '" + projectDir + "'");

                    // check - generify: "target/classes" is a Maven specific output path
                    File classesDir = new File(projectDir, root.subPath);
                    if (classesDir.isDirectory()) {
                        String projectDirName = projectDir.getName();
                        // determine version pos
                        int versionPos = -1;
                        for (int i = 0; i < projectDirName.length() - 1; i++) {
                            if (projectDirName.charAt(i) == '-' && Character.isDigit(projectDirName.charAt(i + 1))) {
                                versionPos = i;
                                break;
                            }
                        }

                        // strip version part
                        String artifactName = versionPos > 0 ?  projectDirName.substring(0, versionPos) : projectDirName;

                        //info("checking if artifact '" + artifactName + "' has output directory " + classesDir);

                        // check - generify: we assume that
                        //    <project-dir>/<module-dir>  --> <cluster-dir>/modules/<module-dir>.jar
                        // but this pattern is specific to the NB Maven Plugin

                        // 1. module names that end with artifact name
                        moduleNames.stream().filter(moduleName -> moduleName.endsWith(artifactName)).forEach(moduleName -> {
                            addPatch(moduleName, classesDir);
                        });

                        // 2. module names that don't end with artifact name, but contain artifact name
                        moduleNames.stream().filter(moduleName -> !moduleName.endsWith(artifactName) && moduleName.contains(artifactName)).forEach(moduleName -> {
                            addPatch(moduleName, classesDir);
                        });
                    }
                }
            }
            if (patchCount == 0) {
                warn("no module patches found for module root " + root);
            } else {
                info(patchCount + " module patch(es) found for module root " + root);
            }
        }

    }

    private void addPatch(String moduleName, File classesDir) {
        String propertyName = "netbeans.patches." + moduleName.replace("-", ".");
        setPropIfNotSet(propertyName, classesDir.getPath());
        patchCount++;
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
                        info("added to application classpath: " + file);
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

    private void setPropIfNotSet(String name, String value) {
        String oldValue = System.getProperty(name);
        if (oldValue == null) {
            info("setting system property: " + name + " = " + value);
            System.setProperty(name, value);
        } else {
            warn("not overriding existing system property: " + name + " = " + oldValue + "(new value: " + value + ")");
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

    private String parseArg(List<String> argList, String name) {
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

    private void loadConf(String path) {
        info("reading configuration from " + path);
        try {
            Properties properties = new Properties();
            try (FileReader reader = new FileReader(path)) {
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

    private void info(String msg) {
        System.out.printf("INFO: %s: %s%n", getClass(), msg);
    }

    private void warn(String msg) {
        System.err.printf("WARNING: %s: %s%n", getClass(), msg);
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

    private class Root {
        final File dir;
        final String subPath;

        private Root(File dir, String subPath) {
            this.dir = dir;
            String s = subPath.replace('/', File.separatorChar);
            if (s.startsWith(File.separator)) {
                s = s.substring(1);
            }
            this.subPath = s;
        }

        @Override
        public String toString() {
            if (subPath.isEmpty()) {
                return dir + File.separator + "$";
            }
            return dir + File.separator + "$" + File.separator + subPath;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Root root = (Root) o;
            return dir.equals(root.dir) && subPath.equals(root.subPath);

        }

        @Override
        public int hashCode() {
            int result = dir.hashCode();
            result = 31 * result + subPath.hashCode();
            return result;
        }
    }


}
