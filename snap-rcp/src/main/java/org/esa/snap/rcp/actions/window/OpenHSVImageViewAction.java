/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
import org.esa.snap.core.util.PreferencesPropertyMap;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.ui.HSVImageProfilePane;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.ui.product.ProductSceneImage;
import org.esa.snap.ui.product.ProductSceneView;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.prefs.Preferences;

/**
 * This action opens an HSV image view on the currently selected Product.
 */
@ActionID(category = "View", id = "OpenHSVImageViewAction")
@ActionRegistration(
        displayName = "#CTL_OpenHSVImageViewAction_MenuText",
        popupText = "#CTL_OpenHSVImageViewAction_MenuText",
        iconBase = "org/esa/snap/rcp/icons/ImageView.gif",
        lazy = true
)
@ActionReferences({
        @ActionReference(path = "Menu/Window", position = 115),
        @ActionReference(path = "Context/Product/Product", position = 50, separatorAfter = 55),
})
@NbBundle.Messages({
        "CTL_OpenHSVImageViewAction_MenuText=Open HSV Image Window",
        "CTL_OpenHSVImageViewAction_ShortDescription=Open an HSV image view for the selected product"
})
public class OpenHSVImageViewAction extends AbstractAction implements HelpCtx.Provider {

    private static final String HELP_ID = "hsvImageProfile";
    private static final String r = "min(round( (floor((6*(h))%6)==0?(v): (floor((6*(h))%6)==1?((1-((s)*((6*(h))%6)-floor((6*(h))%6)))*(v)): (floor((6*(h))%6)==2?((1-(s))*(v)): (floor((6*(h))%6)==3?((1-(s))*(v)): (floor((6*(h))%6)==4?((1-((s)*(1-((6*(h))%6)-floor((6*(h))%6))))*(v)): (floor((6*(h))%6)==5?(v):0)))))) *256), 255)";
    private static final String g = "min(round( (floor((6*(h))%6)==0?((1-((s)*(1-((6*(h))%6)-floor((6*(h))%6))))*(v)): (floor((6*(h))%6)==1?(v): (floor((6*(h))%6)==2?(v): (floor((6*(h))%6)==3?((1-((s)*((6*(h))%6)-floor((6*(h))%6)))*(v)): (floor((6*(h))%6)==4?((1-(s))*(v)): (floor((6*(h))%6)==5?((1-(s))*(v)):0)))))) *256), 255)";
    private static final String b = "min(round( (floor((6*(h))%6)==0?((1-(s))*(v)): (floor((6*(h))%6)==1?((1-(s))*(v)): (floor((6*(h))%6)==2?((1-((s)*(1-((6*(h))%6)-floor((6*(h))%6))))*(v)): (floor((6*(h))%6)==3?(v): (floor((6*(h))%6)==4?(v): (floor((6*(h))%6)==5?((1-((s)*((6*(h))%6)-floor((6*(h))%6)))*(v)):0)))))) *256), 255)";
    private final Product product;

    public OpenHSVImageViewAction(ProductNode node) {
        super(Bundle.CTL_OpenHSVImageViewAction_MenuText());
        product = node.getProduct();
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_OpenHSVImageViewAction_ShortDescription());
    }

    private static ProductSceneImage createProductSceneImageHSV(final String name, final Product product,
                                                                final String[] hsvExpressions,
                                                                final ProgressMonitor pm) throws Exception {
        UIUtils.setRootFrameWaitCursor(SnapApp.getDefault().getMainFrame());
        Band[] rgbBands = null;
        boolean errorOccured = false;
        ProductSceneImage productSceneImage = null;
        try {
            pm.beginTask("Creating HSV image...", 2);
            final String[] rgbaExpressions = convertHSVToRGBExpressions(hsvExpressions);
            rgbBands = OpenRGBImageViewAction.allocateRgbBands(product, rgbaExpressions);

            final Preferences preferences = SnapApp.getDefault().getPreferences();
            final PreferencesPropertyMap configuration = new PreferencesPropertyMap(preferences);
            productSceneImage = new ProductSceneImage(name,
                    rgbBands[0],
                    rgbBands[1],
                    rgbBands[2],
                    configuration,
                    SubProgressMonitor.create(pm, 1));
            productSceneImage.initVectorDataCollectionLayer();
            productSceneImage.initMaskCollectionLayer();
        } catch (Exception e) {
            errorOccured = true;
            throw e;
        } finally {
            pm.done();
            if (rgbBands != null) {
                OpenRGBImageViewAction.releaseRgbBands(rgbBands, errorOccured);
            }
        }
        return productSceneImage;
    }

    private static void nomalizeHSVExpressions(final Product product, String[] hsvExpressions) {
        // normalize
        //range = max - min;
        //normvalue = min(max(((v- min)/range),0), 1);
        boolean modified = product.isModified();

        int i = 0;
        for (String exp : hsvExpressions) {
            if (exp.isEmpty()) continue;

            final String checkForNoDataValue = "";//getCheckForNoDataExpression(product, exp);

            final Band virtBand = createVirtualBand(product, exp, "tmpVirtBand" + i);

            final Stx stx = virtBand.getStx(false, ProgressMonitor.NULL);
            if (stx != null) {
                final double min = stx.getMinimum();
                final double range = stx.getMaximum() - min;
                hsvExpressions[i] = checkForNoDataValue + "min(max((((" + exp + ")- " + min + ")/" + range + "), 0), 1)";
            }
            product.removeBand(virtBand);
            ++i;
        }
        product.setModified(modified);
    }

    private static String getCheckForNoDataExpression(final Product product, final String exp) {
        final String[] bandNames = product.getBandNames();
        StringBuilder checkForNoData = new StringBuilder("(" + exp + " == NaN");
        if (StringUtils.contains(bandNames, exp)) {
            double nodatavalue = product.getBand(exp).getNoDataValue();
            checkForNoData.append(" or " + exp + " == " + nodatavalue);
        }
        checkForNoData.append(") ? NaN : ");

        return checkForNoData.toString();
    }

    public static Band createVirtualBand(final Product product, final String expression, final String name) {
        int width = product.getSceneRasterWidth();
        int height = product.getSceneRasterHeight();
        try {
            final RasterDataNode[] refRasters = BandArithmetic.getRefRasters(expression, product);
            if (refRasters.length > 0) {
                width = refRasters[0].getRasterWidth();
                height = refRasters[0].getRasterHeight();
            }
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid expression: " + expression);
        }

        final VirtualBand virtBand = new VirtualBand(name,
                ProductData.TYPE_FLOAT64,
                width,
                height,
                expression);
        virtBand.setNoDataValueUsed(true);
        product.addBand(virtBand);
        return virtBand;
    }

    private static String[] convertHSVToRGBExpressions(final String[] hsvExpressions) {

        final String h = hsvExpressions[0].isEmpty() ? "0" : hsvExpressions[0];
        final String s = hsvExpressions[1].isEmpty() ? "0" : hsvExpressions[1];
        final String v = hsvExpressions[2].isEmpty() ? "0" : hsvExpressions[2];

        // h,s,v in [0,1]
   /*   float rr = 0, gg = 0, bb = 0;
        float hh = (6 * h) % 6;
		int   c1 = (int) hh;                // floor((6*(h))%6)
		float c2 = hh - c1;                 // ((6*(h))%6)-floor((6*(h))%6)
		float x = (1 - s) * v;              // ((1-(s))*(v))
		float y = (1 - (s * c2)) * v;       // ((1-((s)*((6*(h))%6)-floor((6*(h))%6)))*(v))
		float z = (1 - (s * (1 - c2))) * v; // ((1-((s)*(1-((6*(h))%6)-floor((6*(h))%6))))*(v))
		switch (c1) {
			case 0: rr=v; gg=z; bb=x; break;
			case 1: rr=y; gg=v; bb=x; break;
			case 2: rr=x; gg=v; bb=z; break;
			case 3: rr=x; gg=y; bb=v; break;
			case 4: rr=z; gg=x; bb=v; break;
			case 5: rr=v; gg=x; bb=y; break;
		}
		int N = 256;
		int r = Math.min(Math.round(rr*N),N-1);
		int g = Math.min(Math.round(gg*N),N-1);
		int b = Math.min(Math.round(bb*N),N-1);
        */

        final String[] rgbExpressions = new String[3];
        rgbExpressions[0] = r.replace("(h)", '(' + h + ')').replace("(s)", '(' + s + ')').replace("(v)", '(' + v + ')');
        rgbExpressions[1] = g.replace("(h)", '(' + h + ')').replace("(s)", '(' + s + ')').replace("(v)", '(' + v + ')');
        rgbExpressions[2] = b.replace("(h)", '(' + h + ')').replace("(s)", '(' + s + ')').replace("(v)", '(' + v + ')');
        return rgbExpressions;
    }

    @Override
    public void actionPerformed(final ActionEvent event) {
        if (product != null) {
            openProductSceneViewHSV(product, HELP_ID);
        }
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(HELP_ID);
    }

    public void openProductSceneViewHSV(Product hsvProduct, final String helpId) {

        final Product[] openedProducts = SnapApp.getDefault().getProductManager().getProducts();
        final int[] defaultBandIndices = OpenRGBImageViewAction.getDefaultBandIndices(hsvProduct);

        final Preferences preferences = SnapApp.getDefault().getPreferences();
        final HSVImageProfilePane profilePane = new HSVImageProfilePane(new PreferencesPropertyMap(preferences),
                hsvProduct,
                openedProducts, defaultBandIndices);

        final String title = "Select HSV-Image Channels";
        final boolean ok = profilePane.showDialog(SnapApp.getDefault().getMainFrame(), title, helpId);
        if (!ok) {
            return;
        }
        final String[] hsvExpressions = profilePane.getRgbaExpressions();
        nomalizeHSVExpressions(hsvProduct, hsvExpressions);
        if (profilePane.getStoreProfileInProduct()) {
            RGBImageProfile.storeRgbaExpressions(hsvProduct, hsvExpressions, HSVImageProfilePane.HSV_COMP_NAMES);
        }

        final String sceneName = OpenRGBImageViewAction.createSceneName(hsvProduct, profilePane.getSelectedProfile(), "HSV");
        openProductSceneViewHSV(sceneName, hsvProduct, hsvExpressions);
    }

    /**
     * Creates product scene view using the given HSV expressions.
     */
    public void openProductSceneViewHSV(final String name, final Product product, final String[] hsvExpressions) {

        final SwingWorker<ProductSceneImage, Object> worker = new ProgressMonitorSwingWorker<ProductSceneImage, Object>(
                SnapApp.getDefault().getMainFrame(),
                SnapApp.getDefault().getInstanceName() + " - Creating image for '" + name + '\'') {

            @Override
            protected ProductSceneImage doInBackground(ProgressMonitor pm) throws Exception {
                return createProductSceneImageHSV(name, product, hsvExpressions, pm);
            }

            @Override
            protected void done() {
                SnapApp.getDefault().getMainFrame().setCursor(Cursor.getDefaultCursor());

                String errorMsg = "The HSV image view could not be created.";
                try {
                    final ProductSceneView productSceneView = new ProductSceneView(get());
                    OpenRGBImageViewAction.openDocumentWindow(productSceneView);
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
        SnapApp.getDefault().setStatusBarMessage("Creating HSV image view...");  /*I18N*/
        UIUtils.setRootFrameWaitCursor(SnapApp.getDefault().getMainFrame());
        worker.execute();
    }
}
