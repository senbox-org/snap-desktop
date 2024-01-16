package org.esa.snap.worldwind.productlibrary;

import gov.nasa.worldwind.geom.Position;
import org.esa.snap.core.util.PropertyMap;
import org.esa.snap.ui.loading.CircularProgressIndicatorLabel;
import org.esa.snap.ui.loading.SwingUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 * The panel containing the earth globe.
 */
public abstract class WorldMapPanelWrapper extends JPanel {

    private static final String PREFERENCES_KEY_LAST_WORLD_MAP_PANEL = "last_world_map_panel";

    private static final int WORLD_MAP_2D_FLAT_EARTH = 1;
    private static final int WORLD_MAP_3D_FLAT_EARTH = 2;
    private static final int WORLD_MAP_3D_GLOBE_EARTH = 3;

    public static final float SELECTION_LINE_WIDTH = 1.5f;
    public final static Color SELECTION_FILL_COLOR = new Color(255, 255, 0, 70);
    public final static Color SELECTION_BORDER_COLOR = new Color(255, 255, 0, 255);

    public static final float POLYGON_LINE_WIDTH = 1.0f;
    public final static Color POLYGON_BORDER_COLOR = Color.WHITE;
    public final static Color POLYGON_HIGHLIGHT_BORDER_COLOR = Color.RED;

    public static final Cursor SELECTION_CURSOR = Cursor.getPredefinedCursor(1);
    public static final Cursor DEFAULT_CURSOR = Cursor.getDefaultCursor();

    protected final PolygonsLayerModel polygonsLayerModel;
    private final WorldMap2DPanel worldMap2DPanel;

    private WorldMap3DPanel worldMap3DPanel;
    private WorldMap currentWorldMap;
    private PolygonMouseListener mouseListener;
    private final PropertyMap persistencePreferences;

    protected WorldMapPanelWrapper(PolygonMouseListener mouseListener, Color backgroundColor, PropertyMap persistencePreferences) {
        super(new GridBagLayout());

        this.mouseListener = mouseListener;
        this.persistencePreferences = persistencePreferences;

        setBackground(backgroundColor);
        setOpaque(true);
        setBorder(SwingUtils.LINE_BORDER);

        this.polygonsLayerModel = new PolygonsLayerModel();

        this.worldMap2DPanel = new WorldMap2DPanel(this.polygonsLayerModel);
        this.worldMap2DPanel.setOpaque(false);

        CircularProgressIndicatorLabel circularProgressLabel = new CircularProgressIndicatorLabel();
        circularProgressLabel.setOpaque(false);
        circularProgressLabel.setRunning(true);

        GridBagConstraints c = SwingUtils.buildConstraints(0, 0, GridBagConstraints.NONE, GridBagConstraints.CENTER, 1, 1, 0, 0);
        add(circularProgressLabel, c);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        this.worldMap2DPanel.setEnabled(enabled);
        if (this.worldMap3DPanel != null) {
            this.worldMap3DPanel.setEnabled(enabled);
        }
    }

    public abstract void addWorldMapPanelAsync(boolean flat3DEarth, boolean removeExtraLayers);

    public void clearSelectedArea() {
        if (this.currentWorldMap != null) {
            this.currentWorldMap.disableSelection();
        }
    }

    public void setSelectedArea(Rectangle2D selectedArea) {
        if (this.currentWorldMap != null) {
            this.currentWorldMap.setSelectedArea(selectedArea);
            this.currentWorldMap.refresh();
        }
    }

    public Rectangle2D getSelectedArea() {
        if (this.currentWorldMap != null) {
            return this.currentWorldMap.getSelectedArea();
        }
        return null;
    }

    public void setEyePosition(Path2D.Double polygonPath) {
        if (this.currentWorldMap != null) {
            if (this.currentWorldMap == this.worldMap3DPanel) {
                Rectangle2D rectangleBounds = polygonPath.getBounds2D();
                if (rectangleBounds == null) {
                    throw new NullPointerException("The rectangle bounds is null.");
                }
                Position eyePosition = this.worldMap3DPanel.getView().getEyePosition();
                if (eyePosition == null) {
                    throw new NullPointerException("The eye position is null.");
                }
                Position position = Position.fromDegrees(rectangleBounds.getCenterY(), rectangleBounds.getCenterX(), eyePosition.getElevation());
                this.worldMap3DPanel.getView().setEyePosition(position);
                this.worldMap3DPanel.redrawNow();
            } else if (this.currentWorldMap == this.worldMap2DPanel) {
                // do nothing
            } else {
                throw new IllegalStateException("The world map type '"+this.currentWorldMap + "' is unknown.");
            }
        }
    }

    public void highlightPolygons(Path2D.Double[] polygonPaths) {
        this.polygonsLayerModel.highlightPolygons(polygonPaths);
        if (this.currentWorldMap != null) {
            this.currentWorldMap.refresh();
        }
    }

    public void setPolygons(Path2D.Double[] polygonPaths) {
        this.polygonsLayerModel.setPolygons(polygonPaths);
        if (this.currentWorldMap != null) {
            this.currentWorldMap.refresh();
        }
    }

    public void refresh(){
        if (worldMap3DPanel != null) {
            worldMap3DPanel.initializeBackend(false);
            worldMap3DPanel.reshape(0, 0, 0, 0);
            worldMap3DPanel.revalidate();
        }
    }

    private void processLeftMouseClick(MouseEvent mouseEvent) {
        Point.Double clickedPoint = this.currentWorldMap.convertPointToDegrees(mouseEvent.getPoint());
        if (clickedPoint != null) {
            List<Path2D.Double> polygonPaths = this.polygonsLayerModel.findPolygonsContainsPoint(clickedPoint.getX(), clickedPoint.getY());
            this.mouseListener.leftMouseButtonClicked(polygonPaths);
        }
    }

    private void processRightMouseClick(MouseEvent mouseEvent) {
        WorldMap worldMap = (WorldMap)mouseEvent.getSource();
        JMenuItem selectionMenuItem;
        if (worldMap.getSelectedArea() != null) {
            selectionMenuItem = new JMenuItem("Clear selection");
            selectionMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    JMenuItem menuItem = (JMenuItem)actionEvent.getSource();
                    WorldMap inputWorldMap = (WorldMap)menuItem.getClientProperty(menuItem);
                    inputWorldMap.disableSelection();
                }
            });
        } else {
            selectionMenuItem = new JMenuItem("Start selection");
            selectionMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    JMenuItem menuItem = (JMenuItem)actionEvent.getSource();
                    WorldMap inputWorldMap = (WorldMap)menuItem.getClientProperty(menuItem);
                    inputWorldMap.enableSelection();
                }
            });
        }
        selectionMenuItem.putClientProperty(selectionMenuItem, worldMap);

        JMenu viewMenu = new JMenu("View");
        if (worldMap == this.worldMap2DPanel) {
            viewMenu.add(buildView3DGlobe());
            viewMenu.add(buildView3DFlatEarth());
        } else if (worldMap == this.worldMap3DPanel) {
            if (this.worldMap3DPanel.isGlobeEarth()) {
                viewMenu.add(buildView3DFlatEarth());
            } else {
                viewMenu.add(buildView3DGlobe());
            }
            JMenuItem viewFlatEarthMenuItem = new JMenuItem("2D Flat Earth");
            viewFlatEarthMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    addWorldMapPanel(worldMap2DPanel, WORLD_MAP_2D_FLAT_EARTH);
                }
            });
            viewMenu.add(viewFlatEarthMenuItem);
        }
        JPopupMenu popup = new JPopupMenu();
        popup.add(selectionMenuItem);
        popup.add(viewMenu);
        popup.show((JPanel)worldMap, mouseEvent.getX(), mouseEvent.getY());
    }

    private JMenuItem buildView3DFlatEarth() {
        JMenuItem viewFlatEarthMenuItem = new JMenuItem("3D Flat Earth");
        viewFlatEarthMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                worldMap3DPanel.setFlatEarth();
                addWorldMapPanel(worldMap3DPanel, WORLD_MAP_3D_FLAT_EARTH);
            }
        });
        return viewFlatEarthMenuItem;
    }

    private JMenuItem buildView3DGlobe() {
        JMenuItem viewGlobeMenuItem = new JMenuItem("3D Globe");
        viewGlobeMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                worldMap3DPanel.setGlobeEarth();
                addWorldMapPanel(worldMap3DPanel, WORLD_MAP_3D_GLOBE_EARTH);
            }
        });
        return viewGlobeMenuItem;
    }

    private void addWorldMapPanel(JPanel worldMapPanel, Integer worldMapPanelId) {
        Rectangle2D selectedArea = null;
        if (this.currentWorldMap != null) {
            selectedArea = this.currentWorldMap.getSelectedArea();
        }
        this.currentWorldMap = (WorldMap)worldMapPanel;
        this.currentWorldMap.setSelectedArea(selectedArea);

        removeAll();
        GridBagConstraints c = SwingUtils.buildConstraints(0, 0, GridBagConstraints.BOTH, GridBagConstraints.CENTER, 1, 1, 0, 0);
        add(worldMapPanel, c);
        Container parent = getParent();
        if (parent != null) {
            parent.revalidate();
            parent.repaint();
        }
        refresh();

        // save the world map panel to the preferences
        if (worldMapPanelId != null) {
            this.persistencePreferences.setPropertyInt(PREFERENCES_KEY_LAST_WORLD_MAP_PANEL, worldMapPanelId.intValue());
        }
    }

    public void addWorldWindowPanel(WorldMap3DPanel worldMap3DPanel) {
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                if (isEnabled()) {
                    if (SwingUtilities.isRightMouseButton(mouseEvent)) {
                        processRightMouseClick(mouseEvent);
                    } else if (SwingUtilities.isLeftMouseButton(mouseEvent)) {
                        processLeftMouseClick(mouseEvent);
                    }
                }
            }
        };
        this.worldMap2DPanel.addMouseListener(mouseAdapter);
        JPanel worldMapPanel;
        if (worldMap3DPanel == null) {
            // the world map 3D panel has not been loaded
            worldMapPanel = this.worldMap2DPanel;
        } else {
            // the world map 3d panel has been loaded
            this.worldMap3DPanel = worldMap3DPanel;
            this.worldMap3DPanel.setOpaque(false);
            this.worldMap3DPanel.addMouseListener(mouseAdapter);
            worldMapPanel = this.worldMap3DPanel;
        }
        Integer lastWorldMapPanelId = this.persistencePreferences.getPropertyInt(PREFERENCES_KEY_LAST_WORLD_MAP_PANEL, null);
        if (lastWorldMapPanelId != null) {
            if (lastWorldMapPanelId.intValue() == WORLD_MAP_2D_FLAT_EARTH) {
                worldMapPanel = this.worldMap2DPanel;
            } else if (lastWorldMapPanelId.intValue() == WORLD_MAP_3D_FLAT_EARTH) {
                this.worldMap3DPanel.setFlatEarth();
                worldMapPanel = this.worldMap3DPanel;
            } else if (lastWorldMapPanelId.intValue() == WORLD_MAP_3D_GLOBE_EARTH) {
                this.worldMap3DPanel.setGlobeEarth();
                worldMapPanel = this.worldMap3DPanel;
            }
        }
        addWorldMapPanel(worldMapPanel, null);
    }
}
