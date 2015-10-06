package org.esa.snap.opendap.ui;

import com.bc.ceres.swing.progress.ProgressBarProgressMonitor;
import com.jidesoft.swing.CheckBoxList;
import com.jidesoft.swing.CheckBoxListSelectionModel;
import com.jidesoft.swing.LabeledTextField;
import org.esa.snap.core.ui.GridBagUtils;
import org.esa.snap.core.ui.util.FilteredListModel;
import org.esa.snap.opendap.datamodel.DAPVariable;
import org.esa.snap.opendap.datamodel.OpendapLeaf;
import org.esa.snap.opendap.utils.VariableCollector;
import org.esa.snap.util.logging.BeamLogManager;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;

public class VariableFilter implements FilterComponent, CatalogTree.CatalogTreeListener {

    private static final Logger LOG = Logger.getLogger(VariableFilter.class.getName());
    private static final int MAX_THREAD_COUNT = 10;
    private final JCheckBox filterCheckBox;
    private VariableCollector collector = new VariableCollector();

    private VariableListModel listModel;
    private JButton selectAllButton;
    private JButton selectNoneButton;
    private JButton applyButton;
    private LabeledTextField filterField;
    private FilteredListModel<DAPVariable> filteredListModel;
    private CheckBoxList checkBoxList;
    private List<FilterChangeListener> listeners;
    private final HashSet<VariableFilterPreparator> filterPreparators = new HashSet<>();
    private final List<VariableFilterPreparator> filterPreparatorsInWait = new ArrayList<>();
    private LabelledProgressBarPM pm;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JLabel percentageLabel;
    private double totalWork;
    private double worked;

    public VariableFilter(JCheckBox filterCheckBox, CatalogTree catalogTree) {
        this.filterCheckBox = filterCheckBox;
        catalogTree.addCatalogTreeListener(this);
        listeners = new ArrayList<>();
    }

    @Override
    public JComponent getUI() {
        JPanel panel = GridBagUtils.createPanel();
        initComponents();
        configureComponents();
        addComponents(panel);
        updateUI(false, false, false);
        return panel;
    }


    private void initComponents() {
        applyButton = new JButton("Apply");
        selectAllButton = new JButton("Select all");
        selectNoneButton = new JButton("Select none");
        listModel = new VariableListModel();
        filterField = new LabeledTextField();
        filteredListModel = new FilteredListModel<>(listModel);
        checkBoxList = new ToolTippedCheckBoxList(filteredListModel);
        progressBar = new JProgressBar();
        statusLabel = new JLabel("");
        percentageLabel = new JLabel("");
        pm = new VariableFilterProgressBarProgressMonitor(progressBar, statusLabel, percentageLabel);
    }

    private void configureComponents() {
        selectAllButton.addActionListener(e -> {
            int variableCount = checkBoxList.getModel().getSize();
            int[] selectedIndices = new int[variableCount];
            for (int i = 0; i < variableCount; i++) {
                selectedIndices[i] = i;
            }
            checkBoxList.setCheckBoxListSelectedIndices(selectedIndices);
            updateUI(true, false, true);
        });
        selectNoneButton.addActionListener(e -> {
            checkBoxList.setCheckBoxListSelectedIndices(new int[0]);
            updateUI(true, true, false);
        });
        applyButton.addActionListener(e -> {
            fireFilterChanged();
            updateUI(false, selectAllButton.isEnabled(), selectNoneButton.isEnabled());
        });
        filterCheckBox.setEnabled(false);
        filterCheckBox.addActionListener(e -> {
            boolean useFilter = filterCheckBox.isSelected();
            fireFilterChanged();
            updateUI(useFilter, useFilter, useFilter);
        });
        checkBoxList.getCheckBoxListSelectionModel().addListSelectionListener(e -> {
            CheckBoxListSelectionModel model = (CheckBoxListSelectionModel) e.getSource();
            int anchorSelectionIndex = model.getAnchorSelectionIndex();
            if (e.getValueIsAdjusting() || anchorSelectionIndex == -1) {
                return;
            }
            for (int i = 0; i < listModel.getSize(); i++) {
                DAPVariable variable = listModel.getElementAt(i);
                DAPVariable currentVariable = (DAPVariable) model.getModel().getElementAt(anchorSelectionIndex);
                if (variable.equals(currentVariable)) {
                    boolean isSelected = model.isSelectedIndex(anchorSelectionIndex);
                    setVariableSelected(currentVariable, isSelected);
                }
            }
            updateUI(true, true, true);
        });

        Font font = selectAllButton.getFont().deriveFont(10.0F);
        selectAllButton.setFont(font);
        selectNoneButton.setFont(font);
        filterField.setHintText("Type here to filter variables");
        filterField.getTextField().getDocument().addDocumentListener(new FilterDocumentListener());

        progressBar.setVisible(false);
    }

    private void addComponents(JPanel panel) {
        GridBagConstraints gbc = new GridBagConstraints();

        JScrollPane scrollPane = new JScrollPane(checkBoxList);
        scrollPane.setPreferredSize(new Dimension(250, 100));
        GridBagUtils.addToPanel(panel, statusLabel, gbc, "insets.top=5, anchor=WEST");
        GridBagUtils.addToPanel(panel, progressBar, gbc, "gridx=1,fill=HORIZONTAL, weightx=1.0");
        GridBagUtils.addToPanel(panel, percentageLabel, gbc, "insets.left=5, gridx=2, fill=NONE, weightx=0.0");
        GridBagUtils.addToPanel(panel, filterField, gbc, "insets.left=0, gridx=0, gridy=1, gridwidth=3, fill=HORIZONTAL, weightx=1.0");
        GridBagUtils.addToPanel(panel, scrollPane, gbc, "gridy=2");
        GridBagUtils.addToPanel(panel, selectAllButton, gbc, "insets.right=5, gridy=3, gridwidth=1, fill=NONE, weightx=0");
        GridBagUtils.addToPanel(panel, selectNoneButton, gbc, "gridx=1");
        GridBagUtils.addToPanel(panel, applyButton, gbc, "insets.right=0, gridx=2, gridy=4, anchor=EAST");
    }


    @Override
    public boolean accept(OpendapLeaf leaf) {
        DAPVariable[] dapVariables = leaf.getDAPVariables();
        if (noVariablesAreSelected()) {
            return true;
        }
        for (DAPVariable dapVariable : dapVariables) {
            Boolean isSelected = listModel.variableToSelected.get(dapVariable);
            boolean leafContainsVariable = isSelected == null ? false : isSelected;
            if (leafContainsVariable) {
                return true;
            }
        }
        return false;
    }

    private boolean noVariablesAreSelected() {
        for (Boolean selected : listModel.variableToSelected.values()) {
            if (selected) {
                return false;
            }
        }
        return true;
    }

    private void updateUI(boolean enableApplyButton, boolean enableSelectAllButton, boolean enableSelectNoneButton) {
        boolean notAllSelected = checkBoxList.getModel().getSize() == 0 ||
                checkBoxList.getCheckBoxListSelectedIndices().length <
                        checkBoxList.getModel().getSize();
        boolean someSelected = checkBoxList.getCheckBoxListSelectedIndices().length > 0;
        boolean filtersAvailable = checkBoxList.getModel().getSize() > 0;

        selectAllButton.setEnabled(filterCheckBox.isSelected() && enableSelectAllButton && notAllSelected);
        selectNoneButton.setEnabled(filterCheckBox.isSelected() && enableSelectNoneButton && someSelected);
        applyButton.setEnabled(filterCheckBox.isSelected() && enableApplyButton && filtersAvailable);
        checkBoxList.setEnabled(filterCheckBox.isSelected());
        filterField.setEnabled(filterCheckBox.isSelected());
    }

    @Override
    public void addFilterChangeListener(FilterChangeListener listener) {
        listeners.add(listener);
    }

    private void fireFilterChanged() {
        for (FilterChangeListener listener : listeners) {
            listener.filterChanged();
        }
    }

    public void addVariable(DAPVariable dapVariable) {
        listModel.allVariables.add(dapVariable);
    }

    public void setVariableSelected(DAPVariable dapVariable, boolean selected) {
        listModel.variableToSelected.put(dapVariable, selected);
    }

    public void stopFiltering() {
        filterPreparators.clear();
        filterPreparatorsInWait.clear();
        listModel.allVariables.clear();
        listModel.variableToSelected.clear();
    }

    private static class VariableListModel implements ListModel<DAPVariable> {

        private SortedSet<DAPVariable> allVariables = new TreeSet<>();
        private Map<DAPVariable, Boolean> variableToSelected = new HashMap<>();
        private Set<ListDataListener> listeners = new HashSet<>();

        void addVariables(DAPVariable[] dapVariables) {
            allVariables.addAll(Arrays.asList(dapVariables));
            for (ListDataListener listener : listeners) {
                listener.contentsChanged(new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, getSize() - 1));
            }
        }

        @Override
        public int getSize() {
            return allVariables.size();
        }

        @Override
        public DAPVariable getElementAt(int index) {
            return (DAPVariable) allVariables.toArray()[index];
        }

        @Override
        public void addListDataListener(ListDataListener l) {
            listeners.add(l);
        }

        @Override
        public void removeListDataListener(ListDataListener l) {
            listeners.remove(l);
        }
    }

    @Override
    public void leafAdded(OpendapLeaf leaf, boolean hasNestedDatasets) {
        VariableFilterPreparator filterPreparator = new VariableFilterPreparator(leaf);

        if (filterPreparators.size() <= MAX_THREAD_COUNT) {
            filterPreparators.add(filterPreparator);
            filterPreparator.execute();
            filterCheckBox.setEnabled(false);
            filterCheckBox.setSelected(false);
            updateUI(false, false, false);
        } else {
            filterPreparatorsInWait.add(filterPreparator);
        }

        totalWork++;
    }

    @Override
    public void catalogElementsInsertionFinished() {
        pm.setPreMessage("Scanning variables... ");
        pm.setPostMessage("");
        pm.beginTask("", (int)totalWork);
        pm.worked((int)worked);
    }

    private class VariableFilterPreparator extends SwingWorker<DAPVariable[], Void> {

        private OpendapLeaf leaf;

        private VariableFilterPreparator(OpendapLeaf leaf) {
            this.leaf = leaf;
        }

        @Override
        protected DAPVariable[] doInBackground() throws Exception {
            DAPVariable[] leafVariables = collector.collectDAPVariables(leaf);
            leaf.addDAPVariables(leafVariables);
            return leafVariables;
        }

        @Override
        protected void done() {
            try {
                DAPVariable[] dapVariables = get();
                listModel.addVariables(dapVariables);
            } catch (Exception e) {
                BeamLogManager.getSystemLogger().warning(
                        "Stopping to scan for variables due to exception: " + e.getMessage());
            } finally {
                filterPreparators.remove(this);
                pm.worked(1);
                worked++;
                if (!filterPreparatorsInWait.isEmpty()) {
                    VariableFilterPreparator nextFilterPreparator = filterPreparatorsInWait.remove(0);
                    filterPreparators.add(nextFilterPreparator);
                    nextFilterPreparator.execute();
                }
                int percentage = (int) ((worked / totalWork) * 100);
                pm.setTaskName(percentage + " %");
                if (filterPreparators.isEmpty()) {
                    updateUI(true, true, true);
                    filterCheckBox.setEnabled(true);
                    pm.done();
                    worked = 0;
                    totalWork = 0;
                }
            }
        }
    }

    private static class VariableFilterProgressBarProgressMonitor extends ProgressBarProgressMonitor implements LabelledProgressBarPM {

        private final JProgressBar progressBar;
        private final JLabel preMessageLabel;
        private final JLabel postMessageLabel;

        public VariableFilterProgressBarProgressMonitor(JProgressBar progressBar, JLabel preMessageLabel, JLabel postMessageLabel) {
            super(progressBar, postMessageLabel);
            this.progressBar = progressBar;
            this.preMessageLabel = preMessageLabel;
            this.postMessageLabel = postMessageLabel;
        }

        @Override
        protected void setDescription(String description) {
        }

        @Override
        public void setVisibility(boolean visible) {
            progressBar.setVisible(visible);
            preMessageLabel.setVisible(visible);
            postMessageLabel.setVisible(visible);
        }

        @Override
        protected void setRunning() {
        }

        @Override
        protected void finish() {
        }

        @Override
        public void setPreMessage(String preMessageText) {
            preMessageLabel.setText(preMessageText);
        }

        @Override
        public void setPostMessage(String postMessageText) {
            setTaskName(postMessageText);
        }

        @Override
        public int getTotalWork() {
            throw new IllegalStateException("not implemented");
        }

        @Override
        public int getCurrentWork() {
            throw new IllegalStateException("not implemented");
        }

        @Override
        public void setTooltip(String tooltip) {
        }
    }

    private class ToolTippedCheckBoxList extends CheckBoxList {

        public ToolTippedCheckBoxList(ListModel displayListModel) {
            super(displayListModel);
        }

        @Override
        public String getToolTipText(MouseEvent event) {
            int index = locationToIndex(event.getPoint());
            DAPVariable item = (DAPVariable) getModel().getElementAt(index);
            return item.getInfotext();
        }

    }

    private class FilterDocumentListener implements DocumentListener {

        @Override
        public void insertUpdate(DocumentEvent e) {
            updateFilter(getFilterText(e));
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            updateFilter(getFilterText(e));
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
        }

        private void updateFilter(String text) {
            filteredListModel.setFilter(element -> element.getName().contains(text.trim()));
        }

        private String getFilterText(DocumentEvent e) {
            Document document = e.getDocument();
            String text = null;
            try {
                text = document.getText(0, document.getLength());
            } catch (BadLocationException e1) {
                LOG.severe(e1.getMessage());
            }
            return text;
        }

    }
}
