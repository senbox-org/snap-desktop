package org.esa.snap.product.library.ui.v2.repository.local;

import org.esa.snap.ui.loading.AbstractModalDialog;
import org.esa.snap.ui.loading.SwingUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * The dialog window to allow the user to select the options for scanning a local repository.
 *
 * Created by jcoravu on 20/7/2020.
 */
public class ScanLocalRepositoryOptionsDialog extends AbstractModalDialog {

    private JCheckBox scanFolderRecursivelyCheckBox;
    private JCheckBox generateQuickLookImagesCheckBox;
    private JCheckBox testZipFilesForErrorsCheckBox;

    public ScanLocalRepositoryOptionsDialog(Window parent) {
        super(parent, "Scan folder options", true, null);
    }

    @Override
    protected JPanel buildContentPanel(int gapBetweenColumns, int gapBetweenRows) {
        this.scanFolderRecursivelyCheckBox = new JCheckBox("Search folder recursively");
        this.generateQuickLookImagesCheckBox = new JCheckBox("Generate quick look images");
        this.testZipFilesForErrorsCheckBox = new JCheckBox("Test zip files for errors");

        JPanel contentPanel = new JPanel(new GridBagLayout());

        GridBagConstraints c = SwingUtils.buildConstraints(0, 0, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, 0, 0);
        contentPanel.add(this.scanFolderRecursivelyCheckBox, c);
        c = SwingUtils.buildConstraints(0, 1, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, gapBetweenRows, 0);
        contentPanel.add(this.generateQuickLookImagesCheckBox, c);
        c = SwingUtils.buildConstraints(0, 2, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, gapBetweenRows, 0);
        contentPanel.add(this.testZipFilesForErrorsCheckBox, c);
        c = SwingUtils.buildConstraints(0, 3, GridBagConstraints.BOTH, GridBagConstraints.WEST, 1, 1, 0, 0);
        contentPanel.add(new JLabel(), c);

        return contentPanel;
    }

    @Override
    protected JPanel buildButtonsPanel(ActionListener cancelActionListener) {
        ActionListener okActionListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                okButtonPressed(scanFolderRecursivelyCheckBox.isSelected(), generateQuickLookImagesCheckBox.isSelected(), testZipFilesForErrorsCheckBox.isSelected());
            }
        };
        return buildButtonsPanel("Ok", okActionListener, "Cancel", cancelActionListener);
    }

    protected void okButtonPressed(boolean scanRecursively, boolean generateQuickLookImages, boolean testZipFileForErrors) {
        getJDialog().dispose();
    }
}
