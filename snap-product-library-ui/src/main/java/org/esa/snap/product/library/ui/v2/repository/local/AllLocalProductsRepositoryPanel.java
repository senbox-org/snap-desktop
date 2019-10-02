package org.esa.snap.product.library.ui.v2.repository.local;

import org.esa.snap.product.library.ui.v2.ComponentDimension;
import org.esa.snap.product.library.ui.v2.repository.RepositorySelectionPanel;
import org.esa.snap.product.library.ui.v2.worldwind.WorldWindowPanelWrapper;
import org.esa.snap.product.library.v2.database.LocalRepositoryFolder;

import javax.swing.JButton;
import java.awt.Dimension;

/**
 * Created by jcoravu on 5/8/2019.
 */
public class AllLocalProductsRepositoryPanel extends AbstractLocalProductsRepositoryPanel {

    private final JButton addFolderButton;

    public AllLocalProductsRepositoryPanel(ComponentDimension componentDimension, WorldWindowPanelWrapper worlWindPanel) {
        super(componentDimension, worlWindPanel);

        Dimension buttonSize = new Dimension(componentDimension.getTextFieldPreferredHeight(), componentDimension.getTextFieldPreferredHeight());

        this.addFolderButton = RepositorySelectionPanel.buildButton("/org/esa/snap/resources/images/icons/Add16.png", null, buttonSize, 1);
        this.addFolderButton.setToolTipText("Add new local folder");

        this.scanFoldersButton.setToolTipText("Scan all local folders");
        this.removeFoldersButton.setToolTipText("Remove all local folders");
    }

    @Override
    public JButton[] getTopBarButton() {
        return new JButton[] {this.scanFoldersButton, this.addFolderButton, this.removeFoldersButton};
    }

    @Override
    protected LocalRepositoryFolder getLocalRepositoryFolder() {
        return null;
    }

    @Override
    public String getName() {
        return "All Local Folders";
    }
}
