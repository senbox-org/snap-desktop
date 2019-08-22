package org.esa.snap.rcp.actions.file;

import com.bc.ceres.swing.TableLayout;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductIOPlugInManager;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.vfs.NioPaths;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.RequestProcessor;

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
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.prefs.Preferences;

/**
 * @author Marco Peters
 */
public class ProductOpener {

    public static final String PREFERENCES_KEY_LAST_PRODUCT_DIR = "last_product_open_dir";
    private static final String PREFERENCES_KEY_PREFIX_ALTERNATIVE_READER = "open_alternative_reader.";
    private static final String PREFERENCES_KEY_DONT_SHOW_DIALOG = "multipleReadersDialog.dontShow";
    private static final int IMMEDIATELY = 0;

    private String fileFormat;
    private boolean useAllFileFilter;
    private boolean subsetImportEnabled;
    private File[] files;
    private boolean multiSelectionEnabled;


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
                Dialogs.showError(
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
            Dialogs.showError(Bundle.LBL_NoReaderFoundText());
            return false;
        }

        Preferences preferences = SnapApp.getDefault().getPreferences();
        String userHomePath = SystemUtils.getUserHomeDir().getAbsolutePath();
        Path recentPath = NioPaths.get(preferences.get(PREFERENCES_KEY_LAST_PRODUCT_DIR, userHomePath));
        ProductFileChooser fc = new ProductFileChooser(recentPath.toFile());

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
        if (filters.size() == 1) {
            fc.setFileSelectionMode(filters.get(0).getFileSelectionMode().getValue());
        } else {
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        }

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
        List<File> openedFiles = OpenProductAction.getOpenedProductFiles();
        List<File> fileList = new ArrayList<>(Arrays.asList(files));
        for (File file : files) {
            if(!file.exists()) {
                fileList.remove(file);
                continue;
            }
            if (openedFiles.contains(file)) {
                Dialogs.Answer answer = Dialogs.requestDecision(Bundle.CTL_OpenProductActionName(),
                                                                MessageFormat.format("Product\n" +
                                                                                     "{0}\n" +
                                                                                     "is already opened.\n" +
                                                                                     "Do you want to open another instance?", file),
                                                                true, null);
                if (answer == Dialogs.Answer.NO) {
                    fileList.remove(file);
                } else if (answer == Dialogs.Answer.CANCELLED) {
                    return null;
                }
            }
        }

        RequestProcessor rp = new RequestProcessor("Opening Products", 4, true, true);
        for (File file : fileList) {
            String fileFormatName;
            if (formatName == null) {
                final List<PluginEntry> intendedPlugIns = getPluginsForFile(file, DecodeQualification.INTENDED);
                List<PluginEntry> suitablePlugIns = new ArrayList<>();
                if (intendedPlugIns.size() == 0) { // check for suitable readers only if no intended reader was found
                    suitablePlugIns.addAll(getPluginsForFile(file, DecodeQualification.SUITABLE));
                }

                if (intendedPlugIns.isEmpty() && suitablePlugIns.isEmpty()) {
                    Dialogs.showError(Bundle.LBL_NoReaderFoundText() + String.format("%nFile '%s' can not be opened.", file));
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

            ReadProductOperation operation = new ReadProductOperation(file, fileFormatName);
            RequestProcessor.Task task = rp.create(operation);
            // TODO (mp/20160830) - Cancellation is not working; the thread is not interrupted. Why?
            ProgressHandle handle = ProgressHandleFactory.createHandle("Reading " + file.getName()/*, operation.createCancellable(task)*/);
            operation.attacheProgressHandle(handle);
            task.schedule(IMMEDIATELY);
        }


        return true;
    }

    static List<PluginEntry> getPluginsForFile(File file, DecodeQualification desiredQualification) {
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

    static String getUserSelection(List<PluginEntry> intendedPlugins, List<PluginEntry> suitablePlugIns) {
        final PluginEntry leadPlugin;
        if (!intendedPlugins.isEmpty()) {
            leadPlugin = intendedPlugins.get(0);
        } else {
            leadPlugin = suitablePlugIns.get(0);
        }
        final boolean dontShowDialog = SnapApp.getDefault().getPreferences().getBoolean(PREFERENCES_KEY_DONT_SHOW_DIALOG, false);
        String prefKeyFormat = PREFERENCES_KEY_PREFIX_ALTERNATIVE_READER + leadPlugin.plugin.getClass().getSimpleName();
        final String storedSelection = SnapApp.getDefault().getPreferences().get(prefKeyFormat, null);
        if (dontShowDialog && storedSelection != null) {
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
                                                  Dialogs.getDialogTitle("Multiple Readers Available"),
                                                  NotifyDescriptor.OK_CANCEL_OPTION, NotifyDescriptor.QUESTION_MESSAGE, null,
                                                  NotifyDescriptor.OK_OPTION);
        Object answer = DialogDisplayer.getDefault().notify(d);
        if (NotifyDescriptor.OK_OPTION.equals(answer)) {
            boolean storeResult = decisionCheckBox.isSelected();
            String selectedFormatName = ((ProductReaderPlugIn) pluginsCombobox.getSelectedItem()).getFormatNames()[0];
            if (storeResult) {
                SnapApp.getDefault().getPreferences().put(prefKeyFormat, selectedFormatName);
                SnapApp.getDefault().getPreferences().put(PREFERENCES_KEY_DONT_SHOW_DIALOG, "true");
            }
            return selectedFormatName;
        }

        return null;
    }


    static class PluginEntry implements Comparable<PluginEntry> {

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
