/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.windows;

import com.bc.ceres.glayer.support.ImageLayer;
import org.esa.snap.core.datamodel.*;
import org.locationtech.jts.geom.Point;
import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.pixelinfo.PixelInfoView;
import org.esa.snap.ui.PackageDefaults;
import org.esa.snap.ui.PixelPositionListener;
import org.esa.snap.ui.product.ProductSceneView;
import org.netbeans.api.annotations.common.NonNull;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import javax.swing.JCheckBox;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Experimental top component which displays information about selected pixel.
 */
@TopComponent.Description(
        preferredID = "PixelInfoTopComponent",
        iconBase = "org/esa/snap/rcp/icons/PixelInfo.png",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = PackageDefaults.PIXEL_INFO_MODE,
                           openAtStartup = PackageDefaults.PIXEL_INFO_OPEN,
                           position = PackageDefaults.PIXEL_INFO_POSITION
)
@ActionID(category = "Window", id = "org.esa.snap.rcp.window.PixelInfoTopComponent")
@ActionReference(path = "Menu/View/Tool Windows", position = 0)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_PixelInfoTopComponentName",
        preferredID = "PixelInfoTopComponent"
)
@NbBundle.Messages({
                           "CTL_PixelInfoTopComponentName=" + PackageDefaults.PIXEL_INFO_NAME,
                           "CTL_PixelInfoTopComponentDescription=Displays information about current pixel",
                   })
public final class PixelInfoTopComponent extends ToolTopComponent {

    private ProductSceneView currentView;

    private PixelPositionListener pixelPositionListener;
    private final PixelInfoView pixelInfoView;
    private final PinSelectionChangeListener pinSelectionChangeListener;
    private final PinChangedListener pinChangedListener;
    private final JCheckBox pinCheckbox;

    public PixelInfoTopComponent() {
        setName(Bundle.CTL_PixelInfoTopComponentName());
        setToolTipText(Bundle.CTL_PixelInfoTopComponentDescription());
        putClientProperty(TopComponent.PROP_MAXIMIZATION_DISABLED, Boolean.TRUE);
        putClientProperty(TopComponent.PROP_KEEP_PREFERRED_SIZE_WHEN_SLIDED_IN, Boolean.TRUE);
        pixelPositionListener = new MyPixelPositionListener();
        pinSelectionChangeListener = new PinSelectionChangeListener();
        pinChangedListener = new PinChangedListener();
        pixelInfoView = new PixelInfoView();
        pinCheckbox = new JCheckBox("Snap to selected pin");
        pinCheckbox.setName("pinCheckbox");
        pinCheckbox.setSelected(false);
        pinCheckbox.addActionListener(e -> updatePixelInfo());
        setLayout(new BorderLayout());
        add(pixelInfoView, BorderLayout.CENTER);
        add(pinCheckbox, BorderLayout.SOUTH);

        final SnapApp snapApp = SnapApp.getDefault();
        snapApp.getProductManager().addListener(new PxInfoProductManagerListener());
        setCurrentView(snapApp.getSelectedProductSceneView());
    }

    @Override
    protected void productSceneViewSelected(@NonNull ProductSceneView view) {
        setCurrentView(view);
        if (isSnapToSelectedPin()) {
            snapToSelectedPin();
        }
    }

    @Override
    protected void productSceneViewDeselected(@NonNull ProductSceneView view) {
        setCurrentView(null);
        if (isSnapToSelectedPin()) {
            snapToSelectedPin();
        }
    }

    private void setCurrentView(ProductSceneView view) {
        if (currentView == view) {
            return;
        }
        if (currentView != null) {
            currentView.removePixelPositionListener(pixelPositionListener);
            currentView.removePropertyChangeListener(ProductSceneView.PROPERTY_NAME_SELECTED_PIN,
                                                     pinSelectionChangeListener);
            Product product = currentView.getProduct();
            if (product != null) {
                product.removeProductNodeListener(pinChangedListener);
            }
        } else {
            pixelInfoView.clearProductNodeRefs();
        }
        currentView = view;
        if (currentView != null) {
            currentView.addPixelPositionListener(pixelPositionListener);
            currentView.addPropertyChangeListener(ProductSceneView.PROPERTY_NAME_SELECTED_PIN,
                                                  pinSelectionChangeListener);
            Product product = currentView.getProduct();
            if (product != null) {
                product.addProductNodeListener(pinChangedListener);
            }
        } else {
            pixelInfoView.reset();
        }
    }

    private void updatePixelInfo() {
        if (isSnapToSelectedPin()) {
            SwingUtilities.invokeLater(this::snapToSelectedPin);
        } else {
            pixelInfoView.updatePixelValues(currentView, -1, -1, 0, false);
        }
    }

    private boolean isSnapToSelectedPin() {
        return pinCheckbox.isSelected();
    }

    private void snapToSelectedPin() {
        final Placemark pin = currentView != null ? currentView.getSelectedPin() : null;
        if (pin != null) {
            //todo [multisize_products] replace this very ugly code by using the scene raster transformer - tf 20151113
            PixelPos rasterPos = new PixelPos();
            final Point pinSceneCoords = (Point) pin.getFeature().getDefaultGeometry();
            final Point2D.Double pinSceneCoordsDouble = new Point2D.Double(pinSceneCoords.getX(), pinSceneCoords.getY());
            try {
                currentView.getRaster().getImageToModelTransform().createInverse().transform(pinSceneCoordsDouble, rasterPos);
            } catch (NoninvertibleTransformException e) {
                rasterPos = pin.getPixelPos();
            }
            final int x = MathUtils.floorInt(rasterPos.x);
            final int y = MathUtils.floorInt(rasterPos.y);
            pixelInfoView.updatePixelValues(currentView, x, y, 0, true);
        } else {
            pixelInfoView.updatePixelValues(currentView, -1, -1, 0, false);
        }
    }

    private class PinSelectionChangeListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (isVisible()) {
                updatePixelInfo();
            }
        }
    }

    private class PinChangedListener implements ProductNodeListener {

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            if (Placemark.PROPERTY_NAME_PIXELPOS.equals(event.getPropertyName())) {
                handlePinEvent(event);
            }
        }

        @Override
        public void nodeDataChanged(ProductNodeEvent event) {
            handlePinEvent(event);
        }

        @Override
        public void nodeAdded(ProductNodeEvent event) {
            handlePinEvent(event);
        }

        @Override
        public void nodeRemoved(ProductNodeEvent event) {
            handlePinEvent(event);
        }

        private void handlePinEvent(ProductNodeEvent event) {
            if (currentView != null
                    && event.getSourceNode() == currentView.getSelectedPin()) {
                updatePixelInfo();
            }
        }
    }

    private class MyPixelPositionListener implements PixelPositionListener {
        @Override
        public void pixelPosChanged(ImageLayer baseImageLayer, int pixelX, int pixelY, int currentLevel, boolean pixelPosValid, MouseEvent e) {
            if (isActive()) {
                pixelInfoView.updatePixelValues(currentView, pixelX, pixelY, currentLevel, pixelPosValid);
            }
        }

        @Override
        public void pixelPosNotAvailable() {
            //do nothing
        }

        private boolean isActive() {
            return isVisible() && !isSnapToSelectedPin();
        }
    }

    private class PxInfoProductManagerListener implements ProductManager.Listener {
        @Override
        public void productAdded(ProductManager.Event event) {
            // nothing to do here
        }

        @Override
        public void productRemoved(ProductManager.Event event) {
            pixelInfoView.reset();
        }
    }
}
