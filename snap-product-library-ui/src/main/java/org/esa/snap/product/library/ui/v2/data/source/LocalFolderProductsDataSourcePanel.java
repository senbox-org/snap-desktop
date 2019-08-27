package org.esa.snap.product.library.ui.v2.data.source;

import java.awt.BorderLayout;
import java.nio.file.Path;

/**
 * Created by jcoravu on 5/8/2019.
 */
public class LocalFolderProductsDataSourcePanel extends AbstractProductsDataSourcePanel {

    private final Path localFolderPath;

    public LocalFolderProductsDataSourcePanel(Path localFolderPath) {
        super(new BorderLayout());

        this.localFolderPath = localFolderPath;
    }

    @Override
    public String getName() {
        return this.localFolderPath.toString();
    }
}
