package org.esa.snap.modules;

/**
 * Created by kraftek on 11/4/2016.
 */
public class ModuleConfigBuilder extends AbstractBuilder {
    /* xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                    .append("<!DOCTYPE module PUBLIC \"-//NetBeans//DTD Module Status 1.0//EN\"\n\"http://www.netbeans.org/dtds/module-status-1_0.dtd\">\n")
                    .append("<module name=\"")
                    .append(descriptor.getName())
                    .append("\">\n<param name=\"autoload\">false</param><param name=\"eager\">false</param><param name=\"enabled\">true</param>\n")
                    .append("<param name=\"jar\">modules/")
                    .append(jarName)
                    .append("</param><param name=\"reloadable\">false</param>\n</module>");*/

    private String moduleName;
    private String jarName;
    private boolean autoLoad;
    private boolean eager;
    private boolean enabled;
    private boolean reloadable;

    public ModuleConfigBuilder name(String value) {
        moduleName = value;
        return this;
    }

    public ModuleConfigBuilder jarName(String value) {
        jarName = value;
        return this;
    }

    public ModuleConfigBuilder autoLoad(boolean value) {
        autoLoad = value;
        return this;
    }

    public ModuleConfigBuilder eager(boolean value) {
        eager = value;
        return this;
    }

    public ModuleConfigBuilder enabled(boolean value) {
        enabled = value;
        return this;
    }

    public ModuleConfigBuilder reloadable(boolean value) {
        reloadable = value;
        return this;
    }

    @Override
    public String build() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<!DOCTYPE module PUBLIC \"-//NetBeans//DTD Module Status 1.0//EN\"\n\"http://www.netbeans.org/dtds/module-status-1_0.dtd\">\n" +
                "<module name=\"" + safeValue(moduleName) + "\">\n" +
                "<param name=\"autoload\">" + safeValue(autoLoad) + "</param>\n" +
                "<param name=\"eager\">" + safeValue(eager) + "</param>\n" +
                "<param name=\"enabled\">" + safeValue(enabled) + "</param>\n" +
                "<param name=\"jar\">modules/" + safeValue(jarName) + "</param>\n" +
                "<param name=\"reloadable\">" + safeValue(reloadable) + "</param>\n</module>";
    }
}
