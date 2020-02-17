package org.esa.snap.rcp.actions.file;

import org.esa.snap.core.metadata.MetadataInspector;
import org.esa.snap.ui.loading.AbstractModalDialog;
import org.esa.snap.ui.loading.ILoadingIndicator;
import org.esa.snap.ui.loading.SwingUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by jcoravu on 17/2/2020.
 */
public class AdvancedProductSubsetDialog extends AbstractModalDialog {

    private JTextField hostNameTextField;
    private JTextField portNumberTextField;
    private JTextField usernameTextField;

    public AdvancedProductSubsetDialog(Window parent, String title) {
        super(parent, title, true, null);
    }

    @Override
    protected void onAboutToShow() {
        readProductMetadataAsync();
    }

    @Override
    protected JPanel buildButtonsPanel(ActionListener cancelActionListener) {
        ActionListener okActionListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                okButtonPressed();
            }
        };
        return buildButtonsPanel("Ok", okActionListener, "Cancel", cancelActionListener);
    }

    @Override
    protected JPanel buildContentPanel(int gapBetweenColumns, int gapBetweenRows) {
        Insets defaultTextFieldMargins = buildDefaultTextFieldMargins();
        Insets defaultListItemMargins = buildDefaultListItemMargins();
        createComponents(defaultTextFieldMargins, defaultListItemMargins);

        JPanel contentPanel = new JPanel(new GridBagLayout());

        GridBagConstraints c = SwingUtils.buildConstraints(0, 0, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, 0, 0);
        contentPanel.add(new JLabel("Host name"), c);
        c = SwingUtils.buildConstraints(1, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, 0, gapBetweenColumns);
        contentPanel.add(this.hostNameTextField, c);

        c = SwingUtils.buildConstraints(0, 1, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, gapBetweenRows, 0);
        contentPanel.add(new JLabel("Port number"), c);
        c = SwingUtils.buildConstraints(1, 1, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, gapBetweenRows, gapBetweenColumns);
        contentPanel.add(this.portNumberTextField, c);

        c = SwingUtils.buildConstraints(0, 2, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, gapBetweenRows, 0);
        contentPanel.add(new JLabel("Username"), c);
        c = SwingUtils.buildConstraints(1, 2, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, gapBetweenRows, gapBetweenColumns);
        contentPanel.add(this.usernameTextField, c);

        c = SwingUtils.buildConstraints(1, 3, GridBagConstraints.BOTH, GridBagConstraints.WEST, 1, 1, 0, gapBetweenColumns);
        contentPanel.add(new JLabel(), c);

        computePanelFirstColumn(contentPanel);

        return contentPanel;
    }

    private void createComponents(Insets defaultTextFieldMargins, Insets defaultListItemMargins) {
        this.hostNameTextField = new JTextField();
        this.hostNameTextField.setMargin(defaultTextFieldMargins);
        this.hostNameTextField.setColumns(30);

        this.portNumberTextField = new JTextField();
        this.portNumberTextField.setMargin(defaultTextFieldMargins);

        this.usernameTextField = new JTextField();
        this.usernameTextField.setMargin(defaultTextFieldMargins);
    }

    private void okButtonPressed() {
    }

    private void readProductMetadataAsync() {
        ILoadingIndicator loadingIndicator = getLoadingIndicator();
        int threadId = getNewCurrentThreadId();
        ReadProductInspectorTimerRunnable runnable = new ReadProductInspectorTimerRunnable(loadingIndicator, threadId) {
            @Override
            protected void onSuccessfullyFinish(MetadataInspector.Metadata result) {
                onSuccessfullyLoadingProductMetadata(result);
            }

            @Override
            protected void onFailed(Exception exception) {
                onFailedLoadingProductMetadata(exception);
            }
        };
        runnable.executeAsync();
    }

    private void onSuccessfullyLoadingProductMetadata(MetadataInspector.Metadata result) {
        showInformationDialog("Successfully loading the product metadata", "Loading metadata");
    }

    private void onFailedLoadingProductMetadata(Exception exception) {
        showErrorDialog("Failed to load the product metadata", "Loading metadata");
    }
}
