package org.esa.snap.product.library.ui.v2.repository;

import org.esa.snap.product.library.ui.v2.worldwind.WorldWindowPanelWrapper;

import java.awt.geom.Rectangle2D;

/**
 * Created by jcoravu on 7/8/2019.
 */
public class SelectionAreaParameterComponent extends AbstractParameterComponent<Rectangle2D> {

    private final WorldWindowPanelWrapper worlWindPanel;

    public SelectionAreaParameterComponent(WorldWindowPanelWrapper worlWindPanel, String parameterName, String parameterLabelText, boolean required) {
        super(parameterName, parameterLabelText, required);

        this.worlWindPanel = worlWindPanel;
    }

    @Override
    public WorldWindowPanelWrapper getComponent() {
        return this.worlWindPanel;
    }

    @Override
    public Rectangle2D getParameterValue() {
        return this.worlWindPanel.getSelectedArea();
    }

    @Override
    public void clearParameterValue() {
        this.worlWindPanel.clearSelectedArea();
    }
}
