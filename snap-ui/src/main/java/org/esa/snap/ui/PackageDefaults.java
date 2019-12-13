package org.esa.snap.ui;

import java.awt.*;

/**
 * Defaults constants which may vary between packages: SeaDAS and SNAP.
 *
 * @author Daniel Knowles (NASA)
 * @version $Revision$ $Date$
 */



public class PackageDefaults extends PackageDefaultsSnap {

    public static final String COLOR_SPELLING = "Color"; // differs from snap

    public static final Color IMAGE_BACKGROUND_COLOR = Color.WHITE; // differs from snap
    public static final Color NO_DATA_LAYER_COLOR = new Color(128,128,128); // differs from snap

    // todo ADD THIS
    public static final String INSERT_WKT_GEOMETRY_NAME = "Geometry from WKT";
//    public static final String INSERT_WKT_GEOMETRY_ICON = "seadas/InsertWKTTool24.png"; // differs from snap
    public static final String INSERT_WKT_GEOMETRY_TOOLBAR_NAME = "Geometry";
    public static final int INSERT_WKT_GEOMETRY_TOOLBAR_POSITION = 60;




    public static final String SELECT_TOOL_NAME = "Select";
    public static final String SELECT_TOOL_ICON = "SelectTool24.gif";
    public static final String SELECT_TOOL_TOOLBAR_NAME = "Interactors"; // differs from snap
    public static final int SELECT_TOOL_TOOLBAR_POSITION = 10;  // differs from snap

    public static final String ZOOM_TOOL_NAME = "Zoom";
    public static final String ZOOM_TOOL_ICON = "ZoomTool24.gif";
    public static final String ZOOM_TOOL_TOOLBAR_NAME = "Interactors"; // differs from snap
    public static final int ZOOM_TOOL_TOOLBAR_POSITION = 20;  // differs from snap

    public static final String PANNER_TOOL_NAME = "Pan";
    public static final String PANNER_TOOL_ICON = "PannerTool24.gif";
    public static final String PANNER_TOOL_TOOLBAR_NAME = "Interactors"; // differs from snap
    public static final int PANNER_TOOL_TOOLBAR_POSITION = 0;  // differs from snap




    public static final String OVERLAY_GEOMETRY_NAME = "Geometry Overlay";
    public static final String OVERLAY_GEOMETRY_ICON = "ShapeOverlay24.gif";
    public static final String OVERLAY_GEOMETRY_TOOLBAR_NAME = "Miscellaneous"; // differs from snap
    public static final int OVERLAY_GEOMETRY_TOOLBAR_POSITION = 0;

    public static final String OVERLAY_GCP_NAME = "GCP Overlay";
    public static final String OVERLAY_GCP_ICON = "GcpOverlay24.gif";
    public static final String OVERLAY_GCP_TOOLBAR_NAME = "Miscellaneous"; // differs from snap
    public static final int OVERLAY_GCP_TOOLBAR_POSITION = 10;  // differs from snap

    public static final String OVERLAY_PINS_NAME = "Pin Overlay";
    public static final String OVERLAY_PINS_ICON = "PinOverlay24.gif";
    public static final String OVERLAY_PINS_TOOLBAR_NAME = "Miscellaneous"; // differs from snap
    public static final int OVERLAY_PINS_TOOLBAR_POSITION = 20;  // differs from snap

    public static final String MAGIC_WAND_NAME = "Magic Wand";
    public static final String MAGIC_WAND_ICON = "MagicWand22.png";
    public static final String MAGIC_WAND_TOOLBAR_NAME = "Miscellaneous"; // differs from snap
    public static final int MAGIC_WAND_TOOLBAR_POSITION = 30;  // differs from snap

    public static final String RANGE_FINDER_NAME = "Range Finder";
    public static final String RANGE_FINDER_ICON = "RangeFinder24.gif";
    public static final String RANGE_FINDER_TOOLBAR_NAME = "Miscellaneous"; // differs from snap
    public static final int RANGE_FINDER_TOOLBAR_POSITION = 40;  // differs from snap

    public static final String METADATA_PLOT_NAME = "Metadata Plot";
    public static final String METADATA_PLOT_ICON = "MetadataPlot24.png";
    public static final String METADATA_PLOT_TOOLBAR_NAME = "Miscellaneous"; // differs from snap
    public static final int METADATA_PLOT_TOOLBAR_POSITION = 50;  // differs from snap


    public static final String GEO_CODING_NAME = "Geo-Coding";
    public static final String GEO_CODING_ICON = "PhiLam.gif";
    public static final String GEO_CODING_TOOLBAR_NAME = "Miscellaneous"; // differs from snap
    public static final int GEO_CODING_TOOLBAR_POSITION = 60;  // differs from snap




    public static final String INFORMATION_NAME = "Information";
    public static final String INFORMATION_ICON = "Information.gif";
    public static final String INFORMATION_TOOLBAR_NAME = "Info"; // differs from snap
    public static final int INFORMATION_TOOLBAR_POSITION = 0;
    public static final String INFORMATION_MODE = "Information";
    public static final int INFORMATION_POSITION = 30;
    public static final boolean INFORMATION_OPEN = false;



    public static final String GCP_TOOL_ACTION_NAME = "GCP Tool";
    public static final String GCP_TOOL_ACTION_ICON = "seadas/GcpTool24.png"; // differs from snap
    public static final String GCP_TOOL_ACTION_TOOLBAR_NAME = "Ground Control Points"; // differs from snap
    public static final int GCP_TOOL_ACTION_TOOLBAR_POSITION = 10;

    public static final String GCP_MANAGER_NAME = "GCP Manager";
    public static final String GCP_MANAGER_ICON = "seadas/GcpManager24.png"; // differs from snap
    public static final String GCP_MANAGER_TOOLBAR_NAME = "Ground Control Points"; // differs from snap
    public static final int GCP_MANAGER_TOOLBAR_POSITION = 0;






    public static final String OVERLAY_WORLD_MAP_NAME = "World Map Overlay";
    public static final String OVERLAY_WORLD_MAP_ICON = "seadas/WorldMap24.png"; // differs from snap
    public static final String OVERLAY_WORLD_MAP_TOOLBAR_NAME = "Overlay";
    public static final int OVERLAY_WORLD_MAP_TOOLBAR_POSITION = 20;

    public static final String OVERLAY_NO_DATA_NAME = "No-Data Overlay";
    public static final String OVERLAY_NO_DATA_ICON = "seadas/NoDataOverlay24.png"; // differs from snap
    public static final String OVERLAY_NO_DATA_TOOLBAR_NAME = "Overlay";
    public static final int OVERLAY_NO_DATA_TOOLBAR_POSITION = 0;

    public static final String OVERLAY_GRATICULE_NAME = "Graticule Overlay";
    public static final String OVERLAY_GRATICULE_ICON = "seadas/GraticuleOverlay24.png"; // differs from snap
    public static final String OVERLAY_GRATICULE_TOOLBAR_NAME = "Overlay";
    public static final int OVERLAY_GRATICULE_TOOLBAR_POSITION = 10;





    public static final String VECTOR_DATA_NODE_NAME = "New Vector Data Container";
    public static final String VECTOR_DATA_NODE_ICON = "seadas/NewVectorContainer.png"; // differs from snap
    public static final String VECTOR_DATA_NODE_TOOLBAR_NAME = "Geometry";
    public static final int VECTOR_DATA_NODE_TOOLBAR_POSITION = 0;

    public static final String DRAW_RECTANGLE_NAME = "Draw Rectangle";
    public static final String DRAW_RECTANGLE_ICON = "seadas/DrawRectangleTool24.png"; // differs from snap
    public static final String DRAW_RECTANGLE_TOOLBAR_NAME = "Geometry";
    public static final int DRAW_RECTANGLE_TOOLBAR_POSITION = 10;

    public static final String DRAW_ELLIPSE_NAME = "Draw Ellipse";
    public static final String DRAW_ELLIPSE_ICON = "seadas/DrawEllipseTool24.png"; // differs from snap
    public static final String DRAW_ELLIPSE_TOOLBAR_NAME = "Geometry";
    public static final int DRAW_ELLIPSE_TOOLBAR_POSITION = 20;

    public static final String DRAW_POLYGON_NAME = "Draw Polygon";
    public static final String DRAW_POLYGON_ICON = "seadas/DrawPolygonTool24.png"; // differs from snap
    public static final String DRAW_POLYGON_TOOLBAR_NAME = "Geometry";
    public static final int DRAW_POLYGON_TOOLBAR_POSITION = 30;

    public static final String DRAW_LINE_NAME = "Draw Line";
    public static final String DRAW_LINE_ICON = "seadas/DrawLineTool24.png"; // differs from snap
    public static final String DRAW_LINE_TOOLBAR_NAME = "Geometry";
    public static final int DRAW_LINE_TOOLBAR_POSITION = 40;

    public static final String DRAW_POLYLINE_NAME = "Draw Polyline";
    public static final String DRAW_POLYLINE_ICON = "seadas/DrawPolylineTool24.png"; // differs from snap
    public static final String DRAW_POLYLINE_TOOLBAR_NAME = "Geometry";
    public static final int DRAW_POLYLINE_TOOLBAR_POSITION = 50;








    public static final String STATISTICS_NAME = "Statistics";
    public static final String STATISTICS_ICON = "Statistics.gif";
    public static final String STATISTICS_TOOLBAR_NAME = "Analysis";
    public static final int STATISTICS_TOOLBAR_POSITION = 0; // differs from snap

    public static final String HISTOGRAM_PLOT_NAME = "Histogram";
    public static final String HISTOGRAM_PLOT_ICON = "seadas/Histogram24.png"; // differs from snap
    public static final String HISTOGRAM_PLOT_TOOLBAR_NAME = "Analysis";
    public static final int HISTOGRAM_PLOT_TOOLBAR_POSITION = 10; // differs from snap

    public static final String DENSITY_PLOT_NAME = "Scatter Plot";
    public static final String DENSITY_PLOT_ICON = "seadas/ScatterPlot24.png"; // differs from snap
    public static final String DENSITY_PLOT_TOOLBAR_NAME = "Analysis";
    public static final int DENSITY_PLOT_TOOLBAR_POSITION = 20; // differs from snap

    public static final String SCATTER_PLOT_NAME = "Correlative Plot";
    public static final String SCATTER_PLOT_ICON = "seadas/CorrelativePlot24.png"; // differs from snap
    public static final String SCATTER_PLOT_TOOLBAR_NAME = "Analysis";
    public static final int SCATTER_PLOT_TOOLBAR_POSITION = 30; // differs from snap

    public static final String PROFILE_PLOT_NAME = "Profile Plot";
    public static final String PROFILE_PLOT_ICON = "ProfilePlot.gif";
    public static final String PROFILE_PLOT_TOOLBAR_NAME = "Analysis";
    public static final int PROFILE_PLOT_TOOLBAR_POSITION = 40; // differs from snap

    public static final String SPECTRUM_NAME = "Spectrum View";
    public static final String SPECTRUM_ICON = "seadas/Spectrum24.png"; // differs from snap
    public static final String SPECTRUM_TOOLBAR_NAME = "Analysis";
    public static final int SPECTRUM_TOOLBAR_POSITION = 50; // differs from snap



    public static final String PIN_MANAGER_NAME = "Pin Manager";
    public static final String PIN_MANAGER_ICON = "seadas/PinManager.png"; // differs from snap
    public static final String PIN_MANAGER_TOOLBAR_NAME = "Pins";
    public static final int PIN_MANAGER_TOOLBAR_POSITION = 0; // differs from snap

    public static final String PIN_TOOL_NAME = "Pin Tool";
    public static final String PIN_TOOL_ICON = "seadas/PinPlacer24.png"; // differs from snap
    public static final String PIN_TOOL_TOOLBAR_NAME = "Pins";
    public static final int PIN_TOOL_TOOLBAR_POSITION = 10; // differs from snap



    public static final String OPEN_PRODUCT_ACTION_NAME = "Open Product";
    public static final String OPEN_PRODUCT_ACTION_ICON = "seadas/Open24.png"; // differs from snap
    public static final String OPEN_PRODUCT_ACTION_TOOLBAR_NAME = "File";
    public static final int OPEN_PRODUCT_ACTION_TOOLBAR_POSITION = 10; // differs from snap




    public static final String COLOR_MANIPULATION_NAME = "Color Manager";
    public static final String COLOR_MANIPULATION_MODE = "properties";
    public static final int COLOR_MANIPULATION_POSITION = 10;
    public static final boolean COLOR_MANIPULATION_OPEN = true;

    public static final String PRODUCT_EXPLORER_NAME = "File Manager";
    public static final String PRODUCT_EXPLORER_MODE = "explorer";
    public static final int PRODUCT_EXPLORER_POSITION = 10;
    public static final boolean PRODUCT_EXPLORER_OPEN = true;

    public static final String MASK_MANAGER_NAME = "Mask Manager";
    public static final String MASK_MANAGER_MODE = "properties";
    public static final int MASK_MANAGER_POSITION = 20;
    public static final boolean MASK_MANAGER_OPEN = true;

    public static final String LAYER_MANAGER_NAME = "Layer Manager";
    public static final String LAYER_MANAGER_MODE = "properties";
    public static final int LAYER_MANAGER_POSITION = 30;
    public static final boolean LAYER_MANAGER_OPEN = true;

    public static final String PIXEL_INFO_NAME = "Pixel Info";
    public static final String PIXEL_INFO_MODE = "explorer";
    public static final int PIXEL_INFO_POSITION = 20;
    public static final boolean PIXEL_INFO_OPEN = true;

    public static final String WORLD_MAP_NAME = "World Map";
    public static final String WORLD_MAP_MODE = "navigator";
    public static final int WORLD_MAP_POSITION = 40;
    public static final boolean WORLD_MAP_OPEN = true;

    public static final String WORLD_VIEW_NAME = "World View";
    public static final String WORLD_VIEW_MODE = "navigator";
    public static final int WORLD_VIEW_POSITION = 50;
    public static final boolean WORLD_VIEW_OPEN = true;

    public static final String UNCERTAINTY_NAME = "Uncertainty Visualisation";
    public static final String UNCERTAINTY_MODE = "navigator";
    public static final int UNCERTAINTY_POSITION = 30;
    public static final boolean UNCERTAINTY_OPEN = false;

    public static final String NAVIGATION_NAME = "Navigation";
    public static final String NAVIGATION_MODE = "navigator";
    public static final int NAVIGATION_POSITION = 10;
    public static final boolean NAVIGATION_OPEN = true;

    public static final String PRODUCT_LIBRARY_NAME = "Product Library";
    public static final String PRODUCT_LIBRARY_MODE = "rightSlidingSide";
    public static final int PRODUCT_LIBRARY_POSITION = 0;
    public static final boolean PRODUCT_LIBRARY_OPEN = false;

    public static final String LAYER_EDITOR_NAME = "Layer Editor";
    public static final String LAYER_EDITOR_MODE = "explorer";
    public static final int LAYER_EDITOR_POSITION = 30;
    public static final boolean LAYER_EDITOR_OPEN = true;
}
