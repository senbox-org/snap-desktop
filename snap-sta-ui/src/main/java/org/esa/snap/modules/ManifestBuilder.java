package org.esa.snap.modules;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by kraftek on 11/4/2016.
 */
public class ManifestBuilder extends AbstractBuilder {

    private String version;
    private String javaVersion;
    private Map<String, String> properties;

    public ManifestBuilder() {
        version = "1.0";
        javaVersion = System.getProperty("java.version");
        try {
            javaVersion = javaVersion.substring(0, javaVersion.indexOf("_"));
        }catch(Exception ex){
            javaVersion = javaVersion.substring(0, javaVersion.indexOf("."));
        }
        properties = new HashMap<>();
    }

    public ManifestBuilder version(String value) {
        version = value;
        return this;
    }

    public ManifestBuilder javaVersion(String value) {
        javaVersion = value;
        return this;
    }

    public ManifestBuilder property(String name, String value) {
        properties.putIfAbsent(name, value);
        return this;
    }

    @Override
    public String build(boolean standalone) {
        StringBuilder builder = new StringBuilder();
        builder.append("Manifest-Version: ").append(safeValue(version)).append("\n")
                .append("Created-By: ").append(safeValue(javaVersion)).append("\n");
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            builder.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        return builder.toString();
    }
}
