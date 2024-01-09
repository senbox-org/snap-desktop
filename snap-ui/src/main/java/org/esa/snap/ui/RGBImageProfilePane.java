/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.ui;

import com.bc.ceres.swing.TableLayout;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RGBImageProfile;
import org.esa.snap.core.datamodel.RGBImageProfileManager;
import org.esa.snap.core.datamodel.RgbDefaults;
import org.esa.snap.core.dataop.barithm.BandArithmetic;
import org.esa.snap.core.jexp.ParseException;
import org.esa.snap.core.util.ArrayUtils;
import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.PropertyMap;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.core.util.math.Range;
import org.esa.snap.ui.product.ProductExpressionPane;
import org.esa.snap.ui.tool.ToolButtonFactory;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.*;

import static org.esa.snap.core.datamodel.ColorManipulationDefaults.*;

public class RGBImageProfilePane extends JPanel {

    private static final boolean SHOW_ALPHA = false;

    private static final String[] COLOR_COMP_NAMES = new String[]{
            "Red", /*I18N*/
            "Green", /*I18N*/
            "Blue", /*I18N*/
            "Alpha", /*I18N*/
    };
    public static final Font EXPRESSION_FONT = new Font("Courier", Font.PLAIN, 12);

    private static final Color okMsgColor = new Color(0, 128, 0);
    private static final Color warnMsgColor = new Color(128, 0, 0);

    private PropertyMap preferences;
    private Product product;
    private final Product[] openedProducts;
    private JComboBox<ProfileItem> profileBox;

    private final JComboBox[] rgbaExprBoxes;
    private final JTextField validPixelExpression;
    private final RangeComponents[] rangeComponents;
    private DefaultComboBoxModel<ProfileItem> profileModel;
    private AbstractAction saveAsAction;
    private AbstractAction deleteAction;
    private boolean settingRgbaExpressions;
    private File lastDir;
    protected JCheckBox storeInProductCheck;
    private JLabel referencedRastersAreCompatibleLabel;

    public RGBImageProfilePane(PropertyMap preferences) {
        this(preferences, null, null, null);
    }

    public RGBImageProfilePane(PropertyMap preferences, Product product,
                               final Product[] openedProducts, final int[] defaultBandIndices) {
        this.preferences = preferences;
        this.product = product;
        this.openedProducts = openedProducts;

        AbstractAction openAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                performOpen();
            }
        };
        openAction.putValue(Action.LARGE_ICON_KEY, UIUtils.loadImageIcon("icons/Open24.gif"));
        openAction.putValue(Action.SHORT_DESCRIPTION, "Open an external RGB profile");

        saveAsAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                performSaveAs();
            }
        };
        saveAsAction.putValue(Action.LARGE_ICON_KEY, UIUtils.loadImageIcon("icons/Save24.gif"));
        saveAsAction.putValue(Action.SHORT_DESCRIPTION, "Save the RGB profile");

        deleteAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                performDelete();
            }
        };
        deleteAction.putValue(Action.LARGE_ICON_KEY,
                UIUtils.loadImageIcon("icons/Remove24.gif"));   // todo - use the nicer "cross" icon
        deleteAction.putValue(Action.SHORT_DESCRIPTION, "Delete the selected RGB profile");

        final JPanel storageButtonPanel = new JPanel(new GridLayout(1, 3, 2, 2));
        storageButtonPanel.add(ToolButtonFactory.createButton(openAction, false));
        storageButtonPanel.add(ToolButtonFactory.createButton(saveAsAction, false));
        storageButtonPanel.add(ToolButtonFactory.createButton(deleteAction, false));

        profileModel = new DefaultComboBoxModel<>();
        profileBox = new JComboBox<>(profileModel);
        profileBox.addItemListener(new ProfileSelectionHandler());
        profileBox.setEditable(false);
        profileBox.setName("profileBox");
        setPreferredWidth(profileBox, 200);

        storeInProductCheck = new JCheckBox();
        storeInProductCheck.setText("Store RGB channels as virtual bands in current product");
        storeInProductCheck.setSelected(false);
        storeInProductCheck.setVisible(this.product != null);
        storeInProductCheck.setName("storeInProductCheck");

        final String[] bandNames;
        if (this.product != null) {
            bandNames = this.product.getBandNames();
            // if multiple compatible products, the band names should be prefixed by the index of the product
            // in order to avoid ambiguity
            if (this.openedProducts.length > 1) {
                for (int i = 0; i < bandNames.length; i++) {
                    bandNames[i] = BandArithmetic.getProductNodeNamePrefix(this.product) + bandNames[i];
                }
            }
        } else {
            bandNames = new String[0];
        }
        rgbaExprBoxes = new JComboBox[4];
        for (int i = 0; i < rgbaExprBoxes.length; i++) {
            rgbaExprBoxes[i] = createRgbaBox(bandNames);
            rgbaExprBoxes[i].setName("rgbExprBox_" + i);
        }

        validPixelExpression = new JTextField();

        rangeComponents = new RangeComponents[3];
        for (int i = 0; i < rangeComponents.length; i++) {
            rangeComponents[i] = new RangeComponents();
        }

        JPanel profilePanel = new JPanel(new BorderLayout(2, 2));
        profilePanel.add(new JLabel("Profile: "), BorderLayout.NORTH);
        profilePanel.add(profileBox, BorderLayout.CENTER);
        profilePanel.add(storageButtonPanel, BorderLayout.EAST);

        JPanel colorComponentPanel = new JPanel(new GridBagLayout());
        final GridBagConstraints c3 = new GridBagConstraints();
        c3.anchor = GridBagConstraints.WEST;
        c3.fill = GridBagConstraints.HORIZONTAL;
        c3.insets = new Insets(2, 2, 2, 2);
        final int n = SHOW_ALPHA ? 4 : 3;
        for (int i = 0; i < n; i++) {
            c3.gridy = 2 * i;
            addColorComponentRow(colorComponentPanel, c3, i);

            c3.gridy = 2 * i + 1;
            addColorRangeComponentsRow(colorComponentPanel, c3, i);
        }

        c3.gridy++;
        c3.gridx = 0;
        colorComponentPanel.add(validPixelExpression, c3);

        referencedRastersAreCompatibleLabel = new JLabel();

        TableLayout layout = new TableLayout(1);
        layout.setTableFill(TableLayout.Fill.BOTH);
        layout.setTableWeightX(1.0);
        layout.setRowWeightY(3, 1.0);
        layout.setTablePadding(10, 10);
        setLayout(layout);
        add(profilePanel);
        add(colorComponentPanel);
        layout.setCellFill(2, 0, TableLayout.Fill.NONE);
        layout.setCellAnchor(2, 0, TableLayout.Anchor.NORTHEAST);
        add(referencedRastersAreCompatibleLabel);
        add(storeInProductCheck);
        add(layout.createVerticalSpacer());

        final RGBImageProfile[] registeredProfiles = RGBImageProfileManager.getInstance().getAllProfiles();
        addProfiles(registeredProfiles);
        if (this.product != null) {
            final RGBImageProfile productProfile = RGBImageProfile.getCurrentProfile(this.product);
            if (productProfile.isValid()) {
                final RGBImageProfile similarProfile = findMatchingProfile(productProfile);
                if (similarProfile != null) {
                    selectProfile(similarProfile);
                } else {
                    addNewProfile(productProfile);
                    selectProfile(productProfile);
                }
            } else {
                List<RGBImageProfile> selectableProfiles = new ArrayList<>();
                for (int i = 0; i < profileModel.getSize(); i++) {
                    selectableProfiles.add(profileModel.getElementAt(i).getProfile());
                }
                RGBImageProfile[] selectableProfileArray = selectableProfiles.toArray(new RGBImageProfile[0]);
                RGBImageProfile profile = findProfileForProductPattern(selectableProfileArray, product);
                if (profile != null) {
                    selectProfile(profile);
                }
            }
        }
        setRgbaExpressionsFromSelectedProfile();

        if (profileModel.getSelectedItem() == null) {
            // default
            if (defaultBandIndices != null && defaultBandIndices.length > 0) {
                for (int i = 0; i < defaultBandIndices.length; ++i) {
                    rgbaExprBoxes[i].setSelectedIndex(defaultBandIndices[i]);
                }
            }
        }
    }

    public Product getProduct() {
        return product;
    }

    public void dispose() {
        preferences = null;
        product = null;
        profileModel.removeAllElements();
        profileModel = null;
        profileBox = null;
        saveAsAction = null;
        deleteAction = null;
        Arrays.fill(rgbaExprBoxes, null);
        Arrays.fill(rangeComponents, null);
    }

    public boolean getStoreProfileInProduct() {
        return storeInProductCheck.isSelected();
    }

    /**
     * Gets the selected RGB-image profile if any.
     *
     * @return the selected profile, can be null
     * @see #getRgbaExpressions()
     */
    public RGBImageProfile getSelectedProfile() {
        final ProfileItem profileItem = getSelectedProfileItem();
        return profileItem != null ? profileItem.getProfile() : null;
    }

    /**
     * Gets the selected RGBA expressions as array of 4 strings.
     *
     * @return the selected RGBA expressions, never null
     */
    public String[] getRgbaExpressions() {
        return new String[]{
                getExpression(0),
                getExpression(1),
                getExpression(2),
                getExpression(3),
        };
    }




    public RGBImageProfile getRgbProfile() {
        final String[] rgbaExpressions = getRgbaExpressions();
        final String validPixelExpression = getValidPixelExpression();

        final Range[] ranges = new Range[3];
        for (int i = 0; i < rangeComponents.length; i++) {
            ranges[i] = rangeComponents[i].getRange();
        }

        String[] pattern = null;
        String name = "";
        final RGBImageProfile selectedProfile = getSelectedProfile();
        if (selectedProfile != null) {
            name = selectedProfile.getName();
            pattern = selectedProfile.getPattern();
        }

        return new RGBImageProfile(name, rgbaExpressions, validPixelExpression, pattern, ranges);
    }

    public void addProfiles(RGBImageProfile[] profiles) {
        for (RGBImageProfile profile : profiles) {
            addNewProfile(profile);
        }
        setRgbaExpressionsFromSelectedProfile();
    }

    public RGBImageProfile findMatchingProfile(RGBImageProfile profile) {
        // search in internal profiles first...
        RGBImageProfile matchingProfile = findMatchingProfile(profile, true);
        if (matchingProfile == null) {
            // ...then in non-internal profiles
            matchingProfile = findMatchingProfile(profile, false);
        }
        return matchingProfile;
    }

    public void selectProfile(RGBImageProfile profile) {
        profileModel.setSelectedItem(new ProfileItem(profile));
    }

    public boolean showDialog(Window parent, String title, String helpId) {
        ModalDialog modalDialog = new ModalDialog(parent, title, ModalDialog.ID_OK_CANCEL_HELP, helpId);
        modalDialog.setContent(this);
        final int status = modalDialog.show();
        modalDialog.getJDialog().dispose();
        return status == ModalDialog.ID_OK;
    }

    private String getExpression(int i) {
        return ((JTextField) rgbaExprBoxes[i].getEditor().getEditorComponent()).getText().trim();
    }

    private String getValidPixelExpression() {
        return ((JTextField) validPixelExpression).getText().trim();
    }

    private void setExpression(int i, String expression) {
        rgbaExprBoxes[i].setSelectedItem(expression);
    }


    private void setValidPixelExpression(String validPixelExpression) {
         this.validPixelExpression.setText(validPixelExpression);
    }


    private void performOpen() {
        final SnapFileChooser snapFileChooser = new SnapFileChooser(getProfilesDir());
        snapFileChooser.setFileFilter(
                new SnapFileFilter("RGB-PROFILE", RGBImageProfile.FILENAME_EXTENSION, "RGB-Image Profile Files"));
        final int status = snapFileChooser.showOpenDialog(this);
        if (snapFileChooser.getSelectedFile() == null) {
            return;
        }
        final File file = snapFileChooser.getSelectedFile();
        lastDir = file.getParentFile();
        if (status != SnapFileChooser.APPROVE_OPTION) {
            return;
        }

        final RGBImageProfile profile;
        try {
            profile = RGBImageProfile.loadProfile(file);
            String[] rgbaExpressions = profile.getRgbaExpressions();
            // If profile was saved with a single product index reference, it may not match the current product index,
            // so try to replace it. If it contains multiple indices, then keep them.
            Set<Integer> productRefs = new HashSet<>();
            for (String expression : rgbaExpressions) {
                if (!StringUtils.isNullOrEmpty(expression)) {
                    if (expression.startsWith("$")) {
                        productRefs.add(Integer.parseInt(expression.substring(1, expression.indexOf('.'))));
                    } else {
                        productRefs.add(0);
                    }
                }
            }
            boolean shouldReplace = productRefs.size() == 1 && !productRefs.contains(0);
            for (int i = 0; i < rgbaExpressions.length; i++) {
                if (shouldReplace) {
                    if (!StringUtils.isNullOrEmpty(rgbaExpressions[i])) {
                        rgbaExpressions[i] = rgbaExpressions[i].substring(rgbaExpressions[i].indexOf('.') + 1);
                    }
                }
            }
            profile.setRgbaExpressions(rgbaExpressions);
        } catch (IOException e) {
            AbstractDialog.showErrorDialog(this,
                    String.format("Failed to open RGB-profile '%s':\n%s", file.getName(), e.getMessage()),
                    "Open RGB-Image Profile");
            return;
        }
        if (profile == null) {
            AbstractDialog.showErrorDialog(this,
                    String.format("Invalid RGB-Profile '%s'.", file.getName()),
                    "Open RGB-Image Profile");
            return;
        }

        RGBImageProfileManager.getInstance().addProfile(profile);
        if (product != null && !profile.isApplicableTo(product)) {
            AbstractDialog.showErrorDialog(this,
                    String.format("The selected RGB-Profile '%s'\nis not applicable to the current product.",
                            profile.getName()),
                    "Open RGB-Image Profile");
            return;
        }
        addNewProfile(profile);
    }

    private void performSaveAs() {
        File file = promptForSaveFile();
        if (file == null) {
            return;
        }
        String[] rgbaExpressions = getRgbaExpressions();
        Set<Integer> productRefs = new HashSet<>();
        for (String expression : rgbaExpressions) {
            if (!StringUtils.isNullOrEmpty(expression)) {
                if (expression.startsWith("$")) {
                    productRefs.add(Integer.parseInt(expression.substring(1, expression.indexOf('.'))));
                } else {
                    productRefs.add(0);
                }
            }
        }
        boolean shouldReplace = productRefs.size() == 1 && !productRefs.contains(0);
        for (int i = 0; i < rgbaExpressions.length; i++) {
            if (shouldReplace) {
                rgbaExpressions[i] = rgbaExpressions[i].replace(BandArithmetic.getProductNodeNamePrefix(this.product), "");
            }
        }

        final Range[] ranges = new Range[3];
        for (int i = 0; i < 3; i++) {
            ranges[i] = rangeComponents[i].getRange();
        }
        final RGBImageProfile profile = new RGBImageProfile(FileUtils.getFilenameWithoutExtension(file),
                rgbaExpressions, getValidPixelExpression(), null, ranges);
        try {
            profile.store(file);
        } catch (IOException e) {
            AbstractDialog.showErrorDialog(this,
                    "Failed to save RGB-profile '" + file.getName() + "':\n"
                            + e.getMessage(),
                    "Open RGB-Image Profile");
            return;
        }

        RGBImageProfileManager.getInstance().addProfile(profile);
        addNewProfile(profile);
    }

    private File promptForSaveFile() {
        final SnapFileChooser snapFileChooser = new SnapFileChooser(getProfilesDir());
        snapFileChooser.setFileFilter(new SnapFileFilter("RGB-PROFILE", ".rgb", "RGB-Image Profile Files"));

        File selectedFile;
        while (true) {
            final int status = snapFileChooser.showSaveDialog(this);
            if (snapFileChooser.getSelectedFile() == null) {
                selectedFile = null;
                break;
            }
            selectedFile = snapFileChooser.getSelectedFile();
            lastDir = selectedFile.getParentFile();
            if (status != SnapFileChooser.APPROVE_OPTION) {
                selectedFile = null;
                break;
            }
            if (selectedFile.exists()) {
                final int answer = JOptionPane.showConfirmDialog(RGBImageProfilePane.this,
                        "The file '" + selectedFile.getName()
                                + "' already exists.\n" +
                                "So you really want to overwrite it?",
                        "Safe RGB-Profile As",
                        JOptionPane.YES_NO_CANCEL_OPTION);
                if (answer == JOptionPane.CANCEL_OPTION) {
                    selectedFile = null;
                    break;
                }
                if (answer == JOptionPane.YES_OPTION) {
                    break;
                }
            } else {
                break;
            }
        }
        return selectedFile;
    }

    private void performDelete() {
        final ProfileItem selectedProfileItem = getSelectedProfileItem();
        if (selectedProfileItem != null && !selectedProfileItem.getProfile().isInternal()) {
            profileModel.removeElement(selectedProfileItem);
        }
    }

    private File getProfilesDir() {
        if (lastDir != null) {
            return lastDir;
        } else {
            return RGBImageProfileManager.getProfilesDir();
        }
    }

    private void addNewProfile(RGBImageProfile profile) {
        if (product != null && !profile.isApplicableTo(product)) {
            return;
        }
        final ProfileItem profileItem = new ProfileItem(profile);
        final int index = profileModel.getIndexOf(profileItem);
        if (index == -1) {
            profileModel.addElement(profileItem);
        }
        profileModel.setSelectedItem(profileItem);
    }

    private void setRgbaExpressionsFromSelectedProfile() {
        settingRgbaExpressions = true;
        try {
            final ProfileItem profileItem = getSelectedProfileItem();
            if (profileItem != null) {
                final RGBImageProfile profile = profileItem.getProfile();
                final String[] rgbaExpressions = profile.getRgbaExpressions();
                for (int i = 0; i < rgbaExprBoxes.length; i++) {
                    setExpression(i, rgbaExpressions[i]);
                }

                setValidPixelExpression(profile.getValidPixelExpression());

                if (preferences.getPropertyBool(RgbDefaults.PROPERTY_RGB_OPTIONS_MIN_MAX_RANGE_KEY, RgbDefaults.PROPERTY_RGB_OPTIONS_MIN_MAX_RANGE_DEFAULT)) {

                    final Range redMinMaxPreference = new Range();
                    redMinMaxPreference.setMin(preferences.getPropertyDouble(RgbDefaults.PROPERTY_RGB_OPTIONS_MIN_KEY, RgbDefaults.PROPERTY_RGB_OPTIONS_MIN_DEFAULT));
                    redMinMaxPreference.setMax(preferences.getPropertyDouble(RgbDefaults.PROPERTY_RGB_OPTIONS_MAX_KEY, RgbDefaults.PROPERTY_RGB_OPTIONS_MAX_DEFAULT));
                    rangeComponents[0].set(redMinMaxPreference);
                    rangeComponents[0].enableMinMax(true);

                    final Range greenMinMaxPreference = new Range();
                    greenMinMaxPreference.setMin(preferences.getPropertyDouble(RgbDefaults.PROPERTY_RGB_OPTIONS_MIN_KEY, RgbDefaults.PROPERTY_RGB_OPTIONS_MIN_DEFAULT));
                    greenMinMaxPreference.setMax(preferences.getPropertyDouble(RgbDefaults.PROPERTY_RGB_OPTIONS_MAX_KEY, RgbDefaults.PROPERTY_RGB_OPTIONS_MAX_DEFAULT));
                    rangeComponents[1].set(greenMinMaxPreference);
                    rangeComponents[1].enableMinMax(true);

                    final Range blueMinMaxPreference = new Range();
                    blueMinMaxPreference.setMin(preferences.getPropertyDouble(RgbDefaults.PROPERTY_RGB_OPTIONS_MIN_KEY, RgbDefaults.PROPERTY_RGB_OPTIONS_MIN_DEFAULT));
                    blueMinMaxPreference.setMax(preferences.getPropertyDouble(RgbDefaults.PROPERTY_RGB_OPTIONS_MAX_KEY, RgbDefaults.PROPERTY_RGB_OPTIONS_MAX_DEFAULT));
                    rangeComponents[2].set(blueMinMaxPreference);
                    rangeComponents[2].enableMinMax(true);

                } else {
                    final Range redMinMax = profile.getRedMinMax();
                    rangeComponents[0].set(redMinMax);
                    final Range greenMinMax = profile.getGreenMinMax();
                    rangeComponents[1].set(greenMinMax);
                    final Range blueMinMax = profile.getBlueMinMax();
                    rangeComponents[2].set(blueMinMax);
                }

            } else {
                for (int i = 0; i < rgbaExprBoxes.length; i++) {
                    setExpression(i, "");
                }
                final Range invalidRange = new Range(Double.NaN, Double.NaN);
                for (RangeComponents rangeComponent : rangeComponents) {
                    rangeComponent.set(invalidRange);
                }
            }
        } finally {
            settingRgbaExpressions = false;
        }
        updateUIState();
    }

    private ProfileItem getSelectedProfileItem() {
        return (ProfileItem) profileBox.getSelectedItem();
    }

    private void addColorComponentRow(JPanel p3, final GridBagConstraints constraints, final int index) {
        final JButton editorButton = new JButton("...");
        editorButton.addActionListener(e -> invokeExpressionEditor(index));
        final Dimension preferredSize = rgbaExprBoxes[index].getPreferredSize();
        editorButton.setPreferredSize(new Dimension(preferredSize.height, preferredSize.height));

        constraints.gridx = 0;
        constraints.weightx = 0;
        p3.add(new JLabel(getComponentName(index) + ": "), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1;
        p3.add(rgbaExprBoxes[index], constraints);

        constraints.gridx = 2;
        constraints.weightx = 0;
        p3.add(editorButton, constraints);
    }

    private void addColorRangeComponentsRow(JPanel p3, final GridBagConstraints constraints, final int index) {
        constraints.gridx = 0;
        constraints.weightx = 0;
        p3.add(new JLabel(), constraints);

        final JPanel container = new JPanel();
        final RangeComponents rangeComponent = rangeComponents[index];
        final GridBagConstraints containerConstraints = new GridBagConstraints();
        containerConstraints.gridy = 0;
        containerConstraints.gridx = 0;
        containerConstraints.weightx = 1;
        container.add(rangeComponent.fixedRangeCheckBox, containerConstraints);

        containerConstraints.gridx = 1;
        container.add(rangeComponent.minText, containerConstraints);

        containerConstraints.gridx = 2;
        container.add(rangeComponent.minLabel, containerConstraints);

        containerConstraints.gridx = 3;
        container.add(rangeComponent.maxText, containerConstraints);

        containerConstraints.gridx = 4;
        container.add(rangeComponent.maxLabel, containerConstraints);

        constraints.gridx = 1;
        p3.add(container, constraints);
    }

    protected String getComponentName(final int index) {
        return COLOR_COMP_NAMES[index];
    }

    private void invokeExpressionEditor(final int colorIndex) {
        final Window window = SwingUtilities.getWindowAncestor(this);
        final String title = "Edit " + getComponentName(colorIndex) + " Expression";
        if (product != null) {
            final ExpressionPane pane;
            final Product[] products = getCompatibleProducts(product, openedProducts);
            pane = ProductExpressionPane.createGeneralExpressionPane(products, product, preferences);
            pane.setCode(getExpression(colorIndex));
            int status = pane.showModalDialog(window, title);
            if (status == ModalDialog.ID_OK) {
                setExpression(colorIndex, pane.getCode());
            }
        } else {
            final JTextArea textArea = new JTextArea(8, 48);
            textArea.setFont(EXPRESSION_FONT);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            textArea.setText(getExpression(colorIndex));
            final ModalDialog modalDialog = new ModalDialog(window, title, ModalDialog.ID_OK_CANCEL, "");
            final JPanel panel = new JPanel(new BorderLayout(2, 2));
            panel.add(new JLabel("Expression:"), BorderLayout.NORTH);
            panel.add(new JScrollPane(textArea), BorderLayout.CENTER);
            modalDialog.setContent(panel);
            final int status = modalDialog.show();
            if (status == ModalDialog.ID_OK) {
                setExpression(colorIndex, textArea.getText());
            }
        }
    }

    private static Product[] getCompatibleProducts(final Product targetProduct, final Product[] productsList) {
        final List<Product> compatibleProducts = new ArrayList<>(1);
        compatibleProducts.add(targetProduct);
        final float geolocationEps = 180;
        Debug.trace("BandMathsDialog.geolocationEps = " + geolocationEps);
        Debug.trace("BandMathsDialog.getCompatibleProducts:");
        Debug.trace("  comparing: " + targetProduct.getName());
        if (productsList != null) {
            for (final Product product : productsList) {
                if (targetProduct != product) {
                    Debug.trace("  with:      " + product.getDisplayName());
                    final boolean isCompatibleProduct = targetProduct.isCompatibleProduct(product, geolocationEps);
                    Debug.trace("  result:    " + isCompatibleProduct);
                    if (isCompatibleProduct) {
                        compatibleProducts.add(product);
                    }
                }
            }
        }
        return compatibleProducts.toArray(new Product[compatibleProducts.size()]);
    }

    private JComboBox createRgbaBox(String[] suggestions) {
        final JComboBox<String> comboBox = new JComboBox<>(suggestions);
        setPreferredWidth(comboBox, 320);
        comboBox.setEditable(true);
        final ComboBoxEditor editor = comboBox.getEditor();
        final JTextField textField = (JTextField) editor.getEditorComponent();
        textField.setFont(EXPRESSION_FONT);
        textField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                onRgbaExpressionChanged();
            }

            public void removeUpdate(DocumentEvent e) {
                onRgbaExpressionChanged();
            }

            public void changedUpdate(DocumentEvent e) {
                onRgbaExpressionChanged();
            }
        });
        return comboBox;
    }

    private void onRgbaExpressionChanged() {
        if (settingRgbaExpressions) {
            return;
        }
        final ProfileItem profileItem = getSelectedProfileItem();
        if (profileItem != null) {
            if (isSelectedProfileModified()) {
                profileBox.revalidate();
                profileBox.repaint();
            }
        }
        final RGBImageProfile selectedProfile = getSelectedProfile();
        final String[] rgbaExpressions;
        if (selectedProfile != null) {
            rgbaExpressions = selectedProfile.getRgbaExpressions();
        } else {
            rgbaExpressions = getRgbaExpressions();
        }

        final int defaultProductIndex = ArrayUtils.getElementIndex(product, openedProducts);
        try {
            if (!BandArithmetic.areRastersEqualInSize(openedProducts,
                    defaultProductIndex, rgbaExpressions)) {
                referencedRastersAreCompatibleLabel.setText("Referenced rasters are not of the same size");
                referencedRastersAreCompatibleLabel.setForeground(warnMsgColor);
            } else {
                referencedRastersAreCompatibleLabel.setText("Expressions are valid");
                referencedRastersAreCompatibleLabel.setForeground(okMsgColor);
            }
        } catch (ParseException e) {
            referencedRastersAreCompatibleLabel.setText("Expressions are invalid");
            referencedRastersAreCompatibleLabel.setForeground(warnMsgColor);
        }
        updateUIState();
    }

    private boolean isSelectedProfileModified() {
        final ProfileItem profileItem = getSelectedProfileItem();
        final String[] profileRgbaExpressions = profileItem.getProfile().getRgbaExpressions();
        final String[] userRgbaExpressions = getRgbaExpressions();
        for (int i = 0; i < profileRgbaExpressions.length; i++) {
            final String userRgbaExpression = userRgbaExpressions[i];
            final String profileRgbaExpression = profileRgbaExpressions[i];
            if (!profileRgbaExpression.equals(userRgbaExpression)) {
                return true;
            }
        }
        return false;
    }

    private void updateUIState() {

        final ProfileItem profileItem = getSelectedProfileItem();
        if (profileItem != null) {
            saveAsAction.setEnabled(true);
            deleteAction.setEnabled(!profileItem.getProfile().isInternal());
        } else {
            saveAsAction.setEnabled(isAtLeastOneColorExpressionSet());
            deleteAction.setEnabled(false);
        }
    }

    private boolean isAtLeastOneColorExpressionSet() {
        final JComboBox[] rgbaExprBoxes = this.rgbaExprBoxes;
        for (int i = 0; i < 3; i++) {
            JComboBox rgbaExprBox = rgbaExprBoxes[i];
            final Object selectedItem = rgbaExprBox.getSelectedItem();
            if (selectedItem != null && !selectedItem.toString().trim().equals("")) {
                return true;
            }
        }
        return false;
    }

    private void setPreferredWidth(final JComboBox comboBox, final int width) {
        final Dimension preferredSize = comboBox.getPreferredSize();
        comboBox.setPreferredSize(new Dimension(width, preferredSize.height));
    }

    public static RGBImageProfile findProfileForProductPattern(RGBImageProfile[] rgbImageProfiles, Product product) {
        if (rgbImageProfiles.length == 0) {
            return null;
        }

        String productType = product.getProductType();
        String productName = product.getName();
        String productDesc = product.getDescription();
        RGBImageProfile bestProfile = rgbImageProfiles[0];
        int bestMatchScore = 0;
        for (RGBImageProfile rgbImageProfile : rgbImageProfiles) {
            String[] pattern = rgbImageProfile.getPattern();
            if (pattern == null) {
                continue;
            }
            boolean productTypeMatches = matches(productType, pattern[0]);
            boolean productNameMatches = matches(productName, pattern[1]);
            boolean productDescMatches = matches(productDesc, pattern[2]);
            int currentMatchScore = (productTypeMatches ? 100 : 0) + (productNameMatches ? 10 : 0) + (productDescMatches ? 1 : 0);
            if (currentMatchScore > bestMatchScore) {
                bestProfile = rgbImageProfile;
                bestMatchScore = currentMatchScore;
            }
        }
        return bestProfile;
    }


    private static boolean matches(String textValue, String pattern) {
        return textValue != null
                && pattern != null
                && textValue.matches(pattern.replace("*", ".*").replace("?", "."));
    }


    private class ProfileItem {

        private final RGBImageProfile profile;

        public ProfileItem(RGBImageProfile profile) {
            this.profile = profile;
        }

        public RGBImageProfile getProfile() {
            return profile;
        }

        @Override
        public int hashCode() {
            return getProfile().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            } else if (obj instanceof ProfileItem) {
                ProfileItem profileItem = (ProfileItem) obj;
                return getProfile().equals(profileItem.getProfile());
            }
            return false;
        }

        @Override
        public String toString() {
            String name = profile.getName().replace('_', ' ');
            if (getSelectedProfileItem().equals(this) && isSelectedProfileModified()) {
                name += " (modified)";
            }
            return name;
        }
    }

    private class ProfileSelectionHandler implements ItemListener {

        public void itemStateChanged(ItemEvent e) {
            setRgbaExpressionsFromSelectedProfile();
        }
    }


    public RGBImageProfile findMatchingProfile(RGBImageProfile profile, boolean internal) {
        final int size = profileModel.getSize();
        for (int i = 0; i < size; i++) {
            final ProfileItem item = profileModel.getElementAt(i);
            final RGBImageProfile knownProfile = item.getProfile();
            if (knownProfile.isInternal() == internal
                    && Arrays.equals(profile.getRgbExpressions(), knownProfile.getRgbExpressions())) {
                return knownProfile;
            }
        }
        return null;
    }

    static class RangeComponents {
        final JCheckBox fixedRangeCheckBox;

        final JLabel minLabel;
        final JTextField minText;

        final JLabel maxLabel;
        final JTextField maxText;


        RangeComponents() {
            fixedRangeCheckBox = new JCheckBox("fixed range");
            fixedRangeCheckBox.addActionListener(e -> this.enableMinMax(fixedRangeCheckBox.isSelected()));

            minText = new JTextField(12);
            minLabel = new JLabel("min");
            maxText = new JTextField(12);
            maxLabel = new JLabel("max");

            fixedRangeCheckBox.setSelected(false);
            enableMinMax(false);
        }


        void enableMinMax(boolean enable) {
            minLabel.setEnabled(enable);
            minText.setEnabled(enable);
            maxLabel.setEnabled(enable);
            maxText.setEnabled(enable);
        }

        void set(Range range) {
            final double min = range.getMin();
            final double max = range.getMax();

            boolean enable = false;
            if (!Double.isNaN(min)) {
                minText.setText(Double.toString(min));
                enable = true;
            } else {
                minText.setText("");
            }

            if (!Double.isNaN(max)) {
                maxText.setText(Double.toString(max));
                enable = true;
            } else {
                maxText.setText("");
            }

            enableMinMax(enable);
            fixedRangeCheckBox.setSelected(enable);
        }


        Range getRange() {
            double min = Double.NaN;
            double max = Double.NaN;

            if (fixedRangeCheckBox.isSelected()) {

                try {
                    final String minValueString = minText.getText();
                    if (StringUtils.isNotNullAndNotEmpty(minValueString)) {
                        min = Double.parseDouble(minValueString.trim());
                    }

                    final String maxValueString = maxText.getText();
                    if (StringUtils.isNotNullAndNotEmpty(maxValueString)) {
                        max = Double.parseDouble(maxValueString.trim());
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid input - minimum and maximum must be floating point values");
                }
            }

            return new Range(min, max);
        }
    }
}
