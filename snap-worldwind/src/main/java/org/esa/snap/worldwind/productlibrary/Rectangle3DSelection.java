package org.esa.snap.worldwind.productlibrary;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwindx.examples.util.SectorSelector;

import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;

/**
 * The class stores the coordinates of the selected area for a 3D earth globe.
 *
 * Created by jcoravu on 22/10/2019.
 */
public class Rectangle3DSelection extends SectorSelector {

    // SectorSelector.enable() registers its listeners again on every call, so guard against double enabling
    private boolean selectionEnabled;
    // true between a mouse press that starts drawing, moving or resizing the selection and the matching release
    private boolean interacting;

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
        if (!this.selectionEnabled) {
            this.selectionEnabled = true;
            super.enable();
        }

        setCursor(WorldMapPanelWrapper.SELECTION_CURSOR);
    }

    @Override
    public void disable() {
        if (this.selectionEnabled) {
            this.selectionEnabled = false;
            this.interacting = false;
            super.disable();
        }

        setCursor(WorldMapPanelWrapper.DEFAULT_CURSOR);
    }

    boolean isSelectionEnabled() {
        return this.selectionEnabled;
    }

    @Override
    public void mousePressed(MouseEvent mouseEvent) {
        super.mousePressed(mouseEvent);
        if (mouseEvent.isConsumed() || getOperation() != NONE) {
            this.interacting = true;
        }
    }

    @Override
    public void mouseReleased(MouseEvent mouseEvent) {
        // SectorSelector consumes every left button release, which starves the globe navigation of release events
        // for as long as a selection exists; only let it handle releases that end its own interaction
        if (this.interacting || getOperation() != NONE) {
            this.interacting = false;
            super.mouseReleased(mouseEvent);
        }
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
