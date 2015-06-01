/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.actions.file;

import com.bc.ceres.swing.TableLayout;
import org.esa.snap.framework.dataio.DecodeQualification;
import org.esa.snap.framework.dataio.ProductIOPlugInManager;
import org.esa.snap.framework.dataio.ProductReaderPlugIn;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.SnapDialogs;
import org.esa.snap.util.io.SnapFileChooser;
import org.esa.snap.util.io.SnapFileFilter;
import org.netbeans.api.progress.ProgressUtils;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;
import java.awt.event.ActionEvent;
import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 * @author Norman
 */
@ActionID(
        category = "File",
        id = "OpenProductAction"
)
@ActionRegistration(
        displayName = "#CTL_OpenProductActionName",
        menuText = "#CTL_OpenProductActionMenuText",
        iconBase = "org/esa/snap/rcp/icons/Open.gif"
)
@ActionReferences({
        @ActionReference(path = "Menu/File", position = 10),
        @ActionReference(path = "Toolbars/File")
})
@NbBundle.Messages({
        "CTL_OpenProductActionName=Open Product",
        "CTL_OpenProductActionMenuText=Open Product...",
        "LBL_NoReaderFoundText=No appropriate product reader found.",

})
public final class OpenProductAction extends AbstractAction {

    public static final String PREFERENCES_KEY_RECENTLY_OPENED_PRODUCTS = "recently_opened_products";
    public static final String PREFERENCES_KEY_LAST_PRODUCT_DIR = "last_product_open_dir";
    private static final String PREFERENCES_KEY_PREFIX_ALTERNATIVE_READER = "open_alternative_reader.";



    static RecentPaths getRecentProductPaths() {
        return new RecentPaths(SnapApp.getDefault().getPreferences(), PREFERENCES_KEY_RECENTLY_OPENED_PRODUCTS, true);
    }

    static List<File> getOpenedProductFiles() {
        return Arrays.stream(SnapApp.getDefault().getProductManager().getProducts())
                .map(Product::getFileLocation)
                .filter(file -> file != null)
                .collect(Collectors.toList());
    }

    public File[] getFiles() {
        Object value = getValue("files");
        if (value instanceof File[]) {
            return (File[]) value;
        }
        return null;
    }

    public void setFile(File file) {
        setFiles(file);
    }

    public void setFiles(File... files) {
        putValue("files", files);
    }

    public String getFileFormat() {
        Object value = getValue("fileFormat");
        if (value instanceof String) {
            return (String) value;
        }
        return null;
    }

    public void setFileFormat(String fileFormat) {
        putValue("fileFormat", fileFormat);
    }

    public void setUseAllFileFilter(boolean useAllFileFilter) {
        putValue("useAllFileFilter", useAllFileFilter);
    }

    public boolean getUseAllFileFilter() {
        // by default the All file filter is used
        final Object useAllFileFilter = getValue("useAllFileFilter");
        return useAllFileFilter == null || Boolean.TRUE.equals(useAllFileFilter);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        execute();
    }

    /**
     * Executes the action command.
     *
     * @return {@code Boolean.TRUE} on success, {@code Boolean.FALSE} on failure, or {@code null} on cancellation.
     */
    public Boolean execute() {

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
            filters.add(readerPlugIn.getProductFileFilter());
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

        SnapFileChooser fc = new SnapFileChooser(new File(preferences.get(PREFERENCES_KEY_LAST_PRODUCT_DIR, ".")));
        fc.setDialogTitle(Bundle.CTL_OpenProductActionName());
        fc.setAcceptAllFileFilterUsed(getUseAllFileFilter());
        filters.forEach((filter) -> {
            fc.addChoosableFileFilter(filter);
            if (getFileFormat() != null && getFileFormat().equals(filter.getFormatName())) {
                fc.setFileFilter(filter);
            }
        });
        fc.setDialogType(JFileChooser.OPEN_DIALOG);
        fc.setMultiSelectionEnabled(true);
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);

        int returnVal = fc.showOpenDialog(null);
        if (returnVal != JFileChooser.APPROVE_OPTION) {
            // cancelled
            return null;
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
                final List<PluginEntry> possiblePlugIns = getPossiblePluginsForFile(file);
                if (possiblePlugIns.isEmpty()) {
                    SnapDialogs.showError(Bundle.LBL_NoReaderFoundText() + String.format("%nFile '%s' can not be opened.", file));
                    continue;
                } else if (possiblePlugIns.size() == 1) {
                    PluginEntry entry = possiblePlugIns.get(0);
                    fileFormatName = entry.plugin.getFormatNames()[0];
                } else {
                    Collections.sort(possiblePlugIns);
                    fileFormatName = getUserSelection(possiblePlugIns);
                    if(fileFormatName == null) { // User clicked cancel
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

    private static List<PluginEntry> getPossiblePluginsForFile(File file) {
        final Iterator<ProductReaderPlugIn> allReaderPlugIns = ProductIOPlugInManager.getInstance().getAllReaderPlugIns();
        final List<PluginEntry> possiblePlugIns = new ArrayList<>();
        allReaderPlugIns.forEachRemaining(plugIn -> {
            final DecodeQualification qualification = plugIn.getDecodeQualification(file);
            if (qualification != DecodeQualification.UNABLE) {
                possiblePlugIns.add(new PluginEntry(plugIn, qualification));
            }
        });
        return possiblePlugIns;
    }

    private static String getUserSelection(List<PluginEntry> possiblePlugIns) {
        final PluginEntry leadPlugin = possiblePlugIns.get(0);
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
                                            "Please select one of the following:"));
        final ButtonGroup bg = new ButtonGroup();

        for (PluginEntry plugIn : possiblePlugIns) {
            ProductReaderPlugIn readerPlugIn = plugIn.plugin;
            final String labelText = String.format("<html>%s (<b>%s</b>)", readerPlugIn.getDescription(Locale.getDefault()), plugIn.qualification);
            final JRadioButton readerButton = new JRadioButton(labelText);
            readerButton.putClientProperty("plugin", readerPlugIn);
            readerButton.setSelected(bg.getButtonCount() == 0);
            bg.add(readerButton);
            readerSelectionPanel.add(readerButton);
        }
        JCheckBox decisionCheckBox = new JCheckBox("Remember my decision and don't ask again.", false);
        decisionCheckBox.setHorizontalAlignment(SwingConstants.RIGHT);
        readerSelectionPanel.add(decisionCheckBox);

        NotifyDescriptor d = new NotifyDescriptor(readerSelectionPanel,
                                                  SnapDialogs.getDialogTitle("Multiple Readers Available"),
                                                  NotifyDescriptor.OK_CANCEL_OPTION, NotifyDescriptor.QUESTION_MESSAGE, null, NotifyDescriptor.OK_OPTION);
        Object answer = DialogDisplayer.getDefault().notify(d);
        if (NotifyDescriptor.OK_OPTION.equals(answer)) {
            boolean storeResult = decisionCheckBox.isSelected();
            final Enumeration<AbstractButton> buttons = bg.getElements();
            while (buttons.hasMoreElements()) {
                AbstractButton abstractButton = buttons.nextElement();
                if (abstractButton.isSelected()) {
                    String selectedFormatName = ((ProductReaderPlugIn) abstractButton.getClientProperty("plugin")).getFormatNames()[0];
                    if (storeResult) {
                        SnapApp.getDefault().getPreferences().put(preferencesKey, selectedFormatName);
                    }
                    return selectedFormatName;
                }
            }
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

    private static class PluginEntry  implements Comparable<PluginEntry>{
        ProductReaderPlugIn plugin;
        DecodeQualification qualification;

        public PluginEntry(ProductReaderPlugIn plugin, DecodeQualification qualification) {
            this.plugin = plugin;
            this.qualification = qualification;
        }

        @Override
        public int compareTo(PluginEntry other) {
            final int qualificationComparison = this.qualification.compareTo(other.qualification);
            if(qualificationComparison == 0) {
                final String description1 = this.plugin.getDescription(Locale.getDefault());
                final String description2 = other.plugin.getDescription(Locale.getDefault());
                return description1.compareTo(description2);
            }else {
                return qualificationComparison;
            }
        }

    }
}
