/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.nodes;

import eu.esa.snap.core.datamodel.group.BandGrouping;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.actions.file.CloseProductAction;
import org.openide.awt.UndoRedo;
import org.openide.nodes.Node;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.openide.util.WeakListeners;

import javax.swing.Action;
import java.io.File;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;
import java.util.stream.Stream;

import static org.esa.snap.rcp.nodes.PNNodeSupport.performUndoableProductNodeEdit;

/**
 * A node that represents a {@link Product} (=P).
 * Every {@code PNode} holds a dedicated undo/redo context.
 *
 * @author Norman
 */
public class PNode extends PNNode<Product> implements PreferenceChangeListener {

    private final PContent group;

    public PNode(Product product) {
        this(product, new PContent());
    }

    private PNode(Product product, PContent group) {
        super(product, group);
        this.group = group;
        group.node = this;
        setDisplayName(product.getDisplayName());
        setShortDescription(product.getDescription());
        setIconBaseWithExtension("org/esa/snap/rcp/icons/RsProduct16.gif");
        Preferences preferences = SnapApp.getDefault().getPreferences();
        preferences.addPreferenceChangeListener(WeakListeners.create(PreferenceChangeListener.class, this, preferences));
    }

    public Product getProduct() {
        return getProductNode();
    }

    @Override
    public UndoRedo getUndoRedo() {
        return SnapApp.getDefault().getUndoManager(getProduct());
    }

    @Override
    public boolean canDestroy() {
        return false;
    }

    @Override
    public void destroy() {
        new CloseProductAction(Collections.singletonList(getProduct())).execute();
    }

    @Override
    public Action[] getActions(boolean context) {
        return PNNodeSupport.getContextActions(getProductNode());
    }

    @Override
    public Action getPreferredAction() {
        //Define the action that will be invoked
        //when the user double-clicks on the node:
        return super.getPreferredAction();
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent evt) {
        String key = evt.getKey();
        if (GroupByNodeTypeAction.PREFERENCE_KEY.equals(key)) {
            group.refresh();
        }
    }

    @Override
    public PropertySet[] getPropertySets() {

        Sheet.Set set = new Sheet.Set();
        set.setDisplayName("Product Properties");
        set.put(new PropertySupport.ReadOnly<File>("fileLocation", File.class, "File", "File location") {
            @Override
            public File getValue() {
                return getProduct().getFileLocation();
            }
        });
        set.put(new PropertySupport.ReadWrite<String>("productType", String.class, "Product Type", "The product type identifier") {
            @Override
            public String getValue() {
                return getProduct().getProductType();
            }

            @Override
            public void setValue(String newValue) throws IllegalArgumentException {
                String oldValue = getProduct().getProductType();
                performUndoableProductNodeEdit("Edit Product Type",
                                               getProduct(),
                                               node -> node.setProductType(newValue),
                                               node -> node.setProductType(oldValue));
            }
        });
        set.put(new PropertySupport.ReadWrite<String>("startTime", String.class, "Sensing Start Time", "The product's sensing start time") {
            @Override
            public String getValue() {
                ProductData.UTC startTime = getProduct().getStartTime();
                return startTime != null ? startTime.format() : "";
            }

            @Override
            public void setValue(String s) throws IllegalArgumentException {
                ProductData.UTC oldValue = getProduct().getStartTime();
                try {
                    ProductData.UTC newValue = ProductData.UTC.parse(s);
                    performUndoableProductNodeEdit("Edit Sensing Start Time",
                                                   getProduct(),
                                                   node -> node.setStartTime(newValue),
                                                   node -> node.setStartTime(oldValue));
                } catch (ParseException e) {
                    throw new IllegalArgumentException(e.getMessage());
                }
            }
        });
        set.put(new PropertySupport.ReadWrite<String>("endTime", String.class, "Sensing Stop Time", "The product's sensing stop time") {
            @Override
            public String getValue() {
                ProductData.UTC endTime = getProduct().getEndTime();
                return endTime != null ? endTime.format() : "";
            }

            @Override
            public void setValue(String s) throws IllegalArgumentException {
                Product product = getProduct();
                ProductData.UTC oldValue = product.getEndTime();
                try {
                    ProductData.UTC newValue = ProductData.UTC.parse(s);
                    performUndoableProductNodeEdit("Edit Sensing Stop Time",
                                                   product,
                                                   node -> node.setEndTime(newValue),
                                                   node -> node.setEndTime(oldValue));
                } catch (ParseException e) {
                    throw new IllegalArgumentException(e.getMessage());
                }
            }
        });
        set.put(new PropertySupport.ReadWrite<String>("bandGrouping", String.class, "Band Grouping", "The product's band grouping") {
            @Override
            public String getValue() {
                final BandGrouping autoGrouping = getProduct().getAutoGrouping();
                if (autoGrouping == null) {
                    return "";
                } else {
                    return autoGrouping.toString();
                }
            }

            @Override
            public void setValue(String s) throws IllegalArgumentException {
                BandGrouping oldValue = getProduct().getAutoGrouping();
                BandGrouping newValue = BandGrouping.parse(s);
                performUndoableProductNodeEdit("Edit Band-Grouping",
                                               getProduct(),
                                               node -> node.setAutoGrouping(newValue),
                                               node -> node.setAutoGrouping(oldValue));

            }
        });
        includeAbstractedMetadata(set);

        return Stream.concat(Stream.of(super.getPropertySets()), Stream.of(set)).toArray(PropertySet[]::new);
    }

    private void includeAbstractedMetadata(final Sheet.Set set) {
        final MetadataElement root = getProduct().getMetadataRoot();
        if (root != null) {
            final MetadataElement absRoot = root.getElement("Abstracted_Metadata");
            if (absRoot != null) {
                set.put(new PropertySupport.ReadOnly<String>("mission", String.class, "Mission", "Earth Observation Mission") {
                    @Override
                    public String getValue() {
                        return absRoot.getAttributeString("mission");
                    }
                });
                set.put(new PropertySupport.ReadOnly<String>("mode", String.class, "Acquisition Mode", "Sensor Acquisition Mode") {
                    @Override
                    public String getValue() {
                        return absRoot.getAttributeString("ACQUISITION_MODE");
                    }
                });
                set.put(new PropertySupport.ReadOnly<String>("pass", String.class, "Pass", "Orbital Pass") {
                    @Override
                    public String getValue() {
                        return absRoot.getAttributeString("pass");
                    }
                });
                set.put(new PropertySupport.ReadOnly<String>("track", String.class, "Track", "Relative Orbit") {
                    @Override
                    public String getValue() {
                        return absRoot.getAttributeString("REL_ORBIT");
                    }
                });
                set.put(new PropertySupport.ReadOnly<String>("orbit", String.class, "Orbit", "Absolute Orbit") {
                    @Override
                    public String getValue() {
                        return absRoot.getAttributeString("ABS_ORBIT");
                    }
                });
            }
        }
    }

    private boolean isGroupByNodeType() {
        return SnapApp.getDefault().getPreferences().getBoolean(GroupByNodeTypeAction.PREFERENCE_KEY,
                                                                GroupByNodeTypeAction.PREFERENCE_DEFAULT_VALUE);
    }

    /**
     * A child factory for nodes below a {@link PNode} that holds a {@link Product}.
     *
     * @author Norman
     */
    static class PContent extends PNGroupBase<Object> {

        PNode node;

        @Override
        protected boolean createKeys(List<Object> list) {
            Product product = node.getProduct();
            ProductNodeGroup<MetadataElement> metadataElementGroup = product.getMetadataRoot().getElementGroup();
            if (node.isGroupByNodeType()) {
                if (metadataElementGroup != null) {
                    list.add(new PNGGroup.ME(metadataElementGroup));
                }
                if (product.getIndexCodingGroup().getNodeCount() > 0) {
                    list.add(new PNGGroup.IC(product.getIndexCodingGroup()));
                }
                if (product.getFlagCodingGroup().getNodeCount() > 0) {
                    list.add(new PNGGroup.FC(product.getFlagCodingGroup()));
                }
                if (product.getVectorDataGroup().getNodeCount() > 0) {
                    list.add(new PNGGroup.VDN(product.getVectorDataGroup()));
                }
                if (product.getTiePointGridGroup().getNodeCount() > 0) {
                    list.add(new PNGroupingGroup.TPG(product.getTiePointGridGroup()));
                }
                if (product.getQuicklookGroup().getNodeCount() > 0) {
                    list.add(new PNGGroup.QL(product.getQuicklookGroup()));
                }
                if (product.getBandGroup().getNodeCount() > 0) {
                    list.add(new PNGroupingGroup.B(product.getBandGroup()));
                }
                if (product.getMaskGroup().getNodeCount() > 0) {
                    list.add(new PNGroupingGroup.M(product.getMaskGroup()));
                }
            } else {
                if (metadataElementGroup != null) {
                    list.addAll(Arrays.asList(metadataElementGroup.toArray()));
                }
                list.addAll(Arrays.asList(product.getIndexCodingGroup().toArray()));
                list.addAll(Arrays.asList(product.getFlagCodingGroup().toArray()));
                list.addAll(Arrays.asList(product.getVectorDataGroup().toArray()));
                list.addAll(Arrays.asList(product.getTiePointGridGroup().toArray()));
                list.addAll(Arrays.asList(product.getQuicklookGroup().toArray()));
                list.addAll(Arrays.asList(product.getBandGroup().toArray()));
                list.addAll(Arrays.asList(product.getMaskGroup().toArray()));
            }

            return true;
        }

        @Override
        protected Node createNodeForKey(Object key) {
            if (key instanceof ProductNode) {
                return PNNode.create((ProductNode) key);
            } else {
                return new PNGroupNode((PNGroup) key);
            }
        }
    }
}
