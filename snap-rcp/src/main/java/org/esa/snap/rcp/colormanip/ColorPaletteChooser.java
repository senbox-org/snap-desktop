package org.esa.snap.rcp.colormanip;

import com.bc.ceres.swing.TableLayout;
import org.esa.snap.core.datamodel.ColorPaletteDef;
import org.esa.snap.core.datamodel.ImageInfo;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.util.PropertyMap;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.core.util.math.Range;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.GridBagUtils;
import org.esa.snap.ui.product.ProductSceneView;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Vector;

import static java.awt.Font.ITALIC;
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

    private final String DERIVED_FROM = "Derived from";
    private final String UNNAMED = "Unnamed";
    private boolean discreteDisplay;
    private boolean log10Display;





    public ColorPaletteChooser() {
        super(getPalettes());
        setRenderer(createPaletteRenderer());
        setEditable(false);
//        setSelectedIndex(1);
//        setPreferredSize(getPreferredSize());
//        setMinimumSize(getMinimumSize());;
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
            if (suffix.startsWith(DERIVED_FROM)) {
                name = suffix.trim();
            } else {
                    name = DERIVED_FROM + " " + suffix.trim();
            }
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

        boolean categorizePalettes = configuration.getPropertyBool(PROPERTY_PALETTE_CATEGORIZE_DISPLAY_KEY, PROPERTY_PALETTE_CATEGORIZE_DISPLAY_DEFAULT);
        boolean removePaletteNameExtension = configuration.getPropertyBool(PROPERTY_PALETTE_REMOVE_EXTENSION_KEY, PROPERTY_PALETTE_REMOVE_EXTENSION_DEFAULT);

        final List<ColorPaletteDef> defList = ColorPaletteManager.getDefault().getColorPaletteDefList();
        final Vector<ColorPaletteWrapper> paletteWrappers = new Vector<>();
        final ColorPaletteDef paletteDummy = new ColorPaletteDef(new ColorPaletteDef.Point[]{
                new ColorPaletteDef.Point(.001, Color.WHITE),
                new ColorPaletteDef.Point(1.0, Color.WHITE)
        });

        if (categorizePalettes) {

            for (String currCategory : ColorPaletteManager.getDefault().getCategories()) {

                int matches = 0;
                for (ColorPaletteDef colorPaletteDef : defList) {
                    if (ColorPaletteManager.getDefault().matchesCategory(colorPaletteDef, currCategory)) {
                        matches++;
                    }
                }

                if (matches > 0) {
                    paletteWrappers.add(new ColorPaletteWrapper(ColorPaletteManager.getCategoryDisplay(currCategory), paletteDummy));

                    for (ColorPaletteDef colorPaletteDef : defList) {
                        final String nameFor = getNameForColorPalette(colorPaletteDef, removePaletteNameExtension);

                        if (ColorPaletteManager.getDefault().matchesCategory(colorPaletteDef, currCategory)) {
                            paletteWrappers.add(new ColorPaletteWrapper(nameFor, colorPaletteDef));
                        }
                    }
                }

            }
        } else {
            for (ColorPaletteDef colorPaletteDef : defList) {
                final String nameFor = getNameForColorPalette(colorPaletteDef, removePaletteNameExtension);
                paletteWrappers.add(new ColorPaletteWrapper(nameFor, colorPaletteDef));
            }
        }
        return paletteWrappers;
    }

    private static String getNameForColorPalette(ColorPaletteDef colorPaletteDef, boolean removeExtension) {
        if (removeExtension) {
            return getNameForWithoutExtension(colorPaletteDef);
        } else {
            return ColorPaletteManager.getDefault().getNameFor(colorPaletteDef);
        }
    }


    private static String getNameForWithoutExtension(ColorPaletteDef colorPaletteDef) {
        final String nameFor = ColorPaletteManager.getDefault().getNameFor(colorPaletteDef);
        if (nameFor.toLowerCase().endsWith(".cpd")) {
            return nameFor.substring(0, nameFor.length() - 4);
        } else {
            return nameFor;
        }
    }


    public JLabel getPaletteImage() {

        ColorPaletteDef cpd = getSelectedColorPaletteDefinition();
        final JLabel rampComp = new JLabel(" ") {
            @Override
            public void paint(Graphics g) {
                super.paint(g);
                drawPalette((Graphics2D) g, cpd, g.getClipBounds().getSize(), 0, true);
            }
        };

        return rampComp;
    }

    private ListCellRenderer<ColorPaletteWrapper> createPaletteRenderer() {

        final ProductSceneView view = SnapApp.getDefault().getSelectedProductSceneView();
        PropertyMap configuration = view.getSceneImage().getConfiguration();


        boolean includePaletteImage = configuration.getPropertyBool(PROPERTY_PALETTE_INCLUDE_IMAGE_KEY, PROPERTY_PALETTE_INCLUDE_IMAGE_DEFAULT);

        return new ListCellRenderer<ColorPaletteWrapper>() {
            @Override
            public Component getListCellRendererComponent(JList<? extends ColorPaletteWrapper> list, ColorPaletteWrapper value, int index, boolean isSelected, boolean cellHasFocus) {

                final JLabel nameComp = new JLabel(value.name);

                final ColorPaletteDef cpd = value.cpd;
                final JLabel rampComp = new JLabel(" ") {
                    @Override
                    public void paint(Graphics g) {
                        super.paint(g);
                        drawPalette((Graphics2D) g, cpd, g.getClipBounds().getSize(), index, isSelected);
                    }
                };



                final JPanel panel = GridBagUtils.createPanel();
                GridBagConstraints gbc = new GridBagConstraints();

                gbc.gridx = 0;
                gbc.gridy = 0;
                gbc.anchor = GridBagConstraints.WEST;
                gbc.fill = GridBagConstraints.NONE;
                gbc.weightx = 1;
                gbc.weighty = 1;

                gbc.insets.left = 10;
                gbc.insets.right = 10;
                gbc.insets.bottom = 3;
                gbc.insets.top = 3;



                boolean isCategory = false;
                for (String category : ColorPaletteManager.getDefault().getCategories()) {
                    if (value.name.contains(ColorPaletteManager.getCategoryDisplay(category))) {
                        isCategory = true;
                        break;
                    }
                }


                if (value.name.length() == 0) {
                    panel.add(new JLabel("Default"), gbc);
                } else if (value.name.contains(DERIVED_FROM) || value.name.contains(UNNAMED)) {

                    panel.add(nameComp, gbc);

//                    if (includePaletteImage) {
//                        gbc.fill = GridBagConstraints.HORIZONTAL;
//                        gbc.gridy += 1;
//                        panel.add(rampComp, gbc);
//                        gbc.fill = GridBagConstraints.NONE;
//                    }

                } else {
                    if (isCategory) {
                        nameComp.setOpaque(true);
                        nameComp.setFocusable(false);
                        Font currFont = nameComp.getFont();
                        int newFontSize = (int) Math.floor(currFont.getSize() * 1.1);
                        int newFontStyle = currFont.getStyle() | ITALIC;
                        Font newFont = new Font(currFont.getName(), newFontStyle, newFontSize);
                        nameComp.setFont(newFont);

                        gbc.insets.top = 10;
                        gbc.fill = GridBagConstraints.HORIZONTAL;
                        panel.add(nameComp, gbc);
                        gbc.fill = GridBagConstraints.NONE;
                        gbc.insets.top = 3;

                    } else {
                        if (includePaletteImage) {

                            gbc.insets.top = 10;
                            panel.add(nameComp, gbc);
                            gbc.insets.top = 3;

                            gbc.gridy += 1;
                            gbc.fill = GridBagConstraints.HORIZONTAL;
                            panel.add(rampComp, gbc);
                            gbc.fill = GridBagConstraints.NONE;

                        } else {
                            gbc.insets.top = 3;
                            gbc.insets.bottom = 3;

                            panel.add(nameComp, gbc);
                        }
                    }
                }

                if (isSelected) {
                    list.setToolTipText(value.name);
                }

                return panel;
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
