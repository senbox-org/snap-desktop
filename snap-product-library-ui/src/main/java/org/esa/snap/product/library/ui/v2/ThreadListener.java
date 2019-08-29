package org.esa.snap.product.library.ui.v2;

import org.esa.snap.product.library.ui.v2.repository.AbstractProductsRepositoryPanel;

/**
 * Created by jcoravu on 28/8/2019.
 */
public interface ThreadListener {

    public void onStopExecuting(AbstractProductsRepositoryPanel productsRepositoryPanel);
}
