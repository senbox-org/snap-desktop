package org.esa.snap.product.library.ui.v2;

import org.esa.snap.product.library.ui.v2.repository.AbstractProductsRepositoryPanel;

/**
 * The listener interface for receiving the event when the mission is changed in tre combo box component.
 *
 * Created by jcoravu on 21/8/2019.
 */
public interface MissionParameterListener {

    public void newSelectedMission(String mission, AbstractProductsRepositoryPanel parentDataSource);
}
