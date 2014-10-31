package org.esa.snap.gui;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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
 * @author Norman Fomferra
 */
public class Launcher {


    public static final String APPNAME = "snap";
    private final String[] args;
    private final Map<String, String> variables;

    public Launcher(String[] args) {
        this.args = args;
        variables = new HashMap<>();
    }

    public static void main(String[] args) {
        new Launcher(args).run();
    }

    private void run() {
        variables.putAll(System.getenv());
        variables.put("APPNAME", APPNAME);
        variables.put("HOME", System.getProperty("user.home"));

        // temp
        String appDir = abspath("target", APPNAME);
        if (!exists(appDir)) {
            throw new IllegalStateException("invalid working directory");
        }

        String confFile = path(appDir, "etc", APPNAME + ".conf");
        if (exists(confFile)) {
            loadConf(variables, confFile);
        }

        LinkedList<String> argList = new LinkedList<>(Arrays.asList(args));
        String userDir = getArg(argList, "--userdir");
        if (userDir == null) {
            if (System.getProperty("os.name").equals("Darwin")) {
                userDir = getVar("default_mac_userdir");
            } else {
                userDir = getVar("default_userdir");
            }
            if (userDir != null) {
                userDir = path("target", "userdir");
            }
        }

        String clustersFile = path(appDir, "etc", APPNAME + ".clusters");
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

        List<String> newArgs = new ArrayList<>();
        newArgs.add("-J-Dcom.apple.mrj.application.apple.menu.about.name="+APPNAME);
        newArgs.add("-J-Xdock:name="+APPNAME);
        newArgs.add("-J-Xdock:icon="+path(appDir, APPNAME + ".icns"));
        newArgs.add("--clusters");
        newArgs.add("\"" + clusterPaths + "\"");
        newArgs.add("--userDir");
        newArgs.add("\"" + userDir + "\"");

        runNbExec(newArgs, argList, getVar("default_options"));
    }

    private void runNbExec(List<String> newArgs, LinkedList<String> oldArgs, String defaultOptions) {
        System.out.println("newArgs = " + Arrays.toString(newArgs.toArray()));
        System.out.println("oldArgs = " + Arrays.toString(oldArgs.toArray()));
        System.out.println("defaultOptions = " + defaultOptions);
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

    private String getArg(LinkedList<String> argList, String name) {
        String value = null;
        int i = argList.indexOf(name);
        if (i > 0 && i + 1 < argList.size()) {
            value = argList.get(i + 1);
            argList.remove(i);
            argList.remove(i);
        }
        return value;
    }

    private static boolean loadConf(Map<String, String> variables, String etc) {
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
            return false;
        }
        return true;
    }

    private String getVar(String name) {
        String value = variables.get(name);
        if (value != null) {
            return resolveString(value, variables);
        }
        return null;
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
}
