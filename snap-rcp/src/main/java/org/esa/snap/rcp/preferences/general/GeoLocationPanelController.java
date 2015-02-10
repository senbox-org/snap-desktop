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
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.PropertyEditorRegistry;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.snap.rcp.preferences.DefaultConfigController;
import org.esa.snap.rcp.preferences.Preference;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;

import static com.bc.ceres.swing.TableLayout.cell;

/**
 * The preferences panel handling geo-location details. Sub-level panel to the "Miscellaneous"-panel.
 *
 * @author thomas
 */
@OptionsPanelController.SubRegistration(location = "Advanced",
        displayName = "#Options_DisplayName_GeoLocation",
        keywords = "#Options_Keywords_GeoLocation",
        keywordsCategory = "Geo-Location",
        id = "GeoLocation")
@org.openide.util.NbBundle.Messages({
        "Options_DisplayName_GeoLocation=Geo-Location",
        "Options_Keywords_GeoLocation=geo, location, geo-location, compatibility, differ"
})
public final class GeoLocationPanelController extends DefaultConfigController {

    /**
     * Preferences key for pixel offset-X for display pixel positions
     */
    public static final String PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_X = "pixel.offset.display.x";
    /**
     * Preferences key for pixel offset-Y for display pixel positions
     */
    public static final String PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_Y = "pixel.offset.display.y";
    /**
     * Preferences key for showing floating-point image coordinates
     */
    // todo - check if property key really matches purpose of key
    public static final String PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_SHOW_DECIMALS = "pixel.offset.display.show.decimals";
    /**
     * Preferences key for display style of geo-locations
     */
    public static final String PROPERTY_KEY_DISPLAY_GEOLOCATION_AS_DECIMAL = "geolocation.display.decimal";

    /**
     * Default geo-location epsilon
     */
    public static final float DEFAULT_GEOLOCATION_EPS = 1.0e-4F;
    /**
     * Default value for pixel offset's for display pixel positions
     */
    public static final float PROPERTY_DEFAULT_PIXEL_OFFSET_FOR_DISPLAY = 0.5f;
    /**
     * Default value for pixel offset's for display pixel positions
     */
    public static final boolean PROPERTY_DEFAULT_PIXEL_OFFSET_FOR_DISPLAY_SHOW_DECIMALS = false;
    /**
     * Default value for display style of geo-locations.
     */
    public static final boolean PROPERTY_DEFAULT_DISPLAY_GEOLOCATION_AS_DECIMAL = false;

    protected PropertyContainer createPropertyContainer() {
        return createPropertyContainer(new GeoLocationBean());
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("geo-location");
    }

    @Override
    protected JPanel createPanel(BindingContext context) {
        JComponent visualizer = createOffsetVisualizer(context);
        visualizer.setPreferredSize(new Dimension(60, 60));
        visualizer.setOpaque(true);
        visualizer.setBorder(BorderFactory.createLoweredBevelBorder());

        final TableLayout tableLayout = new TableLayout(3);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);

        final PropertyEditorRegistry registry = PropertyEditorRegistry.getInstance();
        Property paramOffsetX = context.getPropertySet().getProperty(PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_X);
        Property paramOffsetY = context.getPropertySet().getProperty(PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_Y);
        Property paramShowDecimals = context.getPropertySet().getProperty(PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_SHOW_DECIMALS);
        Property paramGeolocationAsDecimal = context.getPropertySet().getProperty(PROPERTY_KEY_DISPLAY_GEOLOCATION_AS_DECIMAL);
        PropertyChangeListener listener = evt -> visualizer.repaint();
        paramOffsetX.addPropertyChangeListener(listener);
        paramOffsetY.addPropertyChangeListener(listener);

        JComponent[] xComponents = registry.findPropertyEditor(paramOffsetX.getDescriptor()).createComponents(paramOffsetX.getDescriptor(), context);
        JComponent[] yComponents = registry.findPropertyEditor(paramOffsetY.getDescriptor()).createComponents(paramOffsetY.getDescriptor(), context);
        JComponent[] showDecimalComponents = registry.findPropertyEditor(paramShowDecimals.getDescriptor()).createComponents(paramShowDecimals.getDescriptor(), context);
        JComponent[] geolocationAsDecimalComponents = registry.findPropertyEditor(paramGeolocationAsDecimal.getDescriptor()).createComponents(paramGeolocationAsDecimal.getDescriptor(), context);

        final JPanel pageUI = new JPanel(tableLayout);

        pageUI.add(xComponents[1]);
        tableLayout.setCellWeightX(0, 1, 1.0);
        pageUI.add(xComponents[0]);

        tableLayout.setCellRowspan(0, 2, 2);
        tableLayout.setCellWeightX(0, 2, 1.0);
        tableLayout.setCellAnchor(0, 2, TableLayout.Anchor.CENTER);
        tableLayout.setCellFill(0, 2, TableLayout.Fill.NONE);
        pageUI.add(visualizer);

        pageUI.add(yComponents[1]);
        tableLayout.setCellWeightX(1, 1, 1.0);
        pageUI.add(yComponents[0]);

        tableLayout.setRowPadding(1, new Insets(10, 0, 4, 4));
        pageUI.add(showDecimalComponents[0], cell(3, 0, 1, 3));
        tableLayout.setRowPadding(2, new Insets(10, 0, 4, 4));
        pageUI.add(geolocationAsDecimalComponents[0], cell(4, 0, 1, 3));

        return createPageUIContentPane(pageUI);
    }

    private JComponent createOffsetVisualizer(BindingContext context) {
        return new JPanel() {

            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                int totWidth = getWidth();
                int totHeight = getHeight();

                if (totWidth == 0 || totHeight == 0) {
                    return;
                }
                if (!(g instanceof Graphics2D)) {
                    return;
                }

                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setStroke(new BasicStroke(2));
                int borderSize = 10;
                int maxPixelWidth = totWidth - 2 * borderSize;
                int maxPixelHeight = totHeight - 2 * borderSize;
                int pixelSize = Math.min(maxPixelHeight, maxPixelWidth);
                Rectangle pixel = new Rectangle((totWidth - pixelSize) / 2, (totHeight - pixelSize) / 2, pixelSize, pixelSize);
                g2d.setColor(Color.blue);
                g2d.drawRect(pixel.x, pixel.y, pixel.width, pixel.height);
                Property paramOffsetX = context.getPropertySet().getProperty(PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_X);
                Property paramOffsetY = context.getPropertySet().getProperty(PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_Y);
                float offsetX = paramOffsetX.getValue();

                float offsetY = paramOffsetY.getValue();
                int posX = Math.round(pixelSize * offsetX + pixel.x);
                int posY = Math.round(pixelSize * offsetY + pixel.y);
                drawPos(g2d, posX, posY);
            }

            private void drawPos(Graphics2D g2d, final int posX, final int posY) {
                g2d.setColor(Color.yellow);
                final int crossLength = 8;
                g2d.drawLine(posX - crossLength, posY, posX + crossLength, posY);
                g2d.drawLine(posX, posY - crossLength, posX, posY + crossLength);
                g2d.setColor(Color.red);

                final int diameter = 3;
                g2d.fillOval(posX - diameter / 2, posY - diameter / 2, diameter, diameter);
            }
        };
    }

    private static JPanel createPageUIContentPane(JPanel pane) {
        JPanel contentPane = GridBagUtils.createPanel();
        final GridBagConstraints gbc = GridBagUtils.createConstraints("fill=HORIZONTAL,anchor=NORTHWEST");
        gbc.insets.top = 15;
        gbc.weightx = 1;
        gbc.weighty = 0;
        contentPane.add(pane, gbc);
        GridBagUtils.addVerticalFiller(contentPane, gbc);
        return contentPane;
    }

    static class GeoLocationBean {

        @Preference(label = "Relative pixel-X offset", key = PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_X, interval = "[0,1]")
        float paramOffsetX = PROPERTY_DEFAULT_PIXEL_OFFSET_FOR_DISPLAY;

        @Preference(label = "Relative pixel-Y offset", key = PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_Y, interval = "[0,1]")
        float paramOffsetY = PROPERTY_DEFAULT_PIXEL_OFFSET_FOR_DISPLAY;

        @Preference(label = "Show floating-point image coordinates", key = PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_SHOW_DECIMALS)
        boolean paramShowDecimals = PROPERTY_DEFAULT_PIXEL_OFFSET_FOR_DISPLAY_SHOW_DECIMALS;

        @Preference(label = "Show geo-location coordinates in decimal degrees", key = PROPERTY_KEY_DISPLAY_GEOLOCATION_AS_DECIMAL)
        boolean paramGeolocationAsDecimal = PROPERTY_DEFAULT_DISPLAY_GEOLOCATION_AS_DECIMAL;
    }

}
