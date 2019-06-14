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
package org.esa.snap.rcp.actions.window;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.dataop.barithm.BandArithmetic;
import org.esa.snap.core.jexp.ParseException;
import org.esa.snap.core.util.ArrayUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.netbeans.docwin.DocumentWindowManager;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.rcp.windows.ProductSceneViewTopComponent;
import org.esa.snap.ui.RGBImageProfilePane;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.ui.product.ProductSceneImage;
import org.esa.snap.ui.product.ProductSceneView;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.awt.UndoRedo;
import org.openide.util.*;
import org.openide.util.actions.Presenter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This action opens an RGB image view on the currently selected Product.
 * Enablement: when a product is selected which contains at least 1 band
 *
 * @author Marco Peters
 * @author Daniel Knowles
 * @author Bing Yang
 */
//Apr2019 - Knowles/Yang - Added access to this tool in the "Image" toolbar including enablement, tooltips and related icon.

@ActionID(category = "View", id = "OpenRGBImageViewAction")
@ActionRegistration(
        displayName = "#CTL_OpenRGBImageViewAction_Name",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Window", position = 110),
        @ActionReference(path = "Toolbars/Image", position = 10),
        @ActionReference(path = "Context/Product/Product", position = 40, separatorBefore = 35),
})
@NbBundle.Messages({
        "CTL_OpenRGBImageViewAction_Name=RGB Image",
        "CTL_OpenRGBImageViewAction_ShortDescription=Opens a 3-channel RGB image view for the selected product"
})
public class OpenRGBImageViewAction extends AbstractAction implements HelpCtx.Provider, LookupListener, Presenter.Menu, Presenter.Toolbar {

    private static final String HELP_ID = "rgbImageProfile";

    private Lookup lookup;
    private final Lookup.Result<ProductNode> viewResult;

    private static final String ICONS_DIRECTORY = "org/esa/snap/rcp/icons/";
    private static final String TOOL_ICON_LARGE = ICONS_DIRECTORY + "RgbImage24.png";
    private static final String TOOL_ICON_SMALL = ICONS_DIRECTORY + "RgbImage16.png";



    // Governs enablement of the RGB GUI access
    private final int MINUMUM_NUM_BANDS = 1;


    public OpenRGBImageViewAction() {this(null);}

    public OpenRGBImageViewAction(ProductNode node) {
        super(Bundle.CTL_OpenRGBImageViewAction_Name());
        putValue(NAME, Bundle.CTL_OpenRGBImageViewAction_Name()+"...");
        putValue(SHORT_DESCRIPTION, Bundle.CTL_OpenRGBImageViewAction_ShortDescription());
        putValue(LARGE_ICON_KEY, ImageUtilities.loadImageIcon(TOOL_ICON_LARGE, false));
        putValue(SMALL_ICON, ImageUtilities.loadImageIcon(TOOL_ICON_SMALL, false));

        Lookup lookup = Utilities.actionsGlobalContext();
        this.lookup = lookup;
        this.viewResult = lookup.lookupResult(ProductNode.class);
        this.viewResult.addLookupListener(WeakListeners.create(LookupListener.class, this, viewResult));
        updateEnabledState();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Product product = SnapApp.getDefault().getSelectedProduct(SnapApp.SelectionSourceHint.AUTO);
        if (product != null) {
            openProductSceneViewRGB(product, HELP_ID);
        }

    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(HELP_ID);
    }

    public void openProductSceneViewRGB(Product rgbProduct, final String helpId) {
        final Product[] openedProducts = SnapApp.getDefault().getProductManager().getProducts();
        final int[] defaultBandIndices = getDefaultBandIndices(rgbProduct);

        final RGBImageProfilePane profilePane = new RGBImageProfilePane(SnapApp.getDefault().getPreferencesPropertyMap(), rgbProduct,
                openedProducts, defaultBandIndices);

        final String title = "Select RGB-Image Channels";
        final boolean ok = profilePane.showDialog(SnapApp.getDefault().getMainFrame(), title, helpId);
        if (!ok) {
            return;
        }
        final String[] rgbaExpressions = profilePane.getRgbaExpressions();
        if (profilePane.getStoreProfileInProduct()) {
            RGBImageProfile.storeRgbaExpressions(rgbProduct, rgbaExpressions);
        }

        final String sceneName = createSceneName(rgbProduct, profilePane.getSelectedProfile(), "RGB");
        openProductSceneViewRGB(sceneName, rgbProduct, rgbaExpressions);
    }

    public static int[] getDefaultBandIndices(final Product product) {

        int[] bandIndices = null;
        final Band[] bands = product.getBands();
        if (bands.length == 1) {
            return new int[]{0};
        } else if (bands.length == 2) {
            return new int[]{0, 1};
        } else if (bands.length > 2) {
            bandIndices = new int[3];
            int cnt = 0, i = 0;
            for (Band band : product.getBands()) {
                final String unit = band.getUnit();
                if (unit != null && unit.contains("intensity")) {
                    bandIndices[i++] = cnt;
                }
                if (i >= bandIndices.length)
                    break;
                ++cnt;
            }
            if (i == 0) {
                while (i < 3) {
                    bandIndices[i] = i++;
                }
            }
            if (i == 1) {
                return new int[]{bandIndices[0]};
            } else if (i == 2) {
                return new int[]{bandIndices[0], bandIndices[1]};
            }
        }
        return bandIndices;
    }

    private void openProductSceneViewRGB(final String name, final Product product, final String[] rgbaExpressions) {
        final SwingWorker<ProductSceneImage, Object> worker = new ProgressMonitorSwingWorker<ProductSceneImage, Object>(
                SnapApp.getDefault().getMainFrame(),
                SnapApp.getDefault().getInstanceName() + " - Creating image for '" + name + "'") {

            @Override
            protected ProductSceneImage doInBackground(ProgressMonitor pm) throws Exception {
                return createProductSceneImageRGB(name, product, rgbaExpressions, pm);
            }

            @Override
            protected void done() {
                SnapApp.getDefault().getMainFrame().setCursor(Cursor.getDefaultCursor());

                String errorMsg = "The RGB image view could not be created.";
                try {
                    ProductSceneView productSceneView = new ProductSceneView(get());
                    openDocumentWindow(productSceneView);
                } catch (OutOfMemoryError e) {
                    Dialogs.showOutOfMemoryError(errorMsg);
                    return;
                } catch (Exception e) {
                    SnapApp.getDefault().handleError(errorMsg, e);
                    return;
                }
                SnapApp.getDefault().setStatusBarMessage("");
            }
        };
        SnapApp.getDefault().setStatusBarMessage("Creating RGB image view...");  /*I18N*/
        UIUtils.setRootFrameWaitCursor(SnapApp.getDefault().getMainFrame());
        worker.execute();
    }

    public static ProductSceneViewTopComponent openDocumentWindow(final ProductSceneView view) {
        UndoRedo.Manager undoManager = SnapApp.getDefault().getUndoManager(view.getProduct());
        ProductSceneViewTopComponent psvTopComponent = new ProductSceneViewTopComponent(view, undoManager);

        DocumentWindowManager.getDefault().openWindow(psvTopComponent);
        psvTopComponent.requestSelected();

        return psvTopComponent;

    }

    private ProductSceneImage createProductSceneImageRGB(String name, final Product product, String[] rgbaExpressions,
                                                         ProgressMonitor pm) throws Exception {
        Band[] rgbBands = null;
        boolean errorOccurred = false;
        ProductSceneImage productSceneImage = null;
        try {
            pm.beginTask("Creating RGB image...", 2);
            rgbBands = allocateRgbBands(product, rgbaExpressions);
            productSceneImage = new ProductSceneImage(name, rgbBands[0],
                    rgbBands[1],
                    rgbBands[2],
                    SnapApp.getDefault().getPreferencesPropertyMap(),
                    SubProgressMonitor.create(pm, 1));
            productSceneImage.initVectorDataCollectionLayer();
            productSceneImage.initMaskCollectionLayer();
        } catch (Exception e) {
            errorOccurred = true;
            throw e;
        } finally {
            pm.done();
            if (rgbBands != null) {
                releaseRgbBands(rgbBands, errorOccurred);
            }
        }
        return productSceneImage;
    }

    public static Band[] allocateRgbBands(final Product product, final String[] rgbaExpressions) {
        final Band[] rgbBands = new Band[3]; // todo - set to [4] as soon as we support alpha
        final boolean productModificationState = product.isModified();
        final Product[] products = SnapApp.getDefault().getProductManager().getProducts();
        final int elementIndex = ArrayUtils.getElementIndex(product, products);
        try {
            //if (!BandArithmetic.areRastersEqualInSize(product, rgbaExpressions)) {
            // If there are multiple product references, the call should consider all the products
            if (!BandArithmetic.areRastersEqualInSize(products, elementIndex, rgbaExpressions)) {
                throw new IllegalArgumentException("Referenced rasters are not of the same size");
            }
        } catch (ParseException e) {
            throw new IllegalArgumentException("Expressions are invalid");
        }
        // if more than 1 products referenced, then don't get the band of the context product
        final Set<Product> referencedProducts = new HashSet<>();
        Arrays.stream(rgbaExpressions).forEach(e -> {
            try {
                referencedProducts.addAll(
                        Arrays.stream(BandArithmetic.getRefRasters(e, products))
                                .map(ProductNode::getProduct)
                                .collect(Collectors.toList())
                );
            } catch (ParseException pex) {
                SystemUtils.LOG.warning(pex.getMessage());
            }
        });
        boolean moreProductsReferences = referencedProducts.size() > 1;
        for (int i = 0; i < rgbBands.length; i++) {
            String expression = rgbaExpressions[i].isEmpty() ? "0" : rgbaExpressions[i];
            Band rgbBand = moreProductsReferences ? null : product.getBand(expression);
            if (rgbBand == null) {
                rgbBand = new ProductSceneView.RGBChannel(product,
                        determineWidth(expression, products, elementIndex),
                        determineHeight(expression, products, elementIndex),
                        RGBImageProfile.RGB_BAND_NAMES[i],
                        expression);
            }
            rgbBands[i] = rgbBand;
        }
        product.setModified(productModificationState);
        return rgbBands;
    }

    private static int determineWidth(String expression, Product[] products, int index) {
        int width = products[index].getSceneRasterWidth();
        try {
            final RasterDataNode[] refRasters = BandArithmetic.getRefRasters(expression, products, index);
            if (refRasters.length > 0) {
                width = refRasters[0].getRasterWidth();
            }
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid expression: " + expression);
        }
        return width;
    }

    private static int determineHeight(String expression, Product[] products, int index) {
        int height = products[index].getSceneRasterHeight();
        try {
            final RasterDataNode[] refRasters = BandArithmetic.getRefRasters(expression, products, index);
            if (refRasters.length > 0) {
                height = refRasters[0].getRasterHeight();
            }
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid expression: " + expression);
        }
        return height;
    }

    public static void releaseRgbBands(Band[] rgbBands, boolean errorOccurred) {
        for (int i = 0; i < rgbBands.length; i++) {
            Band rgbBand = rgbBands[i];
            if (rgbBand != null) {
                if (rgbBand instanceof ProductSceneView.RGBChannel) {
                    if (errorOccurred) {
                        rgbBand.dispose();
                    }
                }
            }
            rgbBands[i] = null;
        }
    }

    public static String createSceneName(Product product, RGBImageProfile rgbImageProfile, String operation) {
        final StringBuilder nameBuilder = new StringBuilder();
        final String productRef = product.getProductRefString();
        if (productRef != null) {
            nameBuilder.append(productRef);
            nameBuilder.append(" ");
        }
        if (rgbImageProfile != null) {
            nameBuilder.append(rgbImageProfile.getName().replace("_", " "));
            nameBuilder.append(" ");
        }
        nameBuilder.append(operation);

        return nameBuilder.toString();
    }


    @Override
    public JMenuItem getMenuPresenter() {
        JMenuItem menuItem = new JMenuItem(this);
        return menuItem;
    }

    @Override
    public Component getToolbarPresenter() {
        JButton button = new JButton(this);
        button.setText(null);
        return button;
    }

    public void resultChanged(LookupEvent ignored) {
        updateEnabledState();
    }

    protected void updateEnabledState() {
        ProductNode productNode = this.lookup.lookup(ProductNode.class);
        setEnabled(productNode != null && productNode.getProduct().getNumBands() > MINUMUM_NUM_BANDS);
    }

}
