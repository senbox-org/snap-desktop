package org.esa.snap.grapheditor.ui;

import java.awt.Dimension;

import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.ModelessDialog;

/**
 * Simple back compatibility class.
 *
 * @author Martino Ferrari (CS Group)
 */
public class GraphBuilderDialog extends ModelessDialog {
 
    public GraphBuilderDialog(final AppContext theAppContext, final String title, final String helpID){
        this(theAppContext, title, helpID, true);
    }

    private GraphBuilderDialog(final AppContext theAppContext, final String title, final String helpID, final boolean allowGraphBuilding) {
        super(theAppContext.getApplicationWindow(), title, 0, helpID);
        super.getJDialog().setMinimumSize(new Dimension(1200, 800));

        this.setContent(new GraphBuilder(theAppContext.getApplicationWindow(), theAppContext));
    }
}