package org.esa.snap.product.library.ui.v2.worldwind;

import gov.nasa.worldwind.geom.Position;
import org.esa.snap.product.library.ui.v2.thread.AbstractRunnable;
import org.esa.snap.ui.loading.CircularProgressIndicatorLabel;
import org.esa.snap.ui.loading.GenericRunnable;
import org.esa.snap.ui.loading.SwingUtils;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EtchedBorder;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

public class WorldMapPanelWrapper extends JPanel {

    public static final float SELECTION_LINE_WIDTH = 1.5f;
    public final static Color SELECTION_FILL_COLOR = new Color(255, 255, 0, 70);
    public final static Color SELECTION_BORDER_COLOR = new Color(255, 255, 0, 255);

    public static final float POLYGON_LINE_WIDTH = 1.0f;
    public final static Color POLYGON_BORDER_COLOR = Color.WHITE;
    public final static Color POLYGON_HIGHLIGHT_BORDER_COLOR = Color.RED;

    public static final Cursor SELECTION_CURSOR = Cursor.getPredefinedCursor(1);
    public static final Cursor DEFAULT_CURSOR = Cursor.getDefaultCursor();

    private final PolygonsLayerModel polygonsLayerModel;
    private final WorldMap2DPanel worldMap2DPanel;

    private WorldMap3DPanel worldMap3DPanel;
    private WorldMap currentWorldMap;
    private PolygonMouseListener mouseListener;

    public WorldMapPanelWrapper(PolygonMouseListener mouseListener) {
        super(new GridBagLayout());

        this.mouseListener = mouseListener;

        setBackground(Color.WHITE);
        setOpaque(true);
        setBorder(new EtchedBorder());

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
    public Color getBackground() {
        return isEnabled() ? super.getBackground() : UIManager.getColor("TextField.inactiveBackground");
    }

    public void addWorldMapPanelAsync(boolean flatWorld, boolean removeExtraLayers) {
        InitWorldMapPanelRunnable thread = new InitWorldMapPanelRunnable(this, flatWorld, removeExtraLayers, this.polygonsLayerModel);
        thread.executeAsync(); // start the thread
    }

    public void clearSelectedArea() {
        if (this.currentWorldMap != null) {
            this.currentWorldMap.disableSelection();
        }
    }

    public Rectangle2D getSelectedArea() {
        if (this.currentWorldMap != null) {
            return this.currentWorldMap.getSelectedArea();
        }
        return null;
    }

    public void setEyePosition(Path2D.Double polygonPath) {
        if (this.worldMap3DPanel != null) {
            Rectangle2D rectangleBounds = polygonPath.getBounds2D();
            Position eyePosition = this.worldMap3DPanel.getView().getEyePosition();
            Position position = Position.fromDegrees(rectangleBounds.getCenterY(), rectangleBounds.getCenterX(), eyePosition.getElevation());
            this.worldMap3DPanel.getView().setEyePosition(position);
            this.worldMap3DPanel.redrawNow();
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
            if (this.worldMap3DPanel.isEarthGlobe()) {
                viewMenu.add(buildView3DFlatEarth());
            } else {
                viewMenu.add(buildView3DGlobe());
            }
            JMenuItem viewFlatEarthMenuItem = new JMenuItem("2D Flat earth");
            viewFlatEarthMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    addWorldMapPanel(worldMap2DPanel);
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
        JMenuItem viewFlatEarthMenuItem = new JMenuItem("3D Flat earth");
        viewFlatEarthMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                worldMap3DPanel.setFlatEarth();
                addWorldMapPanel(worldMap3DPanel);
            }
        });
        return viewFlatEarthMenuItem;
    }

    private JMenuItem buildView3DGlobe() {
        JMenuItem viewGlobeMenuItem = new JMenuItem("3D Globe");
        viewGlobeMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                worldMap3DPanel.setEarthGlobe();
                addWorldMapPanel(worldMap3DPanel);
            }
        });
        return viewGlobeMenuItem;
    }

    private void addWorldMapPanel(JPanel worldMapPanel) {
        Rectangle2D selectedArea = null;
        if (this.currentWorldMap != null) {
            selectedArea = currentWorldMap.getSelectedArea();
        }
        this.currentWorldMap = (WorldMap)worldMapPanel;
        this.currentWorldMap.setSelection(selectedArea);

        removeAll();
        GridBagConstraints c = SwingUtils.buildConstraints(0, 0, GridBagConstraints.BOTH, GridBagConstraints.CENTER, 1, 1, 0, 0);
        add(worldMapPanel, c);
        Container parent = getParent();
        if (parent != null) {
            parent.revalidate();
            parent.repaint();
        }
    }

    private void addWorldWindowPanel(WorldMap3DPanel worldMap3DPanel) {
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                if (SwingUtilities.isRightMouseButton(mouseEvent)) {
                    processRightMouseClick(mouseEvent);
                } else if (SwingUtilities.isLeftMouseButton(mouseEvent)) {
                    processLeftMouseClick(mouseEvent);
                }
            }
        };
        this.worldMap2DPanel.addMouseListener(mouseAdapter);
        JPanel worldMapPanel;
        if (worldMap3DPanel == null) {
            worldMapPanel = this.worldMap2DPanel;
        } else {
            this.worldMap3DPanel = worldMap3DPanel;
            this.worldMap3DPanel.setOpaque(false);
            this.worldMap3DPanel.addMouseListener(mouseAdapter);
            worldMapPanel = worldMap3DPanel;
        }
        addWorldMapPanel(worldMapPanel);
    }

    private static class InitWorldMapPanelRunnable extends AbstractRunnable<WorldMap3DPanel> {

        private final WorldMapPanelWrapper worldWindowPanel;
        private final boolean flatWorld;
        private final boolean removeExtraLayers;
        private final PolygonsLayerModel polygonsLayerModel;

        public InitWorldMapPanelRunnable(WorldMapPanelWrapper worldWindowPanel, boolean flatWorld, boolean removeExtraLayers, PolygonsLayerModel polygonsLayerModel) {
            this.polygonsLayerModel = polygonsLayerModel;
            this.worldWindowPanel = worldWindowPanel;
            this.flatWorld = flatWorld;
            this.removeExtraLayers = removeExtraLayers;
        }

        @Override
        protected WorldMap3DPanel execute() throws Exception {
            return new WorldMap3DPanel(this.flatWorld, this.removeExtraLayers, this.polygonsLayerModel);
        }

        @Override
        protected String getExceptionLoggingMessage() {
            return "Failed to create the world window panel.";
        }

        @Override
        protected final void successfullyExecuting(WorldMap3DPanel result) {
            GenericRunnable<WorldMap3DPanel> runnable = new GenericRunnable<WorldMap3DPanel>(result) {
                @Override
                protected void execute(WorldMap3DPanel item) {
                    worldWindowPanel.addWorldWindowPanel(item);
                }
            };
            SwingUtilities.invokeLater(runnable);
        }

        @Override
        protected void failedExecuting(Exception exception) {
            GenericRunnable<Exception> runnable = new GenericRunnable<Exception>(exception) {
                @Override
                protected void execute(Exception item) {
                    worldWindowPanel.addWorldWindowPanel(null);
                }
            };
            SwingUtilities.invokeLater(runnable);
        }
    }
}
