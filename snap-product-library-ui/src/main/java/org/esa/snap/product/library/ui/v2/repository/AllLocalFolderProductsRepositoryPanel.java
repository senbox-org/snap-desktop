package org.esa.snap.product.library.ui.v2.repository;

import org.esa.snap.product.library.ui.v2.ComponentDimension;
import org.esa.snap.product.library.ui.v2.worldwind.WorldWindowPanelWrapper;
import org.esa.snap.product.library.v2.AllLocalFolderProductsRepository;

import java.awt.GridBagLayout;

/**
 * Created by jcoravu on 5/8/2019.
 */
public class AllLocalFolderProductsRepositoryPanel extends AbstractProductsRepositoryPanel {

    private final AllLocalFolderProductsRepository allLocalFolderProductsRepository;

    public AllLocalFolderProductsRepositoryPanel(ComponentDimension componentDimension, WorldWindowPanelWrapper worlWindPanel) {
        super(worlWindPanel, componentDimension, new GridBagLayout());

        this.allLocalFolderProductsRepository = new AllLocalFolderProductsRepository();
    }

    @Override
    public String getName() {
        return "All Local Folders";
    }

    @Override
    protected void addParameterComponents() {
        addParameterComponents(this.allLocalFolderProductsRepository.getParameters(), 0, 0);
    }
}
