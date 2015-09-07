package org.esa.snap.rcp.actions.file;

import com.bc.ceres.swing.TableLayout;
import org.esa.snap.framework.dataio.DecodeQualification;
import org.esa.snap.framework.dataio.ProductIO;
import org.esa.snap.framework.dataio.ProductIOPlugInManager;
import org.esa.snap.framework.dataio.ProductReaderPlugIn;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.ui.GridBagUtils;
import org.esa.snap.framework.ui.SnapFileChooser;
import org.esa.snap.framework.ui.product.ProductSubsetDialog;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.SnapDialogs;
import org.esa.snap.util.io.FileUtils;
import org.esa.snap.util.io.SnapFileFilter;
import org.netbeans.api.progress.ProgressUtils;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 * @author Marco Peters
 */
class ProductOpener {

    private static final String PREFERENCES_KEY_LAST_PRODUCT_DIR = "last_product_open_dir";
    private static final String PREFERENCES_KEY_PREFIX_ALTERNATIVE_READER = "open_alternative_reader.";

    private static int numSubsetProducts = 0;

    private String fileFormat;
    private boolean useAllFileFilter;
    private boolean subsetImportEnabled;
    private File[] files;
    private boolean multiSelectionEnabled;

    static List<File> getOpenedProductFiles() {
        return Arrays.stream(SnapApp.getDefault().getProductManager().getProducts())
                .map(Product::getFileLocation)
                .filter(file -> file != null)
                .collect(Collectors.toList());
    }

    public void setFiles(File... files) {
        this.files = files;
    }

    public File[] getFiles() {
        return files;
    }

    void setFileFormat(String format) {
        fileFormat = format;
    }

    public String getFileFormat() {
        return fileFormat;
    }

    void setUseAllFileFilter(boolean useAllFileFilter){
        this.useAllFileFilter = useAllFileFilter;
    }

    public boolean isUseAllFileFilter() {
        return useAllFileFilter;
    }

    void setSubsetImportEnabled(boolean subsetImportEnabled) {
        this.subsetImportEnabled = subsetImportEnabled;
    }

    public boolean isSubsetImportEnabled() {
        return subsetImportEnabled;
    }

    public void setMultiSelectionEnabled(boolean multiSelectionEnabled) {
        this.multiSelectionEnabled = multiSelectionEnabled;
    }

    public boolean isMultiSelectionEnabled() {
        return multiSelectionEnabled;
    }

    public Boolean openProduct() {
        File[] configuredFiles = getFiles();
        if (configuredFiles != null) {
            return openProductFilesCheckOpened(getFileFormat(), configuredFiles);
        }

        Iterator<ProductReaderPlugIn> readerPlugIns;
        if (getFileFormat() != null) {
            readerPlugIns = ProductIOPlugInManager.getInstance().getReaderPlugIns(getFileFormat());
        } else {
            readerPlugIns = ProductIOPlugInManager.getInstance().getAllReaderPlugIns();
        }

        List<SnapFileFilter> filters = new ArrayList<>();
        while (readerPlugIns.hasNext()) {
            ProductReaderPlugIn readerPlugIn = readerPlugIns.next();
            SnapFileFilter snapFileFilter = readerPlugIn.getProductFileFilter();
            if (snapFileFilter != null) {
                filters.add(snapFileFilter);
            }
        }
        Collections.sort(filters, (f1, f2) -> {
            String d1 = f1.getDescription();
            String d2 = f2.getDescription();
            return d1 != null ? d1.compareTo(d2) : d2 == null ? 0 : 1;
        });
        if (filters.isEmpty()) {
            SnapDialogs.showError(Bundle.LBL_NoReaderFoundText());
            return false;
        }

        Preferences preferences = SnapApp.getDefault().getPreferences();
        ProductFileChooser fc = new ProductFileChooser(new File(preferences.get(PREFERENCES_KEY_LAST_PRODUCT_DIR, ".")));
        fc.setSubsetEnabled(isSubsetImportEnabled());
        fc.setDialogTitle(SnapApp.getDefault().getInstanceName() + " - " + Bundle.CTL_OpenProductActionName());
        fc.setAcceptAllFileFilterUsed(isUseAllFileFilter());
        filters.forEach((filter) -> {
            fc.addChoosableFileFilter(filter);
            if (getFileFormat() != null && getFileFormat().equals(filter.getFormatName())) {
                fc.setFileFilter(filter);
            }
        });
        fc.setDialogType(JFileChooser.OPEN_DIALOG);
        fc.setMultiSelectionEnabled(isMultiSelectionEnabled());
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);

        int returnVal = fc.showOpenDialog(null);
        if (returnVal != JFileChooser.APPROVE_OPTION) {
            // cancelled
            return null;
        }
        if (fc.getSubsetProduct() != null) {
            SnapApp.getDefault().getProductManager().addProduct(fc.getSubsetProduct());
            return true;
        }

        File[] files = fc.getSelectedFiles();
        if (files == null || files.length == 0) {
            // cancelled
            return null;
        }

        File currentDirectory = fc.getCurrentDirectory();
        if (currentDirectory != null) {
            preferences.put(PREFERENCES_KEY_LAST_PRODUCT_DIR, currentDirectory.toString());
        }

        String formatName = (fc.getFileFilter() instanceof SnapFileFilter)
                            ? ((SnapFileFilter) fc.getFileFilter()).getFormatName()
                            : null;

        return openProductFilesCheckOpened(formatName, files);
    }


    private static Boolean openProductFilesCheckOpened(final String formatName, final File... files) {
        List<File> openedFiles = getOpenedProductFiles();
        List<File> fileList = new ArrayList<>(Arrays.asList(files));
        for (File file : files) {
            if (openedFiles.contains(file)) {
                SnapDialogs.Answer answer = SnapDialogs.requestDecision(Bundle.CTL_OpenProductActionName(),
                                                                        MessageFormat.format("Product\n" +
                                                                                             "{0}\n" +
                                                                                             "is already opened.\n" +
                                                                                             "Do you want to open another instance?", file),
                                                                        true, null);
                if (answer == SnapDialogs.Answer.NO) {
                    fileList.remove(file);
                } else if (answer == SnapDialogs.Answer.CANCELLED) {
                    return null;
                }
            }
        }

        Boolean summaryStatus = true;
        for (File file : fileList) {
            String fileFormatName;
            if (formatName == null) {
                final List<PluginEntry> intendedPlugIns = getPluginsForFile(file, DecodeQualification.INTENDED);
                final List<PluginEntry> suitablePlugIns = getPluginsForFile(file, DecodeQualification.SUITABLE);

                if (intendedPlugIns.isEmpty() && suitablePlugIns.isEmpty()) {
                    SnapDialogs.showError(Bundle.LBL_NoReaderFoundText() + String.format("%nFile '%s' can not be opened.", file));
                    continue;
                } else if (intendedPlugIns.size() == 1) {
                    PluginEntry entry = intendedPlugIns.get(0);
                    fileFormatName = entry.plugin.getFormatNames()[0];
                } else if (intendedPlugIns.size() == 0 && suitablePlugIns.size() == 1) {
                    PluginEntry entry = suitablePlugIns.get(0);
                    fileFormatName = entry.plugin.getFormatNames()[0];
                } else {
                    Collections.sort(intendedPlugIns);
                    Collections.sort(suitablePlugIns);
                    fileFormatName = getUserSelection(intendedPlugIns, suitablePlugIns);
                    if (fileFormatName == null) { // User clicked cancel
                        return null;
                    }
                }
            } else {
                fileFormatName = formatName;
            }


            Boolean status = openProductFileDoNotCheckOpened(file, fileFormatName);
            if (status == null) {
                // Cancelled
                summaryStatus = null;
                break;
            } else if (!Boolean.TRUE.equals(status)) {
                summaryStatus = status;
            }
        }

        return summaryStatus;
    }

    private static List<PluginEntry> getPluginsForFile(File file, DecodeQualification desiredQualification) {
        final Iterator<ProductReaderPlugIn> allReaderPlugIns = ProductIOPlugInManager.getInstance().getAllReaderPlugIns();
        final List<PluginEntry> possiblePlugIns = new ArrayList<>();
        allReaderPlugIns.forEachRemaining(plugIn -> {
            final DecodeQualification qualification = plugIn.getDecodeQualification(file);
            if (qualification == desiredQualification) {
                possiblePlugIns.add(new PluginEntry(plugIn, qualification));
            }
        });
        return possiblePlugIns;
    }

    private static String getUserSelection(List<PluginEntry> intendedPlugins, List<PluginEntry> suitablePlugIns) {
        final PluginEntry leadPlugin;
        if (!intendedPlugins.isEmpty()) {
            leadPlugin = intendedPlugins.get(0);
        } else {
            leadPlugin = suitablePlugIns.get(0);
        }
        String preferencesKey = PREFERENCES_KEY_PREFIX_ALTERNATIVE_READER + leadPlugin.plugin.getClass().getSimpleName();
        final String storedSelection = SnapApp.getDefault().getPreferences().get(preferencesKey, null);
        if (storedSelection != null) {
            return storedSelection;
        }

        final TableLayout layout = new TableLayout(1);
        layout.setTableAnchor(TableLayout.Anchor.WEST);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setTablePadding(4, 4);

        final JPanel readerSelectionPanel = new JPanel(layout);
        readerSelectionPanel.add(new JLabel("<html>Multiple readers are available for the selected file.<br>" +
                                            "The readers might interpret the data differently.<br>" +
                                            "Please select one of the following:"));
        final JComboBox<ProductReaderPlugIn> pluginsCombobox = new JComboBox<>();
        DefaultListCellRenderer cellRenderer = new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setText(((ProductReaderPlugIn) value).getDescription(Locale.getDefault()));
                return this;
            }
        };
        pluginsCombobox.setRenderer(cellRenderer);
        for (PluginEntry plugin : intendedPlugins) {
            pluginsCombobox.addItem(plugin.plugin);
        }
        for (PluginEntry plugin : suitablePlugIns) {
            pluginsCombobox.addItem(plugin.plugin);
        }
        readerSelectionPanel.add(pluginsCombobox);
        JCheckBox decisionCheckBox = new JCheckBox("Remember my decision and don't ask again.", false);
        decisionCheckBox.setHorizontalAlignment(SwingConstants.RIGHT);
        readerSelectionPanel.add(decisionCheckBox);

        NotifyDescriptor d = new NotifyDescriptor(readerSelectionPanel,
                                                  SnapDialogs.getDialogTitle("Multiple Readers Available"),
                                                  NotifyDescriptor.OK_CANCEL_OPTION, NotifyDescriptor.QUESTION_MESSAGE, null,
                                                  NotifyDescriptor.OK_OPTION);
        Object answer = DialogDisplayer.getDefault().notify(d);
        if (NotifyDescriptor.OK_OPTION.equals(answer)) {
            boolean storeResult = decisionCheckBox.isSelected();
            String selectedFormatName = ((ProductReaderPlugIn) pluginsCombobox.getSelectedItem()).getFormatNames()[0];
            if (storeResult) {
                SnapApp.getDefault().getPreferences().put(preferencesKey, selectedFormatName);
            }
            return selectedFormatName;
        }

        return null;
    }


    private static Boolean openProductFileDoNotCheckOpened(File file, String formatName) {
        SnapApp.getDefault().setStatusBarMessage(MessageFormat.format("Reading product ''{0}''...", file.getName()));

        AtomicBoolean cancelled = new AtomicBoolean();
        ReadProductOperation operation = new ReadProductOperation(file, formatName);
        ProgressUtils.runOffEventDispatchThread(operation, Bundle.CTL_OpenProductActionName(), cancelled, true, 50, 3000);

        SnapApp.getDefault().setStatusBarMessage("");

        if (cancelled.get()) {
            return null;
        }

        return operation.getStatus();
    }

    private static class PluginEntry implements Comparable<PluginEntry> {

        ProductReaderPlugIn plugin;
        DecodeQualification qualification;

        public PluginEntry(ProductReaderPlugIn plugin, DecodeQualification qualification) {
            this.plugin = plugin;
            this.qualification = qualification;
        }

        @Override
        public int compareTo(PluginEntry other) {
            final int qualificationComparison = this.qualification.compareTo(other.qualification);
            if (qualificationComparison == 0) {
                final String description1 = this.plugin.getDescription(Locale.getDefault());
                final String description2 = other.plugin.getDescription(Locale.getDefault());
                return description1.compareTo(description2);
            } else {
                return qualificationComparison;
            }
        }
    }

    protected class ProductFileChooser extends SnapFileChooser {

        private static final long serialVersionUID = -8122437634943074658L;

        private JButton subsetButton;
        private Product subsetProduct;

        private JLabel sizeLabel;
        private boolean useSubset;

        public ProductFileChooser(File currentDirectory) {
            super(currentDirectory);
            setDialogType(OPEN_DIALOG);
        }

        /**
         * File chooser only returns a product, if a product subset was created.
         *
         * @return the product subset or null
         */
        public Product getSubsetProduct() {
            return subsetProduct;
        }

        public boolean isSubsetEnabled() {
            return useSubset;
        }

        public void setSubsetEnabled(boolean useSubset) {
            this.useSubset = useSubset;
        }

        @Override
        public int showDialog(Component parent, String approveButtonText) {
            if (isSubsetEnabled()) {
                addSubsetAcessory();
                validate();
            }
            clearCurrentProduct();
            return super.showDialog(parent, approveButtonText);
        }

        private void addSubsetAcessory() {
            subsetButton = new JButton("Subset...");
            subsetButton.setMnemonic('S');
            subsetButton.addActionListener(e -> openProductSubsetDialog());
            subsetButton.setEnabled(false);


            sizeLabel = new JLabel("0 M");
            sizeLabel.setHorizontalAlignment(JLabel.RIGHT);
            JPanel panel = GridBagUtils.createPanel();
            GridBagConstraints gbc = GridBagUtils.createConstraints(
                    "fill=HORIZONTAL,weightx=1,anchor=NORTHWEST,insets.left=7,insets.right=7,insets.bottom=4");
            GridBagUtils.addToPanel(panel, subsetButton, gbc, "gridy=0");
            GridBagUtils.addToPanel(panel, sizeLabel, gbc, "gridy=1");
            GridBagUtils.addVerticalFiller(panel, gbc);

            setAccessory(panel);

            addPropertyChangeListener(e -> {
                String prop = e.getPropertyName();
                if (prop.equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)) {
                    clearCurrentProduct();
                    subsetButton.setEnabled(true);
                } else if (prop.equals(JFileChooser.DIRECTORY_CHANGED_PROPERTY)) {
                    clearCurrentProduct();
                    subsetButton.setEnabled(false);
                }
                updateState();
            });
        }

        private void updateState() {
            setApproveButtonText(Bundle.CTL_ImportProductActionName());
            setApproveButtonMnemonic('I');
            setApproveButtonToolTipText("Imports the entire product.");
            File file = getSelectedFile();
            if (file != null && file.isFile()) {
                long fileSize = Math.round(file.length() / (1024.0 * 1024.0));
                if (fileSize >= 1) {
                    sizeLabel.setText("File size: " + fileSize + " M");
                } else {
                    sizeLabel.setText("File size: < 1 M");
                }
            } else {
                sizeLabel.setText("");
            }
        }

        private void clearCurrentProduct() {
            subsetProduct = null;
        }

        private void openProductSubsetDialog() {

            File file = getSelectedFile();
            if (file == null) {
                // Should not come here...
                return;
            }

            Product product = null;
            try {
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                product = ProductIO.readProduct(file);
            } catch (IOException e) {
                SnapDialogs.showError("The product could not be read:\n" + e.getMessage());
            } finally {
                setCursor(Cursor.getDefaultCursor());
            }

            if (product != null) {
                boolean approve = openProductSubsetDialog(product);
                if (approve) {
                    approveSelection();
                }
            }

            updateState();
        }

        private boolean openProductSubsetDialog(Product product) {
            subsetProduct = null;
            boolean approve = false;
            if (product != null) {
                Frame mainFrame = SnapApp.getDefault().getMainFrame();
                ProductSubsetDialog productSubsetDialog = new ProductSubsetDialog(mainFrame, product);
                if (productSubsetDialog.show() == ProductSubsetDialog.ID_OK) {
                    try {
                        final String newProductName = createNewProductName(product.getName(), numSubsetProducts++);
                        subsetProduct = product.createSubset(productSubsetDialog.getProductSubsetDef(),
                                                             newProductName,
                                                             null);
                        approve = true;
                    } catch (IOException e) {
                        SnapDialogs.showError("Could not create subset:\n" + e.getMessage());
                    }
                }
            }
            return approve;
        }

        private String createNewProductName(String sourceProductName, int productIndex) {
            String newNameBase = "";
            if (sourceProductName != null && sourceProductName.length() > 0) {
                newNameBase = FileUtils.exchangeExtension(sourceProductName, "");
            }
            String newNamePrefix = "subset";
            String newProductName;
            if (newNameBase.length() > 0) {
                newProductName = newNamePrefix + "_" + productIndex + "_" + newNameBase;
            } else {
                newProductName = newNamePrefix + "_" + productIndex;
            }
            return newProductName;
        }

    }

}
