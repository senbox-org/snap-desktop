package org.esa.snap.rcp.magicwand;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.undo.support.DefaultUndoContext;
import com.thoughtworks.xstream.XStream;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.SnapDialogs;
import org.esa.snap.tango.TangoIcons;
import org.esa.snap.ui.ModalDialog;
import org.esa.snap.ui.product.BandChooser;
import org.esa.snap.ui.product.ProductSceneView;
import org.esa.snap.ui.tool.ToolButtonFactory;
import org.openide.util.ImageUtilities;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Insets;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.prefs.Preferences;

import static com.bc.ceres.swing.TableLayout.*;

/**
 * @author Norman Fomferra
 * @since BEAM 4.10
 */
class MagicWandForm {
    public static final int TOLERANCE_SLIDER_RESOLUTION = 1000;
    public static final String PREFERENCES_KEY_LAST_DIR = "beam.magicWandTool.lastDir";

    private MagicWandInteractor interactor;

    private JSlider toleranceSlider;
    boolean adjustingSlider;

    private DefaultUndoContext undoContext;
    private AbstractButton redoButton;
    private AbstractButton undoButton;
    private BindingContext bindingContext;

    private File settingsFile;
    private JLabel infoLabel;
    private AbstractButton minusButton;
    private AbstractButton plusButton;
    private AbstractButton clearButton;
    private AbstractButton saveButton;

    MagicWandForm(MagicWandInteractor interactor) {
        this.interactor = interactor;
    }

    public File getSettingsFile() {
        return settingsFile;
    }

    public BindingContext getBindingContext() {
        return bindingContext;
    }

    public JPanel createPanel() {

        undoContext = new DefaultUndoContext(this);
        interactor.setUndoContext(undoContext);

        bindingContext = new BindingContext(PropertyContainer.createObjectBacked(interactor.getModel()));
        bindingContext.addPropertyChangeListener("tolerance", evt -> {
            adjustSlider();
            interactor.getModel().fireModelChanged(true);
        });

        infoLabel = new JLabel();
        infoLabel.setForeground(Color.DARK_GRAY);

        JLabel toleranceLabel = new JLabel("Tolerance:");
        toleranceLabel.setToolTipText("Sets the maximum Euclidian distance tolerated");

        JTextField toleranceField = new JTextField(10);
        bindingContext.bind("tolerance", toleranceField);
        toleranceField.setText(String.valueOf(interactor.getModel().getTolerance()));

        toleranceSlider = new JSlider(0, TOLERANCE_SLIDER_RESOLUTION);
        toleranceSlider.setSnapToTicks(false);
        toleranceSlider.setPaintTicks(false);
        toleranceSlider.setPaintLabels(false);
        toleranceSlider.addChangeListener(e -> {
            if (!adjustingSlider) {
                int sliderValue = toleranceSlider.getValue();
                bindingContext.getPropertySet().setValue("tolerance", sliderValueToTolerance(sliderValue));
            }
        });

        JTextField minToleranceField = new JTextField(4);
        JTextField maxToleranceField = new JTextField(4);
        bindingContext.bind("minTolerance", minToleranceField);
        bindingContext.bind("maxTolerance", maxToleranceField);
        final PropertyChangeListener minMaxToleranceListener = evt -> adjustSlider();
        bindingContext.addPropertyChangeListener("minTolerance", minMaxToleranceListener);
        bindingContext.addPropertyChangeListener("maxTolerance", minMaxToleranceListener);

        JPanel toleranceSliderPanel = new JPanel(new BorderLayout(2, 2));
        toleranceSliderPanel.add(minToleranceField, BorderLayout.WEST);
        toleranceSliderPanel.add(toleranceSlider, BorderLayout.CENTER);
        toleranceSliderPanel.add(maxToleranceField, BorderLayout.EAST);

        JCheckBox normalizeCheckBox = new JCheckBox("Normalize spectra");
        normalizeCheckBox.setToolTipText("Normalizes collected band sets by dividing their\n" +
                                                 "individual values by the value of the first band");
        bindingContext.bind("normalize", normalizeCheckBox);
        bindingContext.addPropertyChangeListener("normalize", evt -> interactor.getModel().fireModelChanged(true));

        JLabel stLabel = new JLabel("Spectrum transformation:");
        JRadioButton stButton1 = new JRadioButton("Integral");
        JRadioButton stButton2 = new JRadioButton("Identity");
        JRadioButton stButton3 = new JRadioButton("Derivative");
        ButtonGroup stGroup = new ButtonGroup();
        stGroup.add(stButton1);
        stGroup.add(stButton2);
        stGroup.add(stButton3);
        stButton1.setToolTipText("<html>Pixel similarity comparison is performed<br>" +
                                         "on the sums of subsequent band values");
        stButton2.setToolTipText("<html>Pixel similarity comparison is performed<br>" +
                                         "on the original or normalised band values");
        stButton3.setToolTipText("<html>Pixel similarity comparison is performed<br>" +
                                         "on the differences of subsequent band values");
        bindingContext.bind("spectrumTransform", stGroup);
        bindingContext.addPropertyChangeListener("spectrumTransform", evt -> interactor.getModel().fireModelChanged(true));

        JLabel ptLabel = new JLabel("Similarity metric:");
        JRadioButton ptButton1 = new JRadioButton("Distance");
        JRadioButton ptButton2 = new JRadioButton("Average");
        JRadioButton ptButton3 = new JRadioButton("Min-Max");
        ptButton1.setToolTipText("<html>Tests if the minimum of Euclidian distances of a pixel to<br>" +
                                         "each collected bands set is below the threshold");
        ptButton2.setToolTipText("<html>Tests if the Euclidian distances of a pixel to<br>" +
                                         "the average of all collected bands sets is below the threshold");
        ptButton3.setToolTipText("<html>Tests if a pixel is within the min/max limits<br>" +
                                         "of collected bands plus/minus tolerance");
        ButtonGroup ptGroup = new ButtonGroup();
        ptGroup.add(ptButton1);
        ptGroup.add(ptButton2);
        ptGroup.add(ptButton3);
        bindingContext.bind("pixelTest", ptGroup);
        bindingContext.addPropertyChangeListener("pixelTest", evt -> interactor.getModel().fireModelChanged(true));

        plusButton = createToggleButton(TangoIcons.actions_list_add(TangoIcons.Res.R16));
        plusButton.setToolTipText("<html>Switches to pick mode 'plus':<br>" +
                                          "collect spectra used for inclusion");
        plusButton.addActionListener(e -> {
            if (interactor.getModel().getPickMode() != MagicWandModel.PickMode.PLUS) {
                interactor.getModel().setPickMode(MagicWandModel.PickMode.PLUS);
            } else {
                interactor.getModel().setPickMode(MagicWandModel.PickMode.SINGLE);
            }
        });

        minusButton = createToggleButton(TangoIcons.actions_list_remove(TangoIcons.Res.R16));
        minusButton.setToolTipText("<html>Switches to pick mode 'minus':<br>" +
                                           "collect spectra used for exclusion.");
        minusButton.addActionListener(e -> {
            if (interactor.getModel().getPickMode() != MagicWandModel.PickMode.MINUS) {
                interactor.getModel().setPickMode(MagicWandModel.PickMode.MINUS);
            } else {
                interactor.getModel().setPickMode(MagicWandModel.PickMode.SINGLE);
            }
        });

        bindingContext.addPropertyChangeListener("pickMode", evt -> interactor.getModel().fireModelChanged(false));

        final AbstractButton newButton = createButton(TangoIcons.actions_document_new(TangoIcons.R16));
        newButton.setToolTipText("New settings");
        newButton.addActionListener(e -> newSettings());

        final AbstractButton openButton = createButton(TangoIcons.actions_document_open(TangoIcons.R16));
        openButton.setToolTipText("Open settings");
        openButton.addActionListener(e -> openSettings((Component) e.getSource()));

        saveButton = createButton(TangoIcons.actions_document_save(TangoIcons.R16));
        saveButton.setToolTipText("Save settings");
        saveButton.addActionListener(e -> saveSettings((Component) e.getSource(), settingsFile));

        final AbstractButton saveAsButton = createButton(TangoIcons.actions_document_save_as(TangoIcons.R16));
        saveAsButton.setToolTipText("Save settings as");
        saveAsButton.addActionListener(e -> saveSettings((Component) e.getSource(), null));

        undoButton = createButton(TangoIcons.actions_edit_undo(TangoIcons.R16));
        undoButton.addActionListener(e -> {
            if (undoContext.canUndo()) {
                undoContext.undo();
            }
        });
        redoButton = createButton(TangoIcons.actions_edit_redo(TangoIcons.Res.R16));
        redoButton.addActionListener(e -> {
            if (undoContext.canRedo()) {
                undoContext.redo();
            }
        });

        undoContext.addUndoableEditListener(e -> updateState());

        clearButton = createButton(TangoIcons.actions_edit_clear(TangoIcons.Res.R16));
        clearButton.setName("clearButton");
        clearButton.setToolTipText("Removes all collected band ses."); /*I18N*/
        clearButton.addActionListener(e -> clearSpectra());

        AbstractButton filterButton = createButton(ImageUtilities.loadImageIcon("org/esa/snap/rcp/icons/Filter24.gif", false));
        filterButton.setName("filterButton");
        filterButton.setToolTipText("Select bands to included."); /*I18N*/
        filterButton.addActionListener(e -> showBandChooser());

        AbstractButton helpButton = createButton(TangoIcons.apps_help_browser(TangoIcons.Res.R16));
        helpButton.setName("helpButton");
        helpButton.setToolTipText("Help."); /*I18N*/

        JPanel toolPanelN = new JPanel(new GridLayout(-1, 2));
        toolPanelN.add(newButton);
        toolPanelN.add(openButton);
        toolPanelN.add(saveButton);
        toolPanelN.add(saveAsButton);
        toolPanelN.add(clearButton);
        toolPanelN.add(filterButton);
        toolPanelN.add(undoButton);
        toolPanelN.add(redoButton);
        toolPanelN.add(plusButton);
        toolPanelN.add(minusButton);

        JPanel toolPanelS = new JPanel(new GridLayout(-1, 2));
        toolPanelS.add(new JLabel());
        toolPanelS.add(helpButton);

        TableLayout tableLayout = new TableLayout(2);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTablePadding(2, 2);
        tableLayout.setCellColspan(1, 0, tableLayout.getColumnCount());
        Insets insets = new Insets(2, 10, 2, 2);
        //tableLayout.setRowPadding(3, insets);
        //tableLayout.setRowPadding(4, insets);
        //tableLayout.setRowPadding(5, insets);

        tableLayout.setCellPadding(3, 0, insets);
        tableLayout.setCellPadding(4, 0, insets);
        tableLayout.setCellPadding(5, 0, insets);
        tableLayout.setCellPadding(6, 0, insets);
        tableLayout.setCellPadding(3, 1, insets);
        tableLayout.setCellPadding(4, 1, insets);
        tableLayout.setCellPadding(5, 1, insets);

        JPanel subPanel = new JPanel(tableLayout);
        subPanel.add(toleranceLabel, cell(0, 0));
        subPanel.add(toleranceField, cell(0, 1));
        subPanel.add(toleranceSliderPanel, cell(1, 0));

        subPanel.add(stLabel, cell(2, 0));
        subPanel.add(stButton1, cell(3, 0));
        subPanel.add(stButton2, cell(4, 0));
        subPanel.add(stButton3, cell(5, 0));

        subPanel.add(ptLabel, cell(2, 1));
        subPanel.add(ptButton1, cell(3, 1));
        subPanel.add(ptButton2, cell(4, 1));
        subPanel.add(ptButton3, cell(5, 1));

        subPanel.add(normalizeCheckBox, cell(6, 0));
        subPanel.add(infoLabel, cell(6, 1));

        JPanel toolPanel = new JPanel(new BorderLayout(4, 4));
        toolPanel.add(toolPanelN, BorderLayout.NORTH);
        toolPanel.add(new JLabel(), BorderLayout.CENTER);
        toolPanel.add(toolPanelS, BorderLayout.SOUTH);

        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(new EmptyBorder(4, 4, 4, 4));
        panel.add(subPanel, BorderLayout.CENTER);
        panel.add(toolPanel, BorderLayout.EAST);

        adjustSlider();
        updateState();

        return panel;
    }

    private void newSettings() {
        if (proceedWithUnsavedChanges()) {
            settingsFile = null;
            interactor.getModel().clearSpectra();
        }
    }

    private void clearSpectra() {
        interactor.clearSpectra();
    }

    private void openSettings(Component parent) {
        if (proceedWithUnsavedChanges()) {
            File settingsFile = getFile(parent, this.settingsFile, true);
            if (settingsFile == null) {
                return;
            }
            try {
                MagicWandModel model = (MagicWandModel) createXStream().fromXML(FileUtils.readText(settingsFile));
                this.settingsFile = settingsFile;
                interactor.assignModel(model);
                undoContext.getUndoManager().discardAllEdits();
                interactor.setModelModified(false);
                updateState();
            } catch (IOException e) {
                String msg = MessageFormat.format("Failed to open settings:\n{0}", e.getMessage());
                JOptionPane.showMessageDialog(parent, msg, "I/O Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void saveSettings(Component parent, File settingsFile) {
        if (settingsFile == null) {
            settingsFile = getFile(parent, this.settingsFile, false);
            if (settingsFile == null) {
                return;
            }
        }
        try {
            try (FileWriter writer = new FileWriter(settingsFile)) {
                writer.write(createXStream().toXML(interactor.getModel()));
            }
            this.settingsFile = settingsFile;
            undoContext.getUndoManager().discardAllEdits();
            interactor.setModelModified(false);
            interactor.updateForm();
        } catch (IOException e) {
            String msg = MessageFormat.format("Failed to safe settings:\n{0}", e.getMessage());
            JOptionPane.showMessageDialog(parent, msg, "I/O Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private XStream createXStream() {
        XStream xStream = new XStream();
        xStream.setClassLoader(MagicWandModel.class.getClassLoader());
        xStream.alias("magicWandSettings", MagicWandModel.class);
        return xStream;
    }

    private static File getFile(Component parent, File file, boolean open) {
        JFileChooser fileChooser = new JFileChooser(Preferences.userRoot().get(PREFERENCES_KEY_LAST_DIR, System.getProperty("user.home")));
        if (file != null) {
            fileChooser.setSelectedFile(file);
        } else {
            fileChooser.setSelectedFile(new File(fileChooser.getCurrentDirectory(), "magic-wand-settings.xml"));
        }
        while (true) {
            int resp = open ? fileChooser.showOpenDialog(parent) : fileChooser.showSaveDialog(parent);
            File settingsFile = fileChooser.getSelectedFile();
            Preferences.userRoot().put(PREFERENCES_KEY_LAST_DIR, fileChooser.getCurrentDirectory().getPath());
            if (resp != JFileChooser.APPROVE_OPTION) {
                return null;
            }
            if (open || !settingsFile.exists()) {
                return settingsFile;
            }
            String msg = MessageFormat.format("Settings file ''{0}'' already exists." +
                                                      "\nOverwrite?", settingsFile.getName());
            int resp2 = JOptionPane.showConfirmDialog(parent, msg,
                                                      "File exists", JOptionPane.YES_NO_CANCEL_OPTION);
            if (resp2 == JOptionPane.YES_OPTION) {
                return settingsFile;
            }
            if (resp2 == JOptionPane.CANCEL_OPTION) {
                return null;
            }
        }
    }

    void updateState() {
        bindingContext.setComponentsEnabled("spectrumTransform", interactor.getModel().getBandCount() != 1);
        bindingContext.setComponentsEnabled("normalize", interactor.getModel().getBandCount() != 1);

        MagicWandModel model = interactor.getModel();

        infoLabel.setText(String.format("%d(+), %d(-), %d bands",
                                        model.getPlusSpectrumCount(),
                                        model.getMinusSpectrumCount(),
                                        model.getBandCount()));

        plusButton.setSelected(model.getPickMode() == MagicWandModel.PickMode.PLUS);
        minusButton.setSelected(model.getPickMode() == MagicWandModel.PickMode.MINUS);

        saveButton.setEnabled(settingsFile != null && interactor.isModelModified());
        clearButton.setEnabled(model.getSpectrumCount() > 0);

        undoButton.setEnabled(undoContext.canUndo());
        redoButton.setEnabled(undoContext.canRedo());
    }

    private void adjustSlider() {
        adjustingSlider = true;
        double tolerance = interactor.getModel().getTolerance();
        toleranceSlider.setValue(toleranceToSliderValue(tolerance));
        adjustingSlider = false;
    }

    private int toleranceToSliderValue(double tolerance) {
        MagicWandModel model = interactor.getModel();
        double minTolerance = model.getMinTolerance();
        double maxTolerance = model.getMaxTolerance();
        return (int) Math.round(Math.abs(TOLERANCE_SLIDER_RESOLUTION * ((tolerance - minTolerance) / (maxTolerance - minTolerance))));
    }

    private double sliderValueToTolerance(int sliderValue) {
        MagicWandModel model = interactor.getModel();
        double minTolerance = model.getMinTolerance();
        double maxTolerance = model.getMaxTolerance();
        return minTolerance + sliderValue * (maxTolerance - minTolerance) / TOLERANCE_SLIDER_RESOLUTION;
    }

    private static AbstractButton createButton(ImageIcon imageIcon) {
        return ToolButtonFactory.createButton(imageIcon, false);
    }

    private static AbstractButton createToggleButton(ImageIcon imageIcon) {
        return ToolButtonFactory.createButton(imageIcon, true);
    }

    void showBandChooser() {
        final ProductSceneView view = SnapApp.getDefault().getSelectedProductSceneView();
        if (view == null) {
            SnapDialogs.showInformation("Please select an image view first.", null);
            return;
        }
        Product product = view.getProduct();

        Band[] bands = product.getBands();
        if (bands.length == 0) {
            SnapDialogs.showInformation("No bands in product.", null);
            return;
        }

        Set<String> oldBandNames = new HashSet<>(interactor.getModel().getBandNames());
        Set<Band> oldBandSet = new HashSet<>();
        for (Band band : bands) {
            if (oldBandNames.contains(band.getName())) {
                oldBandSet.add(band);
            }
        }
        BandChooser bandChooser = new BandChooser(interactor.getOptionsWindow(),
                                                  "Available Bands and Tie Point Grids",
                                                  "",
                                                  bands,
                                                  oldBandSet.toArray(new Band[oldBandSet.size()]),
                                                  product.getAutoGrouping(), true);

        if (bandChooser.show() == ModalDialog.ID_OK) {
            Band[] newBands = bandChooser.getSelectedBands();
            Arrays.sort(newBands, new SpectralBandComparator());

            List<String> newBandNames = new ArrayList<>();
            for (Band newBand : newBands) {
                newBandNames.add(newBand.getName());
            }

            if (!oldBandNames.containsAll(newBandNames)
                    || !newBandNames.containsAll(oldBandNames)) {
                interactor.getModel().setBandNames(newBandNames);
            }
        }
    }

    private boolean proceedWithUnsavedChanges() {
        if (settingsFile != null && interactor.isModelModified()) {
            String msg = MessageFormat.format("You have unsaved changes." +
                                                      "\nProceed anyway?", settingsFile.getName());
            int resp = JOptionPane.showConfirmDialog(interactor.getOptionsWindow(), msg,
                                                     "New Settings", JOptionPane.YES_NO_OPTION);
            return resp == JOptionPane.YES_OPTION;
        }
        return true;
    }

}
