package org.esa.snap.rcp.colormanip;

import org.esa.snap.core.datamodel.ColorPaletteDef;
import org.esa.snap.core.datamodel.ImageInfo;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.util.PropertyMap;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.core.util.math.Range;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.product.ProductSceneView;

import javax.swing.*;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.List;
import java.util.Vector;

import static org.esa.snap.core.datamodel.ColorManipulationDefaults.*;


/**
 * This class creates the color palette chooser JComboBox contained within the color manipulation tool.
 *
 * @author Brockmann Consult
 * @author Daniel Knowles (NASA)
 * @version $Revision$ $Date$
 */
// DEC 2019 - Knowles
//          - Fixed bug where log scaled color palette would appear crunched in the selector
//          - Added tooltips to show which color palette is being hovered over and selected
//          - Added blue highlights within the renderer to show which color palette is being hovered over selected


class ColorPaletteChooser extends JComboBox<ColorPaletteChooser.ColorPaletteWrapper> {

    private final String DERIVED_FROM = "derived from";
    private final String UNNAMED = "unnamed";
    private boolean discreteDisplay;
    private boolean log10Display;

    public ColorPaletteChooser() {
        super(getPalettes());
        setRenderer(createPaletteRenderer());
        setEditable(false);
    }

    public void removeUserDefinedPalette() {
        if (getItemCount() > 0) {
            final String name = getItemAt(0).name;
            if (UNNAMED.equals(name) || name.startsWith(DERIVED_FROM)) {
                removeItemAt(0);
            }
        }
    }

    public ColorPaletteDef getSelectedColorPaletteDefinition() {
        final int selectedIndex = getSelectedIndex();
        final ComboBoxModel<ColorPaletteWrapper> model = getModel();
        final ColorPaletteWrapper colorPaletteWrapper = model.getElementAt(selectedIndex);
        final ColorPaletteDef cpd = colorPaletteWrapper.cpd;
        cpd.getFirstPoint().setLabel(colorPaletteWrapper.name);
        return cpd;
    }

    public void setSelectedColorPaletteDefinition(ColorPaletteDef cpd) {
        removeUserDefinedPalette();
        final ComboBoxModel<ColorPaletteWrapper> model = getModel();
        for (int i = 0; i < model.getSize(); i++) {
            if (model.getElementAt(i).cpd.equals(cpd)) {
                setSelectedIndex(i);
                return;
            }
        }
        setUserDefinedPalette(cpd);
    }

    public void reloadPalettes() {
        setModel(new DefaultComboBoxModel<>(getPalettes()));
        repaint();
    }

    private void setUserDefinedPalette(ColorPaletteDef userPalette) {
        final String suffix = userPalette.getFirstPoint().getLabel();
        final String name;
        if (suffix != null && suffix.trim().length() > 0) {
            name = DERIVED_FROM + " " + suffix.trim();
        } else {
            name = UNNAMED;
        }
        final ColorPaletteWrapper item = new ColorPaletteWrapper(name, userPalette);
        insertItemAt(item, 0);
        setSelectedIndex(0);
    }

    private static Vector<ColorPaletteWrapper> getPalettes() {
        final ProductSceneView view = SnapApp.getDefault().getSelectedProductSceneView();
        PropertyMap configuration = view.getSceneImage().getConfiguration();

        String paletteSelectorSplit = configuration.getPropertyString(PROPERTY_PALETTE_CATEGORIZE_DISPLAY_KEY, PROPERTY_PALETTE_CATEGORIZE_DISPLAY_DEFAULT);

        final List<ColorPaletteDef> defList = ColorPaletteManager.getDefault().getColorPaletteDefList();
        final Vector<ColorPaletteWrapper> paletteWrappers = new Vector<>();
        final ColorPaletteDef paletteDummy = new ColorPaletteDef(new ColorPaletteDef.Point[]{
                new ColorPaletteDef.Point(.001, Color.WHITE),
                new ColorPaletteDef.Point(1.0, Color.WHITE)
        }); //cpd that goes with "-- Primary Palettes --" and "-- Additional Palettes --"

        if (paletteSelectorSplit.contains(PROPERTY_PALETTE_CATEGORIZE_DISPLAY_OPTION3)) {   //Universal, Standard and Additional Palettes
            paletteWrappers.add(new ColorPaletteWrapper("-- Universal Palettes --", paletteDummy));

            for (ColorPaletteDef colorPaletteDef : defList) {
                if (colorPaletteDef.isUniversal()) {
                    final String nameFor = getNameForWithoutExtension(colorPaletteDef);
                    paletteWrappers.add(new ColorPaletteWrapper(nameFor, colorPaletteDef));
                }
            }

            paletteWrappers.add(new ColorPaletteWrapper("-- Standard Palettes --", paletteDummy));

            for (ColorPaletteDef colorPaletteDef : defList) {
                final String nameFor = getNameForWithoutExtension(colorPaletteDef);
                if (!colorPaletteDef.isUniversal() && colorPaletteDef.isStandard()) {
                    paletteWrappers.add(new ColorPaletteWrapper(nameFor, colorPaletteDef));
                }
            }

            paletteWrappers.add(new ColorPaletteWrapper("-- Additional Palettes --", paletteDummy));

            for (ColorPaletteDef colorPaletteDef : defList) {
                final String nameFor = getNameForWithoutExtension(colorPaletteDef);
                if (!colorPaletteDef.isUniversal() && !colorPaletteDef.isStandard()) {
                    paletteWrappers.add(new ColorPaletteWrapper(nameFor, colorPaletteDef));
                }
            }
        } else if (paletteSelectorSplit.contains(PROPERTY_PALETTE_CATEGORIZE_DISPLAY_OPTION2)){   // Primary and Additional Palettes
            paletteWrappers.add(new ColorPaletteWrapper("-- Primary Palettes --", paletteDummy));

            for (ColorPaletteDef colorPaletteDef : defList) {
                final String nameFor = getNameForWithoutExtension(colorPaletteDef);
                if (colorPaletteDef.isPrimary()) {
                    paletteWrappers.add(new ColorPaletteWrapper(nameFor, colorPaletteDef));
                }
            }

            paletteWrappers.add(new ColorPaletteWrapper("-- Additional Palettes --", paletteDummy));

            for (ColorPaletteDef colorPaletteDef : defList) {
                final String nameFor = getNameForWithoutExtension(colorPaletteDef);
                if (!colorPaletteDef.isPrimary()) {
                    paletteWrappers.add(new ColorPaletteWrapper(nameFor, colorPaletteDef));
                }
            }
        } else {
            for (ColorPaletteDef colorPaletteDef : defList) {
                final String nameFor = getNameForWithoutExtension(colorPaletteDef);
                paletteWrappers.add(new ColorPaletteWrapper(nameFor, colorPaletteDef));
            }
        }
        return paletteWrappers;
    }

    private static String getNameForWithoutExtension(ColorPaletteDef colorPaletteDef) {
        final String nameFor = ColorPaletteManager.getDefault().getNameFor(colorPaletteDef);
        if (nameFor.toLowerCase().endsWith(".cpd")) {
            return nameFor.substring(0, nameFor.length() - 4);
        } else {
            return nameFor;
        }
    }

    private ListCellRenderer<ColorPaletteWrapper> createPaletteRenderer() {

        final ProductSceneView view = SnapApp.getDefault().getSelectedProductSceneView();
        PropertyMap configuration = view.getSceneImage().getConfiguration();

        boolean includePaletteImage = configuration.getPropertyBool(PROPERTY_PALETTE_INCLUDE_IMAGE_KEY, PROPERTY_PALETTE_INCLUDE_IMAGE_DEFAULT);

        return new ListCellRenderer<ColorPaletteWrapper>() {
            @Override
            public Component getListCellRendererComponent(JList<? extends ColorPaletteWrapper> list, ColorPaletteWrapper value, int index, boolean isSelected, boolean cellHasFocus) {
//                final Font font = getFont();
//                final Font smaller = font.deriveFont(font.getSize() * 0.85f);

                final JLabel nameComp = new JLabel(value.name);
//                nameComp.setFont(smaller);

                final ColorPaletteDef cpd = value.cpd;
                final JLabel rampComp = new JLabel(" ") {
                    @Override
                    public void paint(Graphics g) {
                        super.paint(g);
                        drawPalette((Graphics2D) g, cpd, g.getClipBounds().getSize(), index, isSelected);
                    }
                };

                final JPanel palettePanel = new JPanel(new BorderLayout(0, 2));
                if (value.name.contains("-- Primary Palettes --")
                        || value.name.contains("-- Standard Palettes --")
                        || value.name.contains("-- Universal Palettes --")
                        || value.name.contains("-- Additional Palettes --")) {
                    nameComp.setBackground(Color.LIGHT_GRAY);
                    nameComp.setOpaque(true);
                }
                if (includePaletteImage) {
                    final Font font = getFont();
                    final Font smaller = font.deriveFont(font.getSize() * 0.85f);
                    nameComp.setFont(smaller);
                }
                palettePanel.add(nameComp, BorderLayout.NORTH);
                if (includePaletteImage) {
                    if (!(value.name.contains("-- Primary Palettes --")
                            || value.name.contains("-- Standard Palettes --")
                            || value.name.contains("-- Universal Palettes --")
                            || value.name.contains("-- Additional Palettes --"))) {
                        palettePanel.add(rampComp, BorderLayout.CENTER);
                    }
                } else {
                    if (value.name.contains("derived from")) {
                        palettePanel.add(rampComp, BorderLayout.CENTER);
                    }
                }

                if (isSelected) {
                    list.setToolTipText(value.name);
                }

                return palettePanel;
            }
        };
    }

    private void drawPalette(Graphics2D g2, ColorPaletteDef colorPaletteDef, Dimension paletteDim, int index, boolean isSelected) {
        final int width = paletteDim.width;
        final int height = paletteDim.height;

        final ColorPaletteDef cpdCopy = colorPaletteDef.createDeepCopy();
        cpdCopy.setDiscrete(discreteDisplay);
        cpdCopy.setNumColors(width);
        final ImageInfo imageInfo = new ImageInfo(cpdCopy);
        imageInfo.setLogScaled(log10Display);
        imageInfo.setLogScaled(colorPaletteDef.isLogScaled());


        Color[] colorPalette = ImageManager.createColorPalette(imageInfo);

        g2.setStroke(new BasicStroke(1.0f));

        for (int x = 0; x < width; x++) {
            if (isSelected && index != 0) {
                int edgeThickness = 1;
                g2.setColor(colorPalette[x]);
                g2.drawLine(x, (edgeThickness + 1), x, height - (edgeThickness + 1));
                g2.setColor(Color.blue);
                g2.drawLine(x, 0, x, edgeThickness);
                g2.drawLine(x, height - edgeThickness, x, height);
            } else {
                g2.setColor(colorPalette[x]);
                g2.drawLine(x, 0, x, height);
            }
        }
    }

    public void setLog10Display(boolean log10Display) {
        this.log10Display = log10Display;
        repaint();
    }

    public void setDiscreteDisplay(boolean discreteDisplay) {
        this.discreteDisplay = discreteDisplay;
        repaint();
    }

    public Range getRangeFromFile() {
        final ComboBoxModel<ColorPaletteWrapper> model = getModel();
        final int selectedIndex = getSelectedIndex();
        final ColorPaletteWrapper paletteWrapper = model.getElementAt(selectedIndex);
        String name = paletteWrapper.name;
        final ColorPaletteDef cpd;
        if (name.startsWith(DERIVED_FROM)) {
            name = name.substring(DERIVED_FROM.length()).trim();
            if (name.toLowerCase().endsWith(".cpd")) {
                name = FileUtils.getFilenameWithoutExtension(name);
            }
            cpd = findColorPalette(name);
        } else {
            cpd = paletteWrapper.cpd;
        }
        return new Range(cpd.getMinDisplaySample(), cpd.getMaxDisplaySample());
    }

    private ColorPaletteDef findColorPalette(String name) {
        final ComboBoxModel<ColorPaletteWrapper> model = getModel();
        for (int i = 0; i < model.getSize(); i++) {
            final ColorPaletteWrapper paletteWrapper = model.getElementAt(i);
            if (paletteWrapper.name.equals(name)) {
                return paletteWrapper.cpd;
            }
        }
        return null;
    }

    public static final class ColorPaletteWrapper {

        public final String name;

        public final ColorPaletteDef cpd;

        private ColorPaletteWrapper(String name, ColorPaletteDef cpd) {
            this.name = name;
            this.cpd = cpd;
        }
    }
}
