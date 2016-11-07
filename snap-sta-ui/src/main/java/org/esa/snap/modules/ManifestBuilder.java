package org.esa.snap.modules;

/**
 * Created by kraftek on 11/4/2016.
 */
public class ManifestBuilder extends AbstractBuilder {

    private String version;
    private String javaVersion;

    public ManifestBuilder() {
        version = "1.0";
        javaVersion = System.getProperty("java.version");
        javaVersion = javaVersion.substring(0, javaVersion.indexOf("_"));
    }

    public ManifestBuilder version(String value) {
        version = value;
        return this;
    }

    public ManifestBuilder javaVersion(String value) {
        javaVersion = value;
        return this;
    }

    @Override
    public String build() {
        return "Manifest-Version: " + safeValue(version) + "\n" + "Created-By: " + safeValue(javaVersion) + "\n";
    }
}
