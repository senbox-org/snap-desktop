package org.esa.snap.product.library.ui.v2.repository.input;

import org.esa.snap.product.library.ui.v2.worldwind.WorldMapPanelWrapper;

import java.awt.geom.Rectangle2D;

/**
 * Created by jcoravu on 7/8/2019.
 */
public class SelectionAreaParameterComponent extends AbstractParameterComponent<Rectangle2D> {

    private final WorldMapPanelWrapper worldMapPanelWrapper;

    public SelectionAreaParameterComponent(WorldMapPanelWrapper worlWindPanel, String parameterName, String parameterLabelText, boolean required) {
        super(parameterName, parameterLabelText, required);

        this.worldMapPanelWrapper = worlWindPanel;
    }

    @Override
    public WorldMapPanelWrapper getComponent() {
        return this.worldMapPanelWrapper;
    }

    @Override
    public Rectangle2D getParameterValue() {
        return this.worldMapPanelWrapper.getSelectedArea();
    }

    @Override
    public void clearParameterValue() {
        this.worldMapPanelWrapper.clearSelectedArea();
    }
}
