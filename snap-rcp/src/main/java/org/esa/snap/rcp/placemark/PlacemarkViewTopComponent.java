package org.esa.snap.rcp.placemark;

import org.esa.snap.core.datamodel.VectorDataNode;
import org.esa.snap.netbeans.docwin.DocumentTopComponent;
import org.esa.snap.netbeans.docwin.WindowUtilities;
import org.esa.snap.ui.product.ProductPlacemarkView;

import java.awt.BorderLayout;

public class PlacemarkViewTopComponent extends DocumentTopComponent<VectorDataNode, ProductPlacemarkView> {

    private final ProductPlacemarkView placemarkView;

    public PlacemarkViewTopComponent(VectorDataNode document) {
        super(document);
        updateDisplayName();
        setName(getDisplayName());
        placemarkView = new ProductPlacemarkView(document);
        setLayout(new BorderLayout());
        add(placemarkView, BorderLayout.CENTER);
    }

    @Override
    public ProductPlacemarkView getView() {
        return placemarkView;
    }

    private void updateDisplayName() {
        setDisplayName(WindowUtilities.getUniqueTitle(getDocument().getDisplayName(),
                                                      PlacemarkViewTopComponent.class));
    }

}
