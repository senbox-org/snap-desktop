package org.esa.snap.product.library.ui.v2.worldwind;

import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.Model;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.WorldWindowGLJPanel;
import gov.nasa.worldwind.geom.LatLon;
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
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.BasicGLCapabilitiesChooser;
import gov.nasa.worldwind.util.PlacemarkClutterFilter;
import gov.nasa.worldwind.view.orbit.BasicOrbitView;
import gov.nasa.worldwind.view.orbit.FlatOrbitView;
import gov.nasa.worldwindx.examples.ClickAndGoSelectListener;
import gov.nasa.worldwindx.examples.util.SectorSelector;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.Field;

/**
 * Created by jcoravu on 2/9/2019.
 */
public class WorldWindowPanel extends WorldWindowGLJPanel {

    private final SectorSelector selector;
    private final PolygonLayer polygonLayer;

    public WorldWindowPanel(boolean flatWorld, boolean removeExtraLayers) {
        super(null, Configuration.getRequiredGLCapabilities(), new BasicGLCapabilitiesChooser());

        // create the default model as described in the current worldwind properties
        Model model = (Model) WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME);
        LayerList layerList = model.getLayers();
        if (removeExtraLayers) {
            for (Layer layer : layerList) {
                if (layer instanceof CompassLayer || layer instanceof WorldMapLayer || layer instanceof StarsLayer
                        || layer instanceof LandsatI3WMSLayer || layer instanceof SkyGradientLayer) {

                    layerList.remove(layer);
                }
            }
        }
        this.polygonLayer = new PolygonLayer();
        layerList.add(polygonLayer);

        setModel(model);
        if (flatWorld) {
            setFlatEarth();
        } else {
            setEarthGlobe();
        }

        addSelectListener(new ClickAndGoSelectListener(this, WorldMapLayer.class));
        getSceneController().setClutterFilter(new PlacemarkClutterFilter());

        this.selector = new SectorSelector(this.wwd) {
            @Override
            protected void setCursor(Cursor cursor) {
                WorldWindowPanel.this.setCursor((cursor == null) ? Cursor.getDefaultCursor() : cursor);
            }
        };
        this.selector.setInteriorColor(new Color(1f, 1f, 1f, 0.1f));
        this.selector.setBorderColor(new Color(1f, 0f, 0f, 0.5f));
        this.selector.setBorderWidth(2);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                if (mouseEvent.getButton() == MouseEvent.BUTTON3) {
                    showPopupMenu(mouseEvent.getX(), mouseEvent.getY()); // right mouse click
                }
            }
        });
    }

    PolygonLayer getPolygonLayer() {
        return polygonLayer;
    }

    void setBackgroundColor(Color backgroundColor) {
        DrawContext drawContext = getSceneController().getDrawContext();
        Color color = drawContext.getClearColor();
        setValueByReflection(color, "value", backgroundColor.getRGB());
    }

    void clearSelectedArea() {
        this.selector.disable();
    }

    Rectangle2D getSelectedArea() {
        Sector selectedSector = this.selector.getSector();
        return selectedSector.toRectangleDegrees();
    }

    private void setEarthGlobe() {
        getModel().setGlobe(new Earth());
        setView(new BasicOrbitView());
    }

    private void setFlatEarth() {
        getModel().setGlobe(new EarthFlat());
        setView(new FlatOrbitView());
    }

    private boolean isEarthGlobe() {
        return (getModel().getGlobe() instanceof Earth);
    }

    private void showPopupMenu(int mouseX, int mouseY) {
        String selectionText = (this.selector.getSector() == null) ? "Start selection" : "Clear selection";
        JMenuItem selectionMenuItem = new JMenuItem(selectionText);
        selectionMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (selector.getSector() == null) {
                    selector.enable();
                } else {
                    selector.disable();
                }
            }
        });
        String globeText = isEarthGlobe() ? "View flat globe" : "View earth globe";
        JMenuItem globeMenuItem = new JMenuItem(globeText);
        globeMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (isEarthGlobe()) {
                    setFlatEarth();
                } else {
                    setEarthGlobe();
                }
                revalidate();
                repaint();
            }
        });

        JPopupMenu popup = new JPopupMenu();
        popup.add(selectionMenuItem);
        popup.add(globeMenuItem);
        popup.show(this, mouseX, mouseY);
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
                e.printStackTrace();
                clazz = clazz.getSuperclass();
            } catch (Exception e) {
                e.printStackTrace();
                throw new IllegalStateException(e);
            }
        }
        return false;
    }
}
