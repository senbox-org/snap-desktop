package org.esa.snap.product.library.ui.v2.repository.input;

import org.esa.snap.worldwind.productlibrary.WorldMapPanelWrapper;

import javax.swing.*;
import java.awt.geom.Rectangle2D;

/**
 * The parameter component allows the user to select an area from the earth globe panel for the search parameter.
 *
 * Created by jcoravu on 7/8/2019.
 */
public class SelectionAreaParameterComponent extends AbstractParameterComponent<Rectangle2D> {

    private final WorldMapPanelWrapper worldMapPanelWrapper;

    public SelectionAreaParameterComponent(WorldMapPanelWrapper worlWindPanel, String parameterName, String parameterLabelText, boolean required) {
        super(parameterName, parameterLabelText, required);

        this.worldMapPanelWrapper = worlWindPanel;
    }

    @Override
    public JComponent getComponent() {
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

    @Override
    public void setParameterValue(Object value) {
        if (value == null) {
            clearParameterValue();
        } else if (value instanceof Rectangle2D) {
            this.worldMapPanelWrapper.setSelectedArea((Rectangle2D)value);
        } else {
            throw new ClassCastException("The parameter value type '" + value + "' must be '" + Rectangle2D.class+"'.");
        }
    }

    @Override
    public Boolean hasValidValue() {
        Rectangle2D selectedArea = this.worldMapPanelWrapper.getSelectedArea();
        if (selectedArea == null) {
            return null; // the value is not specified
        }
        return true; // the value is specified
    }
}
