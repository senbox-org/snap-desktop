package org.esa.snap.rcp.actions.file;

import com.bc.ceres.swing.TableLayout;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductIOPlugInManager;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.SnapDialogs;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.progress.ProgressUtils;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.Component;
import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 * @author Marco Peters
 */
class ProductOpener {

    public static final String PREFERENCES_KEY_LAST_PRODUCT_DIR = "last_product_open_dir";
    private static final String PREFERENCES_KEY_PREFIX_ALTERNATIVE_READER = "open_alternative_reader.";

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

    void setUseAllFileFilter(boolean useAllFileFilter) {
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
            if (!readerPlugIns.hasNext()) {
                SnapDialogs.showError(
                        Bundle.LBL_NoReaderFoundText() + String.format("%nCan't find reader for the given format '%s'.", getFileFormat()));
                return false;
            }
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
        String userHomePath = SystemUtils.getUserHomeDir().getAbsolutePath();
        ProductFileChooser fc = new ProductFileChooser(new File(preferences.get(PREFERENCES_KEY_LAST_PRODUCT_DIR, userHomePath)));
        fc.setSubsetEnabled(isSubsetImportEnabled());
        fc.setDialogTitle(SnapApp.getDefault().getInstanceName() + " - " + Bundle.CTL_OpenProductActionName());
        fc.setAcceptAllFileFilterUsed(isUseAllFileFilter());
        filters.forEach((filter) -> {
            fc.addChoosableFileFilter(filter);
            if (getFileFormat() != null && getFileFormat().equals(filter.getFormatName())) {
                fc.setFileFilter(filter);
            }
        });
        fc.setMultiSelectionEnabled(isMultiSelectionEnabled());
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);

        int returnVal = fc.showOpenDialog(SnapApp.getDefault().getMainFrame());
        if (returnVal != JFileChooser.APPROVE_OPTION) {
            // cancelled
            return null;
        }

        File[] files = getSelectedFiles(fc);

        if (files == null || files.length == 0) {
            // cancelled
            return null;
        }

        File currentDirectory = fc.getCurrentDirectory();
        if (currentDirectory != null) {
            preferences.put(PREFERENCES_KEY_LAST_PRODUCT_DIR, currentDirectory.toString());
        }

        if (fc.getSubsetProduct() != null) {
            SnapApp.getDefault().getProductManager().addProduct(fc.getSubsetProduct());
            return true;
        }

        String formatName = (fc.getFileFilter() instanceof SnapFileFilter)
                            ? ((SnapFileFilter) fc.getFileFilter()).getFormatName()
                            : null;

        return openProductFilesCheckOpened(formatName, files);
    }

    private File[] getSelectedFiles(ProductFileChooser fc) {
        File[] files = new File[0];
        if (isMultiSelectionEnabled()) {
            files = fc.getSelectedFiles();
        } else {
            File file = fc.getSelectedFile();
            if (file != null) {
                files = new File[]{file};
            }
        }
        return files;
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

        ReadProductOperation operation = new ReadProductOperation(file, formatName);
        final ProgressHandle progressHandle = ProgressHandleFactory.createHandle("Please wait while the data product is being read...", operation);
        ProgressUtils.runOffEventThreadWithProgressDialog(operation, "Reading Product", progressHandle, false, 50, 2000);
        progressHandle.start();
        progressHandle.switchToIndeterminate();

        SnapApp.getDefault().setStatusBarMessage("");

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

}
