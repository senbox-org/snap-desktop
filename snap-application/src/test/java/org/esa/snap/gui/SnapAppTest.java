package org.esa.snap.gui;

import com.bc.ceres.core.ResourceLocator;
import junit.framework.Test;
import org.netbeans.junit.NbModuleSuite;
import org.netbeans.junit.NbTestCase;
import org.openide.modules.ModuleInfo;
import org.openide.util.Lookup;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Locale;
import java.util.logging.Level;

public class SnapAppTest extends NbTestCase {
    static {
        Locale.setDefault(Locale.ENGLISH);
    }

    public static Test suite() {
        return NbModuleSuite.createConfiguration(SnapAppTest.class).
                gui(true).
                failOnMessage(Level.WARNING). // works at least in RELEASE71
                failOnException(Level.INFO).
                enableClasspathModules(false).
                clusters(".*").
                suite(); // RELEASE71+, else use NbModuleSuite.create(NbModuleSuite.createConfiguration(...))
    }

    public SnapAppTest(String n) {
        super(n);
    }

    public void testApplication() throws IOException {
        // pass if there are merely no warnings/exceptions
        /* Example of using Jelly Tools (additional test dependencies required) with gui(true):
        new ActionNoBlock("Help|About", null).performMenu();
        new NbDialogOperator("About").closeByButton();
         */

        Collection<? extends ModuleInfo> modules = Lookup.getDefault().lookupAll(ModuleInfo.class);
        for (ModuleInfo module : modules) {
            System.out.println("module.getDisplayName() = " + module.getDisplayName());
        }

/*
        ClassLoader globalClassLoader = Lookup.getDefault().lookup(ClassLoader.class);
        System.out.println("globalClassLoader = " + globalClassLoader);

        Enumeration<URL> resources = globalClassLoader.getResources("/META-INF/MANIFEST.MF");
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            System.out.println("url = " + url);
        }
*/

        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");

        Collection<Path> resources = ResourceLocator.getResources("META-INF/MANIFEST.MF");
        for (Path path : resources) {
            System.out.println("path = " + path.toUri());
        }
        System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");

    }

}
