package org.esa.snap.rcp.windows;

import org.esa.beam.framework.ui.product.ProductSceneView;


public interface ProductSceneViewSelectionChangeListener {
    void sceneViewSelected(ProductSceneView first, ProductSceneView... more);
    void sceneViewDeselected(ProductSceneView first, ProductSceneView... more);
}
