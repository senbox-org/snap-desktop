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

package org.esa.snap.rcp.layermanager.layersrc.windfield;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.layer.AbstractLayerSourceAssistantPage;
import org.esa.snap.ui.layer.LayerSource;
import org.esa.snap.ui.layer.LayerSourcePageContext;

/**
 * A source for {@link WindFieldLayer}s.
 *
 * @author Norman Fomferra
 * @since BEAM 4.6
 */
public class WindFieldLayerSource implements LayerSource {
    private static final String WINDU_NAME = "zonal_wind";
    private static final String WINDV_NAME = "merid_wind";

    @Override
    public boolean isApplicable(LayerSourcePageContext pageContext) {
        final Product product = SnapApp.getDefault().getSelectedProduct();
        final RasterDataNode windu = product.getRasterDataNode(WINDU_NAME);
        final RasterDataNode windv = product.getRasterDataNode(WINDV_NAME);
        return windu != null && windv != null;
    }

    @Override
    public boolean hasFirstPage() {
        return false;
    }

    @Override
    public AbstractLayerSourceAssistantPage getFirstPage(LayerSourcePageContext pageContext) {
        return null;
    }

    @Override
    public boolean canFinish(LayerSourcePageContext pageContext) {
        return true;
    }

    @Override
    public boolean performFinish(LayerSourcePageContext pageContext) {
        final Product product = SnapApp.getDefault().getSelectedProduct();
        final RasterDataNode windu = product.getRasterDataNode(WINDU_NAME);
        final RasterDataNode windv = product.getRasterDataNode(WINDV_NAME);
        final WindFieldLayer fieldLayer = WindFieldLayerType.createLayer(windu, windv);
        pageContext.getLayerContext().getRootLayer().getChildren().add(0, fieldLayer);
        return true;
    }

    @Override
    public void cancel(LayerSourcePageContext pageContext) {
    }
}
