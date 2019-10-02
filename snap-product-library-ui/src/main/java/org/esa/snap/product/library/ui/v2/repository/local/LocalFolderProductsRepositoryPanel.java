package org.esa.snap.product.library.ui.v2.repository.local;

import org.esa.snap.product.library.ui.v2.ComponentDimension;
import org.esa.snap.product.library.ui.v2.worldwind.WorldWindowPanelWrapper;
import org.esa.snap.product.library.v2.database.LocalRepositoryFolder;

import javax.swing.JButton;

/**
 * Created by jcoravu on 5/8/2019.
 */
public class LocalFolderProductsRepositoryPanel extends AbstractLocalProductsRepositoryPanel {

    private final LocalRepositoryFolder localRepositoryFolder;

    public LocalFolderProductsRepositoryPanel(ComponentDimension componentDimension, WorldWindowPanelWrapper worlWindPanel, LocalRepositoryFolder localRepositoryFolder) {
        super(componentDimension, worlWindPanel);

        this.localRepositoryFolder = localRepositoryFolder;

        this.scanFoldersButton.setToolTipText("Scan local folder");
        this.removeFoldersButton.setToolTipText("Remove local folder");
    }

    @Override
    public JButton[] getTopBarButton() {
        return new JButton[] {this.scanFoldersButton, this.removeFoldersButton};
    }

    @Override
    public LocalRepositoryFolder getLocalRepositoryFolder() {
        return this.localRepositoryFolder;
    }

    @Override
    public String getName() {
        return this.localRepositoryFolder.getPath().toString();
    }
}
