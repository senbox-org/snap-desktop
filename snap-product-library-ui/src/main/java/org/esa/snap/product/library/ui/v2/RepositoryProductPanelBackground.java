package org.esa.snap.product.library.ui.v2;

import org.esa.snap.product.library.ui.v2.repository.AbstractRepositoryProductPanel;
import org.esa.snap.remote.products.repository.RepositoryProduct;

import java.awt.Color;
import java.util.Map;

/**
 * Created by jcoravu on 27/9/2019.
 */
public interface RepositoryProductPanelBackground {

    public Map<String, String> getRemoteMissionVisibleAttributes(String mission);

    public Color getProductPanelBackground(AbstractRepositoryProductPanel productPanel);

    public RepositoryProduct getProductPanelItem(AbstractRepositoryProductPanel repositoryProductPanel);
}
