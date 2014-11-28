/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.gui.actions.tools;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.Validator;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerFilter;
import com.bc.ceres.glayer.support.LayerUtils;
import com.bc.ceres.swing.binding.PropertyPane;
import org.esa.beam.framework.datamodel.PlainFeatureFactory;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.product.VectorDataLayerFilterFactory;
import org.esa.beam.jai.ImageManager;
import org.esa.snap.gui.SnapApp;
import org.esa.snap.gui.nodes.PNodeFactory;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.awt.UndoRedo;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle.Messages;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.undo.AbstractUndoableEdit;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.text.MessageFormat;

@ActionID(
        category = "Tools",
        id = "org.esa.snap.gui.action.tools.CreateVectorDataNodeAction"
)
@ActionRegistration(
        displayName = "#CTL_CreateVectorDataNodeActionText",
        popupText = "#CTL_CreateVectorDataNodeActionPopupText",
        iconBase = "org/esa/snap/gui/icons/NewVectorDataNode16.gif"
)
@ActionReference(
        path = "Toolbars/Tools",
        position = 191
)
@Messages({
                  "CTL_CreateVectorDataNodeActionText=Create Vector Data Container...",
                  "CTL_CreateVectorDataNodeActionPopupText=Create Vector Data Container..."
          })
public class CreateVectorDataNodeAction extends AbstractAction {
    private static final String DIALOG_TITLE = "New Vector Data Container";
    //private static final String KEY_VECTOR_DATA_INITIAL_NAME = "geometry.initialName";


    private static final String HELP_ID = "vectorDataManagement";
    private static int numItems = 1;

    public CreateVectorDataNodeAction() {
        putValue(Action.SHORT_DESCRIPTION, "Create a new vector data container for drawing line and polygons.");
        putValue(Action.SMALL_ICON, ImageUtilities.loadImageIcon("org/esa/snap/gui/icons/NewVectorDataNode16.gif", false));
        putValue(Action.LARGE_ICON_KEY, ImageUtilities.loadImageIcon("org/esa/snap/gui/icons/NewVectorDataNode24.gif", false));
        //putValue("placeAfter", "detachPixelGeoCoding");
        putValue("helpId", "createVectorDataNode");
        putValue("context", "product");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Product product = SnapApp.getInstance().getSelectedProduct();
        if (product != null) {
            DialogData dialogData = new DialogData(product.getVectorDataGroup());
            PropertySet propertySet = PropertyContainer.createObjectBacked(dialogData);
            propertySet.getDescriptor("name").setNotNull(true);
            propertySet.getDescriptor("name").setNotEmpty(true);
            propertySet.getDescriptor("name").setValidator(new NameValidator(product));
            propertySet.getDescriptor("description").setNotNull(true);

            final PropertyPane propertyPane = new PropertyPane(propertySet);
            JPanel panel = propertyPane.createPanel();
            panel.setPreferredSize(new Dimension(400, 100));
            ModalDialog dialog = new MyModalDialog(propertyPane);
            dialog.setContent(panel);
            int i = dialog.show();
            if (i == ModalDialog.ID_OK) {
                createDefaultVectorDataNode(product, dialogData.name, dialogData.description);
            }
        }
    }

    public static VectorDataNode createDefaultVectorDataNode(Product product) {
        return createDefaultVectorDataNode(product,
                                           getDefaultVectorDataNodeName(),
                                           "Default vector data container for geometries (automatically created)");
    }

    public static VectorDataNode createDefaultVectorDataNode(Product product, String name, String description) {
        CoordinateReferenceSystem modelCrs = ImageManager.getModelCrs(product.getGeoCoding());
        SimpleFeatureType type = PlainFeatureFactory.createDefaultFeatureType(modelCrs);
        VectorDataNode vectorDataNode = new VectorDataNode(name, type);
        vectorDataNode.setDescription(description);
        product.getVectorDataGroup().add(vectorDataNode);
        vectorDataNode.getPlacemarkGroup();
        String oldLayerId = selectVectorDataLayer(vectorDataNode);

        UndoRedo.Manager undoManager = PNodeFactory.getInstance().getUndoManager(product);
        undoManager.addEdit(new MyUndoableEdit(product, vectorDataNode, oldLayerId));

        return vectorDataNode;
    }

    private static String selectVectorDataLayer(VectorDataNode vectorDataNode) {
        Layer oldLayer = null;
        ProductSceneView sceneView = SnapApp.getInstance().getSelectedProductSceneView();
        if (sceneView != null) {
            oldLayer = sceneView.getSelectedLayer();
            // todo find new solution
            //SnapApp.getInstance().getProductTree().expand(vectorDataNode);

            sceneView.selectVectorDataLayer(vectorDataNode);

            LayerFilter nodeFilter = VectorDataLayerFilterFactory.createNodeFilter(vectorDataNode);
            Layer newSelectedLayer = LayerUtils.getChildLayer(sceneView.getRootLayer(),
                                                              LayerUtils.SEARCH_DEEP,
                                                              nodeFilter);
            if (newSelectedLayer != null) {
                newSelectedLayer.setVisible(true);
            }
        }
        return oldLayer != null ? oldLayer.getId() : null;
    }

    public static String getDefaultVectorDataNodeName() {
        //return VisatActivator.getInstance().getModuleContext().getRuntimeConfig().getContextProperty(KEY_VECTOR_DATA_INITIAL_NAME, "geometry");
        return "geometry";
    }

    private static String getSelectedLayerId() {
        ProductSceneView sceneView = SnapApp.getInstance().getSelectedProductSceneView();
        if (sceneView != null) {
            Layer selectedLayer = sceneView.getSelectedLayer();
            if (selectedLayer != null) {
                return selectedLayer.getId();
            }
        }
        return null;
    }

    private static class NameValidator implements Validator {
        private final Product product;

        private NameValidator(Product product) {
            this.product = product;
        }

        @Override
        public void validateValue(Property property, Object value) throws ValidationException {
            String name = (String) value;
            if (product.getVectorDataGroup().contains(name)) {
                final String pattern = "A vector data container with name ''{0}'' already exists.\n" +
                        "Please choose another one.";
                throw new ValidationException(MessageFormat.format(pattern, name));
            }
        }
    }

    private static class MyModalDialog extends ModalDialog {
        private final PropertyPane propertyPane;

        private MyModalDialog(PropertyPane propertyPane) {
            super(SnapApp.getInstance().getMainFrame(),
                  DIALOG_TITLE,
                  ModalDialog.ID_OK_CANCEL_HELP,
                  HELP_ID);
            this.propertyPane = propertyPane;
        }

        /**
         * Called in order to perform input validation.
         *
         * @return {@code true} if and only if the validation was successful.
         */
        @Override
        protected boolean verifyUserInput() {
            return !propertyPane.getBindingContext().hasProblems();
        }
    }

    private static class DialogData {
        private String name;
        private String description;

        private DialogData(ProductNodeGroup<VectorDataNode> vectorGroup) {
            String defaultPrefix = getDefaultVectorDataNodeName() + "_";
            name = defaultPrefix + (numItems++);
            while (vectorGroup.contains(name)) {
                name = defaultPrefix + (numItems++);
            }
            description = "";
        }
    }


    private static class MyUndoableEdit extends AbstractUndoableEdit {
        private final Product product;
        private final VectorDataNode vectorDataNode;
        private String oldLayerId;

        public MyUndoableEdit(Product product, VectorDataNode vectorDataNode, String oldLayerId) {
            this.product = product;
            this.vectorDataNode = vectorDataNode;
            this.oldLayerId = oldLayerId;
        }

        @Override
        public void undo() {
            super.undo();
            product.getVectorDataGroup().remove(vectorDataNode);
            setSelectedLayer(oldLayerId);
        }

        @Override
        public void redo() {
            super.redo();
            oldLayerId = getSelectedLayerId();
            product.getVectorDataGroup().add(vectorDataNode);
            selectVectorDataLayer(vectorDataNode);
        }

        @Override
        public String getPresentationName() {
            return "Insert Vector Data Container";
        }

        private void setSelectedLayer(String layerId) {
            if (layerId != null) {
                ProductSceneView sceneView = SnapApp.getInstance().getSelectedProductSceneView();
                if (sceneView != null) {
                    Layer layer = LayerUtils.getChildLayerById(sceneView.getRootLayer(), layerId);
                    if (layer != null) {
                        sceneView.setSelectedLayer(layer);
                    }
                }
            }
        }

    }

}