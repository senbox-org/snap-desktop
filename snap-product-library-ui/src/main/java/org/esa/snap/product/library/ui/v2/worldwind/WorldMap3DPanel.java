package org.esa.snap.product.library.ui.v2.worldwind;

import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.Model;
import gov.nasa.worldwind.SceneController;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.WorldWindowGLJPanel;
import gov.nasa.worldwind.event.RenderingEvent;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.globes.Earth;
import gov.nasa.worldwind.globes.EarthFlat;
import gov.nasa.worldwind.layers.CompassLayer;
import gov.nasa.worldwind.layers.Earth.LandsatI3WMSLayer;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.SkyGradientLayer;
import gov.nasa.worldwind.layers.StarsLayer;
import gov.nasa.worldwind.layers.WorldMapLayer;
import gov.nasa.worldwind.pick.PickedObjectList;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.BasicGLCapabilitiesChooser;
import gov.nasa.worldwind.view.orbit.BasicOrbitView;
import gov.nasa.worldwind.view.orbit.FlatOrbitView;
import gov.nasa.worldwindx.examples.util.SectorSelector;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.Field;
import java.util.List;

/**
 * Created by jcoravu on 2/9/2019.
 */
public class WorldMap3DPanel extends WorldWindowGLJPanel implements WorldMap {

    private final Rectangle3DSelection selector;

    public WorldMap3DPanel(boolean flatWorld, boolean removeExtraLayers, PolygonsLayerModel polygonsLayerModel) {
        super(null, Configuration.getRequiredGLCapabilities(), new BasicGLCapabilitiesChooser());

        // create the default model as described in the current world wind properties
        Model model = (Model) WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME);

        setModel(model);
        if (flatWorld) {
            setFlatEarth();
        } else {
            setEarthGlobe();
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
                        || layer instanceof LandsatI3WMSLayer || layer instanceof SkyGradientLayer) {

                    layerList.remove(layer);
                }
            }
        }

        layerList.add(this.selector.getLayer());

        layerList.add(new Polygons3DLayer(polygonsLayerModel));
    }

    @Override
    public void setSelection(Rectangle2D selectionArea) {
        this.selector.setSelectedArea(selectionArea);
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
            if (pickedObjectList != null && pickedObjectList.size() > 0) {
                Position position = (Position) pickedObjectList.get(0).getObject();
                return new Point.Double(position.getLongitude().getDegrees(), position.getLatitude().getDegrees());
            }
        }
        return null;
    }

    public void setEarthGlobe() {
        getModel().setGlobe(new Earth());
        setView(new BasicOrbitView());
    }

    public void setFlatEarth() {
        getModel().setGlobe(new EarthFlat());
        setView(new FlatOrbitView());
    }

    public boolean isEarthGlobe() {
        return (getModel().getGlobe() instanceof Earth);
    }

    void setBackgroundColor(Color backgroundColor) {
        DrawContext drawContext = getSceneController().getDrawContext();
        Color color = drawContext.getClearColor();
        setValueByReflection(color, "value", backgroundColor.getRGB());
    }

    private static boolean setValueByReflection(Object object, String fieldName, Object fieldValue) {
        Class<?> clazz = object.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(object, fieldValue);
                return true;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        return false;
    }
}
