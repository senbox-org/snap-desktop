/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.rcp.placemark;

import com.bc.ceres.swing.figure.FigureEditorInteractor;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.core.datamodel.PlacemarkDescriptor;
import org.esa.snap.core.datamodel.PlacemarkGroup;
import org.esa.snap.core.datamodel.PlacemarkNameFactory;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.ui.product.ProductSceneView;
import org.opengis.referencing.operation.TransformException;
import org.openide.awt.UndoRedo;

import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;

/**
 * Interactor fort inserting pins and GCPs.
 *
 * @author Norman Fomferra
 */
public abstract class InsertPlacemarkInteractor extends FigureEditorInteractor {

    private final PlacemarkDescriptor placemarkDescriptor;
    private final Cursor cursor;
    private boolean started;

    protected InsertPlacemarkInteractor(PlacemarkDescriptor placemarkDescriptor) {
        this.placemarkDescriptor = placemarkDescriptor;
        this.cursor = createCursor();
    }

    @Override
    public Cursor getCursor() {
        return cursor;
    }

    @Override
    public void mousePressed(MouseEvent event) {
        started = false;
        ProductSceneView sceneView = getProductSceneView(event);
        if (sceneView != null) {
            started = startInteraction(event);
        }
    }

    public PlacemarkDescriptor getPlacemarkDescriptor() {
        return placemarkDescriptor;
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        if (started) {
            ProductSceneView sceneView = getProductSceneView(event);
            if (sceneView != null) {
                sceneView.selectVectorDataLayer(placemarkDescriptor.getPlacemarkGroup(sceneView.getProduct()).getVectorDataNode());
                if (isSingleButton1Click(event)) {
                    insertPlacemark(sceneView);
                }
                stopInteraction(event);
            }
        }
    }

    protected void insertPlacemark(ProductSceneView view) {
        Product product = view.getProduct();
        final String[] uniqueNameAndLabel = PlacemarkNameFactory.createUniqueNameAndLabel(placemarkDescriptor,
                product);
        final String name = uniqueNameAndLabel[0];
        final String label = uniqueNameAndLabel[1];
        PixelPos rasterPos = new PixelPos(view.getCurrentPixelX() + 0.5f, view.getCurrentPixelY() + 0.5f);
        Point2D modelPos = view.getRaster().getImageToModelTransform().transform(rasterPos, new Point2D.Double());
        Point2D scenePos = new Point2D.Double();
        try {
            view.getRaster().getModelToSceneTransform().transform(modelPos, scenePos);
            final AffineTransform sceneToImage = Product.findImageToModelTransform(product.getSceneGeoCoding()).createInverse();
            rasterPos = (PixelPos) sceneToImage.transform(modelPos, new PixelPos());
        } catch (TransformException | NoninvertibleTransformException e) {
            Dialogs.showError("Could not place pin in image due to transformation exception: " + e.getMessage());
            return;
        }
        final Placemark newPlacemark = Placemark.createPointPlacemark(placemarkDescriptor, name, label, "",
                rasterPos, null, product.getSceneGeoCoding());
        PlacemarkGroup placemarkGroup = placemarkDescriptor.getPlacemarkGroup(product);
        String defaultStyleCss = placemarkGroup.getVectorDataNode().getDefaultStyleCss();
        updatePlacemarkStyle(newPlacemark, defaultStyleCss);

        placemarkGroup.add(newPlacemark);
        UndoRedo.Manager undoManager = SnapApp.getDefault().getUndoManager(product);
        if (undoManager != null) {
            undoManager.addEdit(UndoablePlacemarkActionFactory.createUndoablePlacemarkInsertion(product, newPlacemark, placemarkDescriptor));
        }
    }

    protected abstract void updatePlacemarkStyle(Placemark placemark, String defaultStyleCss);

    private Cursor createCursor() {
        final Image cursorImage = placemarkDescriptor.getCursorImage();
        if (cursorImage == null) {
            return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
        }
        return Toolkit.getDefaultToolkit().createCustomCursor(cursorImage,
                placemarkDescriptor.getCursorHotSpot(),
                placemarkDescriptor.getRoleName());
    }

    private ProductSceneView getProductSceneView(MouseEvent event) {
        final Component eventComponent = event.getComponent();
        if (eventComponent instanceof ProductSceneView) {
            return (ProductSceneView) eventComponent;
        }
        final Container parentComponent = eventComponent.getParent();
        if (parentComponent instanceof ProductSceneView) {
            return (ProductSceneView) parentComponent;
        }
        // Case: Scroll bars are displayed
        if (parentComponent.getParent() instanceof ProductSceneView) {
            return (ProductSceneView) parentComponent.getParent();
        }

        return null;
    }

}
