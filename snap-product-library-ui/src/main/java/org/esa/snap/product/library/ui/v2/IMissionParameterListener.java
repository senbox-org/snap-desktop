package org.esa.snap.product.library.ui.v2;

import org.esa.snap.product.library.ui.v2.repository.AbstractProductsRepositoryPanel;

/**
 * Created by jcoravu on 21/8/2019.
 */
public interface IMissionParameterListener {

    public void newSelectedMission(String mission, AbstractProductsRepositoryPanel parentDataSource);
}
