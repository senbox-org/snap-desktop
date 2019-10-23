package org.esa.snap.product.library.ui.v2.worldwind;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwindx.examples.util.SectorSelector;

import java.awt.Cursor;
import java.awt.geom.Rectangle2D;

/**
 * Created by jcoravu on 22/10/2019.
 */
public class Rectangle3DSelection extends SectorSelector {

    public Rectangle3DSelection(WorldWindow worldWindow) {
        super(worldWindow);
    }

    public boolean hasSelection() {
        Sector sector = getShape().getSector();
        return (sector != Sector.EMPTY_SECTOR);
    }

    public void setSelectedArea(Rectangle2D selectedArea) {
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
            return null;
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
}
