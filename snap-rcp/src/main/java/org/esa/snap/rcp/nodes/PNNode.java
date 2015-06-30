/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.nodes;

import com.bc.jexp.ParseException;
import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.FlagCoding;
import org.esa.snap.framework.datamodel.IndexCoding;
import org.esa.snap.framework.datamodel.Mask;
import org.esa.snap.framework.datamodel.MetadataElement;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductData;
import org.esa.snap.framework.datamodel.ProductNode;
import org.esa.snap.framework.datamodel.ProductNodeEvent;
import org.esa.snap.framework.datamodel.ProductNodeGroup;
import org.esa.snap.framework.datamodel.RasterDataNode;
import org.esa.snap.framework.datamodel.TiePointGrid;
import org.esa.snap.framework.datamodel.VectorDataNode;
import org.esa.snap.framework.datamodel.VirtualBand;
import org.esa.snap.framework.dataop.barithm.BandArithmetic;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.SnapDialogs;
import org.esa.snap.rcp.actions.ShowPlacemarkViewAction;
import org.esa.snap.rcp.actions.file.ShowMetadataViewAction;
import org.esa.snap.rcp.actions.view.OpenImageViewAction;
import org.esa.snap.util.StringUtils;
import org.openide.awt.UndoRedo;
import org.openide.nodes.Node;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.openide.util.lookup.Lookups;

import javax.swing.Action;
import java.awt.datatransfer.Transferable;
import java.beans.PropertyEditor;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Stream;

/**
 * A node that represents some {@link org.esa.snap.framework.datamodel.ProductNode} (=PN).
 *
 * @author Norman
 */
abstract class PNNode<T extends ProductNode> extends PNNodeBase {

    private final T productNode;
    private final PNNodeSupport nodeSupport;

    public PNNode(T productNode) {
        this(productNode, null);
    }

    public PNNode(T productNode, PNGroupBase childFactory) {
        super(childFactory, Lookups.singleton(productNode));
        this.productNode = productNode;
        setDisplayName(productNode.getName());
        setShortDescription(productNode.getDescription());
        nodeSupport = PNNodeSupport.create(this, childFactory);
    }

    public T getProductNode() {
        return productNode;
    }

    @Override
    public void nodeChanged(ProductNodeEvent event) {
        if (event.getSourceNode() == getProductNode()) {
            if (ProductNode.PROPERTY_NAME_NAME.equals(event.getPropertyName())) {
                setDisplayName(getProductNode().getName());
            }
            if (ProductNode.PROPERTY_NAME_DESCRIPTION.equals(event.getPropertyName())) {
                setShortDescription(getProductNode().getDescription());
            }
        }
        nodeSupport.nodeChanged(event);
    }

    @Override
    public void nodeDataChanged(ProductNodeEvent event) {
        nodeSupport.nodeDataChanged(event);
    }

    @Override
    public void nodeAdded(ProductNodeEvent event) {
        nodeSupport.nodeAdded(event);
    }

    @Override
    public void nodeRemoved(ProductNodeEvent event) {
        nodeSupport.nodeRemoved(event);
    }

    @Override
    public PropertySet[] getPropertySets() {
        Sheet.Set set = new Sheet.Set();
        set.setDisplayName("Product Node Properties");
        set.put(new PropertySupport.ReadWrite<String>("name", String.class, "Name", "Name of the element") {
            @Override
            public String getValue() {
                return getProductNode().getName();
            }

            @Override
            public void setValue(String val) {
                getProductNode().setName(val);
                // todo - add undoable edit
            }
        });
        set.put(new PropertySupport.ReadWrite<String>("description", String.class, "Description", "Human-readable description of the element") {
            @Override
            public String getValue() {
                return getProductNode().getDescription();
            }

            @Override
            public void setValue(String val) {
                getProductNode().setDescription(val);
                // todo - add undoable edit
            }
        });
        set.put(new PropertySupport.ReadOnly<Boolean>("modified", Boolean.class, "Modified", "Has the element been modified?") {
            @Override
            public Boolean getValue() {
                return getProductNode().isModified();
            }
        });
        return new PropertySet[]{
                set
        };
    }

    @Override
    public Action[] getActions(boolean context) {
        ProductNode productNode1 = getProductNode();
        return PNNodeSupport.getContextActions(productNode1);
    }

    public static Node create(ProductNode productNode) {
        if (productNode instanceof FlagCoding) {
            return new PNNode.FC((FlagCoding) productNode);
        } else if (productNode instanceof IndexCoding) {
            return new PNNode.IC((IndexCoding) productNode);
        } else if (productNode instanceof MetadataElement) {
            return new PNNode.ME((MetadataElement) productNode);
        } else if (productNode instanceof VectorDataNode) {
            return new PNNode.VDN((VectorDataNode) productNode);
        } else if (productNode instanceof TiePointGrid) {
            return new PNNode.TPG((TiePointGrid) productNode);
        } else if (productNode instanceof Mask) {
            return new PNNode.M((Mask) productNode);
        } else if (productNode instanceof Band) {
            return new PNNode.B((Band) productNode);
        }
        throw new IllegalStateException("unhandled product node type: " + productNode.getClass() + " named '" + productNode.getName() + "'");
    }

    static <T extends ProductNode> void deleteProductNode(Product product, ProductNodeGroup<T> group, T productNode) {
        // todo - close all document windows / layers that refer to productNode (nf/mp - 14.01.2015)
        int index = group.indexOf(productNode);
        if (group.remove(productNode)) {
            UndoRedo.Manager manager = SnapApp.getDefault().getUndoManager(product);
            if (manager != null) {
                manager.addEdit(new UndoableProductNodeDeletion<>(group, productNode, index));
            }
        }
    }

    private static StringBuilder append(StringBuilder stringBuilder, String text) {
        if (StringUtils.isNotNullAndNotEmpty(text)) {
            if (stringBuilder.length() > 0) {
                stringBuilder.append(", ");
            }
            stringBuilder.append(text);
        }
        return stringBuilder;
    }


    /**
     * A node that represents a {@link org.esa.snap.framework.datamodel.MetadataElement} (=ME).
     *
     * @author Norman
     */
    static class ME extends PNNode<MetadataElement> {

        public ME(MetadataElement element) {
            super(element, element.getElementGroup() != null ? new PNGGroup.ME(element.getElementGroup()) : null);
            setIconBaseWithExtension("org/esa/snap/rcp/icons/RsMetaData16.gif");
        }

        @Override
        public boolean canDestroy() {
            return getProductNode().getParentElement() != null;
        }

        @Override
        public void destroy() throws IOException {
            deleteProductNode(getProductNode().getProduct(),
                              getProductNode().getParentElement().getElementGroup(),
                              getProductNode());
        }

        @Override
        public Action getPreferredAction() {
            // return new ShowMetadataViewAction(this.getProductNode());
            return new ShowMetadataViewAction();
        }
    }

    /**
     * A node that represents an {@link org.esa.snap.framework.datamodel.IndexCoding} (=IC).
     *
     * @author Norman
     */
    static class IC extends PNNode<IndexCoding> {

        public IC(IndexCoding indexCoding) {
            super(indexCoding);
            setIconBaseWithExtension("org/esa/snap/rcp/icons/RsBandIndexes16.gif");
        }

        @Override
        public boolean canDestroy() {
            return true;
        }

        @Override
        public void destroy() throws IOException {
            deleteProductNode(getProductNode().getProduct(),
                              getProductNode().getProduct().getIndexCodingGroup(),
                              getProductNode());
        }

        @Override
        public Action getPreferredAction() {
            return new ShowMetadataViewAction();
        }
    }

    /**
     * A node that represents a {@link org.esa.snap.framework.datamodel.FlagCoding} (=FC).
     *
     * @author Norman
     */
    static class FC extends PNNode<FlagCoding> {

        public FC(FlagCoding flagCoding) {
            super(flagCoding);
            setIconBaseWithExtension("org/esa/snap/rcp/icons/RsBandFlags16.gif");
        }

        @Override
        public boolean canDestroy() {
            return true;
        }

        @Override
        public void destroy() throws IOException {
            deleteProductNode(getProductNode().getProduct(),
                              getProductNode().getProduct().getFlagCodingGroup(),
                              getProductNode());
        }

        @Override
        public Action getPreferredAction() {
            return new ShowMetadataViewAction();
        }
    }

    /**
     * A node that represents a {@link org.esa.snap.framework.datamodel.VectorDataNode} (=VDN).
     *
     * @author Norman
     */
    static class VDN extends PNNode<VectorDataNode> {

        public VDN(VectorDataNode vectorDataNode) {
            super(vectorDataNode);
            setIconBaseWithExtension("org/esa/snap/rcp/icons/RsVectorData16.gif");
            setShortDescription(createToolTip(vectorDataNode));
        }

        private String createToolTip(final VectorDataNode vectorDataNode) {
            final StringBuilder tooltip = new StringBuilder();
            append(tooltip, vectorDataNode.getDescription());
            append(tooltip, String.format("type %s, %d feature(s)", vectorDataNode.getFeatureType().getTypeName(), vectorDataNode.getFeatureCollection().size()));
            return tooltip.toString();
        }

        @Override
        public boolean canDestroy() {
            return true;
        }

        @Override
        public void destroy() throws IOException {
            deleteProductNode(getProductNode().getProduct(),
                              getProductNode().getProduct().getVectorDataGroup(),
                              getProductNode());
        }

        @Override
        public Action getPreferredAction() {
            return new ShowPlacemarkViewAction();
        }
    }

    /**
     * A node that represents a {@link org.esa.snap.framework.datamodel.TiePointGrid} (=TPG).
     *
     * @author Norman
     */
    static class TPG extends PNNode<TiePointGrid> {

        public TPG(TiePointGrid tiePointGrid) {
            super(tiePointGrid);
            setIconBaseWithExtension("org/esa/snap/rcp/icons/RsBandAsTiePoint16.gif");
            setShortDescription(createToolTip(tiePointGrid));
        }

        private String createToolTip(final TiePointGrid tiePointGrid) {
            StringBuilder tooltip = new StringBuilder();
            append(tooltip, tiePointGrid.getDescription());
            append(tooltip, String.format("%d x %d --> %d x %d pixels", tiePointGrid.getRasterWidth(), tiePointGrid.getRasterHeight(), tiePointGrid.getSceneRasterWidth(), tiePointGrid.getSceneRasterHeight()));
            if (tiePointGrid.getUnit() != null) {
                append(tooltip, String.format(" (%s)", tiePointGrid.getUnit()));
            }
            return tooltip.toString();
        }

        @Override
        public boolean canDestroy() {
            return true;
        }

        @Override
        public void destroy() throws IOException {
            deleteProductNode(getProductNode().getProduct(),
                              getProductNode().getProduct().getTiePointGridGroup(),
                              getProductNode());
        }

        @Override
        public Action getPreferredAction() {
            return new OpenImageViewAction(this.getProductNode());
        }

        @Override
        public PropertySet[] getPropertySets() {

            Sheet.Set set = new Sheet.Set();
            final TiePointGrid tpg = getProductNode();

            set.setDisplayName("Tie-Point Grid Properties");
            set.put(new PropertySupport.ReadWrite<String>("unit", String.class, "Unit", "Geophysical unit") {
                @Override
                public String getValue() {
                    return tpg.getUnit();
                }

                @Override
                public void setValue(String s) {
                    tpg.setUnit(s);
                }
            });
            set.put(new PropertySupport.ReadOnly<String>("dimensions", String.class, "Width x Height", "The width and height of the raster in pixels") {
                @Override
                public String getValue() {
                    return String.format("%d x %d", tpg.getRasterWidth(), tpg.getRasterHeight());
                }
            });

            return Stream.concat(Stream.of(super.getPropertySets()), Stream.of(set)).toArray(PropertySet[]::new);
        }
    }

    /**
     * A node that represents a {@link org.esa.snap.framework.datamodel.Mask} (=M).
     *
     * @author Norman
     */
    static class M extends PNNode<Mask> {

        public M(Mask mask) {
            super(mask);
            setIconBaseWithExtension("org/esa/snap/rcp/icons/RsMask16.gif");
        }

        @Override
        public boolean canDestroy() {
            return true;
        }

        @Override
        public void destroy() throws IOException {
            deleteProductNode(getProductNode().getProduct(),
                              getProductNode().getProduct().getMaskGroup(),
                              getProductNode());
        }

        @Override
        public Action getPreferredAction() {
            return new OpenImageViewAction(this.getProductNode());
        }
    }

    /**
     * A node that represents a {@link org.esa.snap.framework.datamodel.Band} (=B).
     *
     * @author Norman
     */
    static class B extends PNNode<Band> {

        public B(Band band) {
            super(band);
            if (band instanceof VirtualBand) {
                setIconBaseWithExtension("org/esa/snap/rcp/icons/RsBandVirtual16.gif");
            } else if (band.isFlagBand()) {
                setIconBaseWithExtension("org/esa/snap/rcp/icons/RsBandFlags16.gif");
            } else {
                setIconBaseWithExtension("org/esa/snap/rcp/icons/RsBandAsSwath.gif");
            }
            setShortDescription(createToolTip(band));
        }

        private String createToolTip(final Band band) {
            StringBuilder tooltip = new StringBuilder();
            append(tooltip, band.getDescription());
            if (band.getSpectralWavelength() > 0.0) {
                append(tooltip, String.format("%s nm", band.getSpectralWavelength()));
                if (band.getSpectralBandwidth() > 0.0) {
                    append(tooltip, String.format("+/-%s nm", 0.5 * band.getSpectralBandwidth()));
                }
            }
            append(tooltip, String.format("%d x %d pixels", band.getRasterWidth(), band.getRasterHeight()));
            if (band instanceof VirtualBand) {
                append(tooltip, String.format("Expr.: %s", ((VirtualBand) band).getExpression()));
            }
            if (band.getUnit() != null) {
                append(tooltip, String.format(" (%s)", band.getUnit()));
            }
            return tooltip.toString();
        }

        @Override
        public boolean canDestroy() {
            return true;
        }

        @Override
        public void destroy() throws IOException {
            deleteProductNode(getProductNode().getProduct(),
                              getProductNode().getProduct().getBandGroup(),
                              getProductNode());
        }

        @Override
        public Transferable clipboardCopy() throws IOException {
            return super.clipboardCopy();
        }

        @Override
        public boolean canCopy() {
            return true;
        }

        @Override
        public Transferable clipboardCut() throws IOException {
            return super.clipboardCut();
        }

        @Override
        public boolean canCut() {
            return true;
        }

        @Override
        public Action getPreferredAction() {
            return new OpenImageViewAction(this.getProductNode());
        }

        @Override
        public PropertySet[] getPropertySets() {

            Sheet.Set set = new Sheet.Set();
            final Band band = getProductNode();

            set.setDisplayName("Raster Band Properties");
            set.put(new PropertySupport.ReadWrite<String>("unit", String.class, "Unit", "Geophysical Unit") {
                @Override
                public String getValue() {
                    return band.getUnit();
                }

                @Override
                public void setValue(String s) {
                    band.setUnit(s);
                    // todo - add undoable edit
                }
            });
            set.put(new PropertySupport.ReadOnly<String>("dataType", String.class, "Data Type", "Raster data type") {
                @Override
                public String getValue() {
                    return ProductData.getTypeString(band.getDataType());
                }
            });
            set.put(new PropertySupport.ReadOnly<String>("dimensions", String.class, "Width x Height", "Width and height of the raster in pixels") {
                @Override
                public String getValue() {
                    return String.format("%d x %d", band.getRasterWidth(), band.getRasterHeight());
                }
            });
            set.put(new PropertySupport.ReadWrite<Boolean>("useNoDataValue", Boolean.class, "Use No-Data Value", "Is no-data value used?") {
                @Override
                public Boolean getValue() {
                    return band.isNoDataValueUsed();
                }

                @Override
                public void setValue(Boolean val) {
                    band.setNoDataValueUsed(val);
                    // todo - add undoable edit
                }
            });
            set.put(new PropertySupport.ReadWrite<Double>("noDataValue", Double.class, "No-Data Value", "No-data value used to indicate missing pixels") {
                @Override
                public Double getValue() {
                    return band.getNoDataValue();
                }

                @Override
                public void setValue(Double val) {
                    band.setNoDataValue(val);
                    // todo - add undoable edit
                }
            });
            set.put(new PropertySupport.ReadWrite<String>("validPixelExpression", String.class, "Valid Pixel Expression",
                                                          "Boolean expression which is used to identify valid pixels") {
                @Override
                public String getValue() {
                    final String expression = band.getValidPixelExpression();
                    if (expression != null) {
                        return expression;
                    }
                    return "";
                }

                @Override
                public void setValue(String val) {
                    try {
                        final Product product = band.getProduct();
                        final RasterDataNode[] refRasters = BandArithmetic.getRefRasters(val, product);
                        if (refRasters.length > 0 &&
                                (!BandArithmetic.areReferencedRastersCompatible(product, val) ||
                                        refRasters[0].getRasterHeight() != band.getRasterHeight() ||
                                        refRasters[0].getRasterWidth() != band.getRasterWidth())) {
                            SnapDialogs.showInformation("Referenced rasters are incompatible", null);
                        } else {
                            band.setValidPixelExpression(val);
                        }
                        // todo - add undoable edit
                    } catch (ParseException e) {
                        SnapDialogs.showError("Expression is invalid: " + e.getMessage());
                    }
                }
            });
            set.put(new PropertySupport.ReadWrite<Float>("spectralWavelength", Float.class, "Spectral Wavelength", "The spectral wavelength in nanometers") {
                @Override
                public Float getValue() {
                    return band.getSpectralWavelength();
                }

                @Override
                public void setValue(Float val) {
                    band.setSpectralWavelength(val);
                    // todo - add undoable edit
                }
            });
            set.put(new PropertySupport.ReadWrite<Float>("spectralBandWidth", Float.class, "Spectral Bandwidth", "The spectral bandwidth in nanometers") {
                @Override
                public Float getValue() {
                    return band.getSpectralBandwidth();
                }

                @Override
                public void setValue(Float val) {
                    band.setSpectralBandwidth(val);
                    // todo - add undoable edit
                }
            });
            Property<RasterDataNode[]> ancillaryVariables = new PropertySupport.ReadWrite<RasterDataNode[]>("ancillaryVariables",
                                                                                                            RasterDataNode[].class,
                                                                                                            "Ancillary Variables",
                                                                                                            "Names of rasters that serve as ancillary variables, e.g. provide uncertainty") {
                private WeakReference<RastersPropertyEditor> propertyEditorRef;

                @Override
                public RasterDataNode[] getValue() {
                    return band.getAncillaryVariables();
                }

                @Override
                public void setValue(RasterDataNode[] ancillaryVariablesNew) {
                    RasterDataNode[] ancillaryVariablesOld = band.getAncillaryVariables();
                    for (RasterDataNode newVar : ancillaryVariablesNew) {
                        band.addAncillaryVariable(newVar);
                    }
                    HashSet<RasterDataNode> newSet = new HashSet<>(Arrays.asList(ancillaryVariablesNew));
                    for (RasterDataNode oldVar : ancillaryVariablesOld) {
                        if (!newSet.contains(oldVar)) {
                            band.removeAncillaryVariable(oldVar);
                        }
                    }
                    // todo - add undoable edit
                }

                @Override
                public PropertyEditor getPropertyEditor() {
                    RastersPropertyEditor propertyEditor = null;
                    if (propertyEditorRef != null) {
                        propertyEditor = propertyEditorRef.get();
                    }
                    if (propertyEditor == null) {
                        propertyEditor = new RastersPropertyEditor(band.getProduct().getBands());
                        propertyEditorRef = new WeakReference<>(propertyEditor);
                    }
                    return propertyEditor;
                }
            };
            set.put(ancillaryVariables);

            set.put(new PropertySupport.ReadWrite<String>("ancillaryRelation",
                                                          String.class,
                                                          "Ancillary Relation",
                                                          "Relation name used if this raster is an ancillary variable") {
                @Override
                public String getValue() {
                    return band.getAncillaryRelation();
                }

                @Override
                public void setValue(String val) {
                    band.setAncillaryRelation(val);
                    // todo - add undoable edit
                }
            });

            if (band instanceof VirtualBand) {
                final VirtualBand virtualBand = (VirtualBand) band;

                set.put(new PropertySupport.ReadWrite<String>("expression", String.class, "Expression", "Mathematical expression used to compute the raster's pixel values") {
                    @Override
                    public String getValue() {
                        return virtualBand.getExpression();
                    }

                    @Override
                    public void setValue(String val) {
                        virtualBand.setExpression(val);
                        // todo - add undoable edit
                    }
                });
            }

            return Stream.concat(Stream.of(super.getPropertySets()), Stream.of(set)).toArray(PropertySet[]::new);
        }
    }

}
