package org.esa.snap.worldwind.productlibrary;

import gov.nasa.worldwind.BasicModel;
import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.Model;
import gov.nasa.worldwind.SceneController;
import gov.nasa.worldwind.awt.WorldWindowGLJPanel;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.globes.Earth;
import gov.nasa.worldwind.globes.EarthFlat;
import gov.nasa.worldwind.layers.CompassLayer;
import gov.nasa.worldwind.layers.Earth.LandsatI3WMSLayer;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.SkyGradientLayer;
import gov.nasa.worldwind.layers.StarsLayer;
import gov.nasa.worldwind.layers.WorldMapLayer;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.pick.PickedObjectList;
import gov.nasa.worldwind.util.BasicGLCapabilitiesChooser;
import gov.nasa.worldwind.view.orbit.BasicOrbitView;
import gov.nasa.worldwind.view.orbit.FlatOrbitView;
import org.esa.snap.worldwind.AppPanel;
import org.esa.snap.worldwind.LayerConfiguration;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.geom.Rectangle2D;

/**
 * The panel containing the 3D earth globe.
 *
 * Created by jcoravu on 2/9/2019.
 */
public class WorldMap3DPanel extends WorldWindowGLJPanel implements WorldMap {

    private final Rectangle3DSelection selector;

    public WorldMap3DPanel(boolean flatEarth, boolean removeExtraLayers, PolygonsLayerModel polygonsLayerModel) {
        super(null, Configuration.getRequiredGLCapabilities(), new BasicGLCapabilitiesChooser());

        // SNAP's layer configuration, not WorldWind's defaults; the globe is set below
        Model model = new BasicModel(new Earth(), LayerConfiguration.createLayers());

        setModel(model);
        if (flatEarth) {
            setFlatEarth();
        } else {
            setGlobeEarth();
        }

        this.selector = new Rectangle3DSelection(this.wwd) {
            @Override
            protected void setCursor(Cursor cursor) {
                WorldMap3DPanel.this.setCursor((cursor == null) ? WorldMapPanelWrapper.DEFAULT_CURSOR : cursor);
            }
        };
        this.selector.setInteriorColor(WorldMapPanelWrapper.SELECTION_FILL_COLOR);
        this.selector.setBorderColor(WorldMapPanelWrapper.SELECTION_BORDER_COLOR);
        this.selector.setBorderWidth(WorldMapPanelWrapper.SELECTION_LINE_WIDTH);
        this.selector.getLayer().setEnabled(true);

        LayerList layerList = this.wwd.getModel().getLayers();

        if (removeExtraLayers) {
            for (Layer layer : layerList) {
                if (layer instanceof CompassLayer || layer instanceof WorldMapLayer || layer instanceof StarsLayer
                        || layer instanceof LandsatI3WMSLayer || layer instanceof SkyGradientLayer
                        || AppPanel.LANDSAT_LAYER_NAME.equals(layer.getName())) {

                    layerList.remove(layer);
                }
            }
        }

        AppPanel.replacePlaceNameLayer(layerList);

        layerList.add(this.selector.getLayer());

        layerList.add(new Polygons3DLayer(polygonsLayerModel));
    }

    @Override
    public void setSelectedArea(Rectangle2D selectedArea) {
        this.selector.setSelectedArea(selectedArea);
    }

    @Override
    public void refresh() {
        redrawNow();
    }

    @Override
    public void enableSelection() {
        this.selector.enable();
    }

    @Override
    public void disableSelection() {
        this.selector.disable();
    }

    @Override
    public Rectangle2D getSelectedArea() {
        return this.selector.getSelectedArea();
    }

    @Override
    public Point.Double convertPointToDegrees(Point point) {
        SceneController sceneController = getSceneController();
        Point pickPoint = sceneController.getPickPoint();
        if (pickPoint != null && pickPoint.getX() == point.getX() && pickPoint.getY() == point.getY()) {
            PickedObjectList pickedObjectList = sceneController.getPickedObjectList();
            PickedObject terrainObject = (pickedObjectList != null) ? pickedObjectList.getTerrainObject() : null;
            if (terrainObject != null && terrainObject.getPosition() != null) {
                Position position = terrainObject.getPosition();
                return new Point.Double(position.getLongitude().getDegrees(), position.getLatitude().getDegrees());
            }
        }
        return null;
    }

    public void setGlobeEarth() {
        getModel().setGlobe(new Earth());
        setView(new BasicOrbitView());
    }

    public void setFlatEarth() {
        getModel().setGlobe(new EarthFlat());
        setView(new FlatOrbitView());
    }

    public boolean isGlobeEarth() {
        return (getModel().getGlobe() instanceof Earth);
    }
}
