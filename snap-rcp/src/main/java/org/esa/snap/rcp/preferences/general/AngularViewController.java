/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.rcp.preferences.general;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.PropertyEditorRegistry;
import com.bc.ceres.swing.binding.PropertyPane;
import org.esa.snap.core.util.PropertyMap;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.preferences.DefaultConfigController;
import org.esa.snap.rcp.preferences.Preference;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;

import javax.swing.*;
import java.awt.*;

/**
 * Panel handling general preferences for Angular View.
 *
 * @author Daniel Knowles (NASA)
 */
@org.openide.util.NbBundle.Messages({
        "Options_DisplayName_AngularView=" + "Angular View",
        "Options_Keywords_AngularView=Angular View, general"
})
@OptionsPanelController.SubRegistration(location = "GeneralPreferences",
        displayName = "#Options_DisplayName_AngularView",
        keywords = "#Options_Keywords_AngularView",
        keywordsCategory = "Angular",
        id = "angularViewController",
        position = 10)


public final class AngularViewController extends DefaultConfigController {


    Property restoreDefaults;

    boolean propertyValueChangeEventsEnabled = true;

    private static final String PROPERTY_ROOT_KEY = "angular.view.v2.";

    public static final String PROPERTY_ANGULAR_VIEW_RANGE_DOMAIN_SECTION_KEY =  PROPERTY_ROOT_KEY + "plot.range_domain.section";
    public static final String PROPERTY_ANGULAR_VIEW_RANGE_DOMAIN_SECTION_LABEL = "Plot Range & Domain";
    public static final String PROPERTY_ANGULAR_VIEW_RANGE_DOMAIN_SECTION_TOOLTIP = "Sets plot range and domain";


    public static final String PROPERTY_ANGULAR_VIEW_XAXIS_SET_BOUNDS_KEY = PROPERTY_ROOT_KEY + ".xaxis.set.bounds";
    public static final String PROPERTY_ANGULAR_VIEW_XAXIS_SET_BOUNDS_LABEL = "Set Bounds X-Axis";
    public static final String PROPERTY_ANGULAR_VIEW_XAXIS_SET_BOUNDS_TOOLTIP = "Sets bounds of x-axis (uses X-Axis Min and X-Axis Max)";
    public static boolean PROPERTY_ANGULAR_VIEW_XAXIS_SET_BOUNDS_DEFAULT = false;
    
    public static final String PROPERTY_ANGULAR_VIEW_XAXIS_MIN_KEY = PROPERTY_ROOT_KEY + ".xaxis.min";
    public static final String PROPERTY_ANGULAR_VIEW_XAXIS_MIN_LABEL = "X-Axis Min";
    public static final String PROPERTY_ANGULAR_VIEW_XAXIS_MIN_TOOLTIP = "Sets minimum bound of x-axis (blank entry uses auto-bounding to data)";
    public static String PROPERTY_ANGULAR_VIEW_XAXIS_MIN_DEFAULT = "";

    public static final String PROPERTY_ANGULAR_VIEW_XAXIS_MAX_KEY = PROPERTY_ROOT_KEY + ".xaxis.max";
    public static final String PROPERTY_ANGULAR_VIEW_XAXIS_MAX_LABEL = "X-Axis Max";
    public static final String PROPERTY_ANGULAR_VIEW_XAXIS_MAX_TOOLTIP = "Sets maximum bound of x-axis (blank entry uses auto-bounding to data)";
    public static String PROPERTY_ANGULAR_VIEW_XAXIS_MAX_DEFAULT = "";


    public static final String PROPERTY_ANGULAR_VIEW_YAXIS_SET_BOUNDS_KEY = PROPERTY_ROOT_KEY + ".yaxis.set.bounds";
    public static final String PROPERTY_ANGULAR_VIEW_YAXIS_SET_BOUNDS_LABEL = "Set Bounds Y-Axis";
    public static final String PROPERTY_ANGULAR_VIEW_YAXIS_SET_BOUNDS_TOOLTIP = "Sets bounds of y-axis (uses Y-Axis Min and Y-Axis Max)";
    public static boolean PROPERTY_ANGULAR_VIEW_YAXIS_SET_BOUNDS_DEFAULT = false;
    
    public static final String PROPERTY_ANGULAR_VIEW_YAXIS_MIN_KEY = PROPERTY_ROOT_KEY + ".yaxis.min";
    public static final String PROPERTY_ANGULAR_VIEW_YAXIS_MIN_LABEL = "Y-Axis Min";
    public static final String PROPERTY_ANGULAR_VIEW_YAXIS_MIN_TOOLTIP = "Sets minimum bound of x-axis (blank entry uses auto-bounding to data)";
    public static String PROPERTY_ANGULAR_VIEW_YAXIS_MIN_DEFAULT = "";

    public static final String PROPERTY_ANGULAR_VIEW_YAXIS_MAX_KEY = PROPERTY_ROOT_KEY + ".yaxis.max";
    public static final String PROPERTY_ANGULAR_VIEW_YAXIS_MAX_LABEL = "Y-Axis Max";
    public static final String PROPERTY_ANGULAR_VIEW_YAXIS_MAX_TOOLTIP = "Sets maximum bound of x-axis (blank entry uses auto-bounding to data)";
    public static String PROPERTY_ANGULAR_VIEW_YAXIS_MAX_DEFAULT = "";



    public static final String PROPERTY_ANGULAR_VIEW_PLOT_GRAPHICS_SECTION_KEY =  PROPERTY_ROOT_KEY + "plot.graphics.section";
    public static final String PROPERTY_ANGULAR_VIEW_PLOT_GRAPHICS_SECTION_LABEL = "Plot Graphics";
    public static final String PROPERTY_ANGULAR_VIEW_PLOT_GRAPHICS_SECTION_TOOLTIP = "Sets formatting of plot graphics";



    public static final String PROPERTY_ANGULAR_VIEW_TITLE_KEY = PROPERTY_ROOT_KEY + ".plot.title";
    public static final String PROPERTY_ANGULAR_VIEW_TITLE_LABEL = "Plot Title";
    public static final String PROPERTY_ANGULAR_VIEW_TITLE_TOOLTIP = "Sets title of plot";
    public static String PROPERTY_ANGULAR_VIEW_TITLE_DEFAULT = "Angular Plot";


//    public static final String PROPERTY_ANGULAR_VIEW_FOREGROUND_COLOR_KEY = PROPERTY_ROOT_KEY + ".foreground.color";
//    public static final String PROPERTY_ANGULAR_VIEW_FOREGROUND_COLOR_LABEL = "Foreground Color";
//    public static final String PROPERTY_ANGULAR_VIEW_FOREGROUND_COLOR_TOOLTIP = "Sets foreground color of the plot";
    public static Color PROPERTY_ANGULAR_VIEW_FOREGROUND_COLOR_DEFAULT = Color.BLACK;

    public static final String PROPERTY_ANGULAR_VIEW_BACKGROUND_COLOR_KEY = PROPERTY_ROOT_KEY + "plot.background.color";
    public static final String PROPERTY_ANGULAR_VIEW_BACKGROUND_COLOR_LABEL = "Plot Background Color";
    public static final String PROPERTY_ANGULAR_VIEW_BACKGROUND_COLOR_TOOLTIP = "Sets background color of the plot";
    public static Color PROPERTY_ANGULAR_VIEW_BACKGROUND_COLOR_DEFAULT = Color.WHITE;

//    public static final String PROPERTY_ANGULAR_VIEW_MARGIN_BACKGROUND_COLOR_KEY = PROPERTY_ROOT_KEY + ".margin.background.color";
//    public static final String PROPERTY_ANGULAR_VIEW_MARGIN_BACKGROUND_COLOR_LABEL = "Margin Background Color";
//    public static final String PROPERTY_ANGULAR_VIEW_MARGIN_BACKGROUND_COLOR_TOOLTIP = "Sets background color of the margin";
    public static Color PROPERTY_ANGULAR_VIEW_MARGIN_BACKGROUND_COLOR_DEFAULT = Color.WHITE;

//    public static final String PROPERTY_ANGULAR_VIEW_LEGEND_BACKGROUND_COLOR_KEY = PROPERTY_ROOT_KEY + ".legend.background.color";
//    public static final String PROPERTY_ANGULAR_VIEW_LEGEND_BACKGROUND_COLOR_LABEL = "Legend Background Color";
//    public static final String PROPERTY_ANGULAR_VIEW_LEGEND_BACKGROUND_COLOR_TOOLTIP = "Sets background color of the legend";
    public static Color PROPERTY_ANGULAR_VIEW_LEGEND_BACKGROUND_COLOR_DEFAULT = Color.WHITE;

    
    public static final String PROPERTY_ANGULAR_VIEW_GRIDLINE_COLOR_KEY = PROPERTY_ROOT_KEY + ".gridlines.color";
    public static final String PROPERTY_ANGULAR_VIEW_GRIDLINE_COLOR_LABEL = "Plot Gridlines Color";
    public static final String PROPERTY_ANGULAR_VIEW_GRIDLINE_COLOR_TOOLTIP = "Sets color of the plot gridlines";
    public static Color PROPERTY_ANGULAR_VIEW_GRIDLINE_COLOR_DEFAULT = Color.LIGHT_GRAY;

    public static final String PROPERTY_ANGULAR_VIEW_GRIDLINE_SHOW_KEY = PROPERTY_ROOT_KEY + ".gridlines.show";
    public static final String PROPERTY_ANGULAR_VIEW_GRIDLINE_SHOW_LABEL = "Show Gridlines";
    public static final String PROPERTY_ANGULAR_VIEW_GRIDLINE_SHOW_TOOLTIP = "show plot gridlines";
    public static boolean PROPERTY_ANGULAR_VIEW_GRIDLINE_SHOW_DEFAULT = true;






    public static final String PROPERTY_ANGULAR_VIEW_LINE_INDEX_KEY = PROPERTY_ROOT_KEY + "plot.line_index";
    public static final String PROPERTY_ANGULAR_VIEW_LINE_INDEX_LABEL = "Plot Line Type (Index)";
    public static final String PROPERTY_ANGULAR_VIEW_LINE_INDEX_TOOLTIP = "Sets the default plot line type (index of selector)";
    public static String PROPERTY_ANGULAR_VIEW_LINE_INDEX_DEFAULT = "1";
    public static int PROPERTY_ANGULAR_VIEW_LINE_INDEX_DEFAULT_INT = 1;

    public static final String PROPERTY_ANGULAR_VIEW_SYMBOL_INDEX_KEY = PROPERTY_ROOT_KEY + "plot.symbol_index";
    public static final String PROPERTY_ANGULAR_VIEW_SYMBOL_INDEX_LABEL = "Plot Symbol (Index)";
    public static final String PROPERTY_ANGULAR_VIEW_SYMBOL_INDEX_TOOLTIP = "Sets the default plot symbol (index of selector)";
    public static String PROPERTY_ANGULAR_VIEW_SYMBOL_INDEX_DEFAULT = "1";
    public static int PROPERTY_ANGULAR_VIEW_SYMBOL_INDEX_DEFAULT_INT = 1;

    public static final String PROPERTY_ANGULAR_VIEW_SYMBOL_SIZE_KEY = PROPERTY_ROOT_KEY + "plot.symbol_size";
    public static final String PROPERTY_ANGULAR_VIEW_SYMBOL_SIZE_LABEL = "Plot Symbol Size";
    public static final String PROPERTY_ANGULAR_VIEW_SYMBOL_SIZE_TOOLTIP = "Sets the default plot symbol size";
    public static String PROPERTY_ANGULAR_VIEW_SYMBOL_SIZE_DEFAULT = "3";
    public static int PROPERTY_ANGULAR_VIEW_SYMBOL_SIZE_DEFAULT_INT = 3;

    public static final String PROPERTY_ANGULAR_VIEW_PLOT_COLOR_KEY = PROPERTY_ROOT_KEY + "plot.color";
    public static final String PROPERTY_ANGULAR_VIEW_PLOT_COLOR_LABEL = "Plot Color";
    public static final String PROPERTY_ANGULAR_VIEW_PLOT_COLOR_TOOLTIP = "Sets the default color of the plot lines and symbols";
    public static Color PROPERTY_ANGULAR_VIEW_PLOT_COLOR_DEFAULT = Color.BLACK;
    
    
    

    public static final String PROPERTY_ANGULAR_VIEW_GROUP1_SECTION_KEY =  PROPERTY_ROOT_KEY + "plot.group1.section";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP1_SECTION_LABEL = "Plot Graphics: Group 1";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP1_SECTION_TOOLTIP = "Sets plot graphics based on the specified wavelength for Group 1";

    public static final String PROPERTY_ANGULAR_VIEW_GROUP1_MIN_WAVELENGTH_KEY = PROPERTY_ROOT_KEY + "plot.group1.min_wavelength";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP1_MIN_WAVELENGTH_LABEL = "Group 1 - Min Wavelength (nm)";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP1_MIN_WAVELENGTH_TOOLTIP = "<html>Minimum Wavelength for Group 5<br>wavelength=-1 indicates group is disabled</html>";
    public static double PROPERTY_ANGULAR_VIEW_GROUP1_MIN_WAVELENGTH_DEFAULT = 100.0;

    public static final String PROPERTY_ANGULAR_VIEW_GROUP1_MAX_WAVELENGTH_KEY = PROPERTY_ROOT_KEY + "plot.group1.max_wavelength";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP1_MAX_WAVELENGTH_LABEL = "Group 1 - Max Wavelength (nm)";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP1_MAX_WAVELENGTH_TOOLTIP = "<html>Maximum Wavelength for Group 5<br>wavelength=-1 indicates group is disabled</html>";
    public static double PROPERTY_ANGULAR_VIEW_GROUP1_MAX_WAVELENGTH_DEFAULT = 399.9;

    public static final String PROPERTY_ANGULAR_VIEW_GROUP1_LINE_INDEX_KEY = PROPERTY_ROOT_KEY + "plot.group1.line_index";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP1_LINE_INDEX_LABEL = "Group 1 - Plot Line Type (Index)";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP1_LINE_INDEX_TOOLTIP = "Sets the plot line type (index of selector) for Group 1";
    public static String PROPERTY_ANGULAR_VIEW_GROUP1_LINE_INDEX_DEFAULT = "1";
    public static int PROPERTY_ANGULAR_VIEW_GROUP1_LINE_INDEX_DEFAULT_INT = 1;

    public static final String PROPERTY_ANGULAR_VIEW_GROUP1_SYMBOL_INDEX_KEY = PROPERTY_ROOT_KEY + "plot.group1.symbol_index";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP1_SYMBOL_INDEX_LABEL = "Group 1 - Plot Symbol (Index)";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP1_SYMBOL_INDEX_TOOLTIP = "Sets the plot symbol (index of selector) for Group 1";
    public static String PROPERTY_ANGULAR_VIEW_GROUP1_SYMBOL_INDEX_DEFAULT = "1";
    public static int PROPERTY_ANGULAR_VIEW_GROUP1_SYMBOL_INDEX_DEFAULT_INT = 1;

    public static final String PROPERTY_ANGULAR_VIEW_GROUP1_SYMBOL_SIZE_KEY = PROPERTY_ROOT_KEY + "plot.group1.symbol_size";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP1_SYMBOL_SIZE_LABEL = "Group 1 - Plot Symbol Size";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP1_SYMBOL_SIZE_TOOLTIP = "Sets the plot symbol size for Group 1";
    public static String PROPERTY_ANGULAR_VIEW_GROUP1_SYMBOL_SIZE_DEFAULT = "3";
    public static int PROPERTY_ANGULAR_VIEW_GROUP1_SYMBOL_SIZE_DEFAULT_INT = 3;

    public static final String PROPERTY_ANGULAR_VIEW_GROUP1_COLOR_KEY = PROPERTY_ROOT_KEY + "plot.group1.color";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP1_COLOR_LABEL = "Group 1 - Plot Color";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP1_COLOR_TOOLTIP = "Sets the color of the plot lines and symbols for Group 1";
    public static Color PROPERTY_ANGULAR_VIEW_GROUP1_COLOR_DEFAULT = new Color(114,77,163);




    public static final String PROPERTY_ANGULAR_VIEW_GROUP2_SECTION_KEY =  PROPERTY_ROOT_KEY + "plot.group2.section";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP2_SECTION_LABEL = "Plot Graphics: Group 2";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP2_SECTION_TOOLTIP = "Sets plot graphics based on the specified wavelength for Group 2";

    public static final String PROPERTY_ANGULAR_VIEW_GROUP2_MIN_WAVELENGTH_KEY = PROPERTY_ROOT_KEY + "plot.group2.min_wavelength";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP2_MIN_WAVELENGTH_LABEL = "Group 2 - Min Wavelength (nm)";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP2_MIN_WAVELENGTH_TOOLTIP = "<html>Minimum Wavelength for Group 5<br>wavelength=-1 indicates group is disabled</html>";
    public static double PROPERTY_ANGULAR_VIEW_GROUP2_MIN_WAVELENGTH_DEFAULT = 400.0;

    public static final String PROPERTY_ANGULAR_VIEW_GROUP2_MAX_WAVELENGTH_KEY = PROPERTY_ROOT_KEY + "plot.group2.max_wavelength";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP2_MAX_WAVELENGTH_LABEL = "Group 2 - Max Wavelength (nm)";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP2_MAX_WAVELENGTH_TOOLTIP = "<html>Maximum Wavelength for Group 5<br>wavelength=-1 indicates group is disabled</html>";
    public static double PROPERTY_ANGULAR_VIEW_GROUP2_MAX_WAVELENGTH_DEFAULT = 499.9;

    public static final String PROPERTY_ANGULAR_VIEW_GROUP2_LINE_INDEX_KEY = PROPERTY_ROOT_KEY + "plot.group2.line_index";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP2_LINE_INDEX_LABEL = "Group 2 - Plot Line Type (Index)";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP2_LINE_INDEX_TOOLTIP = "Sets the plot line type (index of selector) for Group 2";
    public static String PROPERTY_ANGULAR_VIEW_GROUP2_LINE_INDEX_DEFAULT = "1";
    public static int PROPERTY_ANGULAR_VIEW_GROUP2_LINE_INDEX_DEFAULT_INT = 1;

    public static final String PROPERTY_ANGULAR_VIEW_GROUP2_SYMBOL_INDEX_KEY = PROPERTY_ROOT_KEY + "plot.group2.symbol_index";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP2_SYMBOL_INDEX_LABEL = "Group 2 - Plot Symbol (Index)";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP2_SYMBOL_INDEX_TOOLTIP = "Sets the plot symbol (index of selector) for Group 2";
    public static String PROPERTY_ANGULAR_VIEW_GROUP2_SYMBOL_INDEX_DEFAULT = "1";
    public static int PROPERTY_ANGULAR_VIEW_GROUP2_SYMBOL_INDEX_DEFAULT_INT = 1;

    public static final String PROPERTY_ANGULAR_VIEW_GROUP2_SYMBOL_SIZE_KEY = PROPERTY_ROOT_KEY + "plot.group2.symbol_size";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP2_SYMBOL_SIZE_LABEL = "Group 2 - Plot Symbol Size";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP2_SYMBOL_SIZE_TOOLTIP = "Sets the plot symbol size for Group 2";
    public static String PROPERTY_ANGULAR_VIEW_GROUP2_SYMBOL_SIZE_DEFAULT = "3";
    public static int PROPERTY_ANGULAR_VIEW_GROUP2_SYMBOL_SIZE_DEFAULT_INT = 3;

    public static final String PROPERTY_ANGULAR_VIEW_GROUP2_COLOR_KEY = PROPERTY_ROOT_KEY + "plot.group2.color";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP2_COLOR_LABEL = "Group2 - Plot Color";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP2_COLOR_TOOLTIP = "Sets the color of the plot lines whose associated wavelengths are specified as Group 2";
    public static Color PROPERTY_ANGULAR_VIEW_GROUP2_COLOR_DEFAULT = Color.BLUE;;





    public static final String PROPERTY_ANGULAR_VIEW_GROUP3_SECTION_KEY =  PROPERTY_ROOT_KEY + "plot.group3.section";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP3_SECTION_LABEL = "Plot Graphics: Group 3";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP3_SECTION_TOOLTIP = "Sets plot graphics based on the specified wavelength for Group 3";

    public static final String PROPERTY_ANGULAR_VIEW_GROUP3_MIN_WAVELENGTH_KEY = PROPERTY_ROOT_KEY + "plot.group3.min_wavelength";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP3_MIN_WAVELENGTH_LABEL = "Group 3 - Min Wavelength (nm)";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP3_MIN_WAVELENGTH_TOOLTIP = "<html>Minimum Wavelength for Group 5<br>wavelength=-1 indicates group is disabled</html>";
    public static double PROPERTY_ANGULAR_VIEW_GROUP3_MIN_WAVELENGTH_DEFAULT = 500.0;

    public static final String PROPERTY_ANGULAR_VIEW_GROUP3_MAX_WAVELENGTH_KEY = PROPERTY_ROOT_KEY + "plot.group3.max_wavelength";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP3_MAX_WAVELENGTH_LABEL = "Group 3 - Max Wavelength (nm)";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP3_MAX_WAVELENGTH_TOOLTIP = "<html>Maximum Wavelength for Group 5<br>wavelength=-1 indicates group is disabled</html>";
    public static double PROPERTY_ANGULAR_VIEW_GROUP3_MAX_WAVELENGTH_DEFAULT = 599.0;

    public static final String PROPERTY_ANGULAR_VIEW_GROUP3_LINE_INDEX_KEY = PROPERTY_ROOT_KEY + "plot.group3.line_index";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP3_LINE_INDEX_LABEL = "Group 3 - Plot Line Type (Index)";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP3_LINE_INDEX_TOOLTIP = "Sets the plot line type (index of selector) for Group 3";
    public static String PROPERTY_ANGULAR_VIEW_GROUP3_LINE_INDEX_DEFAULT = "1";
    public static int PROPERTY_ANGULAR_VIEW_GROUP3_LINE_INDEX_DEFAULT_INT = 1;

    public static final String PROPERTY_ANGULAR_VIEW_GROUP3_SYMBOL_INDEX_KEY = PROPERTY_ROOT_KEY + "plot.group3.symbol_index";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP3_SYMBOL_INDEX_LABEL = "Group 3 - Plot Symbol (Index)";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP3_SYMBOL_INDEX_TOOLTIP = "Sets the plot symbol (index of selector) for Group 3";
    public static String PROPERTY_ANGULAR_VIEW_GROUP3_SYMBOL_INDEX_DEFAULT = "1";
    public static int PROPERTY_ANGULAR_VIEW_GROUP3_SYMBOL_INDEX_DEFAULT_INT = 1;

    public static final String PROPERTY_ANGULAR_VIEW_GROUP3_SYMBOL_SIZE_KEY = PROPERTY_ROOT_KEY + "plot.group3.symbol_size";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP3_SYMBOL_SIZE_LABEL = "Group 3 - Plot Symbol Size";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP3_SYMBOL_SIZE_TOOLTIP = "Sets the plot symbol size for Group 3";
    public static String PROPERTY_ANGULAR_VIEW_GROUP3_SYMBOL_SIZE_DEFAULT = "3";
    public static int PROPERTY_ANGULAR_VIEW_GROUP3_SYMBOL_SIZE_DEFAULT_INT = 3;
    
    public static final String PROPERTY_ANGULAR_VIEW_GROUP3_COLOR_KEY = PROPERTY_ROOT_KEY + "plot.group3.color";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP3_COLOR_LABEL = "Group3 - Plot Color";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP3_COLOR_TOOLTIP = "Sets the color of the plot lines whose associated wavelengths are specified as Group 3";
    public static Color PROPERTY_ANGULAR_VIEW_GROUP3_COLOR_DEFAULT = new Color(0,180,0) ;





    public static final String PROPERTY_ANGULAR_VIEW_GROUP4_SECTION_KEY =  PROPERTY_ROOT_KEY + "plot.group4.section";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP4_SECTION_LABEL = "Plot Graphics: Group 4";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP4_SECTION_TOOLTIP = "Sets plot graphics based on the specified wavelength for Group 4";

    public static final String PROPERTY_ANGULAR_VIEW_GROUP4_MIN_WAVELENGTH_KEY = PROPERTY_ROOT_KEY + "plot.group4.min_wavelength";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP4_MIN_WAVELENGTH_LABEL = "Group 4 - Min Wavelength (nm)";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP4_MIN_WAVELENGTH_TOOLTIP = "<html>Minimum Wavelength for Group 5<br>wavelength=-1 indicates group is disabled</html>";
    public static double PROPERTY_ANGULAR_VIEW_GROUP4_MIN_WAVELENGTH_DEFAULT = 600.0;

    public static final String PROPERTY_ANGULAR_VIEW_GROUP4_MAX_WAVELENGTH_KEY = PROPERTY_ROOT_KEY + "plot.group4.max_wavelength";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP4_MAX_WAVELENGTH_LABEL = "Group 4 - Max Wavelength (nm)";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP4_MAX_WAVELENGTH_TOOLTIP = "<html>Maximum Wavelength for Group 5<br>wavelength=-1 indicates group is disabled</html>";
    public static double PROPERTY_ANGULAR_VIEW_GROUP4_MAX_WAVELENGTH_DEFAULT = 724.9;

    public static final String PROPERTY_ANGULAR_VIEW_GROUP4_LINE_INDEX_KEY = PROPERTY_ROOT_KEY + "plot.group4.line_index";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP4_LINE_INDEX_LABEL = "Group 4 - Plot Line Type (Index)";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP4_LINE_INDEX_TOOLTIP = "Sets the plot line type (index of selector) for Group 4";
    public static String PROPERTY_ANGULAR_VIEW_GROUP4_LINE_INDEX_DEFAULT = "1";
    public static int PROPERTY_ANGULAR_VIEW_GROUP4_LINE_INDEX_DEFAULT_INT = 1;

    public static final String PROPERTY_ANGULAR_VIEW_GROUP4_SYMBOL_INDEX_KEY = PROPERTY_ROOT_KEY + "plot.group4.symbol_index";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP4_SYMBOL_INDEX_LABEL = "Group 4 - Plot Symbol (Index)";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP4_SYMBOL_INDEX_TOOLTIP = "Sets the plot symbol (index of selector) for Group 4";
    public static String PROPERTY_ANGULAR_VIEW_GROUP4_SYMBOL_INDEX_DEFAULT = "1";
    public static int PROPERTY_ANGULAR_VIEW_GROUP4_SYMBOL_INDEX_DEFAULT_INT = 1;

    public static final String PROPERTY_ANGULAR_VIEW_GROUP4_SYMBOL_SIZE_KEY = PROPERTY_ROOT_KEY + "plot.group4.symbol_size";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP4_SYMBOL_SIZE_LABEL = "Group 4 - Plot Symbol Size";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP4_SYMBOL_SIZE_TOOLTIP = "Sets the plot symbol size for Group 4";
    public static String PROPERTY_ANGULAR_VIEW_GROUP4_SYMBOL_SIZE_DEFAULT = "3";
    public static int PROPERTY_ANGULAR_VIEW_GROUP4_SYMBOL_SIZE_DEFAULT_INT = 3;
    
    public static final String PROPERTY_ANGULAR_VIEW_GROUP4_COLOR_KEY = PROPERTY_ROOT_KEY + "plot.group4.color";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP4_COLOR_LABEL = "Group4 - Plot Color";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP4_COLOR_TOOLTIP = "Sets the color of the plot lines whose associated wavelengths are specified as Group 4";
    public static Color PROPERTY_ANGULAR_VIEW_GROUP4_COLOR_DEFAULT = new Color (230,0,0);




    public static final String PROPERTY_ANGULAR_VIEW_GROUP5_SECTION_KEY =  PROPERTY_ROOT_KEY + "plot.group5.section";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP5_SECTION_LABEL = "Plot Graphics: Group 5";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP5_SECTION_TOOLTIP = "Sets plot graphics based on the specified wavelength for Group 5";

    public static final String PROPERTY_ANGULAR_VIEW_GROUP5_MIN_WAVELENGTH_KEY = PROPERTY_ROOT_KEY + "plot.group5.min_wavelength";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP5_MIN_WAVELENGTH_LABEL = "Group 5 - Min Wavelength (nm)";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP5_MIN_WAVELENGTH_TOOLTIP = "<html>Minimum Wavelength for Group 5<br>wavelength=-1 indicates group is disabled</html>";
    public static double PROPERTY_ANGULAR_VIEW_GROUP5_MIN_WAVELENGTH_DEFAULT = 725.0;

    public static final String PROPERTY_ANGULAR_VIEW_GROUP5_MAX_WAVELENGTH_KEY = PROPERTY_ROOT_KEY + "plot.group5.max_wavelength";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP5_MAX_WAVELENGTH_LABEL = "Group 5 - Max Wavelength (nm)";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP5_MAX_WAVELENGTH_TOOLTIP = "<html>Maximum Wavelength for Group 5<br>wavelength=-1 indicates group is disabled</html>";
    public static double PROPERTY_ANGULAR_VIEW_GROUP5_MAX_WAVELENGTH_DEFAULT = 15000.0;

    public static final String PROPERTY_ANGULAR_VIEW_GROUP5_LINE_INDEX_KEY = PROPERTY_ROOT_KEY + "plot.group5.line_index";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP5_LINE_INDEX_LABEL = "Group 5 - Plot Line Type (Index)";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP5_LINE_INDEX_TOOLTIP = "Sets the plot line type (index of selector) for Group 5";
    public static String PROPERTY_ANGULAR_VIEW_GROUP5_LINE_INDEX_DEFAULT = "1";
    public static int PROPERTY_ANGULAR_VIEW_GROUP5_LINE_INDEX_DEFAULT_INT = 1;

    public static final String PROPERTY_ANGULAR_VIEW_GROUP5_SYMBOL_INDEX_KEY = PROPERTY_ROOT_KEY + "plot.group5.symbol_index";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP5_SYMBOL_INDEX_LABEL = "Group 5 - Plot Symbol (Index)";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP5_SYMBOL_INDEX_TOOLTIP = "Sets the plot symbol (index of selector) for Group 5";
    public static String PROPERTY_ANGULAR_VIEW_GROUP5_SYMBOL_INDEX_DEFAULT = "1";
    public static int PROPERTY_ANGULAR_VIEW_GROUP5_SYMBOL_INDEX_DEFAULT_INT = 1;

    public static final String PROPERTY_ANGULAR_VIEW_GROUP5_SYMBOL_SIZE_KEY = PROPERTY_ROOT_KEY + "plot.group5.symbol_size";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP5_SYMBOL_SIZE_LABEL = "Group 5 - Plot Symbol Size";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP5_SYMBOL_SIZE_TOOLTIP = "Sets the plot symbol size for Group 5";
    public static String PROPERTY_ANGULAR_VIEW_GROUP5_SYMBOL_SIZE_DEFAULT = "3";
    public static int PROPERTY_ANGULAR_VIEW_GROUP5_SYMBOL_SIZE_DEFAULT_INT = 3;

    public static final String PROPERTY_ANGULAR_VIEW_GROUP5_COLOR_KEY = PROPERTY_ROOT_KEY + "plot.group5.color";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP5_COLOR_LABEL = "Group 5 - Plot Color";
    public static final String PROPERTY_ANGULAR_VIEW_GROUP5_COLOR_TOOLTIP = "Sets the color of the plot lines and symbols for Group 5";
    public static Color PROPERTY_ANGULAR_VIEW_GROUP5_COLOR_DEFAULT = new Color (75,0,0);
    
    


    // Restore to defaults


    private static final String PROPERTY_RESTORE_KEY_SUFFIX = PROPERTY_ROOT_KEY + ".restore.defaults";

    public static final String PROPERTY_RESTORE_SECTION_KEY = PROPERTY_RESTORE_KEY_SUFFIX + ".section";
    public static final String PROPERTY_RESTORE_SECTION_LABEL = "Restore";
    public static final String PROPERTY_RESTORE_SECTION_TOOLTIP = "Restores preferences to the package defaults";

    public static final String PROPERTY_RESTORE_DEFAULTS_NAME = PROPERTY_RESTORE_KEY_SUFFIX + ".apply";
    public static final String PROPERTY_RESTORE_DEFAULTS_LABEL = "Restore Defaults (Angular View Preferences)";
    public static final String PROPERTY_RESTORE_DEFAULTS_TOOLTIP = "Restore all RGB Image preferences to the original default";
    public static final boolean PROPERTY_RESTORE_DEFAULTS_DEFAULT = false;




    protected PropertySet createPropertySet() {
        return createPropertySet(new GeneralLayerBean());
    }


    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("colorManipulationPreferences");
    }

    @Override
    protected JPanel createPanel(BindingContext context) {


        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_RANGE_DOMAIN_SECTION_KEY, true);

        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_XAXIS_SET_BOUNDS_KEY, PROPERTY_ANGULAR_VIEW_XAXIS_SET_BOUNDS_DEFAULT);
        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_XAXIS_MIN_KEY, PROPERTY_ANGULAR_VIEW_XAXIS_MIN_DEFAULT);
        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_XAXIS_MAX_KEY, PROPERTY_ANGULAR_VIEW_XAXIS_MAX_DEFAULT);

        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_YAXIS_SET_BOUNDS_KEY, PROPERTY_ANGULAR_VIEW_YAXIS_SET_BOUNDS_DEFAULT);
        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_YAXIS_MIN_KEY, PROPERTY_ANGULAR_VIEW_YAXIS_MIN_DEFAULT);
        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_YAXIS_MAX_KEY, PROPERTY_ANGULAR_VIEW_YAXIS_MAX_DEFAULT);


        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_PLOT_GRAPHICS_SECTION_KEY, true);

        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_TITLE_KEY, PROPERTY_ANGULAR_VIEW_TITLE_DEFAULT);

//        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_FOREGROUND_COLOR_KEY, PROPERTY_ANGULAR_VIEW_FOREGROUND_COLOR_DEFAULT);
        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_BACKGROUND_COLOR_KEY, PROPERTY_ANGULAR_VIEW_BACKGROUND_COLOR_DEFAULT);
//        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_MARGIN_BACKGROUND_COLOR_KEY, PROPERTY_ANGULAR_VIEW_MARGIN_BACKGROUND_COLOR_DEFAULT);
//        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_LEGEND_BACKGROUND_COLOR_KEY, PROPERTY_ANGULAR_VIEW_LEGEND_BACKGROUND_COLOR_DEFAULT);
        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_GRIDLINE_COLOR_KEY, PROPERTY_ANGULAR_VIEW_GRIDLINE_COLOR_DEFAULT);
        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_GRIDLINE_SHOW_KEY, PROPERTY_ANGULAR_VIEW_GRIDLINE_SHOW_DEFAULT);


        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_LINE_INDEX_KEY, PROPERTY_ANGULAR_VIEW_LINE_INDEX_DEFAULT);
        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_SYMBOL_INDEX_KEY, PROPERTY_ANGULAR_VIEW_SYMBOL_INDEX_DEFAULT);
        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_SYMBOL_SIZE_KEY, PROPERTY_ANGULAR_VIEW_SYMBOL_SIZE_DEFAULT);
        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_PLOT_COLOR_KEY, PROPERTY_ANGULAR_VIEW_PLOT_COLOR_DEFAULT);
        
        
        
        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_GROUP1_SECTION_KEY, true);
        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_GROUP1_MIN_WAVELENGTH_KEY, PROPERTY_ANGULAR_VIEW_GROUP1_MIN_WAVELENGTH_DEFAULT);
        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_GROUP1_MAX_WAVELENGTH_KEY, PROPERTY_ANGULAR_VIEW_GROUP1_MAX_WAVELENGTH_DEFAULT);
        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_GROUP1_LINE_INDEX_KEY, PROPERTY_ANGULAR_VIEW_GROUP1_LINE_INDEX_DEFAULT);
        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_GROUP1_SYMBOL_INDEX_KEY, PROPERTY_ANGULAR_VIEW_GROUP1_SYMBOL_INDEX_DEFAULT);
        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_GROUP1_SYMBOL_SIZE_KEY, PROPERTY_ANGULAR_VIEW_GROUP1_SYMBOL_SIZE_DEFAULT);
        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_GROUP1_COLOR_KEY, PROPERTY_ANGULAR_VIEW_GROUP1_COLOR_DEFAULT);

        
        
        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_GROUP2_SECTION_KEY, true);
        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_GROUP2_MIN_WAVELENGTH_KEY, PROPERTY_ANGULAR_VIEW_GROUP2_MIN_WAVELENGTH_DEFAULT);
        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_GROUP2_MAX_WAVELENGTH_KEY, PROPERTY_ANGULAR_VIEW_GROUP2_MAX_WAVELENGTH_DEFAULT);
        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_GROUP2_LINE_INDEX_KEY, PROPERTY_ANGULAR_VIEW_GROUP2_LINE_INDEX_DEFAULT);
        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_GROUP2_SYMBOL_INDEX_KEY, PROPERTY_ANGULAR_VIEW_GROUP2_SYMBOL_INDEX_DEFAULT);
        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_GROUP2_SYMBOL_SIZE_KEY, PROPERTY_ANGULAR_VIEW_GROUP2_SYMBOL_SIZE_DEFAULT);
        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_GROUP2_COLOR_KEY, PROPERTY_ANGULAR_VIEW_GROUP2_COLOR_DEFAULT);
        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_GROUP2_COLOR_KEY, PROPERTY_ANGULAR_VIEW_GROUP2_COLOR_DEFAULT);



        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_GROUP3_SECTION_KEY, true);
        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_GROUP3_MIN_WAVELENGTH_KEY, PROPERTY_ANGULAR_VIEW_GROUP3_MIN_WAVELENGTH_DEFAULT);
        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_GROUP3_MAX_WAVELENGTH_KEY, PROPERTY_ANGULAR_VIEW_GROUP3_MAX_WAVELENGTH_DEFAULT);
        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_GROUP3_LINE_INDEX_KEY, PROPERTY_ANGULAR_VIEW_GROUP3_LINE_INDEX_DEFAULT);
        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_GROUP3_SYMBOL_INDEX_KEY, PROPERTY_ANGULAR_VIEW_GROUP3_SYMBOL_INDEX_DEFAULT);
        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_GROUP3_SYMBOL_SIZE_KEY, PROPERTY_ANGULAR_VIEW_GROUP3_SYMBOL_SIZE_DEFAULT);
        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_GROUP3_COLOR_KEY, PROPERTY_ANGULAR_VIEW_GROUP3_COLOR_DEFAULT);
        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_GROUP3_COLOR_KEY, PROPERTY_ANGULAR_VIEW_GROUP3_COLOR_DEFAULT);




        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_GROUP4_SECTION_KEY, true);
        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_GROUP4_MIN_WAVELENGTH_KEY, PROPERTY_ANGULAR_VIEW_GROUP4_MIN_WAVELENGTH_DEFAULT);
        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_GROUP4_MAX_WAVELENGTH_KEY, PROPERTY_ANGULAR_VIEW_GROUP4_MAX_WAVELENGTH_DEFAULT);
        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_GROUP4_LINE_INDEX_KEY, PROPERTY_ANGULAR_VIEW_GROUP4_LINE_INDEX_DEFAULT);
        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_GROUP4_SYMBOL_INDEX_KEY, PROPERTY_ANGULAR_VIEW_GROUP4_SYMBOL_INDEX_DEFAULT);
        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_GROUP4_SYMBOL_SIZE_KEY, PROPERTY_ANGULAR_VIEW_GROUP4_SYMBOL_SIZE_DEFAULT);
        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_GROUP4_COLOR_KEY, PROPERTY_ANGULAR_VIEW_GROUP4_COLOR_DEFAULT);
        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_GROUP4_COLOR_KEY, PROPERTY_ANGULAR_VIEW_GROUP4_COLOR_DEFAULT);

        
        
        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_GROUP5_SECTION_KEY, true);
        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_GROUP5_MIN_WAVELENGTH_KEY, PROPERTY_ANGULAR_VIEW_GROUP5_MIN_WAVELENGTH_DEFAULT);
        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_GROUP5_MAX_WAVELENGTH_KEY, PROPERTY_ANGULAR_VIEW_GROUP5_MAX_WAVELENGTH_DEFAULT);
        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_GROUP5_LINE_INDEX_KEY, PROPERTY_ANGULAR_VIEW_GROUP5_LINE_INDEX_DEFAULT);
        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_GROUP5_SYMBOL_INDEX_KEY, PROPERTY_ANGULAR_VIEW_GROUP5_SYMBOL_INDEX_DEFAULT);
        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_GROUP5_SYMBOL_SIZE_KEY, PROPERTY_ANGULAR_VIEW_GROUP5_SYMBOL_SIZE_DEFAULT);
        initPropertyDefaults(context, PROPERTY_ANGULAR_VIEW_GROUP5_COLOR_KEY, PROPERTY_ANGULAR_VIEW_GROUP5_COLOR_DEFAULT);


        restoreDefaults =  initPropertyDefaults(context, PROPERTY_RESTORE_DEFAULTS_NAME, PROPERTY_RESTORE_DEFAULTS_DEFAULT);




        //
        // Create UI
        //

        TableLayout tableLayout = new TableLayout(2);
        tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        tableLayout.setTablePadding(new Insets(4, 10, 0, 0));
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setColumnWeightX(1, 1.0);

        JPanel pageUI = new JPanel(tableLayout);

        PropertyEditorRegistry registry = PropertyEditorRegistry.getInstance();

        PropertySet propertyContainer = context.getPropertySet();
        Property[] properties = propertyContainer.getProperties();

        int currRow = 0;
        for (Property property : properties) {
            PropertyDescriptor descriptor = property.getDescriptor();
            PropertyPane.addComponent(currRow, tableLayout, pageUI, context, registry, descriptor);
            currRow++;
        }

        pageUI.add(tableLayout.createVerticalSpacer());

        JPanel parent = new JPanel(new BorderLayout());
        parent.add(pageUI, BorderLayout.CENTER);
        parent.add(Box.createHorizontalStrut(50), BorderLayout.EAST);
        return parent;
    }



    @Override
    protected void configure(BindingContext context) {

        // Handle resetDefaults events - set all other components to defaults
        restoreDefaults.addPropertyChangeListener(evt -> {
            handleRestoreDefaults(context);
        });



        // Add listeners to all components in order to uncheck restoreDefaults checkbox accordingly

        PropertySet propertyContainer = context.getPropertySet();
        Property[] properties = propertyContainer.getProperties();

        for (Property property : properties) {
            if (property != restoreDefaults) {
                property.addPropertyChangeListener(evt -> {
                    handlePreferencesPropertyValueChange(context);
                });
            }
        }
    }






    /**
     * Test all properties to determine whether the current value is the default value
     *
     * @param context
     * @return
     * @author Daniel Knowles
     */
    private boolean isDefaults(BindingContext context) {

        PropertySet propertyContainer = context.getPropertySet();
        Property[] properties = propertyContainer.getProperties();

        for (Property property : properties) {
            if (property != restoreDefaults && property.getDescriptor().getDefaultValue() != null)
                if (!property.getValue().equals(property.getDescriptor().getDefaultValue())) {
                    return false;
                }
        }

        return true;
    }


    /**
     * Handles the restore defaults action
     *
     * @param context
     * @author Daniel Knowles
     */
    private void handleRestoreDefaults(BindingContext context) {
        if (propertyValueChangeEventsEnabled) {
            propertyValueChangeEventsEnabled = false;
            try {
                if (restoreDefaults.getValue()) {

                    PropertySet propertyContainer = context.getPropertySet();
                    Property[] properties = propertyContainer.getProperties();

                    for (Property property : properties) {
                        if (property != restoreDefaults && property.getDescriptor().getDefaultValue() != null)
                            property.setValue(property.getDescriptor().getDefaultValue());
                    }
                }
            } catch (ValidationException e) {
                e.printStackTrace();
            }
            propertyValueChangeEventsEnabled = true;

            context.setComponentsEnabled(PROPERTY_RESTORE_DEFAULTS_NAME, false);
        }
    }







    /**
     * Set restoreDefault component because a property has changed
     * @param context
     * @author Daniel Knowles
     */
    private void handlePreferencesPropertyValueChange(BindingContext context) {
        if (propertyValueChangeEventsEnabled) {
            propertyValueChangeEventsEnabled = false;
            try {
                restoreDefaults.setValue(isDefaults(context));
                context.setComponentsEnabled(PROPERTY_RESTORE_DEFAULTS_NAME, !isDefaults(context));
            } catch (ValidationException e) {
                e.printStackTrace();
            }
            propertyValueChangeEventsEnabled = true;
        }
    }


    /**
     * Initialize the property descriptor default value
     *
     * @param context
     * @param propertyName
     * @param propertyDefault
     * @return
     * @author Daniel Knowles
     */
    private Property initPropertyDefaults(BindingContext context, String propertyName, Object propertyDefault) {

        Property property = context.getPropertySet().getProperty(propertyName);

        property.getDescriptor().setDefaultValue(propertyDefault);

        return property;
    }










    @SuppressWarnings("UnusedDeclaration")
    static class GeneralLayerBean {



        @Preference(key = PROPERTY_ANGULAR_VIEW_RANGE_DOMAIN_SECTION_KEY,
                label = PROPERTY_ANGULAR_VIEW_RANGE_DOMAIN_SECTION_LABEL    ,
                description = PROPERTY_ANGULAR_VIEW_RANGE_DOMAIN_SECTION_TOOLTIP)
        boolean rangeDomainSection = true;

        @Preference(label = PROPERTY_ANGULAR_VIEW_XAXIS_SET_BOUNDS_LABEL,
                key = PROPERTY_ANGULAR_VIEW_XAXIS_SET_BOUNDS_KEY,
                description = PROPERTY_ANGULAR_VIEW_XAXIS_SET_BOUNDS_TOOLTIP)
        boolean angularViewXaxisSetBoundsDefault = PROPERTY_ANGULAR_VIEW_XAXIS_SET_BOUNDS_DEFAULT;

        @Preference(label = PROPERTY_ANGULAR_VIEW_XAXIS_MIN_LABEL,
                key = PROPERTY_ANGULAR_VIEW_XAXIS_MIN_KEY,
                description = PROPERTY_ANGULAR_VIEW_XAXIS_MIN_TOOLTIP)
        String angularViewXaxisMinDefault = PROPERTY_ANGULAR_VIEW_XAXIS_MIN_DEFAULT;

        @Preference(label = PROPERTY_ANGULAR_VIEW_XAXIS_MAX_LABEL,
                key = PROPERTY_ANGULAR_VIEW_XAXIS_MAX_KEY,
                description = PROPERTY_ANGULAR_VIEW_XAXIS_MAX_TOOLTIP)
        String angularViewXaxisMaxDefault = PROPERTY_ANGULAR_VIEW_XAXIS_MAX_DEFAULT;

        @Preference(label = PROPERTY_ANGULAR_VIEW_YAXIS_SET_BOUNDS_LABEL,
                key = PROPERTY_ANGULAR_VIEW_YAXIS_SET_BOUNDS_KEY,
                description = PROPERTY_ANGULAR_VIEW_YAXIS_SET_BOUNDS_TOOLTIP)
        boolean angularViewYaxisSetBoundsDefault = PROPERTY_ANGULAR_VIEW_YAXIS_SET_BOUNDS_DEFAULT;

        @Preference(label = PROPERTY_ANGULAR_VIEW_YAXIS_MIN_LABEL,
                key = PROPERTY_ANGULAR_VIEW_YAXIS_MIN_KEY,
                description = PROPERTY_ANGULAR_VIEW_YAXIS_MIN_TOOLTIP)
        String angularViewYaxisMinDefault = PROPERTY_ANGULAR_VIEW_YAXIS_MIN_DEFAULT;

        @Preference(label = PROPERTY_ANGULAR_VIEW_YAXIS_MAX_LABEL,
                key = PROPERTY_ANGULAR_VIEW_YAXIS_MAX_KEY,
                description = PROPERTY_ANGULAR_VIEW_YAXIS_MAX_TOOLTIP)
        String angularViewYaxisMaxDefault = PROPERTY_ANGULAR_VIEW_YAXIS_MAX_DEFAULT;



        @Preference(key = PROPERTY_ANGULAR_VIEW_PLOT_GRAPHICS_SECTION_KEY,
                label = PROPERTY_ANGULAR_VIEW_PLOT_GRAPHICS_SECTION_LABEL    ,
                description = PROPERTY_ANGULAR_VIEW_PLOT_GRAPHICS_SECTION_TOOLTIP)
        boolean plotGraphicsSection = true;

        @Preference(label = PROPERTY_ANGULAR_VIEW_TITLE_LABEL,
                key = PROPERTY_ANGULAR_VIEW_TITLE_KEY,
                description = PROPERTY_ANGULAR_VIEW_TITLE_TOOLTIP)
        String angularViewTitleDefault = PROPERTY_ANGULAR_VIEW_TITLE_DEFAULT;

        
//        @Preference(label = PROPERTY_ANGULAR_VIEW_FOREGROUND_COLOR_LABEL,
//                key = PROPERTY_ANGULAR_VIEW_FOREGROUND_COLOR_KEY,
//                description = PROPERTY_ANGULAR_VIEW_FOREGROUND_COLOR_TOOLTIP)
//        Color angularViewForegroundColorDefault = PROPERTY_ANGULAR_VIEW_FOREGROUND_COLOR_DEFAULT;

        @Preference(label = PROPERTY_ANGULAR_VIEW_BACKGROUND_COLOR_LABEL,
                key = PROPERTY_ANGULAR_VIEW_BACKGROUND_COLOR_KEY,
                description = PROPERTY_ANGULAR_VIEW_BACKGROUND_COLOR_TOOLTIP)
        Color angularViewBackgroundColorDefault = PROPERTY_ANGULAR_VIEW_BACKGROUND_COLOR_DEFAULT;

//        @Preference(label = PROPERTY_ANGULAR_VIEW_MARGIN_BACKGROUND_COLOR_LABEL,
//                key = PROPERTY_ANGULAR_VIEW_MARGIN_BACKGROUND_COLOR_KEY,
//                description = PROPERTY_ANGULAR_VIEW_MARGIN_BACKGROUND_COLOR_TOOLTIP)
//        Color angularViewMarginBackgroundColorDefault = PROPERTY_ANGULAR_VIEW_MARGIN_BACKGROUND_COLOR_DEFAULT;
//
//        @Preference(label = PROPERTY_ANGULAR_VIEW_LEGEND_BACKGROUND_COLOR_LABEL,
//                key = PROPERTY_ANGULAR_VIEW_LEGEND_BACKGROUND_COLOR_KEY,
//                description = PROPERTY_ANGULAR_VIEW_LEGEND_BACKGROUND_COLOR_TOOLTIP)
//        Color angularViewLegendBackgroundColorDefault = PROPERTY_ANGULAR_VIEW_LEGEND_BACKGROUND_COLOR_DEFAULT;

        @Preference(label = PROPERTY_ANGULAR_VIEW_GRIDLINE_COLOR_LABEL,
                key = PROPERTY_ANGULAR_VIEW_GRIDLINE_COLOR_KEY,
                description = PROPERTY_ANGULAR_VIEW_GRIDLINE_COLOR_TOOLTIP)
        Color angularViewGridlineColorDefault = PROPERTY_ANGULAR_VIEW_GRIDLINE_COLOR_DEFAULT;


        @Preference(label = PROPERTY_ANGULAR_VIEW_GRIDLINE_SHOW_LABEL,
                key = PROPERTY_ANGULAR_VIEW_GRIDLINE_SHOW_KEY,
                description = PROPERTY_ANGULAR_VIEW_GRIDLINE_SHOW_TOOLTIP)
        boolean angularViewGridlineShowDefault = PROPERTY_ANGULAR_VIEW_GRIDLINE_SHOW_DEFAULT;





        @Preference( key = PROPERTY_ANGULAR_VIEW_LINE_INDEX_KEY,
                label = PROPERTY_ANGULAR_VIEW_LINE_INDEX_LABEL,
                valueSet = {"0","1","2","3","4","5","6","7"},
                description = PROPERTY_ANGULAR_VIEW_LINE_INDEX_TOOLTIP)
        String angularViewLineIndexDefault = PROPERTY_ANGULAR_VIEW_LINE_INDEX_DEFAULT;

        @Preference( key = PROPERTY_ANGULAR_VIEW_SYMBOL_INDEX_KEY,
                label = PROPERTY_ANGULAR_VIEW_SYMBOL_INDEX_LABEL,
                valueSet = {"0","1","2","3","4","5","6","7","8","9","10"},
                description = PROPERTY_ANGULAR_VIEW_SYMBOL_INDEX_TOOLTIP)
        String angularViewSymbolIndexDefault = PROPERTY_ANGULAR_VIEW_SYMBOL_INDEX_DEFAULT;

        @Preference( key = PROPERTY_ANGULAR_VIEW_SYMBOL_SIZE_KEY,
                label = PROPERTY_ANGULAR_VIEW_SYMBOL_SIZE_LABEL,
                valueSet = {"1","2","3","4","5","6","7","8","9"},
                description = PROPERTY_ANGULAR_VIEW_SYMBOL_SIZE_TOOLTIP)
        String angularViewSymbolSizeDefault = PROPERTY_ANGULAR_VIEW_SYMBOL_SIZE_DEFAULT;

        @Preference( key = PROPERTY_ANGULAR_VIEW_PLOT_COLOR_KEY,
                label = PROPERTY_ANGULAR_VIEW_PLOT_COLOR_LABEL,
                description = PROPERTY_ANGULAR_VIEW_PLOT_COLOR_TOOLTIP)
        Color angularViewColorDefault = PROPERTY_ANGULAR_VIEW_PLOT_COLOR_DEFAULT;

        
        
        




        @Preference(key = PROPERTY_ANGULAR_VIEW_GROUP1_SECTION_KEY,
                label = PROPERTY_ANGULAR_VIEW_GROUP1_SECTION_LABEL    ,
                description = PROPERTY_ANGULAR_VIEW_GROUP1_SECTION_TOOLTIP)
        boolean group1Section = true;
        
        @Preference( key = PROPERTY_ANGULAR_VIEW_GROUP1_MIN_WAVELENGTH_KEY,
                label = PROPERTY_ANGULAR_VIEW_GROUP1_MIN_WAVELENGTH_LABEL,
                interval = "[-1, 15000]",
                description = PROPERTY_ANGULAR_VIEW_GROUP1_MIN_WAVELENGTH_TOOLTIP)
        double angularViewGroup1MinWavelengthDefault = PROPERTY_ANGULAR_VIEW_GROUP1_MIN_WAVELENGTH_DEFAULT;

        @Preference( key = PROPERTY_ANGULAR_VIEW_GROUP1_MAX_WAVELENGTH_KEY,
                label = PROPERTY_ANGULAR_VIEW_GROUP1_MAX_WAVELENGTH_LABEL,
                interval = "[-1, 15000]",
                description = PROPERTY_ANGULAR_VIEW_GROUP1_MAX_WAVELENGTH_TOOLTIP)
        double angularViewGroup1MaxWavelengthDefault = PROPERTY_ANGULAR_VIEW_GROUP1_MAX_WAVELENGTH_DEFAULT;
        
        @Preference( key = PROPERTY_ANGULAR_VIEW_GROUP1_LINE_INDEX_KEY,
                label = PROPERTY_ANGULAR_VIEW_GROUP1_LINE_INDEX_LABEL,
                valueSet = {"0","1","2","3","4","5","6","7"},
                description = PROPERTY_ANGULAR_VIEW_GROUP1_LINE_INDEX_TOOLTIP)
        String angularViewGroup1LineIndexDefault = PROPERTY_ANGULAR_VIEW_GROUP1_LINE_INDEX_DEFAULT;
        
        @Preference( key = PROPERTY_ANGULAR_VIEW_GROUP1_SYMBOL_INDEX_KEY,
                label = PROPERTY_ANGULAR_VIEW_GROUP1_SYMBOL_INDEX_LABEL,
                valueSet = {"0","1","2","3","4","5","6","7","8","9","10"},
                description = PROPERTY_ANGULAR_VIEW_GROUP1_SYMBOL_INDEX_TOOLTIP)
        String angularViewGroup1SymbolIndexDefault = PROPERTY_ANGULAR_VIEW_GROUP1_SYMBOL_INDEX_DEFAULT;

        @Preference( key = PROPERTY_ANGULAR_VIEW_GROUP1_SYMBOL_SIZE_KEY,
                label = PROPERTY_ANGULAR_VIEW_GROUP1_SYMBOL_SIZE_LABEL,
                valueSet = {"1","2","3","4","5","6","7","8","9"},
                description = PROPERTY_ANGULAR_VIEW_GROUP1_SYMBOL_SIZE_TOOLTIP)
        String angularViewGroup1SymbolSizeDefault = PROPERTY_ANGULAR_VIEW_GROUP1_SYMBOL_SIZE_DEFAULT;

        @Preference( key = PROPERTY_ANGULAR_VIEW_GROUP1_COLOR_KEY,
                label = PROPERTY_ANGULAR_VIEW_GROUP1_COLOR_LABEL,
                description = PROPERTY_ANGULAR_VIEW_GROUP1_COLOR_TOOLTIP)
        Color angularViewGroup1ColorDefault = PROPERTY_ANGULAR_VIEW_GROUP1_COLOR_DEFAULT;






        @Preference(key = PROPERTY_ANGULAR_VIEW_GROUP2_SECTION_KEY,
                label = PROPERTY_ANGULAR_VIEW_GROUP2_SECTION_LABEL    ,
                description = PROPERTY_ANGULAR_VIEW_GROUP2_SECTION_TOOLTIP)
        boolean group2Section = true;

        @Preference( key = PROPERTY_ANGULAR_VIEW_GROUP2_MIN_WAVELENGTH_KEY,
                label = PROPERTY_ANGULAR_VIEW_GROUP2_MIN_WAVELENGTH_LABEL,
                interval = "[-1, 15000]",
                description = PROPERTY_ANGULAR_VIEW_GROUP2_MIN_WAVELENGTH_TOOLTIP)
        double angularViewGroup2MinWavelengthDefault = PROPERTY_ANGULAR_VIEW_GROUP2_MIN_WAVELENGTH_DEFAULT;

        @Preference( key = PROPERTY_ANGULAR_VIEW_GROUP2_MAX_WAVELENGTH_KEY,
                label = PROPERTY_ANGULAR_VIEW_GROUP2_MAX_WAVELENGTH_LABEL,
                interval = "[-1, 15000]",
                description = PROPERTY_ANGULAR_VIEW_GROUP2_MAX_WAVELENGTH_TOOLTIP)
        double angularViewGroup2MaxWavelengthDefault = PROPERTY_ANGULAR_VIEW_GROUP2_MAX_WAVELENGTH_DEFAULT;

        @Preference( key = PROPERTY_ANGULAR_VIEW_GROUP2_LINE_INDEX_KEY,
                label = PROPERTY_ANGULAR_VIEW_GROUP2_LINE_INDEX_LABEL,
                valueSet = {"0","1","2","3","4","5","6","7"},
                description = PROPERTY_ANGULAR_VIEW_GROUP2_LINE_INDEX_TOOLTIP)
        String angularViewGroup2LineIndexDefault = PROPERTY_ANGULAR_VIEW_GROUP2_LINE_INDEX_DEFAULT;

        @Preference( key = PROPERTY_ANGULAR_VIEW_GROUP2_SYMBOL_INDEX_KEY,
                label = PROPERTY_ANGULAR_VIEW_GROUP2_SYMBOL_INDEX_LABEL,
                valueSet = {"0","1","2","3","4","5","6","7","8","9","10"},
                description = PROPERTY_ANGULAR_VIEW_GROUP2_SYMBOL_INDEX_TOOLTIP)
        String angularViewGroup2SymbolIndexDefault = PROPERTY_ANGULAR_VIEW_GROUP2_SYMBOL_INDEX_DEFAULT;

        @Preference( key = PROPERTY_ANGULAR_VIEW_GROUP2_SYMBOL_SIZE_KEY,
                label = PROPERTY_ANGULAR_VIEW_GROUP2_SYMBOL_SIZE_LABEL,
                valueSet = {"1","2","3","4","5","6","7","8","9"},
                description = PROPERTY_ANGULAR_VIEW_GROUP2_SYMBOL_SIZE_TOOLTIP)
        String angularViewGroup2SymbolSizeDefault = PROPERTY_ANGULAR_VIEW_GROUP2_SYMBOL_SIZE_DEFAULT;
        
        @Preference( key = PROPERTY_ANGULAR_VIEW_GROUP2_COLOR_KEY,
                label = PROPERTY_ANGULAR_VIEW_GROUP2_COLOR_LABEL,
                description = PROPERTY_ANGULAR_VIEW_GROUP2_COLOR_TOOLTIP)
        Color angularViewGroup2ColorDefault = PROPERTY_ANGULAR_VIEW_GROUP2_COLOR_DEFAULT;






        @Preference(key = PROPERTY_ANGULAR_VIEW_GROUP3_SECTION_KEY,
                label = PROPERTY_ANGULAR_VIEW_GROUP3_SECTION_LABEL    ,
                description = PROPERTY_ANGULAR_VIEW_GROUP3_SECTION_TOOLTIP)
        boolean group3Section = true;

        @Preference( key = PROPERTY_ANGULAR_VIEW_GROUP3_MIN_WAVELENGTH_KEY,
                label = PROPERTY_ANGULAR_VIEW_GROUP3_MIN_WAVELENGTH_LABEL,
                interval = "[-1, 15000]",
                description = PROPERTY_ANGULAR_VIEW_GROUP3_MIN_WAVELENGTH_TOOLTIP)
        double angularViewGroup3MinWavelengthDefault = PROPERTY_ANGULAR_VIEW_GROUP3_MIN_WAVELENGTH_DEFAULT;

        @Preference( key = PROPERTY_ANGULAR_VIEW_GROUP3_MAX_WAVELENGTH_KEY,
                label = PROPERTY_ANGULAR_VIEW_GROUP3_MAX_WAVELENGTH_LABEL,
                interval = "[-1, 15000]",
                description = PROPERTY_ANGULAR_VIEW_GROUP3_MAX_WAVELENGTH_TOOLTIP)
        double angularViewGroup3MaxWavelengthDefault = PROPERTY_ANGULAR_VIEW_GROUP3_MAX_WAVELENGTH_DEFAULT;

        @Preference( key = PROPERTY_ANGULAR_VIEW_GROUP3_LINE_INDEX_KEY,
                label = PROPERTY_ANGULAR_VIEW_GROUP3_LINE_INDEX_LABEL,
                valueSet = {"0","1","2","3","4","5","6","7"},
                description = PROPERTY_ANGULAR_VIEW_GROUP3_LINE_INDEX_TOOLTIP)
        String angularViewGroup3LineIndexDefault = PROPERTY_ANGULAR_VIEW_GROUP3_LINE_INDEX_DEFAULT;

        @Preference( key = PROPERTY_ANGULAR_VIEW_GROUP3_SYMBOL_INDEX_KEY,
                label = PROPERTY_ANGULAR_VIEW_GROUP3_SYMBOL_INDEX_LABEL,
                valueSet = {"0","1","2","3","4","5","6","7","8","9","10"},
                description = PROPERTY_ANGULAR_VIEW_GROUP3_SYMBOL_INDEX_TOOLTIP)
        String angularViewGroup3SymbolIndexDefault = PROPERTY_ANGULAR_VIEW_GROUP3_SYMBOL_INDEX_DEFAULT;

        @Preference( key = PROPERTY_ANGULAR_VIEW_GROUP3_SYMBOL_SIZE_KEY,
                label = PROPERTY_ANGULAR_VIEW_GROUP3_SYMBOL_SIZE_LABEL,
                valueSet = {"1","2","3","4","5","6","7","8","9"},
                description = PROPERTY_ANGULAR_VIEW_GROUP3_SYMBOL_SIZE_TOOLTIP)
        String angularViewGroup3SymbolSizeDefault = PROPERTY_ANGULAR_VIEW_GROUP3_SYMBOL_SIZE_DEFAULT;

        @Preference( key = PROPERTY_ANGULAR_VIEW_GROUP3_COLOR_KEY,
                label = PROPERTY_ANGULAR_VIEW_GROUP3_COLOR_LABEL,
                description = PROPERTY_ANGULAR_VIEW_GROUP3_COLOR_TOOLTIP)
        Color angularViewGroup3ColorDefault = PROPERTY_ANGULAR_VIEW_GROUP3_COLOR_DEFAULT;





        @Preference(key = PROPERTY_ANGULAR_VIEW_GROUP4_SECTION_KEY,
                label = PROPERTY_ANGULAR_VIEW_GROUP4_SECTION_LABEL    ,
                description = PROPERTY_ANGULAR_VIEW_GROUP4_SECTION_TOOLTIP)
        boolean group4Section = true;

        @Preference( key = PROPERTY_ANGULAR_VIEW_GROUP4_MIN_WAVELENGTH_KEY,
                label = PROPERTY_ANGULAR_VIEW_GROUP4_MIN_WAVELENGTH_LABEL,
                interval = "[-1, 15000]",
                description = PROPERTY_ANGULAR_VIEW_GROUP4_MIN_WAVELENGTH_TOOLTIP)
        double angularViewGroup4MinWavelengthDefault = PROPERTY_ANGULAR_VIEW_GROUP4_MIN_WAVELENGTH_DEFAULT;

        @Preference( key = PROPERTY_ANGULAR_VIEW_GROUP4_MAX_WAVELENGTH_KEY,
                label = PROPERTY_ANGULAR_VIEW_GROUP4_MAX_WAVELENGTH_LABEL,
                interval = "[-1, 15000]",
                description = PROPERTY_ANGULAR_VIEW_GROUP4_MAX_WAVELENGTH_TOOLTIP)
        double angularViewGroup4MaxWavelengthDefault = PROPERTY_ANGULAR_VIEW_GROUP4_MAX_WAVELENGTH_DEFAULT;

        @Preference( key = PROPERTY_ANGULAR_VIEW_GROUP4_LINE_INDEX_KEY,
                label = PROPERTY_ANGULAR_VIEW_GROUP4_LINE_INDEX_LABEL,
                valueSet = {"0","1","2","3","4","5","6","7"},
                description = PROPERTY_ANGULAR_VIEW_GROUP4_LINE_INDEX_TOOLTIP)
        String angularViewGroup4LineIndexDefault = PROPERTY_ANGULAR_VIEW_GROUP4_LINE_INDEX_DEFAULT;

        @Preference( key = PROPERTY_ANGULAR_VIEW_GROUP4_SYMBOL_INDEX_KEY,
                label = PROPERTY_ANGULAR_VIEW_GROUP4_SYMBOL_INDEX_LABEL,
                valueSet = {"0","1","2","3","4","5","6","7","8","9","10"},
                description = PROPERTY_ANGULAR_VIEW_GROUP4_SYMBOL_INDEX_TOOLTIP)
        String angularViewGroup4SymbolIndexDefault = PROPERTY_ANGULAR_VIEW_GROUP4_SYMBOL_INDEX_DEFAULT;

        @Preference( key = PROPERTY_ANGULAR_VIEW_GROUP4_SYMBOL_SIZE_KEY,
                label = PROPERTY_ANGULAR_VIEW_GROUP4_SYMBOL_SIZE_LABEL,
                valueSet = {"1","2","3","4","5","6","7","8","9"},
                description = PROPERTY_ANGULAR_VIEW_GROUP4_SYMBOL_SIZE_TOOLTIP)
        String angularViewGroup4SymbolSizeDefault = PROPERTY_ANGULAR_VIEW_GROUP4_SYMBOL_SIZE_DEFAULT;
        
        @Preference( key = PROPERTY_ANGULAR_VIEW_GROUP4_COLOR_KEY,
                label = PROPERTY_ANGULAR_VIEW_GROUP4_COLOR_LABEL,
                description = PROPERTY_ANGULAR_VIEW_GROUP4_COLOR_TOOLTIP)
        Color angularViewGroup4ColorDefault = PROPERTY_ANGULAR_VIEW_GROUP4_COLOR_DEFAULT;




        @Preference(key = PROPERTY_ANGULAR_VIEW_GROUP5_SECTION_KEY,
                label = PROPERTY_ANGULAR_VIEW_GROUP5_SECTION_LABEL    ,
                description = PROPERTY_ANGULAR_VIEW_GROUP5_SECTION_TOOLTIP)
        boolean group5Section = true;

        @Preference( key = PROPERTY_ANGULAR_VIEW_GROUP5_MIN_WAVELENGTH_KEY,
                label = PROPERTY_ANGULAR_VIEW_GROUP5_MIN_WAVELENGTH_LABEL,
                interval = "[-1, 15000]",
                description = PROPERTY_ANGULAR_VIEW_GROUP5_MIN_WAVELENGTH_TOOLTIP)
        double angularViewGroup5MinWavelengthDefault = PROPERTY_ANGULAR_VIEW_GROUP5_MIN_WAVELENGTH_DEFAULT;

        @Preference( key = PROPERTY_ANGULAR_VIEW_GROUP5_MAX_WAVELENGTH_KEY,
                label = PROPERTY_ANGULAR_VIEW_GROUP5_MAX_WAVELENGTH_LABEL,
                interval = "[-1, 15000]",
                description = PROPERTY_ANGULAR_VIEW_GROUP5_MAX_WAVELENGTH_TOOLTIP)
        double angularViewGroup5MaxWavelengthDefault = PROPERTY_ANGULAR_VIEW_GROUP5_MAX_WAVELENGTH_DEFAULT;

        @Preference( key = PROPERTY_ANGULAR_VIEW_GROUP5_LINE_INDEX_KEY,
                label = PROPERTY_ANGULAR_VIEW_GROUP5_LINE_INDEX_LABEL,
                valueSet = {"0","1","2","3","4","5","6","7"},
                description = PROPERTY_ANGULAR_VIEW_GROUP5_LINE_INDEX_TOOLTIP)
        String angularViewGroup5LineIndexDefault = PROPERTY_ANGULAR_VIEW_GROUP5_LINE_INDEX_DEFAULT;

        @Preference( key = PROPERTY_ANGULAR_VIEW_GROUP5_SYMBOL_INDEX_KEY,
                label = PROPERTY_ANGULAR_VIEW_GROUP5_SYMBOL_INDEX_LABEL,
                valueSet = {"0","1","2","3","4","5","6","7","8","9","10"},
                description = PROPERTY_ANGULAR_VIEW_GROUP5_SYMBOL_INDEX_TOOLTIP)
        String angularViewGroup5SymbolIndexDefault = PROPERTY_ANGULAR_VIEW_GROUP5_SYMBOL_INDEX_DEFAULT;

        @Preference( key = PROPERTY_ANGULAR_VIEW_GROUP5_SYMBOL_SIZE_KEY,
                label = PROPERTY_ANGULAR_VIEW_GROUP5_SYMBOL_SIZE_LABEL,
                valueSet = {"1","2","3","4","5","6","7","8","9"},
                description = PROPERTY_ANGULAR_VIEW_GROUP5_SYMBOL_SIZE_TOOLTIP)
        String angularViewGroup5SymbolSizeDefault = PROPERTY_ANGULAR_VIEW_GROUP5_SYMBOL_SIZE_DEFAULT;

        @Preference( key = PROPERTY_ANGULAR_VIEW_GROUP5_COLOR_KEY,
                label = PROPERTY_ANGULAR_VIEW_GROUP5_COLOR_LABEL,
                description = PROPERTY_ANGULAR_VIEW_GROUP5_COLOR_TOOLTIP)
        Color angularViewGroup5ColorDefault = PROPERTY_ANGULAR_VIEW_GROUP5_COLOR_DEFAULT;
        

        
        // Restore Defaults

        @Preference(label = PROPERTY_RESTORE_SECTION_LABEL,
                key = PROPERTY_RESTORE_SECTION_KEY,
                description = PROPERTY_RESTORE_SECTION_TOOLTIP)
        boolean restoreDefaultsSection = true;

        @Preference(label = PROPERTY_RESTORE_DEFAULTS_LABEL,
                key = PROPERTY_RESTORE_DEFAULTS_NAME,
                description = PROPERTY_RESTORE_DEFAULTS_TOOLTIP)
        boolean restoreDefaults = PROPERTY_RESTORE_DEFAULTS_DEFAULT;
    }



    public static boolean getPreferenceXaxisSetBounds() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyBool(PROPERTY_ANGULAR_VIEW_XAXIS_SET_BOUNDS_KEY, PROPERTY_ANGULAR_VIEW_XAXIS_SET_BOUNDS_DEFAULT);
    }

    public static String getPreferenceXaxisMin() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(PROPERTY_ANGULAR_VIEW_XAXIS_MIN_KEY, PROPERTY_ANGULAR_VIEW_XAXIS_MIN_DEFAULT);
    }

    public static String getPreferenceXaxisMax() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(PROPERTY_ANGULAR_VIEW_XAXIS_MAX_KEY, PROPERTY_ANGULAR_VIEW_XAXIS_MAX_DEFAULT);
    }


    public static boolean getPreferenceYaxisSetBounds() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyBool(PROPERTY_ANGULAR_VIEW_YAXIS_SET_BOUNDS_KEY, PROPERTY_ANGULAR_VIEW_YAXIS_SET_BOUNDS_DEFAULT);
    }

    public static String getPreferenceYaxisMin() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(PROPERTY_ANGULAR_VIEW_YAXIS_MIN_KEY, PROPERTY_ANGULAR_VIEW_YAXIS_MIN_DEFAULT);
    }

    public static String getPreferenceYaxisMax() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(PROPERTY_ANGULAR_VIEW_YAXIS_MAX_KEY, PROPERTY_ANGULAR_VIEW_YAXIS_MAX_DEFAULT);
    }


    public static String getPreferenceTitle() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(PROPERTY_ANGULAR_VIEW_TITLE_KEY, PROPERTY_ANGULAR_VIEW_TITLE_DEFAULT);
    }



//    public static Color getPreferenceForegroundColor() {
//        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
//        return preferences.getPropertyColor(PROPERTY_ANGULAR_VIEW_FOREGROUND_COLOR_KEY, PROPERTY_ANGULAR_VIEW_FOREGROUND_COLOR_DEFAULT);
//    }

    public static Color getPreferenceBackgroundColor() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyColor(PROPERTY_ANGULAR_VIEW_BACKGROUND_COLOR_KEY, PROPERTY_ANGULAR_VIEW_BACKGROUND_COLOR_DEFAULT);
    }

//    public static Color getPreferenceMarginBackgroundColor() {
//        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
//        return preferences.getPropertyColor(PROPERTY_ANGULAR_VIEW_MARGIN_BACKGROUND_COLOR_KEY, PROPERTY_ANGULAR_VIEW_MARGIN_BACKGROUND_COLOR_DEFAULT);
//    }
//
//    public static Color getPreferenceLegendBackgroundColor() {
//        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
//        return preferences.getPropertyColor(PROPERTY_ANGULAR_VIEW_LEGEND_BACKGROUND_COLOR_KEY, PROPERTY_ANGULAR_VIEW_LEGEND_BACKGROUND_COLOR_DEFAULT);
//    }





    public static int getPreferenceLineIndex() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        String stringValue = preferences.getPropertyString(PROPERTY_ANGULAR_VIEW_LINE_INDEX_KEY, PROPERTY_ANGULAR_VIEW_LINE_INDEX_DEFAULT);
        return convertStringToInt(stringValue, PROPERTY_ANGULAR_VIEW_LINE_INDEX_DEFAULT_INT);
    }

    public static int getPreferenceSymbolIndex() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        String stringValue =  preferences.getPropertyString(PROPERTY_ANGULAR_VIEW_SYMBOL_INDEX_KEY, PROPERTY_ANGULAR_VIEW_SYMBOL_INDEX_DEFAULT);
        return convertStringToInt(stringValue, PROPERTY_ANGULAR_VIEW_SYMBOL_INDEX_DEFAULT_INT);
    }

    public static int getPreferenceSymbolSize() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        String stringValue =  preferences.getPropertyString(PROPERTY_ANGULAR_VIEW_SYMBOL_SIZE_KEY, PROPERTY_ANGULAR_VIEW_SYMBOL_SIZE_DEFAULT);
        return convertStringToInt(stringValue, PROPERTY_ANGULAR_VIEW_SYMBOL_SIZE_DEFAULT_INT);
    }

    public static Color getPreferencePlotColor() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyColor(PROPERTY_ANGULAR_VIEW_PLOT_COLOR_KEY, PROPERTY_ANGULAR_VIEW_PLOT_COLOR_DEFAULT);
    }
    
    
    


    public static double getPreferenceGroup1MinWavelength() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyDouble(PROPERTY_ANGULAR_VIEW_GROUP1_MIN_WAVELENGTH_KEY, PROPERTY_ANGULAR_VIEW_GROUP1_MIN_WAVELENGTH_DEFAULT);
    }

    public static double getPreferenceGroup1MaxWavelength() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyDouble(PROPERTY_ANGULAR_VIEW_GROUP1_MAX_WAVELENGTH_KEY, PROPERTY_ANGULAR_VIEW_GROUP1_MAX_WAVELENGTH_DEFAULT);
    }

    public static int getPreferenceGroup1LineIndex() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        String stringValue = preferences.getPropertyString(PROPERTY_ANGULAR_VIEW_GROUP1_LINE_INDEX_KEY, PROPERTY_ANGULAR_VIEW_GROUP1_LINE_INDEX_DEFAULT);
        return convertStringToInt(stringValue, PROPERTY_ANGULAR_VIEW_GROUP1_LINE_INDEX_DEFAULT_INT);
    }

    public static int getPreferenceGroup1SymbolIndex() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        String stringValue =  preferences.getPropertyString(PROPERTY_ANGULAR_VIEW_GROUP1_SYMBOL_INDEX_KEY, PROPERTY_ANGULAR_VIEW_GROUP1_SYMBOL_INDEX_DEFAULT);
        return convertStringToInt(stringValue, PROPERTY_ANGULAR_VIEW_GROUP1_SYMBOL_INDEX_DEFAULT_INT);
    }

    public static int getPreferenceGroup1SymbolSize() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        String stringValue =  preferences.getPropertyString(PROPERTY_ANGULAR_VIEW_GROUP1_SYMBOL_SIZE_KEY, PROPERTY_ANGULAR_VIEW_GROUP1_SYMBOL_SIZE_DEFAULT);
        return convertStringToInt(stringValue, PROPERTY_ANGULAR_VIEW_GROUP1_SYMBOL_SIZE_DEFAULT_INT);
    }

    public static Color getPreferenceGroup1Color() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyColor(PROPERTY_ANGULAR_VIEW_GROUP1_COLOR_KEY, PROPERTY_ANGULAR_VIEW_GROUP1_COLOR_DEFAULT);
    }








    public static double getPreferenceGroup2MinWavelength() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyDouble(PROPERTY_ANGULAR_VIEW_GROUP2_MIN_WAVELENGTH_KEY, PROPERTY_ANGULAR_VIEW_GROUP2_MIN_WAVELENGTH_DEFAULT);
    }

    public static double getPreferenceGroup2MaxWavelength() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyDouble(PROPERTY_ANGULAR_VIEW_GROUP2_MAX_WAVELENGTH_KEY, PROPERTY_ANGULAR_VIEW_GROUP2_MAX_WAVELENGTH_DEFAULT);
    }

    public static int getPreferenceGroup2LineIndex() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        String stringValue = preferences.getPropertyString(PROPERTY_ANGULAR_VIEW_GROUP2_LINE_INDEX_KEY, PROPERTY_ANGULAR_VIEW_GROUP2_LINE_INDEX_DEFAULT);
        return convertStringToInt(stringValue, PROPERTY_ANGULAR_VIEW_GROUP2_LINE_INDEX_DEFAULT_INT);
    }

    public static int getPreferenceGroup2SymbolIndex() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        String stringValue =  preferences.getPropertyString(PROPERTY_ANGULAR_VIEW_GROUP2_SYMBOL_INDEX_KEY, PROPERTY_ANGULAR_VIEW_GROUP2_SYMBOL_INDEX_DEFAULT);
        return convertStringToInt(stringValue, PROPERTY_ANGULAR_VIEW_GROUP2_SYMBOL_INDEX_DEFAULT_INT);
    }

    public static int getPreferenceGroup2SymbolSize() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        String stringValue =  preferences.getPropertyString(PROPERTY_ANGULAR_VIEW_GROUP2_SYMBOL_SIZE_KEY, PROPERTY_ANGULAR_VIEW_GROUP2_SYMBOL_SIZE_DEFAULT);
        return convertStringToInt(stringValue, PROPERTY_ANGULAR_VIEW_GROUP2_SYMBOL_SIZE_DEFAULT_INT);
    }

    public static Color getPreferenceGroup2Color() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyColor(PROPERTY_ANGULAR_VIEW_GROUP2_COLOR_KEY, PROPERTY_ANGULAR_VIEW_GROUP2_COLOR_DEFAULT);
    }







    public static double getPreferenceGroup3MinWavelength() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyDouble(PROPERTY_ANGULAR_VIEW_GROUP3_MIN_WAVELENGTH_KEY, PROPERTY_ANGULAR_VIEW_GROUP3_MIN_WAVELENGTH_DEFAULT);
    }

    public static double getPreferenceGroup3MaxWavelength() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyDouble(PROPERTY_ANGULAR_VIEW_GROUP3_MAX_WAVELENGTH_KEY, PROPERTY_ANGULAR_VIEW_GROUP3_MAX_WAVELENGTH_DEFAULT);
    }

    public static int getPreferenceGroup3LineIndex() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        String stringValue = preferences.getPropertyString(PROPERTY_ANGULAR_VIEW_GROUP3_LINE_INDEX_KEY, PROPERTY_ANGULAR_VIEW_GROUP3_LINE_INDEX_DEFAULT);
        return convertStringToInt(stringValue, PROPERTY_ANGULAR_VIEW_GROUP3_LINE_INDEX_DEFAULT_INT);
    }

    public static int getPreferenceGroup3SymbolIndex() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        String stringValue =  preferences.getPropertyString(PROPERTY_ANGULAR_VIEW_GROUP3_SYMBOL_INDEX_KEY, PROPERTY_ANGULAR_VIEW_GROUP3_SYMBOL_INDEX_DEFAULT);
        return convertStringToInt(stringValue, PROPERTY_ANGULAR_VIEW_GROUP3_SYMBOL_INDEX_DEFAULT_INT);
    }

    public static int getPreferenceGroup3SymbolSize() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        String stringValue =  preferences.getPropertyString(PROPERTY_ANGULAR_VIEW_GROUP3_SYMBOL_SIZE_KEY, PROPERTY_ANGULAR_VIEW_GROUP3_SYMBOL_SIZE_DEFAULT);
        return convertStringToInt(stringValue, PROPERTY_ANGULAR_VIEW_GROUP3_SYMBOL_SIZE_DEFAULT_INT);
    }

    public static Color getPreferenceGroup3Color() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyColor(PROPERTY_ANGULAR_VIEW_GROUP3_COLOR_KEY, PROPERTY_ANGULAR_VIEW_GROUP3_COLOR_DEFAULT);
    }





    
    

    public static double getPreferenceGroup4MinWavelength() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyDouble(PROPERTY_ANGULAR_VIEW_GROUP4_MIN_WAVELENGTH_KEY, PROPERTY_ANGULAR_VIEW_GROUP4_MIN_WAVELENGTH_DEFAULT);
    }

    public static double getPreferenceGroup4MaxWavelength() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyDouble(PROPERTY_ANGULAR_VIEW_GROUP4_MAX_WAVELENGTH_KEY, PROPERTY_ANGULAR_VIEW_GROUP4_MAX_WAVELENGTH_DEFAULT);
    }

    public static int getPreferenceGroup4LineIndex() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        String stringValue = preferences.getPropertyString(PROPERTY_ANGULAR_VIEW_GROUP4_LINE_INDEX_KEY, PROPERTY_ANGULAR_VIEW_GROUP4_LINE_INDEX_DEFAULT);
        return convertStringToInt(stringValue, PROPERTY_ANGULAR_VIEW_GROUP4_LINE_INDEX_DEFAULT_INT);
    }

    public static int getPreferenceGroup4SymbolIndex() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        String stringValue =  preferences.getPropertyString(PROPERTY_ANGULAR_VIEW_GROUP4_SYMBOL_INDEX_KEY, PROPERTY_ANGULAR_VIEW_GROUP4_SYMBOL_INDEX_DEFAULT);
        return convertStringToInt(stringValue, PROPERTY_ANGULAR_VIEW_GROUP4_SYMBOL_INDEX_DEFAULT_INT);
    }

    public static int getPreferenceGroup4SymbolSize() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        String stringValue =  preferences.getPropertyString(PROPERTY_ANGULAR_VIEW_GROUP4_SYMBOL_SIZE_KEY, PROPERTY_ANGULAR_VIEW_GROUP4_SYMBOL_SIZE_DEFAULT);
        return convertStringToInt(stringValue, PROPERTY_ANGULAR_VIEW_GROUP4_SYMBOL_SIZE_DEFAULT_INT);
    }

    public static Color getPreferenceGroup4Color() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyColor(PROPERTY_ANGULAR_VIEW_GROUP4_COLOR_KEY, PROPERTY_ANGULAR_VIEW_GROUP4_COLOR_DEFAULT);
    }











    public static double getPreferenceGroup5MinWavelength() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyDouble(PROPERTY_ANGULAR_VIEW_GROUP5_MIN_WAVELENGTH_KEY, PROPERTY_ANGULAR_VIEW_GROUP5_MIN_WAVELENGTH_DEFAULT);
    }

    public static double getPreferenceGroup5MaxWavelength() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyDouble(PROPERTY_ANGULAR_VIEW_GROUP5_MAX_WAVELENGTH_KEY, PROPERTY_ANGULAR_VIEW_GROUP5_MAX_WAVELENGTH_DEFAULT);
    }

    public static int getPreferenceGroup5LineIndex() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        String stringValue = preferences.getPropertyString(PROPERTY_ANGULAR_VIEW_GROUP5_LINE_INDEX_KEY, PROPERTY_ANGULAR_VIEW_GROUP5_LINE_INDEX_DEFAULT);
        return convertStringToInt(stringValue, PROPERTY_ANGULAR_VIEW_GROUP5_LINE_INDEX_DEFAULT_INT);
    }

    public static int getPreferenceGroup5SymbolIndex() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        String stringValue =  preferences.getPropertyString(PROPERTY_ANGULAR_VIEW_GROUP5_SYMBOL_INDEX_KEY, PROPERTY_ANGULAR_VIEW_GROUP5_SYMBOL_INDEX_DEFAULT);
        return convertStringToInt(stringValue, PROPERTY_ANGULAR_VIEW_GROUP5_SYMBOL_INDEX_DEFAULT_INT);
    }

    public static int getPreferenceGroup5SymbolSize() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        String stringValue =  preferences.getPropertyString(PROPERTY_ANGULAR_VIEW_GROUP5_SYMBOL_SIZE_KEY, PROPERTY_ANGULAR_VIEW_GROUP5_SYMBOL_SIZE_DEFAULT);
        return convertStringToInt(stringValue, PROPERTY_ANGULAR_VIEW_GROUP5_SYMBOL_SIZE_DEFAULT_INT);
    }

    public static Color getPreferenceGroup5Color() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyColor(PROPERTY_ANGULAR_VIEW_GROUP5_COLOR_KEY, PROPERTY_ANGULAR_VIEW_GROUP5_COLOR_DEFAULT);
    }






    public static Color getPreferenceGridlinesColor() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyColor(PROPERTY_ANGULAR_VIEW_GRIDLINE_COLOR_KEY, PROPERTY_ANGULAR_VIEW_GRIDLINE_COLOR_DEFAULT);
    }

    public static boolean getPreferenceGridlinesShow() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyBool(PROPERTY_ANGULAR_VIEW_GRIDLINE_SHOW_KEY, PROPERTY_ANGULAR_VIEW_GRIDLINE_SHOW_DEFAULT);
    }

    private static int convertStringToInt(String stringValue, int defaultIntValue) {
        int intValue;
        try {
            intValue = Integer.valueOf(stringValue);
        } catch (Exception e) {
            intValue = defaultIntValue;
        }

        return intValue;
    }

}
