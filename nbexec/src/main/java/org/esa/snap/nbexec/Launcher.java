package org.esa.snap.nbexec;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A plain Java NetBeans Platform launcher which mimics the core functionality of the NB's native launcher
 * {@code nbexec}. Can be used for easy debugging of NB Platform (Maven) applications when using the NB IDE is not
 * an option.
 * <p>
 * <i>IMPORTANT NOTE: This launcher only implements a subset of the functionality the native NetBeans
 * launcher {@code nbexec} provides. For example, you cannot update plugins and then let the application
 * restart itself.<br>
 * The recommended way to run/debug applications build on the NetBeans platform is either using the NetBeans
 * Maven plugin (via {@code nbm:run-platform}) or directly using the NetBeans IDE.
 * </i>
 * <p>
 * Usage:
 * <pre>
 *    Launcher [--patches &lt;patches&gt;] [--clusters &lt;clusters&gt;] [--branding &lt;app&gt;]
 *             [--userdir &lt;userdir&gt;] [--cachedir &lt;cachedir&gt;] &lt;args&gt;
 * </pre>
 * where the {@code clusters}, {@code branding}, {@code userdir}, and {@code cachedir} options are the same as
 * for the native launcher.
 * The current working directory must be the target deployment directory, {@code $appmodule/target/$app}.
 * <p>
 * The Launcher takes care of any changed code in modules indicated by the <i>patches</i> patterns given by the
 * {@code patches} option. the <i>patches</i> patterns may contain multiple patterns separated by a semicolon (;)
 * on Windows systems and a colon (:) on Unixes. Every patch pattern must contain a single wildcard character ($).
 * The default patch pattern is {@code $appmodule/../../../$/target/classes}<br> and is always included.
 * <p>
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

    private static final String CLUSTERS_EXT = ".clusters";

    // Command-line arguments
    private final String[] args;

    // Contains all environment variables and all variables from ${some-dir}/${app-name}/etc/${app-name}.conf
    private final Map<String, String> configuration;

    public static void main(String[] args) {
        new Launcher(args).run();
    }

    private Launcher(String[] args) {
        this.args = args;
        this.configuration = new HashMap<>();
    }

    private void run() {

        Path installationDir = Paths.get("").toAbsolutePath();

        Path etcDir = installationDir.resolve("etc");
        Path platformDir = installationDir.resolve("platform");
        if (!Files.isDirectory(etcDir) || !Files.isDirectory(platformDir)) {
            throw new IllegalStateException("Not a valid installation directory: " + installationDir);
        }

        LinkedList<String> argList = new LinkedList<>(Arrays.asList(args));
        String clusterDirs = parseArg(argList, "--clusters");
        String brandingToken = parseArg(argList, "--branding");
        String userDir = parseArg(argList, "--userdir");
        String cacheDir = parseArg(argList, "--cachedir");

        // Collect project dirs.
        // Default is "../../../$/target/classes" which refers to a Maven specific directory layout:
        //
        // ${parent-1}/
        //     pom.xml
        //     ${nb-app-module-dir}/
        //         pom.xml
        //         src/
        //         target/
        //             ${app}    // -> must be current working directory
        //     ${nb-nbm-module-dir-1}/
        //     ${nb-nbm-module-dir-2}/
        //     ${nb-nbm-module-dir-3}/
        //     ...
        // ${parent-2}/
        //     pom.xml
        //     ${nb-nbm-module-dir-1}/
        //     ${nb-nbm-module-dir-2}/
        //     ...
        //
        Set<Patch> patches = parseClusterPatches(argList);
        List<Path> clustersFiles;
        try (Stream<Path> etcFiles = Files.list(etcDir)) {
            clustersFiles = etcFiles
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(CLUSTERS_EXT))
                    .collect(Collectors.toList());
            if (clustersFiles.isEmpty()) {
                throw new IllegalStateException(String.format("no '*.clusters' file found in '%s'", etcDir));
            } else if (clustersFiles.size() > 1) {
                throw new IllegalStateException(String.format("multiple '*.clusters' files found in '%s'", etcDir));
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        Path clustersFile = clustersFiles.get(0);
        String clustersFileName = clustersFile.getFileName().toString();
        String appName = clustersFileName.substring(0, clustersFileName.length() - CLUSTERS_EXT.length());

        Path confFile = etcDir.resolve(appName + ".conf");

        configuration.putAll(System.getenv());
        setConfigurationVariableIfNotSet("APPNAME", appName);
        setConfigurationVariableIfNotSet("HOME", System.getProperty("user.home"));

        if (Files.isRegularFile(confFile)) {
            loadConf(confFile);
        }

        // Parse "default_options"
        List<String> defaultOptionList = new LinkedList<>();
        String defaultOptions = getVar("default_options");
        String defaultClusterDirs = null;
        String defaultBrandingToken = null;
        String defaultUserDir = null;
        String defaultCacheDir = null;
        if (defaultOptions != null) {
            defaultOptionList = parseOptions(defaultOptions);
            defaultClusterDirs = parseArg(defaultOptionList, "--clusters");
            defaultBrandingToken = parseArg(defaultOptionList, "--branding");
            defaultUserDir = parseArg(defaultOptionList, "--userdir");
            defaultCacheDir = parseArg(defaultOptionList, "--cachedir");
        }

        if (defaultUserDir == null) {
            // From nbexec:
            if ("Darwin".equals(System.getProperty("os.name"))) {
                defaultUserDir = getVar("default_mac_userdir");
            } else {
                defaultUserDir = getVar("default_userdir");
            }

            // .. but not used here because our default is the nbm standard location
            if (defaultUserDir == null) {
                defaultUserDir = installationDir.resolve("..").resolve("userdir").toString();
            }
        }

        if (clusterDirs == null) {
            clusterDirs = defaultClusterDirs;
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

        List<String> clusterList = readLines(clustersFile);
        clusterList = toAbsolutePaths(clusterList);

        String extraClusterPaths = getVar("extra_clusters");
        if (extraClusterPaths != null) {
            clusterList.add(extraClusterPaths);
        }

        if (clusterDirs != null) {
            clusterList.addAll(toAbsolutePaths(Arrays.asList(clusterDirs.split(File.pathSeparator))));
        }

        String clusterPaths = toPathsString(clusterList);

        List<URL> classPathList = new ArrayList<>();
        buildClasspath(userDir, classPathList);
        buildClasspath(platformDir.toString(), classPathList);

        if ("true".equals(getVar("KDE_FULL_SESSION"))) {
            setSystemPropertyIfNotSet("netbeans.running.environment", "kde");
        } else if (getVar("GNOME_DESKTOP_SESSION_ID") != null) {
            setSystemPropertyIfNotSet("netbeans.running.environment", "gnome");
        }

        // todo - address following warning:
        // WARNING [org.netbeans.modules.autoupdate.ui.actions.AutoupdateSettings]: The property "netbeans.default_userdir_root" was not set!

        if (getVar("DEFAULT_USERDIR_ROOT") != null) {
            setSystemPropertyIfNotSet("netbeans.default_userdir_root", getVar("DEFAULT_USERDIR_ROOT"));
        }

        setSystemPropertyIfNotSet("netbeans.home", platformDir.toString());
        setSystemPropertyIfNotSet("netbeans.dirs", clusterPaths);
        setSystemPropertyIfNotSet("netbeans.logger.console", "true");
        setSystemPropertyIfNotSet("com.apple.mrj.application.apple.menu.about.name", brandingToken);

        List<String> remainingDefaultOptions = parseJavaOptions(defaultOptionList, false);
        List<String> remainingArgs = parseJavaOptions(argList, true);

        setPatchModules(clusterList, patches);

        List<String> newArgList = new ArrayList<>();
        newArgList.add("--branding");
        newArgList.add(brandingToken);
        newArgList.add("--userdir");
        newArgList.add(userDir);
        newArgList.add("--cachedir");
        newArgList.add(cacheDir);
        newArgList.addAll(remainingArgs);
        newArgList.addAll(remainingDefaultOptions);

        Path restartMarkerFile = Paths.get(userDir, "var", "restart");
        try {
            Files.deleteIfExists(restartMarkerFile);
        } catch (IOException e) {
            // So what?
        }


        Path restartExeFile;
        try (Stream<Path> list = Files.list(installationDir.resolve("bin"))) {
            restartExeFile = list
                    .filter(Files::isExecutable)
                    .filter(p -> p.getFileName().toString().startsWith("restart."))
                    .findFirst().orElse(null);
        } catch (Exception e) {
            restartExeFile = null;
        }

        final Path _restartExeFile = restartExeFile;

        if (_restartExeFile != null) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (Files.exists(restartMarkerFile)) {
                    String processName = ManagementFactory.getRuntimeMXBean().getName();
                    Logger.getLogger("").info("Shut down: " + processName);
                    String pid = processName.split("@")[0];
                    try {
                        new ProcessBuilder().command(_restartExeFile.toString(), pid).start();
                    } catch (IOException e) {
                        Logger.getLogger("").log(Level.SEVERE, "Failed to restart: " + _restartExeFile, e);
                        //SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "Failed to restart:\n" + e.getMessage()));
                    }
                }
            }));
        }

        runMain(classPathList, newArgList);


    }

    private Set<Patch> parseClusterPatches(LinkedList<String> argList) {
        Set<Patch> patches = new LinkedHashSet<>();
        // Add Maven-specific output directory for application module
        //patches.add(Patch.parse("../../../$/target/classes"));
        while (true) {
            String patchPatterns = parseArg(argList, "--patches");
            if (patchPatterns != null) {
                String[] patterns = patchPatterns.split(File.pathSeparator);
                for (String pattern : patterns) {
                    patches.add(Patch.parse(pattern));
                }
            } else {
                break;
            }
        }
        return patches;
    }

    private List<String> parseJavaOptions(List<String> defaultOptionList, boolean fail) {
        List<String> remainingDefaultOptions = new ArrayList<>();
        for (String option : defaultOptionList) {
            if (option.startsWith("-J")) {
                if (option.startsWith("-J-D")) {
                    String kv = option.substring(4);
                    int i = kv.indexOf("=");
                    if (i > 0) {
                        setSystemPropertyIfNotSet(kv.substring(0, i), kv.substring(i + 1));
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
    private void setPatchModules(List<String> clusterList, Set<Patch> patches) {

        String JAR_EXT = ".jar";
        List<String> moduleNames = new ArrayList<>();
        for (String clusterDir : clusterList) {
            Path clusterModulesDir = Paths.get(clusterDir).resolve("modules");
            try {
                Files.list(clusterModulesDir).forEach(path -> {
                    String fileName = path.getFileName().toString();
                    if (fileName.endsWith(JAR_EXT)) {
                        String moduleName = fileName.substring(0, fileName.length() - JAR_EXT.length());
                        //info("candidate patch-providing module in development: " + moduleName);
                        moduleNames.add(moduleName);
                    }
                });
            } catch (IOException e) {
                warn("failed to list entries of " + clusterModulesDir);
            }
        }

        for (Patch patch : patches) {
            patchCount = 0;

            Path parentSourceDir = patch.dir;
            if (Files.isDirectory(parentSourceDir)) {
                try {
                    List<Path> moduleSourceDirs = Files.list(parentSourceDir)
                            .filter(moduleSourceDir -> Files.isDirectory(moduleSourceDir))
                            .collect(Collectors.toList());

                    for (Path moduleSourceDir : moduleSourceDirs) {
                        addPatchForModuleSourceDir(moduleSourceDir, moduleNames, patch);
                    }
                } catch (IOException e) {
                    warn("failed to list entries of " + parentSourceDir);
                }

                if (patchCount == 0 && parentSourceDir.getFileName() != null) {
                    // Maybe patch points to single-module project directory, so let's see
                    addPatchForModuleSourceDir(parentSourceDir, moduleNames, patch);
                }
            }

            if (patchCount == 0) {
                warn("no module patches found for pattern " + patch);
            } else {
                info(patchCount + " module patch(es) found for pattern " + patch);
            }
        }

    }

    private boolean addPatchForModuleSourceDir(Path moduleSourceDir, List<String> moduleNames, Patch patch) {
        String moduleSourceName = moduleSourceDir.getFileName().toString();
        if (!moduleSourceName.startsWith(".")) {
            //info("checking '" + moduleSourceDir + "'");
            Path modulePatchDir = moduleSourceDir.resolve(patch.subPath);
            if (Files.isDirectory(modulePatchDir)) {
                //info("checking if artifact '" + artifactName + "' has output directory " + classesDir);
                for (String moduleName : moduleNames) {
                    if (moduleName.endsWith(moduleSourceName)) {
                        addPatch(moduleName, modulePatchDir);
                        return true;
                    }
                }
                for (String moduleName : moduleNames) {
                    if (moduleName.contains(moduleSourceName)) {
                        addPatch(moduleName, modulePatchDir);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void addPatch(String moduleName, Path classesDir) {
        String propertyName = "netbeans.patches." + moduleName.replace("-", ".");
        setSystemPropertyIfNotSet(propertyName, classesDir.toString());
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

        boolean firstArgQuoted;
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
        try {
            Files.list(Paths.get(path)).forEach(file -> {
                if (Files.isDirectory(file)) {
                    appendToClasspath(file.toString(), classPathList);
                } else if (Files.isRegularFile(file)) {
                    String s = file.getFileName().toString().toLowerCase();
                    if (s.endsWith(".jar") || s.endsWith(".zip")) {
                        try {
                            URL url = file.toUri().toURL();
                            classPathList.add(url);
                            info("added to application classpath: " + file);
                        } catch (MalformedURLException e) {
                            throw new IllegalStateException(e);

                        }
                    }
                }
            });
        } catch (IOException e) {
            warn("failed to list entries of " + path);
        }
    }

    private void setConfigurationVariableIfNotSet(String varName, String varValue) {
        if (!configuration.containsKey(varName)) {
            configuration.put(varName, varValue);
        }
    }

    private void setSystemPropertyIfNotSet(String name, String value) {
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
        return paths.stream()
                .map(path -> Paths.get(path).toAbsolutePath().toString())
                .collect(Collectors.toList());
    }

    private static List<String> readLines(Path path) {
        try {
            return Files.readAllLines(path);
        } catch (IOException e) {
            return Collections.emptyList();
        }
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
        String value = configuration.get(name);
        if (value != null) {
            return resolveString(value, configuration);
        }
        return null;
    }

    private void loadConf(Path path) {
        info("reading configuration from " + path);
        try {
            Properties properties = new Properties();
            try (Reader reader = Files.newBufferedReader(path)) {
                properties.load(reader);
            }
            Set<String> propertyNames = properties.stringPropertyNames();
            for (String propertyName : propertyNames) {
                String propertyValue = properties.getProperty(propertyName);
                if (propertyValue.startsWith("\"") && propertyValue.endsWith("\"")) {
                    propertyValue = propertyValue.substring(1, propertyValue.length() - 1);
                }
                configuration.put(propertyName, propertyValue);
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

    public static class Patch {
        public static final char WILDCARD_CHAR = '$';

        private final Path dir;
        private final String subPath;

        public static Patch parse(String pattern) {
            int wcPos = pattern.indexOf(WILDCARD_CHAR);
            if (wcPos >= 0) {
                String subPath = pattern.substring(wcPos + 1);
                if (subPath.startsWith(File.separator) || subPath.startsWith("/")) {
                    subPath = subPath.substring(1);
                }
                if (subPath.indexOf(WILDCARD_CHAR) > 0) {
                    throw new IllegalArgumentException(String.format("patch pattern must contain a single wildcard '%s': %s",
                                                                     WILDCARD_CHAR, pattern));
                }
                return new Patch(Paths.get(pattern.substring(0, wcPos)).toAbsolutePath().normalize(), subPath);
            } else {
                throw new IllegalArgumentException(String.format("patch pattern must contain wildcard '%s': %s",
                                                                 WILDCARD_CHAR, pattern));
            }
        }

        private Patch(Path dir, String subPath) {
            this.dir = dir;
            this.subPath = subPath;
        }

        public Path getDir() {
            return dir;
        }

        public String getSubPath() {
            return subPath;
        }

        @Override
        public String toString() {
            if (subPath.isEmpty()) {
                return dir + File.separator + WILDCARD_CHAR;
            }
            return dir + File.separator + WILDCARD_CHAR + File.separator + subPath;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Patch patch = (Patch) o;
            return dir.equals(patch.dir) && subPath.equals(patch.subPath);
        }

        @Override
        public int hashCode() {
            int result = dir.hashCode();
            result = 31 * result + subPath.hashCode();
            return result;
        }
    }


}
