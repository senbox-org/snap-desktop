package org.esa.snap.modules;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Builder class for Nbm/Info/Info.xml
 *
 * @author  Cosmin Cara
 */
public class InfoBuilder extends AbstractBuilder {

    private String codebaseName;
    private String distribution;
    private int downloadSize;
    private String homePage;
    private boolean needsRestart;
    private Date releaseDate;
    private boolean isEssentialModule;
    private boolean showInClient;
    private String moduleName;
    private String longDescription;
    private String shortDescription;
    private String displayCategory;
    private String implementationVersion;
    private String specificationVersion;
    private String javaVersion;
    private Map<String, String> dependencies;

    public InfoBuilder() { this.dependencies = new HashMap<>(); }

    public InfoBuilder codebase(String value) {
        codebaseName = value;
        return this;
    }

    public InfoBuilder distribution(String value) {
        distribution = value;
        return this;
    }

    public InfoBuilder downloadSize(int value) {
        downloadSize = value;
        return this;
    }

    public InfoBuilder homePage(String value) {
        homePage = value;
        return this;
    }

    public InfoBuilder longDescription(String value) {
        longDescription = value;
        return this;
    }

    public InfoBuilder shortDescription(String value) {
        shortDescription = value;
        return this;
    }

    public InfoBuilder displayCategory(String value) {
        displayCategory = value;
        return this;
    }

    public InfoBuilder implementationVersion(String value) {
        implementationVersion = value;
        return this;
    }

    public InfoBuilder specificationVersion(String value) {
        specificationVersion = value;
        return this;
    }

    public InfoBuilder javaVersion(String value) {
        javaVersion = value;
        return this;
    }

    public InfoBuilder dependency(String name, String version) {
        dependencies.put(name, version);
        return this;
    }

    public InfoBuilder releaseDate(Date value) {
        releaseDate = value;
        return this;
    }

    public InfoBuilder needsRestart(boolean value) {
        needsRestart = value;
        return this;
    }

    public InfoBuilder isEssentialModule(boolean value) {
        isEssentialModule = value;
        return this;
    }

    public InfoBuilder showInClient(boolean value) {
        showInClient = value;
        return this;
    }

    public InfoBuilder moduleName(String value) {
        moduleName = value;
        return this;
    }

    @Override
    public String build(boolean standalone) {
        StringBuilder xmlBuilder = new StringBuilder();
        if (standalone) {
            xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                    .append("<!DOCTYPE module PUBLIC \"-//NetBeans//DTD Autoupdate Module Info 2.5//EN\" \"http://www.netbeans.org/dtds/autoupdate-info-2_5.dtd\">");
        }
        xmlBuilder.append("<module codenamebase=\"").append(safeValue(codebaseName).toLowerCase()).append("\" ")
                .append("distribution=\"").append(safeValue(distribution)).append("\" ")
                .append("downloadsize=\"").append(downloadSize).append("\" ")
                .append("homepage=\"").append(safeValue(homePage)).append("\" ")
                .append("needsrestart=\"").append(safeValue(needsRestart)).append("\" ")
                .append("releasedate=\"").append(new SimpleDateFormat("yyyy/MM/dd").format(releaseDate != null ? releaseDate : new Date())).append("\">\n")
                .append("<manifest AutoUpdate-Essential-Module=\"").append(safeValue(isEssentialModule)).append("\" ")
                .append("AutoUpdate-Show-In-Client=\"").append(safeValue(showInClient)).append("\" ")
                .append("OpenIDE-Module=\"").append(safeValue(moduleName)).append("\" ")
                .append("OpenIDE-Module-Display-Category=\"").append(safeValue(displayCategory)).append("\" ")
                .append("OpenIDE-Module-Implementation-Version=\"").append(safeValue(implementationVersion)).append("\" ")
                .append("OpenIDE-Module-Java-Dependencies=\"Java &gt; ").append(safeValue(javaVersion)).append("\" ")
                .append("OpenIDE-Module-Long-Description=\"&lt;p&gt;").append(safeValue(longDescription)).append("&lt;/p&gt;\" ")
                .append("OpenIDE-Module-Module-Dependencies=\"");
        String depValues = "";
        for (Map.Entry<String, String> entry : dependencies.entrySet()) {
            depValues += entry.getKey() + " &gt; " + entry.getValue() + ", ";
        }
        if (depValues.length() > 0) {
            depValues = depValues.substring(0, depValues.length() - 2);
        }
        xmlBuilder.append(depValues);
        xmlBuilder.append("\" ")
                .append("OpenIDE-Module-Name=\"").append(moduleName).append("\" ")
                .append("OpenIDE-Module-Requires=\"org.openide.modules.ModuleFormat1\" ")
                .append("OpenIDE-Module-Short-Description=\"").append(shortDescription).append("\" ")
                .append("OpenIDE-Module-Specification-Version=\"").append(safeValue(specificationVersion)).append("\"/>\n</module>");
        return xmlBuilder.toString();
    }
}
