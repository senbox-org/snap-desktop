package org.esa.snap.ui.help;

import eu.esa.snap.netbeans.javahelp.api.Help;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;

import java.awt.Toolkit;

/**
 * Helper class to invoke the help system.
 */
public class HelpDisplayer {
    /**
     * Invoke the help system with the provided help ID.
     *
     * @param helpId the help ID to be shown, if null the default help will be shown
     */
    public static void show(String helpId) {
        show(helpId != null ? new HelpCtx(helpId) : null);
    }

    /**
     * Invoke the help system with the provided help context.
     *
     * @param helpCtx the context to be shown, if null the default help will be shown
     */
    public static void show(HelpCtx helpCtx) {
        Help helpImpl = Lookup.getDefault().lookup(Help.class);
        if (helpImpl == null) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        helpImpl.showHelp(helpCtx);
    }


}
