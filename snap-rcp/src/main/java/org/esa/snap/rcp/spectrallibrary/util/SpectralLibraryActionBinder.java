package org.esa.snap.rcp.spectrallibrary.util;

import com.bc.ceres.glayer.support.ImageLayer;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.rcp.spectrallibrary.controller.SpectralLibraryController;
import org.esa.snap.rcp.spectrallibrary.model.SpectralLibraryViewModel;
import org.esa.snap.rcp.spectrallibrary.model.UiStatus;
import org.esa.snap.rcp.spectrallibrary.ui.SpectralLibraryPanel;
import org.esa.snap.speclib.model.*;
import org.esa.snap.ui.PixelPositionListener;
import org.esa.snap.ui.product.ProductSceneView;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.*;
import java.util.List;


public class SpectralLibraryActionBinder {


    private final SpectralLibraryViewModel vm;
    private final SpectralLibraryController controller;
    private final SpectralLibraryPanel panel;
    private final ProductSceneViewProvider viewProvider;

    private volatile int cursorX = -1;
    private volatile int cursorY = -1;
    private volatile int cursorLevel = -1;
    private volatile boolean cursorValid = false;

    private ProductSceneView currentView;

    private final PixelPositionListener cursorTracker = new PixelPositionListener() {
        @Override
        public void pixelPosChanged(ImageLayer imageLayer,
                                    int pixelX, int pixelY, int level,
                                    boolean pixelPosValid, MouseEvent e) {
            cursorX = pixelX;
            cursorY = pixelY;
            cursorLevel = level;
            cursorValid = pixelPosValid;
        }

        @Override
        public void pixelPosNotAvailable() {
            cursorValid = false;
            cursorX = cursorY = cursorLevel = -1;
        }
    };

    private final MouseAdapter clickListener = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (!panel.getExtractAtCursorToggle().isSelected()) {
                return;
            }
            if (!SwingUtilities.isLeftMouseButton(e)) {
                return;
            }

            if (!cursorValid || cursorX < 0 || cursorY < 0) {
                vm.setStatus(UiStatus.warn("Cursor position not available"));
                return;
            }

            extractCursorOnce();
        }
    };

    public interface ProductSceneViewProvider {
        ProductSceneView getSelectedProductSceneView();
    }


    public SpectralLibraryActionBinder(SpectralLibraryViewModel vm,
                                       SpectralLibraryController controller,
                                       SpectralLibraryPanel panel,
                                       ProductSceneViewProvider viewProvider) {
        this.vm = vm;
        this.controller = controller;
        this.panel = panel;
        this.viewProvider = viewProvider;
    }


    public void bind() {
        wireToolbarBasics();
        wireImportExport();
        wireExtractionPins();
        wireExtractionCursor();
        wirePreviewAddButtons();
    }


    private void wireToolbarBasics() {
        panel.getRefreshButton().addActionListener(e -> {
            controller.reloadLibraries();
            controller.refreshActiveLibraryProfiles();
            vm.setStatus(UiStatus.info("Refreshed"));
        });

        panel.getDeleteLibraryButton().addActionListener(e -> {
                int answer = JOptionPane.showConfirmDialog(panel, "Are you sure you want to delete this library?");
                if (answer == JOptionPane.YES_OPTION) {
                    vm.getActiveLibraryId().ifPresent(controller::deleteLibrary);
                }
            }
        );

        panel.getRenameLibraryButton().addActionListener(e -> {
            UUID uuid = vm.getActiveLibraryId().orElse(null);
            String name = JOptionPane.showInputDialog(panel, "New Library name:", "New_Library");
            if (name == null) {
                return;
            }
            name = name.trim();
            if (name.isEmpty()) {
                return;
            }

            controller.renameLibrary(uuid, name);
        });

        panel.getRemoveSelectedProfilesButton().addActionListener(e -> {
                UUID libId = vm.getActiveLibraryId().orElse(null);
                if (libId == null) {
                    return;
                }

                int[] viewRows = panel.getLibraryTable().getSelectedRows();
                if (viewRows == null || viewRows.length == 0) {
                    vm.setStatus(UiStatus.warn("No profiles selected"));
                    return;
                }

                List<UUID> ids = new ArrayList<>(viewRows.length);
                for (int viewRow : viewRows) {
                    int modelRow = panel.getLibraryTable().convertRowIndexToModel(viewRow);
                    ids.add(panel.getLibraryTableModel().getIdAt(modelRow));
                }
                controller.removeLibraryProfiles(libId, ids);
            }
        );

        panel.getPreviewSelectedProfilesButton().addActionListener(e -> {
            int[] viewRows = panel.getLibraryTable().getSelectedRows();
            if (viewRows == null || viewRows.length == 0) {
                vm.setStatus(UiStatus.warn("No profiles selected"));
                return;
            }

            controller.clearPreview();

            List<SpectralProfile> selected = new ArrayList<>(viewRows.length);
            for (int viewRow : viewRows) {
                int modelRow = panel.getLibraryTable().convertRowIndexToModel(viewRow);
                SpectralProfile p = panel.getLibraryTableModel().getAt(modelRow);
                if (p != null) {
                    selected.add(p);
                }
            }

            controller.setPreviewProfiles(selected);
        });

        panel.getAddAttributeButton().addActionListener(e -> {
            Optional<UUID> libId = vm.getActiveLibraryId();
            if (libId.isEmpty()) {
                vm.setStatus(UiStatus.warn("No active library"));
                return;
            }

            Optional<AttributeDialogResult> resOpt = showAddAttributeDialog(panel);
            if (resOpt.isEmpty()) {
                return;
            }

            AttributeDialogResult res = resOpt.get();

            if (res.key == null || res.key.isBlank()) {
                vm.setStatus(UiStatus.warn("Attribute key is empty"));
                return;
            }

            AttributeValue defaultValue = null;
            try {
                defaultValue = parseAttributeValue(res.type, res.defaultValueText);
            } catch (IllegalArgumentException ex) {
                vm.setStatus(UiStatus.warn("Invalid default value: " + ex.getMessage()));
                return;
            }

            AttributeDef def = new AttributeDef(
                    res.key.trim(),
                    res.type,
                    false,
                    defaultValue,
                    null,
                    null
            );

            AttributeValue fillValue;
            try {
                fillValue = parseAttributeValue(res.type, res.defaultValueText);
            } catch (IllegalArgumentException ex) {
                vm.setStatus(UiStatus.warn("Invalid default value: " + ex.getMessage()));
                return;
            }

            controller.addAttributeToActiveLibrary(def, fillValue);
            }
        );

        panel.getClearPreviewButton().addActionListener(e -> controller.clearPreview());

        panel.getCreateFromProductButton().addActionListener(e -> {
            ProductSceneView view = viewProvider.getSelectedProductSceneView();
            if (view == null || view.getProduct() == null) {
                vm.setStatus(UiStatus.warn("No product view selected"));
                return;
            }
            Product product = view.getProduct();

            List<Band> bands = Arrays.asList(product.getBands());
            String name = JOptionPane.showInputDialog(panel, "Library name:", "Library");
            if (name == null) {
                return;
            }
            name = name.trim();
            if (name.isEmpty()) {
                return;
            }

            controller.createLibraryFromBands(name, bands);
        });

        panel.getLibraryCombo().addActionListener(e -> {
            Object sel = panel.getLibraryCombo().getSelectedItem();
            if (sel instanceof SpectralLibrary lib) {
                controller.setActiveLibrary(lib.getId());
            }
        });
    }

    private void wireImportExport() {
        panel.getExportButton().addActionListener(e -> doExport());
        panel.getImportButton().addActionListener(e -> doImport());
    }


    private void wireExtractionPins() {
        panel.getExtractSelectedPinsButton().addActionListener(e -> extractPins(true));
        panel.getExtractAllPinsButton().addActionListener(e -> extractPins(false));
    }

    private void extractPins(boolean selectedOnly) {
        ProductSceneView view = viewProvider.getSelectedProductSceneView();
        if (view == null || view.getProduct() == null) {
            vm.setStatus(UiStatus.warn("No product view selected"));
            return;
        }
        Product product = view.getProduct();

        SpectralLibrary lib = getSelectedLibraryFromCombo();
        if (lib == null) {
            vm.setStatus(UiStatus.warn("No active library (axis required)"));
            return;
        }

        List<Band> bands = BandSelectionUtils.getSpectralBands(product);
        if (bands.isEmpty()) {
            vm.setStatus(UiStatus.warn("No spectral bands in product"));
            return;
        }

        List<Placemark> pins = selectedOnly
                ? Arrays.asList(view.getSelectedPins())
                : Arrays.asList(product.getPinGroup().toArray(new Placemark[0]));

        pins = pins.stream().filter(p -> p != null).toList();
        if (pins.isEmpty()) {
            vm.setStatus(UiStatus.warn(selectedOnly ? "No selected pins" : "No pins in product"));
            return;
        }

        int level = 0;
        String yUnit = lib.getDefaultYUnit().orElse("value");

        controller.extractPreviewFromPins(
                product,
                lib.getAxis(),
                yUnit,
                bands,
                pins,
                level,
                "pin"
        );
    }


    private void wireExtractionCursor() {
        panel.getExtractAtCursorToggle().addActionListener(e -> {
            boolean armed = panel.getExtractAtCursorToggle().isSelected();
            if (armed) {
                attachCursorHooks();
            }
            else {
                detachCursorHooks();
            }
        });
    }

    private void attachCursorHooks() {
        ProductSceneView view = viewProvider.getSelectedProductSceneView();
        if (view == null) {
            vm.setStatus(UiStatus.warn("No product view selected"));
            panel.getExtractAtCursorToggle().setSelected(false);
            return;
        }

        detachCursorHooks();
        currentView = view;
        currentView.addPixelPositionListener(cursorTracker);
        currentView.getLayerCanvas().addMouseListener(clickListener);

        vm.setStatus(UiStatus.info("Cursor extraction armed (click in view)"));
    }

    private void detachCursorHooks() {
        if (currentView != null) {
            currentView.removePixelPositionListener(cursorTracker);
            currentView.getLayerCanvas().removeMouseListener(clickListener);
        }
        currentView = null;
        cursorValid = false;
        cursorX = cursorY = cursorLevel = -1;
    }

    private void extractCursorOnce() {
        ProductSceneView view = currentView != null ? currentView : viewProvider.getSelectedProductSceneView();
        if (view == null || view.getProduct() == null) {
            return;
        }

        Product product = view.getProduct();

        SpectralLibrary lib = getSelectedLibraryFromCombo();
        if (lib == null) {
            vm.setStatus(UiStatus.warn("No active library (axis required)"));
            return;
        }

        List<Band> bands = BandSelectionUtils.getSpectralBands(product);
        if (bands.isEmpty()) {
            vm.setStatus(UiStatus.warn("No spectral bands in product"));
            return;
        }

        int level = (cursorLevel >= 0) ? cursorLevel : ImageLayer.getLevel(view.getRaster().getMultiLevelModel(), view.getViewport());
        String yUnit = lib.getDefaultYUnit().orElse("value");

        controller.extractPreviewAtCursor(
                product,
                lib.getAxis(),
                yUnit,
                bands,
                cursorX,
                cursorY,
                level,
                "cursor_" + cursorX + "_" + cursorY
        );
    }


    private void wirePreviewAddButtons() {
        panel.getAddAllPreviewButton().addActionListener(e -> controller.addAllPreviewToLibrary());
        panel.getAddSelectedPreviewButton().addActionListener(e -> controller.addSelectedPreviewToLibrary());
    }


    private SpectralLibrary getSelectedLibraryFromCombo() {
        Object sel = panel.getLibraryCombo().getSelectedItem();
        return (sel instanceof SpectralLibrary lib) ? lib : null;
    }




    private void doImport() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Import Spectral Library");
        fileChooser.setApproveButtonText("Import");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Spectral Libraries (*.sli, *.hdr, *)", "sli", "hdr"));

        int ok = fileChooser.showOpenDialog(panel);
        if (ok != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = fileChooser.getSelectedFile();
        controller.importLibraryFromFile(file);
    }

    private void doExport() {
        if (vm.getActiveLibraryId().isEmpty()) {
            vm.setStatus(UiStatus.warn("No active library"));
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Spectral Library");
        fileChooser.setApproveButtonText("Export");
        fileChooser.setFileFilter(new FileNameExtensionFilter("ENVI Spectral Library (*.sli)", "sli"));

        String base = "spectral-library";
        Object sel = panel.getLibraryCombo().getSelectedItem();
        if (sel instanceof SpectralLibrary lib && lib.getName() != null && !lib.getName().isBlank()) {
            base = lib.getName().trim().replaceAll("[\\\\/:*?\"<>|]", "_");
        }
        fileChooser.setSelectedFile(new File(base + ".sli"));

        int ok = fileChooser.showSaveDialog(panel);
        if (ok != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = fileChooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".sli")) {
            file = new File(file.getParentFile(), file.getName() + ".sli");
        }

        controller.exportActiveLibraryToFile(file);
    }


    private static Optional<AttributeDialogResult> showAddAttributeDialog(Component parent) {
        JTextField keyField = new JTextField(20);

        AttributeType[] allowed = Arrays.stream(AttributeType.values())
                .filter(t -> t != AttributeType.EMBEDDED_SPECTRUM)
                .toArray(AttributeType[]::new);
        JComboBox<AttributeType> typeCombo = new JComboBox<>(allowed);
        typeCombo.setSelectedItem(AttributeType.STRING);

        JTextField defaultField = new JTextField(20);
        JLabel defaultHint = new JLabel(" ");
        defaultHint.setForeground(Color.GRAY);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;

        int r = 0;
        gc.gridx = 0; gc.gridy = r; gc.weightx = 0;
        form.add(new JLabel("Key:"), gc);
        gc.gridx = 1; gc.gridy = r; gc.weightx = 1;
        form.add(keyField, gc);

        r++;
        gc.gridx = 0; gc.gridy = r; gc.weightx = 0;
        form.add(new JLabel("Type:"), gc);
        gc.gridx = 1; gc.gridy = r; gc.weightx = 1;
        form.add(typeCombo, gc);

        r++;
        gc.gridx = 0; gc.gridy = r; gc.weightx = 0;
        form.add(new JLabel("Default value:"), gc);
        gc.gridx = 1; gc.gridy = r; gc.weightx = 1;
        form.add(defaultField, gc);

        r++;
        gc.gridx = 1; gc.gridy = r; gc.weightx = 1;
        form.add(defaultHint, gc);

        Runnable updateHint = () -> {
            AttributeType t = (AttributeType) typeCombo.getSelectedItem();
            if (t == null) {
                t = AttributeType.STRING;
            }

            String ex = exampleFor(t);
            defaultHint.setText(ex.isBlank() ? " " : ("Example: " + ex));

            defaultField.setEnabled(true);

            if (defaultField.getText().trim().isEmpty() || isShowingPlaceholder(defaultField)) {
                installPlaceholder(defaultField, ex);
            }
        };
        typeCombo.addActionListener(e -> updateHint.run());
        updateHint.run();

        int rc = JOptionPane.showConfirmDialog(
                parent,
                form,
                "Add Attribute to Library",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (rc != JOptionPane.OK_OPTION) {
            return Optional.empty();
        }

        String key = keyField.getText();
        AttributeType type = (AttributeType) typeCombo.getSelectedItem();
        if (type == null) {
            type = AttributeType.STRING;
        }

        String rawDefault = defaultField.getText();
        if (isShowingPlaceholder(defaultField)) {
            rawDefault = (String) defaultField.getClientProperty("placeholderText");
        }

        return Optional.of(new AttributeDialogResult(
                key,
                type,
                rawDefault
        ));
    }

    private static String exampleFor(AttributeType t) {
        if (t == null) {
            return "";
        }
        return switch (t) {
            case STRING -> "attribute_value";
            case INT -> "42";
            case LONG -> "12345678900";
            case DOUBLE -> "0.123";
            case BOOLEAN -> "true";
            case STRING_LIST -> "a,b,c";
            case DOUBLE_ARRAY -> "0.1,0.2,0.3";
            case INT_ARRAY -> "1,2,3";
            case STRING_MAP -> "k1=v1,k2=v2";
            default -> "";
        };
    }

    private static void installPlaceholder(JTextField field, String placeholder) {
        field.putClientProperty("placeholderText", placeholder == null ? "" : placeholder);

        if (field.getText().trim().isEmpty() && !placeholder.isBlank()) {
            field.setForeground(Color.GRAY);
            field.setText(placeholder);
        }

        for (var l : field.getFocusListeners()) {
            if (l.getClass().getName().equals(PlaceholderFocusListener.class.getName())) {
                return;
            }
        }
        field.addFocusListener(new PlaceholderFocusListener());
    }

    private static boolean isShowingPlaceholder(JTextField field) {
        String ph = (String) field.getClientProperty("placeholderText");
        return ph != null
                && !ph.isBlank()
                && Color.GRAY.equals(field.getForeground())
                && ph.equals(field.getText());
    }



    private static AttributeValue parseAttributeValue(AttributeType type, String raw) {
        String s = raw == null ? "" : raw.trim();

        if (s.isEmpty()) {
            throw new IllegalArgumentException("value must not be empty");
        }

        return switch (type) {
            case STRING -> AttributeValue.ofString(s);
            case INT -> {
                try {
                    yield AttributeValue.ofInt(Integer.parseInt(s));
                }
                catch (NumberFormatException e) { throw new IllegalArgumentException("expected int"); }
            }
            case LONG -> {
                try {
                    yield AttributeValue.ofLong(Long.parseLong(s));
                }
                catch (NumberFormatException e) {
                    throw new IllegalArgumentException("expected long");
                }
            }
            case DOUBLE -> {
                try {
                    yield AttributeValue.ofDouble(Double.parseDouble(s));
                }
                catch (NumberFormatException e) {
                    throw new IllegalArgumentException("expected double");
                }
            }
            case BOOLEAN -> {
                String sl = s.toLowerCase();
                if (sl.equals("true") || sl.equals("1") || sl.equals("yes")) {
                    yield AttributeValue.ofBoolean(true);
                }
                if (sl.equals("false") || sl.equals("0") || sl.equals("no")) {
                    yield AttributeValue.ofBoolean(false);
                }
                throw new IllegalArgumentException("expected true/false");
            }
            case STRING_LIST -> AttributeValue.ofStringList(parseStringList(s));
            case DOUBLE_ARRAY -> AttributeValue.ofDoubleArray(parseDoubleArray(s));
            case INT_ARRAY -> AttributeValue.ofIntArray(parseIntArray(s));
            case STRING_MAP -> AttributeValue.ofStringMap(parseStringMap(s));
            case EMBEDDED_SPECTRUM -> throw new IllegalArgumentException("EMBEDDED_SPECTRUM is not supported via manual input yet");
        };
    }

    private static List<String> parseStringList(String s) {
        // comma-separated: a,b,c
        String[] parts = s.split(",");
        List<String> out = new ArrayList<>();
        for (String p : parts) {
            String t = p.trim();
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        if (out.isEmpty()) {
            throw new IllegalArgumentException("string list is empty");
        }
        return out;
    }

    private static double[] parseDoubleArray(String s) {
        // comma-separated numbers
        String[] parts = s.split(",");
        double[] out = new double[parts.length];
        int n = 0;
        for (String p : parts) {
            String t = p.trim();
            if (t.isEmpty()) {
                continue;
            }
            try {
                out[n++] = Double.parseDouble(t);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("invalid double: " + t);
            }
        }
        if (n == 0) {
            throw new IllegalArgumentException("double array is empty");
        }
        if (n == out.length) {
            return out;
        }
        return Arrays.copyOf(out, n);
    }

    private static int[] parseIntArray(String s) {
        // comma-separated numbers
        String[] parts = s.split(",");
        int[] out = new int[parts.length];
        int n = 0;
        for (String p : parts) {
            String t = p.trim();
            if (t.isEmpty()) {
                continue;
            }
            try {
                out[n++] = Integer.parseInt(t);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("invalid int: " + t);
            }
        }
        if (n == 0) {
            throw new IllegalArgumentException("int array is empty");
        }
        if (n == out.length) {
            return out;
        }
        return Arrays.copyOf(out, n);
    }

    private static Map<String, String> parseStringMap(String s) {
        // format: key1=value1, key2=value2
        // comma-separated pairs, '=' separates key/value
        String[] pairs = s.split(",");
        Map<String, String> out = new LinkedHashMap<>();
        for (String pair : pairs) {
            String t = pair.trim();
            if (t.isEmpty()) {
                continue;
            }

            int eq = t.indexOf('=');
            if (eq <= 0) {
                throw new IllegalArgumentException("invalid map entry (expected key=value): " + t);
            }

            String k = t.substring(0, eq).trim();
            String v = t.substring(eq + 1).trim();

            if (k.isEmpty()) {
                throw new IllegalArgumentException("empty map key");
            }
            out.put(k, v);
        }
        if (out.isEmpty()) {
            throw new IllegalArgumentException("map is empty");
        }
        return out;
    }


    private static final class AttributeDialogResult {
        final String key;
        final AttributeType type;
        final String defaultValueText;

        private AttributeDialogResult(String key, AttributeType type, String defaultValueText) {
            this.key = key;
            this.type = type;
            this.defaultValueText = defaultValueText;
        }
    }

    private static final class PlaceholderFocusListener extends FocusAdapter {
        @Override
        public void focusGained(FocusEvent e) {
            JTextField field = (JTextField) e.getComponent();
            if (isShowingPlaceholder(field)) {
                field.setText("");
                field.setForeground(UIManager.getColor("TextField.foreground"));
            }
        }

        @Override
        public void focusLost(FocusEvent e) {
            JTextField field = (JTextField) e.getComponent();
            String ph = (String) field.getClientProperty("placeholderText");
            if (ph == null || ph.isBlank()) {
                return;
            }

            if (field.getText().trim().isEmpty()) {
                field.setForeground(Color.GRAY);
                field.setText(ph);
            }
        }
    }
}

