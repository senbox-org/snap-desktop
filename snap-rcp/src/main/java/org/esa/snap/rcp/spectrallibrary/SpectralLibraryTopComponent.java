package org.esa.snap.rcp.spectrallibrary;

import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductManager;
import org.esa.snap.core.datamodel.ProductNodeEvent;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.ProductNodeListenerAdapter;
import org.esa.snap.core.datamodel.VectorDataNode;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.spectrallibrary.util.SpectralLibraryActionBinder;
import org.esa.snap.rcp.spectrallibrary.util.ViewModelBinder;
import org.esa.snap.rcp.spectrallibrary.controller.SpectralLibraryController;
import org.esa.snap.rcp.spectrallibrary.model.SpectralLibraryViewModel;
import org.esa.snap.rcp.spectrallibrary.ui.SpectralLibraryPanel;
import org.esa.snap.rcp.windows.ToolTopComponent;
import org.esa.snap.speclib.model.SpectralLibrary;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import org.openide.util.HelpCtx;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;


@TopComponent.Description(
        preferredID = "SpectralLibraryTopComponent2",
        iconBase = "org/esa/snap/rcp/icons/Spectrum.gif"
)
@TopComponent.Registration(mode = "SpectralLibrary", openAtStartup = false, position = 85)
@ActionID(category = "Window", id = "org.esa.snap.rcp.spectrallibrary.SpectralLibraryTopComponent2")
@ActionReferences({
        @ActionReference(path = "Menu/Optical", position = 1),
        @ActionReference(path = "Menu/View/Tool Windows"),
        @ActionReference(path = "Toolbars/Tool Windows")
})
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_SpectralLibraryTopComponent_Name",
        preferredID = "SpectralLibraryTopComponent2"
)
@NbBundle.Messages({
        "CTL_SpectralLibraryTopComponent_Name=Spectral Library",
        "CTL_SpectralLibraryTopComponent_HelpId=showSpectralLibraryTool"
})
public class SpectralLibraryTopComponent extends ToolTopComponent {


    private final SpectralLibraryViewModel vm = new SpectralLibraryViewModel();
    private final SpectralLibraryPanel panel = new SpectralLibraryPanel(this);

    private final SpectralLibraryController controller = new SpectralLibraryController(vm, panel.getPreviewPanel());

    private final ViewModelBinder vmBinder = new ViewModelBinder(vm, panel);
    private final SpectralLibraryActionBinder actionBinder = new SpectralLibraryActionBinder(vm, controller, panel, this::getSelectedProductSceneView);
    private final ProductManager.Listener productManagerListener = new PreviewProductManagerListener();
    private final ProductNodeListenerAdapter productNodeListener = new PreviewProductNodeListener();
    private final Set<Product> observedProducts = Collections.newSetFromMap(new IdentityHashMap<>());


    public SpectralLibraryTopComponent() {
        setDisplayName(Bundle.CTL_SpectralLibraryTopComponent_Name());
        setLayout(new BorderLayout(4, 4));
        add(panel, BorderLayout.CENTER);

        panel.getLibraryCombo().setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof SpectralLibrary lib) {
                    setText(lib.getName());
                }
                return this;
            }
        });

        vmBinder.bind();
        actionBinder.bind();
    }


    @Override
    protected void componentOpened() {
        controller.init();
    }

    @Override
    public void componentShowing() {
        ProductManager productManager = SnapApp.getDefault().getProductManager();
        productManager.addListener(productManagerListener);
        for (Product product : productManager.getProducts()) {
            observeProduct(product);
        }
    }

    @Override
    public void componentHidden() {
        ProductManager productManager = SnapApp.getDefault().getProductManager();
        productManager.removeListener(productManagerListener);
        for (Product product : Set.copyOf(observedProducts)) {
            unobserveProduct(product);
        }
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(Bundle.CTL_SpectralLibraryTopComponent_HelpId());
    }

    private void observeProduct(Product product) {
        if (product != null && observedProducts.add(product)) {
            product.addProductNodeListener(productNodeListener);
        }
    }

    private void unobserveProduct(Product product) {
        if (product != null && observedProducts.remove(product)) {
            product.removeProductNodeListener(productNodeListener);
        }
    }

    private Product productFromEvent(ProductNodeEvent event) {
        Product product = event.getSourceNode().getProduct();
        if (product == null) {
            ProductNodeGroup group = event.getGroup();
            if (group != null) {
                product = group.getProduct();
            }
        }
        return product;
    }

    private static Set<String> featureIdsFromValue(Object value) {
        Set<String> ids = new HashSet<>();
        collectFeatureIds(value, ids);
        return ids;
    }

    private static Map<String, String> featureGeometryWktsFromValue(Object value) {
        Map<String, String> geometryWkts = new LinkedHashMap<>();
        collectFeatureGeometryWkts(value, geometryWkts);
        return geometryWkts;
    }

    private static void collectFeatureIds(Object value, Set<String> ids) {
        if (value instanceof SimpleFeature feature) {
            if (feature.getID() != null) {
                ids.add(feature.getID());
            }
            return;
        }
        if (value instanceof SimpleFeature[] features) {
            for (SimpleFeature feature : features) {
                collectFeatureIds(feature, ids);
            }
            return;
        }
        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                collectFeatureIds(item, ids);
            }
        }
    }

    private static void collectFeatureGeometryWkts(Object value, Map<String, String> geometryWkts) {
        if (value instanceof SimpleFeature feature) {
            if (feature.getID() != null) {
                geometryWkts.put(feature.getID(), geometryWktOf(feature));
            }
            return;
        }
        if (value instanceof SimpleFeature[] features) {
            for (SimpleFeature feature : features) {
                collectFeatureGeometryWkts(feature, geometryWkts);
            }
            return;
        }
        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                collectFeatureGeometryWkts(item, geometryWkts);
            }
        }
    }

    private static String geometryWktOf(SimpleFeature feature) {
        Object geometry = feature.getDefaultGeometry();
        return geometry instanceof Geometry ? ((Geometry) geometry).toText() : null;
    }

    private class PreviewProductManagerListener implements ProductManager.Listener {
        @Override
        public void productAdded(ProductManager.Event event) {
            observeProduct(event.getProduct());
        }

        @Override
        public void productRemoved(ProductManager.Event event) {
            Product product = event.getProduct();
            controller.removePreviewProfilesForProduct(product);
            unobserveProduct(product);
        }
    }

    private class PreviewProductNodeListener extends ProductNodeListenerAdapter {
        @Override
        public void nodeChanged(ProductNodeEvent event) {
            if (event.getSourceNode() instanceof VectorDataNode vectorDataNode
                    && VectorDataNode.PROPERTY_NAME_FEATURE_COLLECTION.equals(event.getPropertyName())) {
                if (event.getOldValue() != null && event.getNewValue() == null) {
                    Set<String> featureIds = featureIdsFromValue(event.getOldValue());
                    if (!featureIds.isEmpty()) {
                        controller.removePreviewProfilesForGeometryFeatures(vectorDataNode.getProduct(), vectorDataNode.getName(), featureIds);
                    }
                } else if (event.getOldValue() != null && event.getNewValue() != null) {
                    Map<String, String> geometryWkts = featureGeometryWktsFromValue(event.getNewValue());
                    if (!geometryWkts.isEmpty()) {
                        controller.removePreviewProfilesForChangedGeometryFeatures(vectorDataNode.getProduct(), vectorDataNode.getName(), geometryWkts);
                    }
                }
            }
        }

        @Override
        public void nodeRemoved(ProductNodeEvent event) {
            if (event.getSourceNode() instanceof VectorDataNode vectorDataNode) {
                controller.removePreviewProfilesForVectorDataNode(productFromEvent(event), vectorDataNode);
            } else if (event.getSourceNode() instanceof Placemark placemark) {
                controller.removePreviewProfilesForPin(productFromEvent(event), placemark.getName());
            }
        }
    }

}
