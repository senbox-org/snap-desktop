/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.ui.product;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.grender.Viewport;
import com.bc.ceres.grender.support.BufferedImageRendering;
import com.bc.ceres.grender.support.DefaultViewport;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import eu.esa.snap.core.datamodel.group.BandGroup;
import eu.esa.snap.core.datamodel.group.BandGroupsManager;
import org.apache.commons.lang3.ArrayUtils;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.dataop.barithm.BandArithmetic;
import org.esa.snap.core.gpf.common.SubsetOp;
import org.esa.snap.core.image.ColoredBandImageMultiLevelSource;
import org.esa.snap.core.jexp.ParseException;
import org.esa.snap.core.jexp.Term;
import org.esa.snap.core.metadata.MetadataInspector;
import org.esa.snap.core.param.ParamChangeEvent;
import org.esa.snap.core.param.ParamChangeListener;
import org.esa.snap.core.param.ParamGroup;
import org.esa.snap.core.param.Parameter;
import org.esa.snap.core.subset.PixelSubsetRegion;
import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.ui.GridBagUtils;
import org.esa.snap.ui.ModalDialog;
import org.esa.snap.ui.SliderBoxImageDisplay;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.ui.util.GeoCodingUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A modal dialog used to specify data product subsets.
 */
public class ProductSubsetDialog extends ModalDialog {

    private static final String MEM_LABEL_TEXT = "Estimated, raw storage size: "; /*I18N*/
    private static final Color MEM_LABEL_WARN_COLOR = Color.red;
    private static final Color MEM_LABEL_NORM_COLOR = Color.black;
    private static final int MAX_THUMBNAIL_WIDTH = 148;
    private static final int MIN_SUBSET_SIZE = 1;
    private static final Font SMALL_PLAIN_FONT = new Font("SansSerif", Font.PLAIN, 10);
    private static final Font SMALL_ITALIC_FONT = SMALL_PLAIN_FONT.deriveFont(Font.ITALIC);

    private final Product product;
    private ProductSubsetDef productSubsetDef;
    private final ProductSubsetDef givenProductSubsetDef;
    private JLabel memLabel;
    private SpatialSubsetPane spatialSubsetPane;
    private ProductNodeSubsetPane bandSubsetPane;
    private ProductNodeSubsetPane tiePointGridSubsetPane;
    private ProductNodeSubsetPane metadataSubsetPane;
    private final double memWarnLimit;
    private static final double DEFAULT_MEM_WARN_LIMIT = 1000.0;
    private final AtomicBoolean updatingUI;
    private final BandGroupsManager bandGroupsManager;
    private final ProductSubsetByPolygonUiComponents productSubsetByPolygonUiComponents = new ProductSubsetByPolygonUiComponents(this.getJDialog());

    /**
     * Constructs a new subset dialog.
     *
     * @param window  the parent window
     * @param product the product for which the subset is to be specified, must not be {@code null}
     */
    public ProductSubsetDialog(Window window, Product product) {
        this(window, product, DEFAULT_MEM_WARN_LIMIT);
    }

    /**
     * Constructs a new subset dialog.
     *
     * @param window       the parent window
     * @param product      the product for which the subset is to be specified, must not be {@code null}
     * @param memWarnLimit the warning limit in megabytes
     */
    public ProductSubsetDialog(Window window, Product product, double memWarnLimit) {
        this(window, product, null, memWarnLimit);
    }

    /**
     * Constructs a new subset dialog.
     *
     * @param window           the parent window
     * @param product          the product for which the subset is to be specified, must not be {@code null}
     * @param productSubsetDef the initial product subset definition, can be {@code null}
     */
    public ProductSubsetDialog(Window window,
                               Product product,
                               ProductSubsetDef productSubsetDef) {
        this(window, product, productSubsetDef, DEFAULT_MEM_WARN_LIMIT);
    }

    /**
     * Constructs a new subset dialog.
     *
     * @param window           the parent window
     * @param product          the product for which the subset is to be specified, must not be {@code null}
     * @param productSubsetDef the initial product subset definition, can be {@code null}
     * @param memWarnLimit     the warning limit in megabytes
     */
    public ProductSubsetDialog(Window window,
                               Product product,
                               ProductSubsetDef productSubsetDef,
                               double memWarnLimit) {
        super(window, "Specify Product Subset", ID_OK | ID_CANCEL | ID_HELP, "subsetDialog");
        Guardian.assertNotNull("product", product);
        this.product = product;
        givenProductSubsetDef = productSubsetDef;
        this.productSubsetDef = new ProductSubsetDef("undefined");
        this.memWarnLimit = memWarnLimit;
        updatingUI = new AtomicBoolean(false);
        bandGroupsManager = getBandGroupsManager();
        createUI();
    }

    public Product getProduct() {
        return product;
    }

    public ProductSubsetDef getProductSubsetDef() {
        return productSubsetDef;
    }

    @Override
    protected void onOK() {
        boolean ok;
        ok = checkReferencedRastersIncluded();
        if (!ok) {
            return;
        }
        ok = checkFlagDatasetIncluded();
        if (!ok) {
            return;
        }

        spatialSubsetPane.cancelThumbnailLoader();
        spatialSubsetPane.updateProductSubset();
        if (productSubsetDef != null && productSubsetDef.isEntireProductSelected()) {
            productSubsetDef = null;
        }
        super.onOK();
    }

    private boolean checkReferencedRastersIncluded() {
        final Set<String> notIncludedNames = new TreeSet<>();
        String[] nodeNames = productSubsetDef.getNodeNames();
        if (nodeNames != null) {
            final String[] includedNodeNames = nodeNames;
            for (final String nodeName : includedNodeNames) {
                final RasterDataNode rasterDataNode = product.getRasterDataNode(nodeName);
                if (rasterDataNode != null) {
                    collectNotIncludedReferences(rasterDataNode, notIncludedNames);
                }
            }
        }

        boolean ok = true;
        if (!notIncludedNames.isEmpty()) {
            StringBuilder nameListText = new StringBuilder();
            for (String notIncludedName : notIncludedNames) {
                nameListText.append("  '").append(notIncludedName).append("'\n");
            }

            final String pattern = "The following dataset(s) are referenced but not included\n" +
                    "in your current subset definition:\n" +
                    "{0}\n" +
                    "If you do not include these dataset(s) into your selection,\n" +
                    "you might get unexpected results while working with the\n" +
                    "resulting product.\n\n" +
                    "Do you wish to include the referenced dataset(s) into your\n" +
                    "subset definition?\n"; /*I18N*/
            final MessageFormat format = new MessageFormat(pattern);
            int status = JOptionPane.showConfirmDialog(getJDialog(),
                    format.format(new Object[]{nameListText.toString()}),
                    "Incomplete Subset Definition", /*I18N*/
                    JOptionPane.YES_NO_CANCEL_OPTION);
            if (status == JOptionPane.YES_OPTION) {
                final String[] nodenames = notIncludedNames.toArray(new String[notIncludedNames.size()]);
                productSubsetDef.addNodeNames(nodenames);
                ok = true;
            } else if (status == JOptionPane.NO_OPTION) {
                ok = true;
            } else if (status == JOptionPane.CANCEL_OPTION) {
                ok = false;
            }
        }
        return ok;
    }

    private void collectNotIncludedReferences(final RasterDataNode rasterDataNode, final Set<String> notIncludedNames) {
        final RasterDataNode[] referencedNodes = getReferencedNodes(rasterDataNode);
        for (final RasterDataNode referencedNode : referencedNodes) {
            final String name = referencedNode.getName();
            if (!productSubsetDef.isNodeAccepted(name) && !notIncludedNames.contains(name)) {
                notIncludedNames.add(name);
                collectNotIncludedReferences(referencedNode, notIncludedNames);
            }
        }
    }

    private static RasterDataNode[] getReferencedNodes(final RasterDataNode node) {
        final Product product = node.getProduct();
        if (product != null) {
            final List<String> expressions = new ArrayList<>(10);
            if (node.getValidPixelExpression() != null) {
                expressions.add(node.getValidPixelExpression());
            }
            final ProductNodeGroup<Mask> overlayMaskGroup = node.getOverlayMaskGroup();
            if (overlayMaskGroup.getNodeCount() > 0) {
                final Mask[] overlayMasks = overlayMaskGroup.toArray(new Mask[overlayMaskGroup.getNodeCount()]);
                for (final Mask overlayMask : overlayMasks) {
                    final String expression;
                    if (overlayMask.getImageType() == Mask.BandMathsType.INSTANCE) {
                        expression = Mask.BandMathsType.getExpression(overlayMask);
                    } else if (overlayMask.getImageType() == Mask.RangeType.INSTANCE) {
                        expression = Mask.RangeType.getExpression(overlayMask);
                    } else {
                        expression = null;
                    }
                    if (expression != null) {
                        expressions.add(expression);
                    }
                }
            }
            if (node instanceof VirtualBand) {
                final VirtualBand virtualBand = (VirtualBand) node;
                expressions.add(virtualBand.getExpression());
            }

            final ArrayList<Term> termList = new ArrayList<>(10);
            for (final String expression : expressions) {
                try {
                    final Term term = product.parseExpression(expression);
                    if (term != null) {
                        termList.add(term);
                    }
                } catch (ParseException e) {
                    // @todo se handle parse exception
                    Debug.trace(e);
                }
            }

            return BandArithmetic.getRefRasters(termList.toArray(new Term[termList.size()]));
        }
        return new RasterDataNode[0];
    }

    private boolean checkFlagDatasetIncluded() {
        final String[] nodeNames = productSubsetDef.getNodeNames();
        final List<String> flagDsNameList = new ArrayList<>(10);
        boolean flagDsInSubset = false;
        for (int i = 0; i < product.getNumBands(); i++) {
            Band band = product.getBandAt(i);
            if (band.getFlagCoding() != null) {
                flagDsNameList.add(band.getName());
                if (StringUtils.contains(nodeNames, band.getName())) {
                    flagDsInSubset = true;
                }
                break;
            }
        }

        final int numFlagDs = flagDsNameList.size();
        boolean ok = true;
        if (numFlagDs > 0 && !flagDsInSubset) {
            int status = JOptionPane.showConfirmDialog(getJDialog(),
                    "No flag dataset selected.\n\n"
                            + "If you do not include a flag dataset in the subset,\n"
                            + "you will not be able to create bitmask overlays.\n\n"
                            + "Do you wish to include the available flag dataset(s)\n"
                            + "in the current subset?\n",
                    "No Flag Dataset Selected",
                    JOptionPane.YES_NO_CANCEL_OPTION
            );
            if (status == JOptionPane.YES_OPTION) {
                productSubsetDef.addNodeNames(flagDsNameList.toArray(new String[numFlagDs]));
                ok = true;
            } else if (status == JOptionPane.NO_OPTION) {
                /* OK, no flag datasets wanted */
                ok = true;
            } else if (status == JOptionPane.CANCEL_OPTION) {
                ok = false;
            }
        }

        return ok;
    }

    @Override
    protected void onCancel() {
        spatialSubsetPane.cancelThumbnailLoader();
        super.onCancel();
    }

    private BandGroupsManager getBandGroupsManager() {
        final BandGroupsManager bandGroupsManager;
        try {
            bandGroupsManager = BandGroupsManager.getInstance();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return bandGroupsManager;
    }

    private void createUI() {
        memLabel = new JLabel("####", SwingConstants.RIGHT);

        JTabbedPane tabbedPane = new JTabbedPane();
        setComponentName(tabbedPane, "TabbedPane");

        spatialSubsetPane = createSpatialSubsetPane();
        setComponentName(spatialSubsetPane, "SpatialSubsetPane");
        if (spatialSubsetPane != null) {
            tabbedPane.addTab("Spatial Subset", spatialSubsetPane); /*I18N*/
        }

        bandSubsetPane = createBandSubsetPane();
        setComponentName(bandSubsetPane, "BandSubsetPane");
        if (bandSubsetPane != null) {
            tabbedPane.addTab("Band Subset", bandSubsetPane);
        }

        tiePointGridSubsetPane = createTiePointGridSubsetPane();
        setComponentName(tiePointGridSubsetPane, "TiePointGridSubsetPane");
        if (tiePointGridSubsetPane != null) {
            tabbedPane.addTab("Tie-Point Grid Subset", tiePointGridSubsetPane);
        }

        metadataSubsetPane = createAnnotationSubsetPane();
        setComponentName(metadataSubsetPane, "MetadataSubsetPane");
        if (metadataSubsetPane != null) {
            tabbedPane.addTab("Metadata Subset", metadataSubsetPane);
        }

        tabbedPane.setPreferredSize(new Dimension(600, 400));
        tabbedPane.setSelectedIndex(0);

        JPanel contentPane = new JPanel(new BorderLayout(4, 4));
        setComponentName(contentPane, "ContentPane");

        contentPane.add(tabbedPane, BorderLayout.CENTER);
        contentPane.add(memLabel, BorderLayout.SOUTH);
        setContent(contentPane);

        updateSubsetDefNodeNameList();
    }

    private SpatialSubsetPane createSpatialSubsetPane() {
        return new SpatialSubsetPane();
    }

    private ProductNodeSubsetPane createBandSubsetPane() {
        Band[] bands = product.getBands();
        if (bands.length == 0) {
            return null;
        }
        return new ProductNodeSubsetPane(product.getBands(),
                new String[]{"latitude", "longitude"},
                true);
    }

    private ProductNodeSubsetPane createTiePointGridSubsetPane() {
        TiePointGrid[] tiePointGrids = product.getTiePointGrids();
        if (tiePointGrids.length == 0) {
            return null;
        }
        String[] geoCodingTiePointNames = GeoCodingUtil.getTiePointGridsFromGeoCoding(product.getSceneGeoCoding());
        String[] alwaysIncludedTiePointGridNames = ArrayUtils.addAll(geoCodingTiePointNames, "latitude", "longitude");

        return new ProductNodeSubsetPane(product.getTiePointGrids(),
                alwaysIncludedTiePointGridNames,
                true);
    }

    private ProductNodeSubsetPane createAnnotationSubsetPane() {
        final MetadataElement metadataRoot = product.getMetadataRoot();
        final MetadataElement[] metadataElements = metadataRoot.getElements();
        final String[] metaNodes;
        if (metadataElements.length == 0) {
            return null;
        }
        // metadata elements must be added to includeAlways list
        // to ensure that they are selected if isIgnoreMetadata is set to false
        if (givenProductSubsetDef != null && !givenProductSubsetDef.isIgnoreMetadata()) {
            metaNodes = new String[metadataElements.length];
            for (int i = 0; i < metadataElements.length; i++) {
                final MetadataElement metadataElement = metadataElements[i];
                metaNodes[i] = metadataElement.getName();
            }
        } else {
            metaNodes = new String[0];
        }
        final String[] includeNodes = StringUtils.addToArray(metaNodes, Product.HISTORY_ROOT_NAME);
        return new ProductNodeSubsetPane(metadataElements, includeNodes, true);
    }

    private static void setComponentName(JComponent component, String name) {
        if (component != null) {
            Container parent = component.getParent();
            if (parent != null) {
                component.setName(parent.getName() + "." + name);
            } else {
                component.setName(name);
            }
        }
    }

    private void updateSubsetDefNodeNameList() {
        /* We don't use this option! */
        productSubsetDef.setIgnoreMetadata(false);
        productSubsetDef.setNodeNames(null);
        if (bandSubsetPane != null) {
            productSubsetDef.addNodeNames(bandSubsetPane.getSubsetNames());
        }
        if (tiePointGridSubsetPane != null) {
            productSubsetDef.addNodeNames(tiePointGridSubsetPane.getSubsetNames());
        }
        if (metadataSubsetPane != null) {
            productSubsetDef.addNodeNames(metadataSubsetPane.getSubsetNames());
        }
        updateMemDisplay();
    }

    private void updateMemDisplay() {
        if (product != null) {
            long storageMem = product.getRawStorageSize(productSubsetDef);
            double factor = 1.0 / (1024 * 1024);
            double megas = MathUtils.round(factor * storageMem, 10);
            if (megas > memWarnLimit) {
                memLabel.setForeground(MEM_LABEL_WARN_COLOR);
            } else {
                memLabel.setForeground(MEM_LABEL_NORM_COLOR);
            }
            memLabel.setText(MEM_LABEL_TEXT + megas + "M");
        } else {
            memLabel.setText(" ");
        }
    }

    private class SpatialSubsetPane extends JPanel
            implements ActionListener, ParamChangeListener, SliderBoxImageDisplay.SliderBoxChangeListener {

        final JTabbedPane tabbedPane = new JTabbedPane();
        private Parameter paramX1;
        private Parameter paramY1;
        private Parameter paramX2;
        private Parameter paramY2;
        private Parameter paramSX;
        private Parameter paramSY;
        private Parameter paramWestLon1;
        private Parameter paramEastLon2;
        private Parameter paramNorthLat1;
        private Parameter paramSouthLat2;

        private SliderBoxImageDisplay imageCanvas;
        private JCheckBox fixSceneWidthCheck;
        private JCheckBox fixSceneHeightCheck;
        private JLabel subsetWidthLabel;
        private JLabel subsetHeightLabel;
        private JLabel sourceWidthLabel;
        private JLabel sourceHeightLabel;
        private int thumbNailSubSampling;
        private JButton setToVisibleButton;
        private JScrollPane imageScrollPane;
        private ProgressMonitorSwingWorker<BufferedImage, Object> thumbnailLoader;

        private JComboBox referenceCombo;
        String _oldReference;

        private SpatialSubsetPane() {
            if (product.isMultiSize()) {
                referenceCombo = new JComboBox();
                for (String bandName : product.getBandNames()) {
                    referenceCombo.addItem(bandName);
                }
                referenceCombo.setSelectedItem(product.getBandAt(0));
                _oldReference = (String) referenceCombo.getSelectedItem();
                referenceCombo.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        int w;
                        int h;
                        if (product.isMultiSize()) {
                            w = product.getBand((String) referenceCombo.getSelectedItem()).getRasterWidth();
                            h = product.getBand((String) referenceCombo.getSelectedItem()).getRasterHeight();
                        } else {
                            w = product.getSceneRasterWidth();
                            h = product.getSceneRasterHeight();
                        }
                        final int wMin = MIN_SUBSET_SIZE;
                        final int hMin = MIN_SUBSET_SIZE;
                        paramX1.getProperties().setMaxValue((w - wMin - 1) > 0 ? w - wMin - 1 : 0);
                        paramY1.getProperties().setMaxValue((h - hMin - 1) > 0 ? h - hMin - 1 : 0);
                        paramX2.getProperties().setMaxValue(w - 1);
                        paramY2.getProperties().setMaxValue(h - 1);

                        updateUIState(new ParamChangeEvent(this, new Parameter("geo_"), null));
                        _oldReference = (String) referenceCombo.getSelectedItem();
                    }
                });
            }
            initParameters();
            createUI();
        }

        private void createUI() {


            setThumbnailSubsampling();
            final Dimension imageSize = getScaledImageSize();
            thumbnailLoader = new ProgressMonitorSwingWorker<BufferedImage, Object>(this,
                    "Loading thumbnail image...") {

                @Override
                protected BufferedImage doInBackground(ProgressMonitor pm) throws Exception {
                    return createThumbNailImage(imageSize, pm);
                }

                @Override
                protected void done() {
                    BufferedImage thumbnail = null;
                    try {
                        thumbnail = get();
                    } catch (Exception ignored) {
                    }

                    if (thumbnail != null) {
                        imageCanvas.setImage(thumbnail);
                    }

                }

            };
            thumbnailLoader.execute();
            imageCanvas = new SliderBoxImageDisplay(imageSize.width, imageSize.height, this);
            imageCanvas.setSize(imageSize.width, imageSize.height);
            setComponentName(imageCanvas, "ImageCanvas");


            imageScrollPane = new JScrollPane(imageCanvas);
            imageScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            imageScrollPane.getVerticalScrollBar().setUnitIncrement(20);
            imageScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
            imageScrollPane.getHorizontalScrollBar().setUnitIncrement(20);
            imageScrollPane.getViewport().setExtentSize(new Dimension(MAX_THUMBNAIL_WIDTH, 2 * MAX_THUMBNAIL_WIDTH));
            setComponentName(imageScrollPane, "ImageScrollPane");


            subsetWidthLabel = new JLabel("####", SwingConstants.RIGHT);
            subsetHeightLabel = new JLabel("####", SwingConstants.RIGHT);


            int sceneWidth;
            int sceneHeight;
            if (product.isMultiSize()) {
                sceneWidth = product.getBand((String) referenceCombo.getSelectedItem()).getRasterWidth();
                sceneHeight = product.getBand((String) referenceCombo.getSelectedItem()).getRasterHeight();
            } else {
                sceneWidth = product.getSceneRasterWidth();
                sceneHeight = product.getSceneRasterHeight();
            }
            sourceWidthLabel = new JLabel(String.valueOf(sceneWidth), SwingConstants.RIGHT);
            sourceHeightLabel = new JLabel(String.valueOf(sceneHeight), SwingConstants.RIGHT);

            setToVisibleButton = new JButton("Use Preview");/*I18N*/
            setToVisibleButton.setMnemonic('v');
            setToVisibleButton.setToolTipText("Use coordinates of visible thumbnail area"); /*I18N*/
            setToVisibleButton.addActionListener(this);
            setComponentName(setToVisibleButton, "UsePreviewButton");


            fixSceneWidthCheck = new JCheckBox("Fix full width");
            fixSceneWidthCheck.setMnemonic('w');
            fixSceneWidthCheck.setToolTipText("Checks whether or not to fix the full scene width");
            fixSceneWidthCheck.addActionListener(this);
            setComponentName(fixSceneWidthCheck, "FixWidthCheck");

            fixSceneHeightCheck = new JCheckBox("Fix full height");
            fixSceneHeightCheck.setMnemonic('h');
            fixSceneHeightCheck.setToolTipText("Checks whether or not to fix the full scene height");
            fixSceneHeightCheck.addActionListener(this);
            setComponentName(fixSceneHeightCheck, "FixHeightCheck");


            JPanel textInputPane = GridBagUtils.createPanel();
            setComponentName(textInputPane, "TextInputPane");
            setComponentName(tabbedPane, "coordinatePane");
            tabbedPane.addTab("Pixel Coordinates", createPixelCoordinatesPane());
            tabbedPane.addTab("Geo Coordinates", createGeoCoordinatesPane());
            tabbedPane.addTab("Polygon", productSubsetByPolygonUiComponents.getImportVectorFilePanel());
            tabbedPane.setEnabledAt(1, canUseGeoCoordinates(product));

            final MetadataInspector.Metadata productMetadata = new MetadataInspector.Metadata(product.getSceneRasterWidth(), product.getSceneRasterHeight());
            productMetadata.setGeoCoding(product.getSceneGeoCoding());
            productSubsetByPolygonUiComponents.setTargetProductMetadata(productMetadata);

            GridBagConstraints gbc = GridBagUtils.createConstraints(
                    "insets.left=7,anchor=WEST,fill=HORIZONTAL, weightx=1.0");
            GridBagUtils.setAttributes(gbc, "gridwidth=2");
            GridBagUtils.addToPanel(textInputPane, tabbedPane, gbc, "gridx=0,gridy=0");

            GridBagUtils.setAttributes(gbc, "insets.top=7,gridwidth=1");
            GridBagUtils.addToPanel(textInputPane, new JLabel("Scene step X:"), gbc, "gridx=0,gridy=1");
            GridBagUtils.addToPanel(textInputPane, UIUtils.createSpinner(paramSX, 1, "#0"), gbc, "gridx=1,gridy=1");
            GridBagUtils.setAttributes(gbc, "insets.top=1");
            GridBagUtils.addToPanel(textInputPane, new JLabel("Scene step Y:"), gbc, "gridx=0,gridy=2");
            GridBagUtils.addToPanel(textInputPane, UIUtils.createSpinner(paramSY, 1, "#0"), gbc, "gridx=1,gridy=2");

            GridBagUtils.setAttributes(gbc, "insets.top=4");
            GridBagUtils.addToPanel(textInputPane, new JLabel("Subset scene width:"), gbc, "gridx=0,gridy=3");
            GridBagUtils.addToPanel(textInputPane, subsetWidthLabel, gbc, "gridx=1,gridy=3");

            GridBagUtils.setAttributes(gbc, "insets.top=1");
            GridBagUtils.addToPanel(textInputPane, new JLabel("Subset scene height:"), gbc, "gridx=0,gridy=4");
            GridBagUtils.addToPanel(textInputPane, subsetHeightLabel, gbc, "gridx=1,gridy=4");


            GridBagUtils.setAttributes(gbc, "insets.top=4,gridwidth=1");
            GridBagUtils.addToPanel(textInputPane, new JLabel("Source scene width:"), gbc, "gridx=0,gridy=5");
            GridBagUtils.addToPanel(textInputPane, sourceWidthLabel, gbc, "gridx=1,gridy=5");

            GridBagUtils.setAttributes(gbc, "insets.top=1");
            GridBagUtils.addToPanel(textInputPane, new JLabel("Source scene height:"), gbc, "gridx=0,gridy=6");
            GridBagUtils.addToPanel(textInputPane, sourceHeightLabel, gbc, "gridx=1,gridy=6");

            GridBagUtils.setAttributes(gbc, "insets.top=7,gridwidth=1, gridheight=2");
            GridBagUtils.addToPanel(textInputPane, setToVisibleButton, gbc, "gridx=0,gridy=7");

            GridBagUtils.setAttributes(gbc, "insets.top=7,gridwidth=1, gridheight=1");
            GridBagUtils.addToPanel(textInputPane, fixSceneWidthCheck, gbc, "gridx=1,gridy=7");

            GridBagUtils.setAttributes(gbc, "insets.top=1,gridwidth=1");
            GridBagUtils.addToPanel(textInputPane, fixSceneHeightCheck, gbc, "gridx=1,gridy=8");

            JPanel referencePanel = new JPanel();
            if (product.isMultiSize()) {
                BoxLayout boxlayoutRef = new BoxLayout(referencePanel, BoxLayout.X_AXIS);
                referencePanel.setLayout(boxlayoutRef);
                referencePanel.add(new JLabel("Reference Band:"));
                referencePanel.add(referenceCombo);
            }

            JPanel centerPanel = new JPanel();
            BoxLayout boxlayout = new BoxLayout(centerPanel, BoxLayout.Y_AXIS);
            centerPanel.setLayout(boxlayout);
            centerPanel.add(referencePanel);
            centerPanel.add(textInputPane);


            setLayout(new BorderLayout(4, 4));
            add(imageScrollPane, BorderLayout.WEST);
            add(centerPanel, BorderLayout.CENTER);
            setBorder(BorderFactory.createEmptyBorder(7, 7, 7, 7));

            updateUIState(null);
            imageCanvas.scrollRectToVisible(imageCanvas.getSliderBoxBounds());
        }

        private boolean canUseGeoCoordinates(Product product) {
            final GeoCoding geoCoding = product.getSceneGeoCoding();
            return geoCoding != null && geoCoding.canGetPixelPos() && geoCoding.canGetGeoPos();
        }

        private boolean canUseGeoCoordinates(RasterDataNode rasterDataNode) {
            final GeoCoding geoCoding = rasterDataNode.getGeoCoding();
            return geoCoding != null && geoCoding.canGetPixelPos() && geoCoding.canGetGeoPos();
        }

        private JPanel createGeoCoordinatesPane() {
            JPanel geoCoordinatesPane = GridBagUtils.createPanel();
            setComponentName(geoCoordinatesPane, "geoCoordinatesPane");

            GridBagConstraints gbc = GridBagUtils.createConstraints(
                    "insets.left=3,anchor=WEST,fill=HORIZONTAL, weightx=1.0");
            GridBagUtils.setAttributes(gbc, "insets.top=4");
            GridBagUtils.addToPanel(geoCoordinatesPane, new JLabel("North latitude bound:"), gbc, "gridx=0,gridy=0");
            GridBagUtils.addToPanel(geoCoordinatesPane, UIUtils.createSpinner(paramNorthLat1, 1.0, "#0.00#"),
                    gbc, "gridx=1,gridy=0");
            GridBagUtils.setAttributes(gbc, "insets.top=1");
            GridBagUtils.addToPanel(geoCoordinatesPane, new JLabel("West longitude bound:"), gbc, "gridx=0,gridy=1");
            GridBagUtils.addToPanel(geoCoordinatesPane, UIUtils.createSpinner(paramWestLon1, 1.0, "#0.00#"),
                    gbc, "gridx=1,gridy=1");

            GridBagUtils.setAttributes(gbc, "insets.top=4");
            GridBagUtils.addToPanel(geoCoordinatesPane, new JLabel("South latitude bound:"), gbc, "gridx=0,gridy=2");
            GridBagUtils.addToPanel(geoCoordinatesPane, UIUtils.createSpinner(paramSouthLat2, 1.0, "#0.00#"),
                    gbc, "gridx=1,gridy=2");
            GridBagUtils.setAttributes(gbc, "insets.top=1");
            GridBagUtils.addToPanel(geoCoordinatesPane, new JLabel("East longitude bound:"), gbc, "gridx=0,gridy=3");
            GridBagUtils.addToPanel(geoCoordinatesPane, UIUtils.createSpinner(paramEastLon2, 1.0, "#0.00#"),
                    gbc, "gridx=1,gridy=3");
            return geoCoordinatesPane;
        }

        private JPanel createPixelCoordinatesPane() {
            GridBagConstraints gbc = GridBagUtils.createConstraints(
                    "insets.left=3,anchor=WEST,fill=HORIZONTAL, weightx=1.0");
            JPanel pixelCoordinatesPane = GridBagUtils.createPanel();
            setComponentName(pixelCoordinatesPane, "pixelCoordinatesPane");

            GridBagUtils.setAttributes(gbc, "insets.top=4");
            GridBagUtils.addToPanel(pixelCoordinatesPane, new JLabel("Scene start X:"), gbc, "gridx=0,gridy=0");
            GridBagUtils.addToPanel(pixelCoordinatesPane, UIUtils.createSpinner(paramX1, 25, "#0"),
                    gbc, "gridx=1,gridy=0");
            GridBagUtils.setAttributes(gbc, "insets.top=1");
            GridBagUtils.addToPanel(pixelCoordinatesPane, new JLabel("Scene start Y:"), gbc, "gridx=0,gridy=1");
            GridBagUtils.addToPanel(pixelCoordinatesPane, UIUtils.createSpinner(paramY1, 25, "#0"),
                    gbc, "gridx=1,gridy=1");

            GridBagUtils.setAttributes(gbc, "insets.top=4");
            GridBagUtils.addToPanel(pixelCoordinatesPane, new JLabel("Scene end X:"), gbc, "gridx=0,gridy=2");
            GridBagUtils.addToPanel(pixelCoordinatesPane, UIUtils.createSpinner(paramX2, 25, "#0"),
                    gbc, "gridx=1,gridy=2");
            GridBagUtils.setAttributes(gbc, "insets.top=1");
            GridBagUtils.addToPanel(pixelCoordinatesPane, new JLabel("Scene end Y:"), gbc, "gridx=0,gridy=3");
            GridBagUtils.addToPanel(pixelCoordinatesPane, UIUtils.createSpinner(paramY2, 25, "#0"),
                    gbc, "gridx=1,gridy=3");
            return pixelCoordinatesPane;
        }

        private void setThumbnailSubsampling() {
            int w;
            if (product.isMultiSize()) {
                w = product.getBand((String) referenceCombo.getSelectedItem()).getRasterWidth();
            } else {
                w = product.getSceneRasterWidth();
            }

            thumbNailSubSampling = w / MAX_THUMBNAIL_WIDTH;
            if (thumbNailSubSampling <= 1) {
                thumbNailSubSampling = 1;
            }
        }

        public void cancelThumbnailLoader() {
            if (thumbnailLoader != null) {
                thumbnailLoader.cancel(true);
            }
        }

        public boolean isThumbnailLoaderCanceled() {
            return thumbnailLoader != null && thumbnailLoader.isCancelled();
        }

        @Override
        public void sliderBoxChanged(Rectangle sliderBoxBounds) {
            int x1 = sliderBoxBounds.x * thumbNailSubSampling;
            int y1 = sliderBoxBounds.y * thumbNailSubSampling;
            int x2 = x1 + sliderBoxBounds.width * thumbNailSubSampling;
            int y2 = y1 + sliderBoxBounds.height * thumbNailSubSampling;
            int w = product.getSceneRasterWidth();
            int h = product.getSceneRasterHeight();
            if (product.isMultiSize()) {
                w = product.getBand((String) referenceCombo.getSelectedItem()).getRasterWidth();
                h = product.getBand((String) referenceCombo.getSelectedItem()).getRasterHeight();
            }
            if (x1 < 0) {
                x1 = 0;
            }
            if (x1 > w - 2) {
                x1 = w - 2;
            }
            if (y1 < 0) {
                y1 = 0;
            }
            if (y1 > h - 2) {
                y1 = h - 2;
            }
            if (x2 < 1) {
                x2 = 1;
            }
            if (x2 > w - 1) {
                x2 = w - 1;
            }
            if (y2 < 1) {
                y2 = 1;
            }
            if (y2 > h - 1) {
                y2 = h - 1;
            }
            // first reset the bounds, otherwise negative regions can occur
            paramX1.setValue(0, null);
            paramY1.setValue(0, null);
            paramX2.setValue(w - 1, null);
            paramY2.setValue(h - 1, null);

            paramX1.setValue(x1, null);
            paramY1.setValue(y1, null);
            paramX2.setValue(x2, null);
            paramY2.setValue(y2, null);
        }

        /**
         * Invoked when an action occurs.
         */
        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource().equals(fixSceneWidthCheck)) {
                imageCanvas.setImageWidthFixed(fixSceneWidthCheck.isSelected());
                final boolean enable = !fixSceneWidthCheck.isSelected();
                paramX1.setUIEnabled(enable);
                paramX2.setUIEnabled(enable);
            }
            if (e.getSource().equals(fixSceneHeightCheck)) {
                imageCanvas.setImageHeightFixed(fixSceneHeightCheck.isSelected());
                final boolean enable = !fixSceneHeightCheck.isSelected();
                paramY1.setUIEnabled(enable);
                paramY2.setUIEnabled(enable);
            }
            if (e.getSource().equals(setToVisibleButton)) {
                imageCanvas.setSliderBoxBounds(imageScrollPane.getViewport().getViewRect(), true);
            }
        }

        /**
         * Called if the value of a parameter changed.
         *
         * @param event the parameter change event
         */
        @Override
        public void parameterValueChanged(ParamChangeEvent event) {
            updateUIState(event);
        }

        private void initParameters() {
            ParamGroup pg = new ParamGroup();
            addPixelParameter(pg);
            addGeoParameter(pg);
            pg.addParamChangeListener(this);
        }

        private void addGeoParameter(ParamGroup pg) {

            paramNorthLat1 = new Parameter("geo_lat1", 90.0);
            paramNorthLat1.getProperties().setDescription("North bound latitude");
            paramNorthLat1.getProperties().setPhysicalUnit("°");
            paramNorthLat1.getProperties().setMinValue(-90.0);
            paramNorthLat1.getProperties().setMaxValue(90.0);
            pg.addParameter(paramNorthLat1);

            paramWestLon1 = new Parameter("geo_lon1", -180.0);
            paramWestLon1.getProperties().setDescription("West bound longitude");
            paramWestLon1.getProperties().setPhysicalUnit("°");
            paramWestLon1.getProperties().setMinValue(-180.0);
            paramWestLon1.getProperties().setMaxValue(180.0);
            pg.addParameter(paramWestLon1);

            paramSouthLat2 = new Parameter("geo_lat2", -90.0);
            paramSouthLat2.getProperties().setDescription("South bound latitude");
            paramSouthLat2.getProperties().setPhysicalUnit("°");
            paramSouthLat2.getProperties().setMinValue(-90.0);
            paramSouthLat2.getProperties().setMaxValue(90.0);
            pg.addParameter(paramSouthLat2);

            paramEastLon2 = new Parameter("geo_lon2", 180.0);
            paramEastLon2.getProperties().setDescription("East bound longitude");
            paramEastLon2.getProperties().setPhysicalUnit("°");
            paramEastLon2.getProperties().setMinValue(-180.0);
            paramEastLon2.getProperties().setMaxValue(180.0);
            pg.addParameter(paramEastLon2);

            boolean canUseGeocoding;
            if (product.isMultiSize()) {
                canUseGeocoding = canUseGeoCoordinates(product.getBand((String) referenceCombo.getSelectedItem()));
            } else {
                canUseGeocoding = canUseGeoCoordinates(product);
            }
            if (canUseGeocoding) {
                syncLatLonWithXYParams();
            }
        }

        private void addPixelParameter(ParamGroup pg) {
            int w;
            int h;
            if (product.isMultiSize()) {
                w = product.getBand((String) referenceCombo.getSelectedItem()).getRasterWidth();
                h = product.getBand((String) referenceCombo.getSelectedItem()).getRasterHeight();
            } else {
                w = product.getSceneRasterWidth();
                h = product.getSceneRasterHeight();
            }

            int x1 = 0;
            int y1 = 0;
            int x2 = w - 1;
            int y2 = h - 1;
            int sx = 1;
            int sy = 1;

            if (givenProductSubsetDef != null) {
                Rectangle region;
                if (product.isMultiSize() && givenProductSubsetDef.getRegionMap() != null && givenProductSubsetDef.getRegionMap().containsKey((String) referenceCombo.getSelectedItem())) {
                    region = givenProductSubsetDef.getRegionMap().get((String) referenceCombo.getSelectedItem()).getSubsetExtent();
                } else {
                    region = givenProductSubsetDef.getRegion();
                }

                if (region != null) {
                    x1 = region.x;
                    y1 = region.y;
                    final int preX2 = x1 + region.width - 1;
                    if (preX2 < x2) {
                        x2 = preX2;
                    }
                    final int preY2 = y1 + region.height - 1;
                    if (preY2 < y2) {
                        y2 = preY2;
                    }
                }
                sx = givenProductSubsetDef.getSubSamplingX();
                sy = givenProductSubsetDef.getSubSamplingY();
            }

            final int wMin = MIN_SUBSET_SIZE;
            final int hMin = MIN_SUBSET_SIZE;

            paramX1 = new Parameter("source_x1", x1);
            paramX1.getProperties().setDescription("Start X co-ordinate given in pixels"); /*I18N*/
            paramX1.getProperties().setMinValue(0);
            paramX1.getProperties().setMaxValue((w - wMin - 1) > 0 ? w - wMin - 1 : 0);

            paramY1 = new Parameter("source_y1", y1);
            paramY1.getProperties().setDescription("Start Y co-ordinate given in pixels"); /*I18N*/
            paramY1.getProperties().setMinValue(0);
            paramY1.getProperties().setMaxValue((h - hMin - 1) > 0 ? h - hMin - 1 : 0);

            paramX2 = new Parameter("source_x2", x2);
            paramX2.getProperties().setDescription("End X co-ordinate given in pixels");/*I18N*/
            paramX2.getProperties().setMinValue(wMin - 1);
            final Integer maxValue = w - 1;
            paramX2.getProperties().setMaxValue(maxValue);

            paramY2 = new Parameter("source_y2", y2);
            paramY2.getProperties().setDescription("End Y co-ordinate given in pixels");/*I18N*/
            paramY2.getProperties().setMinValue(hMin - 1);
            paramY2.getProperties().setMaxValue(h - 1);

            paramSX = new Parameter("source_sx", sx);
            paramSX.getProperties().setDescription("Sub-sampling in X-direction given in pixels");/*I18N*/
            paramSX.getProperties().setMinValue(1);
            paramSX.getProperties().setMaxValue(w / wMin + 1);

            paramSY = new Parameter("source_sy", sy);
            paramSY.getProperties().setDescription("Sub-sampling in Y-direction given in pixels");/*I18N*/
            paramSY.getProperties().setMinValue(1);
            paramSY.getProperties().setMaxValue(h / hMin + 1);

            pg.addParameter(paramX1);
            pg.addParameter(paramY1);
            pg.addParameter(paramX2);
            pg.addParameter(paramY2);
            pg.addParameter(paramSX);
            pg.addParameter(paramSY);
        }

        private void updateUIState(ParamChangeEvent event) {
            if (updatingUI.compareAndSet(false, true)) {
                try {
                    boolean canUseGeocoding;
                    if (product.isMultiSize()) {
                        canUseGeocoding = canUseGeoCoordinates(product.getBand((String) referenceCombo.getSelectedItem()));
                    } else {
                        canUseGeocoding = canUseGeoCoordinates(product);
                    }
                    if (event != null && canUseGeocoding) {
                        final String parmName = event.getParameter().getName();
                        updateParams(parmName);
                    }
                    updateProductSubset();
                    updateSubsetInfoUi();
                    updateThumbnailUi();
                } finally {
                    updatingUI.set(false);
                }
            }
        }

        private void updateParams(String parmName){
            if (parmName.startsWith("geo_")) {
                final GeoPos geoPos1 = new GeoPos((Double) paramNorthLat1.getValue(),
                        (Double) paramWestLon1.getValue());
                final GeoPos geoPos2 = new GeoPos((Double) paramSouthLat2.getValue(),
                        (Double) paramEastLon2.getValue());

                updateXYParams(geoPos1, geoPos2);
            } else if (parmName.startsWith("source_x") || parmName.startsWith("source_y")) {
                syncLatLonWithXYParams();
            }
        }

        private void updateProductSubset(){
            int sx = ((Number) paramSX.getValue()).intValue();
            int sy = ((Number) paramSY.getValue()).intValue();
            productSubsetDef.setSubSampling(sx, sy);
            final Rectangle subsetRectangle = getSubsetRectangle();
            final org.locationtech.jts.geom.Polygon subsetPolygon = getSubsetPolygon();
            if (product.isMultiSize()) {
                productSubsetDef.setRegionMap(SubsetOp.computeRegionMap(subsetRectangle, subsetPolygon,
                        (String) referenceCombo.getSelectedItem(),
                        product, null));
            } else {
                productSubsetDef.setSubsetRegion(new PixelSubsetRegion(subsetRectangle, 0));
            }
            updateMemDisplay();
        }


        private void updateSubsetInfoUi(){
            final Dimension s;
            if (product.isMultiSize()) {
                s = productSubsetDef.getSceneRasterSize(product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(),
                        (String) referenceCombo.getSelectedItem());
            } else {
                s = productSubsetDef.getSceneRasterSize(product.getSceneRasterWidth(),
                        product.getSceneRasterHeight());
            }
            subsetWidthLabel.setText(String.valueOf(s.getWidth()));
            subsetHeightLabel.setText(String.valueOf(s.getHeight()));
            final int sceneWidth;
            final int sceneHeight;
            if (product.isMultiSize()) {
                sceneWidth = product.getBand((String) referenceCombo.getSelectedItem()).getRasterWidth();
                sceneHeight = product.getBand((String) referenceCombo.getSelectedItem()).getRasterHeight();
            } else {
                sceneWidth = product.getSceneRasterWidth();
                sceneHeight = product.getSceneRasterHeight();
            }
            sourceHeightLabel.setText(String.valueOf(sceneHeight));
            sourceWidthLabel.setText(String.valueOf(sceneWidth));
        }

        private void updateThumbnailUi(){
            final int x1 = ((Number) paramX1.getValue()).intValue();
            final int y1 = ((Number) paramY1.getValue()).intValue();
            final int x2 = ((Number) paramX2.getValue()).intValue();
            final int y2 = ((Number) paramY2.getValue()).intValue();
            setThumbnailSubsampling();
            final int sliderBoxX1 = x1 / thumbNailSubSampling;
            final int sliderBoxY1 = y1 / thumbNailSubSampling;
            final int sliderBoxX2 = x2 / thumbNailSubSampling;
            final int sliderBoxY2 = y2 / thumbNailSubSampling;
            final int sliderBoxW = sliderBoxX2 - sliderBoxX1 + 1;
            final int sliderBoxH = sliderBoxY2 - sliderBoxY1 + 1;
            final Rectangle box = getScaledRectangle(new Rectangle(sliderBoxX1, sliderBoxY1, sliderBoxW, sliderBoxH));
            imageCanvas.setSliderBoxBounds(box);
        }

        private Rectangle getSubsetRectangle() {
            try {
                if (isOnPolygonTab() && productSubsetByPolygonUiComponents.getProductSubsetByPolygon().isLoaded()) {
                    final GeoCoding geoCoding = getProductGeocoding();
                    return productSubsetByPolygonUiComponents.getProductSubsetByPolygon().getExtentOfPolygonProjectedToGeocoding(geoCoding);
                } else {
                    final int x1 = ((Number) paramX1.getValue()).intValue();
                    final int y1 = ((Number) paramY1.getValue()).intValue();
                    final int x2 = ((Number) paramX2.getValue()).intValue();
                    final int y2 = ((Number) paramY2.getValue()).intValue();
                    return computeROIToPositiveAxis(x1, y1, x2, y2);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private org.locationtech.jts.geom.Polygon getSubsetPolygon() {
            if (isOnPolygonTab() && productSubsetByPolygonUiComponents.getProductSubsetByPolygon().isLoaded()) {
                final GeoCoding geoCoding = getProductGeocoding();
                return productSubsetByPolygonUiComponents.getProductSubsetByPolygon().getSubsetPolygonProjectedToGeocoding(geoCoding);
            }
            return null;
        }

        private boolean isOnPolygonTab(){
            return tabbedPane.getSelectedIndex() == 2;
        }

        private Rectangle computeROIToPositiveAxis(int x1, int y1, int x2, int y2) {
            // keep positive dimensions of the ROI
            int diffX = x2 - x1;
            int diffY = y2 - y1;
            if (diffX < 0)
                x1 = x1 + diffX - 1;
            if (diffY < 0)
                y1 = y1 + diffY - 1;
            return new Rectangle(x1, y1, Math.abs(diffX) + 1, Math.abs(diffY) + 1);
        }

        private void syncLatLonWithXYParams() {
            final PixelPos pixelPos1 = new PixelPos(((Number) paramX1.getValue()).intValue(),
                    ((Number) paramY1.getValue()).intValue());
            final PixelPos pixelPos2 = new PixelPos(((Number) paramX2.getValue()).intValue(),
                    ((Number) paramY2.getValue()).intValue());
            final GeoCoding geoCoding = getProductGeocoding();
            final GeoPos geoPos1 = geoCoding.getGeoPos(pixelPos1, null);
            final GeoPos geoPos2 = geoCoding.getGeoPos(pixelPos2, null);
            if (geoPos1.isValid()) {
                double lat = geoPos1.getLat();
                lat = MathUtils.crop(lat, -90.0, 90.0);
                paramNorthLat1.setValue(lat, ex -> true);
                double lon = geoPos1.getLon();
                lon = MathUtils.crop(lon, -180.0, 180.0);
                paramWestLon1.setValue(lon, ex -> true);
            }
            if (geoPos2.isValid()) {
                double lat = geoPos2.getLat();
                lat = MathUtils.crop(lat, -90.0, 90.0);
                paramSouthLat2.setValue(lat, ex -> true);
                double lon = geoPos2.getLon();
                lon = MathUtils.crop(lon, -180.0, 180.0);
                paramEastLon2.setValue(lon, ex -> true);
            }
        }

        private void updateXYParams(GeoPos geoPos1, GeoPos geoPos2) {
            final GeoCoding geoCoding = getProductGeocoding();
            final PixelPos pixelPos1 = geoCoding.getPixelPos(geoPos1, null);
            if (!pixelPos1.isValid()) {
                pixelPos1.setLocation(0, 0);
            }
            final PixelPos pixelPos2 = geoCoding.getPixelPos(geoPos2, null);
            if (!pixelPos2.isValid()) {
                if (product.isMultiSize()) {
                    pixelPos2.setLocation(product.getBand((String) referenceCombo.getSelectedItem()).getRasterWidth(),
                            product.getBand((String) referenceCombo.getSelectedItem()).getRasterHeight());
                } else {
                    pixelPos2.setLocation(product.getSceneRasterWidth(),
                            product.getSceneRasterHeight());
                }

            }
            final Rectangle2D.Float region = new Rectangle2D.Float();
            region.setFrameFromDiagonal(pixelPos1.x, pixelPos1.y, pixelPos2.x, pixelPos2.y);
            final Rectangle2D.Float productBounds;
            if (product.isMultiSize()) {
                productBounds = new Rectangle2D.Float(0, 0,
                        product.getBand((String) referenceCombo.getSelectedItem()).getRasterWidth(),
                        product.getBand((String) referenceCombo.getSelectedItem()).getRasterHeight());
            } else {
                productBounds = new Rectangle2D.Float(0, 0,
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight());
            }

            Rectangle2D finalRegion = productBounds.createIntersection(region);

            paramX1.setValue((int) finalRegion.getMinX(), ex -> true);
            paramY1.setValue((int) finalRegion.getMinY(), ex -> true);
            paramX2.setValue((int) finalRegion.getMaxX() - 1, ex -> true);
            paramY2.setValue((int) finalRegion.getMaxY() - 1, ex -> true);
        }

        private Dimension getScaledImageSize() {

            final int w;
            final int h;
            if (product.isMultiSize()) {
                w = (product.getBand((String) referenceCombo.getSelectedItem()).getRasterWidth() - 1) / thumbNailSubSampling + 1;
                h = (product.getBand((String) referenceCombo.getSelectedItem()).getRasterHeight() - 1) / thumbNailSubSampling + 1;
            } else {
                w = (product.getSceneRasterWidth() - 1) / thumbNailSubSampling + 1;
                h = (product.getSceneRasterHeight() - 1) / thumbNailSubSampling + 1;
            }
            final Rectangle rectangle = new Rectangle(w, h);
            return getScaledRectangle(rectangle).getSize();
        }

        private Rectangle getScaledRectangle(Rectangle rectangle) {

            final GeoCoding geoCoding = getProductGeocoding();
            final AffineTransform i2mTransform = Product.findImageToModelTransform(geoCoding);
            final double scaleX = i2mTransform.getScaleX();
            final double scaleY = i2mTransform.getScaleY();
            double scaleFactorY = Math.abs(scaleY / scaleX);
            final AffineTransform scaleTransform = AffineTransform.getScaleInstance(1.0, scaleFactorY);
            return scaleTransform.createTransformedShape(rectangle).getBounds();
        }

        private GeoCoding getProductGeocoding(){
            final GeoCoding geoCoding;
            if (product.isMultiSize()) {
                geoCoding = product.getBand((String) referenceCombo.getSelectedItem()).getGeoCoding();
            } else {
                geoCoding = product.getSceneGeoCoding();
            }
            return geoCoding;
        }

        private BufferedImage createThumbNailImage(Dimension imgSize, ProgressMonitor pm) {
            Assert.notNull(pm, "pm");

            String thumbNailBandName = getThumbnailBandName();
            Band thumbNailBand = product.getBand(thumbNailBandName);

            Debug.trace("ProductSubsetDialog: Reading thumbnail data for band '" + thumbNailBandName + "'...");
            pm.beginTask("Creating thumbnail image", 5);
            BufferedImage image = null;
            try {
                MultiLevelSource multiLevelSource = ColoredBandImageMultiLevelSource.create(thumbNailBand,
                        SubProgressMonitor.create(pm, 1));
                final ImageLayer imageLayer = new ImageLayer(multiLevelSource);
                final int imageWidth = imgSize.width;
                final int imageHeight = imgSize.height;
                final int imageType = BufferedImage.TYPE_3BYTE_BGR;
                image = new BufferedImage(imageWidth, imageHeight, imageType);
                Viewport snapshotVp = new DefaultViewport(isModelYAxisDown(imageLayer));
                final BufferedImageRendering imageRendering = new BufferedImageRendering(image, snapshotVp);

                final Graphics2D graphics = imageRendering.getGraphics();
                graphics.setColor(getBackground());
                graphics.fillRect(0, 0, imageWidth, imageHeight);

                snapshotVp.zoom(imageLayer.getModelBounds());
                snapshotVp.moveViewDelta(snapshotVp.getViewBounds().x, snapshotVp.getViewBounds().y);
                imageLayer.render(imageRendering);

                pm.worked(4);
            } finally {
                pm.done();
            }
            return image;
        }

        private boolean isModelYAxisDown(ImageLayer baseImageLayer) {
            return baseImageLayer.getImageToModelTransform().getDeterminant() > 0.0;
        }

        private String getThumbnailBandName() {
            return ProductUtils.findSuitableQuicklookBandName(product);
        }
    }

    private class ProductNodeSubsetPane extends JPanel {

        private final ProductNode[] productNodes;
        private final String[] includeAlways;
        private List<JCheckBox> checkers;
        private JCheckBox allCheck;
        private JCheckBox noneCheck;
        private final boolean selected;
        private JCheckBox selectBandCheck;
        private JComboBox<String> groupNamesBox;

        private ProductNodeSubsetPane(ProductNode[] productNodes, boolean selected) {
            this(productNodes, null, selected);
        }

        private ProductNodeSubsetPane(ProductNode[] productNodes, String[] includeAlways, boolean selected) {
            this.productNodes = productNodes;
            this.includeAlways = includeAlways;
            this.selected = selected;
            createUI();
        }

        private void createUI() {

            ActionListener productNodeCheckListener = e -> updateUIState();

            checkers = new ArrayList<>(10);
            JPanel checkersPane = GridBagUtils.createPanel();
            setComponentName(checkersPane, "CheckersPane");

            GridBagConstraints gbc = GridBagUtils.createConstraints("insets.left=4,anchor=WEST,fill=HORIZONTAL");
            for (int i = 0; i < productNodes.length; i++) {
                ProductNode productNode = productNodes[i];

                String name = productNode.getName();
                JCheckBox productNodeCheck = new JCheckBox(name);
                productNodeCheck.setSelected(selected);
                productNodeCheck.setFont(SMALL_PLAIN_FONT);
                productNodeCheck.addActionListener(productNodeCheckListener);

                if (includeAlways != null
                        && StringUtils.containsIgnoreCase(includeAlways, name)) {
                    productNodeCheck.setSelected(true);
                    productNodeCheck.setEnabled(false);
                } else if (givenProductSubsetDef != null) {
                    productNodeCheck.setSelected(givenProductSubsetDef.containsNodeName(name));
                }
                checkers.add(productNodeCheck);

                String description = productNode.getDescription();
                JLabel productNodeLabel = new JLabel(description != null ? description : " ");
                productNodeLabel.setFont(SMALL_ITALIC_FONT);

                GridBagUtils.addToPanel(checkersPane, productNodeCheck, gbc, "weightx=0,gridx=0,gridy=" + i);
                GridBagUtils.addToPanel(checkersPane, productNodeLabel, gbc, "weightx=1,gridx=1,gridy=" + i);
            }
            // Add a last 'filler' row
            GridBagUtils.addToPanel(checkersPane, new JLabel(" "), gbc,
                    "gridwidth=2,weightx=1,weighty=1,gridx=0,gridy=" + productNodes.length);

            ActionListener allCheckListener = e -> {
                handleSelectCheckBoxes(e);
            };

            allCheck = new JCheckBox("Select all");
            allCheck.setName("selectAll");
            allCheck.setMnemonic('a');
            allCheck.setSelected(true);
            allCheck.addActionListener(allCheckListener);

            noneCheck = new JCheckBox("Select none");
            noneCheck.setName("SelectNone");
            noneCheck.setMnemonic('n');
            noneCheck.addActionListener(allCheckListener);

            selectBandCheck = new JCheckBox("Select band group");
            selectBandCheck.setName("SelectBandGroup");
            selectBandCheck.setMnemonic('b');
            selectBandCheck.addActionListener(allCheckListener);

            final BandGroup[] bandGroups = bandGroupsManager.getGroupsMatchingProduct(product);
            final String[] groupNames = new String[bandGroups.length];
            for (int i = 0; i < bandGroups.length; i++) {
                groupNames[i] = bandGroups[i].getName();
            }
            groupNamesBox = new JComboBox<>(groupNames);
            if (groupNames.length == 0) {
                groupNamesBox.setEnabled(false);
                selectBandCheck.setEnabled(false);
            }
            groupNamesBox.addActionListener(e -> {handleSelectCheckBoxes(e);});

            JScrollPane scrollPane = new JScrollPane(checkersPane);
            scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollPane.getVerticalScrollBar().setUnitIncrement(20);
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            scrollPane.getHorizontalScrollBar().setUnitIncrement(20);

            JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
            buttonRow.add(allCheck);
            buttonRow.add(noneCheck);
            buttonRow.add(selectBandCheck);
            buttonRow.add(groupNamesBox);

            setLayout(new BorderLayout());
            add(scrollPane, BorderLayout.CENTER);
            add(buttonRow, BorderLayout.SOUTH);
            setBorder(BorderFactory.createEmptyBorder(7, 7, 7, 7));

            updateUIState();
        }

        void updateUIState() {
            if (allCheck.isSelected()) {
                noneCheck.setSelected(false);
                selectBandCheck.setSelected(false);
            } else if (noneCheck.isSelected()) {
                allCheck.setSelected(false);
                selectBandCheck.setSelected(false);
            } else {
                allCheck.setSelected(false);
                noneCheck.setSelected(false);
            }
            updateSubsetDefNodeNameList();
        }

        void handleSelectCheckBoxes(ActionEvent e) {
            if (e.getSource() == allCheck) {
                checkAllProductNodes(true);
                noneCheck.setSelected(false);
                selectBandCheck.setSelected(false);
            } else if (e.getSource() == noneCheck) {
                checkAllProductNodes(false);
                allCheck.setSelected(false);
                selectBandCheck.setSelected(false);
            } else {
                checkProductNodesOfGroup();
                allCheck.setSelected(false);
                noneCheck.setSelected(false);
            }
            updateSubsetDefNodeNameList();
        }

        String[] getSubsetNames() {
            String[] names = new String[countChecked(true)];
            int pos = 0;
            for (int i = 0; i < checkers.size(); i++) {
                JCheckBox checker = checkers.get(i);
                if (checker.isSelected()) {
                    ProductNode productNode = productNodes[i];
                    names[pos] = productNode.getName();
                    pos++;
                }
            }
            return names;
        }

        void checkAllProductNodes(boolean checked) {
            for (JCheckBox checker : checkers) {
                if (checker.isEnabled()) {
                    checker.setSelected(checked);
                }
            }
        }

        void checkProductNodesOfGroup() {
            final BandGroup[] bandGroups = bandGroupsManager.getGroupsMatchingProduct(product);
            final String selectedBandGroupName = (String) groupNamesBox.getSelectedItem();

            BandGroup selectedBandGroup = null;
            for (final BandGroup bandGroup : bandGroups) {
                if (bandGroup.getName().equals(selectedBandGroupName)) {
                    selectedBandGroup = bandGroup;
                    break;
                }
            }
            final String[] bandNames = selectedBandGroup.getMatchingBandNames(product);
            for (JCheckBox checker : checkers) {
                boolean bandContained = false;
                for (final String bandName : bandNames) {
                    if (checker.getText().equals(bandName)) {
                        bandContained = true;
                        break;
                    }
                }
                checker.setSelected(bandContained);
            }
        }

        boolean areAllProductNodesChecked(boolean checked) {
            return countChecked(checked) == checkers.size();
        }

        int countChecked(boolean checked) {
            int counter = 0;
            for (JCheckBox checker : checkers) {
                if (checker.isSelected() == checked) {
                    counter++;
                }
            }
            return counter;
        }
    }
}