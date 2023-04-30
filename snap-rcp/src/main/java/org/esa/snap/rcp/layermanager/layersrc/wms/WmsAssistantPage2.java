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

package org.esa.snap.rcp.layermanager.layersrc.wms;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.layer.AbstractLayerSourceAssistantPage;
import org.esa.snap.ui.layer.LayerSourcePageContext;
import org.geotools.ows.wms.CRSEnvelope;
import org.geotools.ows.wms.Layer;
import org.geotools.ows.wms.StyleImpl;
import org.geotools.ows.wms.WMSCapabilities;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

class WmsAssistantPage2 extends AbstractLayerSourceAssistantPage {

    private JLabel infoLabel;
    private JTree layerTree;
    private CoordinateReferenceSystem modelCRS;

    WmsAssistantPage2() {
        super("Select Layer");
    }

    @Override
    public boolean performFinish() {
        WmsLayerSource.insertWmsLayer(getContext());
        return true;
    }

    @Override
    public AbstractLayerSourceAssistantPage getNextPage() {
        return new WmsAssistantPage3();
    }

    @Override
    public boolean hasNextPage() {
        return true;
    }

    @Override
    public boolean validatePage() {
        return getContext().getPropertyValue(WmsLayerSource.PROPERTY_NAME_SELECTED_LAYER) != null;
    }

    @Override
    public Component createPageComponent() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(new EmptyBorder(4, 4, 4, 4));
        panel.add(new JLabel("Available layers:"), BorderLayout.NORTH);

        LayerSourcePageContext context = getContext();
        modelCRS = (CoordinateReferenceSystem) context.getLayerContext().getCoordinateReferenceSystem();

        WMSCapabilities wmsCapabilities = (WMSCapabilities) context.getPropertyValue(
                WmsLayerSource.PROPERTY_NAME_WMS_CAPABILITIES);
        layerTree = new JTree(new WmsTreeModel(wmsCapabilities.getLayer()));
        layerTree.setRootVisible(false);
        layerTree.setShowsRootHandles(true);
        layerTree.setExpandsSelectedPaths(true);
        layerTree.setCellRenderer(new MyDefaultTreeCellRenderer());
        layerTree.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        layerTree.getSelectionModel().addTreeSelectionListener(new LayerTreeSelectionListener());
        panel.add(new JScrollPane(layerTree), BorderLayout.CENTER);
        infoLabel = new JLabel(" ");
        panel.add(infoLabel, BorderLayout.SOUTH);
        getContext().setPropertyValue(WmsLayerSource.PROPERTY_NAME_SELECTED_LAYER, null);
        return panel;
    }

    @SuppressWarnings({"unchecked"})
    private String getMatchingCRSCode(Layer layer) {
        Set<String> srsSet = layer.getSrs();
        String modelSRS = CRS.toSRS(modelCRS);
        if (modelSRS != null) {
            for (String srs : srsSet) {
                try {
                    final CoordinateReferenceSystem crs = CRS.decode(srs,true);
                    if (CRS.equalsIgnoreMetadata(crs, modelCRS)) {
                        return srs;
                    }
                } catch (FactoryException ignore) {
                }
            }
        }
        return null;
    }

    static String getLatLonBoundingBoxText(CRSEnvelope bbox) {
        if (bbox == null) {
            return "Lon = ?° ... ?°, Lat = ?° ... ?°";
        }
        return String.format("Lon = %.3f° ... %.3f°, Lat = %.3f° ... %.3f°",
                             bbox.getMinX(), bbox.getMaxX(),
                             bbox.getMinY(), bbox.getMaxY());
    }

    private static class MyDefaultTreeCellRenderer extends DefaultTreeCellRenderer {

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                                                      boolean leaf, int row, boolean hasFocus) {
            JLabel label = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            String text;
            if (value instanceof Layer) {
                String title;
                Layer layer = (Layer) value;
                title = layer.getTitle();
                if (title == null) {
                    title = layer.getName();
                }
                if (title == null) {
                    title = layer.toString();
                }
                StringBuilder sb = new StringBuilder(String.format("<html><b>%s</b>", title));

                Layer[] children = layer.getChildren();
                if (children.length > 1) {
                    sb.append(String.format(" (%d children)", children.length));
                } else if (children.length == 1) {
                    sb.append(" (1 child)");
                }

                text = sb.append("</html>").toString();
            } else if (value instanceof WMSCapabilities) {
                WMSCapabilities capabilities = (WMSCapabilities) value;
                text = String.format("<html><b>%s</b></html>", capabilities.getService().getName());
            } else {
                text = String.format("<html><b>%s</b></html>", value);
            }
            label.setText(text);
            return label;
        }

    }

    private class LayerTreeSelectionListener implements TreeSelectionListener {

        @Override
        public void valueChanged(TreeSelectionEvent e) {
            LayerSourcePageContext context = getContext();
            TreePath selectedLayerPath = layerTree.getSelectionModel().getSelectionPath();
            Layer selectedLayer = (Layer) selectedLayerPath.getLastPathComponent();
            if (selectedLayer != null) {
                String crsCode = getMatchingCRSCode(selectedLayer);
                if (crsCode == null) {
                    infoLabel.setForeground(Color.RED.darker());
                    infoLabel.setText("Coordinate system not supported.");
                } else {
                    RasterDataNode raster = SnapApp.getDefault().getSelectedProductSceneView().getRaster();
                    AffineTransform g2mTransform = Product.findImageToModelTransform(raster.getGeoCoding());
                    Rectangle2D bounds = g2mTransform.createTransformedShape(
                            new Rectangle(0, 0, raster.getRasterWidth(),
                                          raster.getRasterHeight())).getBounds2D();
                    CRSEnvelope crsEnvelope = new CRSEnvelope(crsCode, bounds.getMinX(), bounds.getMinY(),
                                                              bounds.getMaxX(),
                                                              bounds.getMaxY());
                    context.setPropertyValue(WmsLayerSource.PROPERTY_NAME_CRS_ENVELOPE, crsEnvelope);
                    List<StyleImpl> styles = selectedLayer.getStyles();
                    if (!styles.isEmpty()) {
                        context.setPropertyValue(WmsLayerSource.PROPERTY_NAME_SELECTED_STYLE, styles.get(0));
                    } else {
                        context.setPropertyValue(WmsLayerSource.PROPERTY_NAME_SELECTED_STYLE, null);
                    }
                    context.setPropertyValue(WmsLayerSource.PROPERTY_NAME_SELECTED_LAYER, selectedLayer);
                    infoLabel.setForeground(Color.DARK_GRAY);
                    infoLabel.setText(getLatLonBoundingBoxText(selectedLayer.getLatLonBoundingBox()));
                }
            } else {
                infoLabel.setForeground(Color.DARK_GRAY);
                infoLabel.setText("");
            }
            context.updateState();
        }

    }

    private static class WmsTreeModel implements TreeModel {

        private final WeakHashMap<TreeModelListener, Object> treeModelListeners;
        private Layer rootLayer;

        private WmsTreeModel(Layer rootLayer) {
            this.rootLayer = rootLayer;
            treeModelListeners = new WeakHashMap<>();
        }

        @Override
        public Object getRoot() {
            return rootLayer;
        }

        @Override
        public Object getChild(Object parent, int index) {
            Layer layer = (Layer) parent;
            return layer.getChildren()[index];
        }

        @Override
        public int getChildCount(Object parent) {
            Layer layer = (Layer) parent;
            return layer.getChildren().length;
        }

        @Override
        public boolean isLeaf(Object node) {
            Layer layer = (Layer) node;
            return layer.getChildren() != null && layer.getChildren().length == 0;
        }

        @Override
        public int getIndexOfChild(Object parent, Object child) {
            Layer layer = (Layer) parent;
            int index = Arrays.binarySearch(layer.getChildren(), child);
            return index < 0 ? -1 : index;
        }

        @Override
        public void valueForPathChanged(TreePath path, Object newValue) {
            fireTreeNodeChanged(path);
        }

        @Override
        public void addTreeModelListener(TreeModelListener l) {
            treeModelListeners.put(l, "");
        }

        @Override
        public void removeTreeModelListener(TreeModelListener l) {
            treeModelListeners.remove(l);
        }

        protected void fireTreeNodeChanged(TreePath treePath) {
            TreeModelEvent event = new TreeModelEvent(this, treePath);
            for (TreeModelListener treeModelListener : treeModelListeners.keySet()) {
                treeModelListener.treeNodesChanged(event);
            }
        }

    }

}