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
package org.esa.snap.rcp.actions.vector;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.Validator;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerFilter;
import com.bc.ceres.glayer.support.LayerUtils;
import com.bc.ceres.swing.binding.PropertyPane;
import org.esa.snap.core.datamodel.PlainFeatureFactory;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.VectorDataNode;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.nodes.UndoableProductNodeInsertion;
import org.esa.snap.ui.ModalDialog;
import org.esa.snap.ui.PackageDefaults;
import org.esa.snap.ui.product.ProductSceneView;
import org.esa.snap.ui.product.VectorDataLayerFilterFactory;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.awt.UndoRedo;
import org.openide.util.ContextAwareAction;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle.Messages;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.text.MessageFormat;

@ActionID(category = "Tools", id = "CreateVectorDataNodeAction" )
@ActionRegistration(
        displayName = "#CTL_CreateVectorDataNodeActionText",
        popupText = "#CTL_CreateVectorDataNodeActionPopupText",
        lazy = false
)
@ActionReferences({
        @ActionReference(
                path = "Menu/" + PackageDefaults.VECTOR_DATA_NODE_MENU_PATH,
                position = PackageDefaults.VECTOR_DATA_NODE_MENU_POSITION),
        @ActionReference(
                path = "Toolbars/" + PackageDefaults.VECTOR_DATA_NODE_TOOLBAR_NAME,
                position = PackageDefaults.VECTOR_DATA_NODE_TOOLBAR_POSITION)
})
@Messages({
        "CTL_CreateVectorDataNodeActionText=" + PackageDefaults.VECTOR_DATA_NODE_NAME,
        "CTL_CreateVectorDataNodeActionPopupText=" + PackageDefaults.VECTOR_DATA_NODE_DESCRIPTION
})
public class CreateVectorDataNodeAction extends AbstractAction implements ContextAwareAction, LookupListener {
    private static final String HELP_ID = "vectorDataManagement";
    private static int numItems = 1;
    private Lookup lkp;
    private Lookup.Result<ProductNode> result;

    public CreateVectorDataNodeAction() {
        this(Utilities.actionsGlobalContext());
    }

    public CreateVectorDataNodeAction(Lookup lkp) {
        super(Bundle.CTL_CreateVectorDataNodeActionText());
        this.lkp = lkp;
        result = this.lkp.lookupResult(ProductNode.class);
        result.addLookupListener(WeakListeners.create(LookupListener.class, this, result));
        putValue(Action.LARGE_ICON_KEY, ImageUtilities.loadImageIcon("org/esa/snap/rcp/icons/" + PackageDefaults.VECTOR_DATA_NODE_ICON, false));
        putValue(Action.SMALL_ICON, ImageUtilities.loadImageIcon("org/esa/snap/rcp/icons/" + PackageDefaults.VECTOR_DATA_NODE_SMALL_ICON, false));
        setEnabled(false);
    }

    public static VectorDataNode createDefaultVectorDataNode(Product product) {
        return createDefaultVectorDataNode(product,
                getDefaultVectorDataNodeName(),
                "Default vector data container for geometries (automatically created)");
    }

    public static VectorDataNode createDefaultVectorDataNode(Product product, String name, String description) {
        CoordinateReferenceSystem modelCrs = product.getSceneCRS();
        SimpleFeatureType type = PlainFeatureFactory.createDefaultFeatureType(modelCrs);
        VectorDataNode vectorDataNode = new VectorDataNode(name, type);
        vectorDataNode.setDescription(description);
        product.getVectorDataGroup().add(vectorDataNode);
        vectorDataNode.getPlacemarkGroup();
        String oldLayerId = selectVectorDataLayer(vectorDataNode);

        UndoRedo.Manager undoManager = SnapApp.getDefault().getUndoManager(product);
        if (undoManager != null) {
            undoManager.addEdit(new UndoableVectorDataNodeInsertion(product, vectorDataNode, oldLayerId));
        }

        return vectorDataNode;
    }

    private static String selectVectorDataLayer(VectorDataNode vectorDataNode) {
        Layer oldLayer = null;
        ProductSceneView sceneView = SnapApp.getDefault().getSelectedProductSceneView();
        if (sceneView != null) {
            oldLayer = sceneView.getSelectedLayer();
            // todo find new solution
            //SnapApp.getDefault().getProductTree().expand(vectorDataNode);

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
        return "geometry";
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new CreateVectorDataNodeAction(actionContext);
    }

    @Override
    public void resultChanged(LookupEvent ev) {
        ProductNode productNode = SnapApp.getDefault().getSelectedProductNode(SnapApp.SelectionSourceHint.VIEW);
        setEnabled(productNode != null);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ProductNode productNode = SnapApp.getDefault().getSelectedProductNode(SnapApp.SelectionSourceHint.VIEW);
        if (productNode == null) {
            return;
        }
        Product product = productNode.getProduct();
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
            super(SnapApp.getDefault().getMainFrame(),
                    Bundle.CTL_CreateVectorDataNodeActionText(),
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


    private static class UndoableVectorDataNodeInsertion extends UndoableProductNodeInsertion<VectorDataNode> {

        private String oldLayerId;

        public UndoableVectorDataNodeInsertion(Product product, VectorDataNode vectorDataNode, String oldLayerId) {
            super(product.getVectorDataGroup(), vectorDataNode);
            this.oldLayerId = oldLayerId;
        }

        private static String getSelectedLayerId() {
            ProductSceneView sceneView = SnapApp.getDefault().getSelectedProductSceneView();
            if (sceneView != null) {
                Layer selectedLayer = sceneView.getSelectedLayer();
                if (selectedLayer != null) {
                    return selectedLayer.getId();
                }
            }
            return null;
        }

        @Override
        public void undo() {
            super.undo(); // removes VDN
            setSelectedLayer(oldLayerId);
        }

        @Override
        public void redo() {
            oldLayerId = getSelectedLayerId();
            super.redo(); // inserts VDN
            selectVectorDataLayer(getProductNode());
        }

        private void setSelectedLayer(String layerId) {
            if (layerId != null) {
                ProductSceneView sceneView = SnapApp.getDefault().getSelectedProductSceneView();
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
