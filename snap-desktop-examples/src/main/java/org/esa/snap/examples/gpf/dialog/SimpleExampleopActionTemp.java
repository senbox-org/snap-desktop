package org.esa.snap.examples.gpf.dialog;

import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import java.awt.event.ActionEvent;

@ActionID(category = "Processors", id = "org.esa.snap.examples.gpf.dialog.SimpleExampleOpAction")
@ActionRegistration(displayName = "#CTL_SimpleExampleOpActionText", lazy = false)
@ActionReference(path = "Menu/Examples/Processing", position = 10000)
@NbBundle.Messages({
        "CTL_SimpleExampleOpActionText=Simple Example Processor",
        "CTL_SimpleExampleOpActionDescription=Simple example processor with custom user interface."
})
public class SimpleExampleopActionTemp extends AbstractSnapAction {
    public static final String HELP_ID = "sampleScientificTool";

    public SimpleExampleopActionTemp() {
        setHelpId(HELP_ID);
        putValue(NAME, Bundle.CTL_SimpleExampleOpActionText());
        putValue(SHORT_DESCRIPTION, Bundle.CTL_SimpleExampleOpActionDescription());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final OperatorMetadata opMetadata = SimpleExampleOp.class.getAnnotation(OperatorMetadata.class);
        final SimpleExampleDialog operatorDialog = new SimpleExampleDialog(opMetadata.alias(), getAppContext(),
                                                                           "Simple Example of a Simple Processor",
                                                                           HELP_ID);
        operatorDialog.getJDialog().pack();
        operatorDialog.show();
    }

}
