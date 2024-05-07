package org.esa.snap.worldwind.productlibrary;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwindx.examples.util.SectorSelector;

import java.awt.geom.Rectangle2D;

/**
 * The class stores the coordinates of the selected area for a 3D earth globe.
 *
 * Created by jcoravu on 22/10/2019.
 */
public class Rectangle3DSelection extends SectorSelector {

    public Rectangle3DSelection(WorldWindow worldWindow) {
        super(worldWindow);
    }

    public void setSelectedArea(Rectangle2D selectedArea) {
        addSelectionLayerIfMissing();
        Sector newSector;
        if (selectedArea == null) {
            newSector = Sector.EMPTY_SECTOR;
        } else {
            newSector = Sector.fromDegrees(selectedArea);
        }
        getShape().setSector(newSector);
    }

    public Rectangle2D getSelectedArea() {
        Sector sector = getShape().getSector();
        if (sector == Sector.EMPTY_SECTOR) {
            return null; // no selected area
        }
        return sector.toRectangleDegrees();
    }

    @Override
    public void enable() {
        super.enable();

        setCursor(WorldMapPanelWrapper.SELECTION_CURSOR);
    }

    @Override
    public void disable() {
        super.disable();

        setCursor(WorldMapPanelWrapper.DEFAULT_CURSOR);
    }

    private void addSelectionLayerIfMissing() {
        LayerList layers = this.getWwd().getModel().getLayers();
        if(!layers.contains(this.getLayer())) {
            layers.add(this.getLayer());
        }
        if(!this.getLayer().isEnabled()) {
            this.getLayer().setEnabled(true);
        }
    }
}
