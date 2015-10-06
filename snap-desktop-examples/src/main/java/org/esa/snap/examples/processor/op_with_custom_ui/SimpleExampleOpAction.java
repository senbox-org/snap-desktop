package org.esa.snap.examples.processor.op_with_custom_ui;

import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import java.awt.event.ActionEvent;

@ActionID(category = "Processors", id = "org.esa.snap.examples.gpf.dialog.SimpleExampleOpAction")
@ActionRegistration(displayName = "#CTL_SimpleExampleOpActionText", lazy = false)
@ActionReference(path = "Menu/Raster/Examples", position = 10)
@NbBundle.Messages({
        "CTL_SimpleExampleOpActionText=Simple Example Processor",
        "CTL_SimpleExampleOpActionDescription=Simple example processor with custom user interface."
})
public class SimpleExampleOpAction extends AbstractSnapAction {
    public static final String HELP_ID = "sampleScientificTool";

    public SimpleExampleOpAction() {
        setHelpId(HELP_ID);
        putValue(NAME, Bundle.CTL_SimpleExampleOpActionText());
        putValue(SHORT_DESCRIPTION, Bundle.CTL_SimpleExampleOpActionDescription());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final OperatorMetadata opMetadata = SimpleExampleOp.class.getAnnotation(OperatorMetadata.class);
        final SimpleExampleDialog operatorDialog = new SimpleExampleDialog(opMetadata.alias(), getAppContext(),
                                                                           Bundle.CTL_SimpleExampleOpActionText(),
                                                                           HELP_ID);
        operatorDialog.getJDialog().pack();
        operatorDialog.show();
    }

}
