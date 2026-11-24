package org.esa.snap.ui.classfraction;

import eu.esa.snap.core.renderer.classfraction.ClassBandGroupConfig;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.ui.AbstractDialog;
import org.esa.snap.ui.ModalDialog;
import org.esa.snap.ui.color.ColorTableCellEditor;

import javax.swing.BorderFactory;
import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;


/**
 * Modal editor for a single product's Class Fraction / Probability display configuration.
 */
public final class ClassFractionRendererDialog extends ModalDialog {


    private static final String TITLE = "Open Class Fraction / Probability View";
    public static final int ID_SHOW_STATISTICS = ID_OTHER;

    private final ClassFractionRendererController controller;
    private final JTextField displayNameField;
    private final JComboBox<Band> addBandCombo;
    private final JSpinner displayScaleSpinner;
    private final JComboBox<ClassBandGroupConfig.ValidityPolicy> validityCombo;
    private final ClassBandTableModel tableModel;
    private final JTable table;


    public ClassFractionRendererDialog(Window parent, ClassFractionRendererController controller) {
        this(parent, controller, TITLE);
    }

    public ClassFractionRendererDialog(Window parent, ClassFractionRendererController controller, String title) {
        super(parent, title, ModalDialog.ID_OK_CANCEL_HELP, new Object[]{"Show Statistics"}, "classFractionRenderer");
        this.controller = controller;
        getButton(ID_OK).setText("Open View");
        arrangeActionButtons();

        ClassFractionRendererModel model = controller.getModel();
        displayNameField = new JTextField(model.getDisplayName(), 32);
        addBandCombo = new JComboBox<>();
        addBandCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                String label = value instanceof Band ? ((Band) value).getName() : "";
                return super.getListCellRendererComponent(list, label, index, isSelected, cellHasFocus);
            }
        });
        displayScaleSpinner = new JSpinner(new SpinnerNumberModel(model.getDisplayScale(), 0.000001, 1_000_000.0, 0.1));
        validityCombo = new JComboBox<>(ClassBandGroupConfig.ValidityPolicy.values());
        validityCombo.setSelectedItem(model.getValidityPolicy());
        configureChoiceLabels(validityCombo);
        tableModel = new ClassBandTableModel(model);
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(22);

        configureTable();
        setContent(createContent());
        refreshAvailableBands();
    }


    public ClassFractionRendererController getController() {
        return controller;
    }

    @Override
    protected boolean verifyUserInput() {
        return applyAndValidateModel();
    }

    private boolean applyAndValidateModel() {
        updateModel();
        try {
            controller.createConfiguration();
            return true;
        } catch (IllegalArgumentException e) {
            AbstractDialog.showErrorDialog(getJDialog(), e.getMessage(), TITLE);
            return false;
        }
    }

    @Override
    protected void onOther() {
        if (!applyAndValidateModel()) {
            return;
        }
        setButtonID(ID_SHOW_STATISTICS);
        hide();
    }

    private void arrangeActionButtons() {
        AbstractButton statistics = null;
        for (Component component : getButtonPanel().getComponents()) {
            if (component instanceof AbstractButton && "Show Statistics".equals(((AbstractButton) component).getText())) {
                statistics = (AbstractButton) component;
                break;
            }
        }
        if (statistics == null) {
            throw new IllegalStateException("Statistics button is missing");
        }
        getButtonPanel().removeAll();
        getButtonPanel().add(Box.createHorizontalGlue());
        addActionButton(getButton(ID_OK));
        addActionButton(statistics);
        addActionButton(getButton(ID_CANCEL));
        addActionButton(getButton(ID_HELP));
        getButtonPanel().revalidate();
        getButtonPanel().repaint();
    }

    private void addActionButton(AbstractButton button) {
        if (getButtonPanel().getComponentCount() > 1) {
            getButtonPanel().add(Box.createHorizontalStrut(4));
        }
        getButtonPanel().add(button);
    }

    private JPanel createContent() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panel.add(createSettingsPanel(), BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(createBottomPanel(), BorderLayout.SOUTH);
        panel.setPreferredSize(new Dimension(800, 380));
        return panel;
    }

    private JPanel createSettingsPanel() {
        JPanel settings = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(2, 2, 2, 2);
        constraints.anchor = GridBagConstraints.WEST;
        settings.add(new JLabel("View name:"), constraints);
        constraints.gridx = 1;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        settings.add(displayNameField, constraints);
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.weightx = 0.0;
        constraints.fill = GridBagConstraints.NONE;
        settings.add(new JLabel("Display scale:"), constraints);
        constraints.gridx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        settings.add(displayScaleSpinner, constraints);
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.fill = GridBagConstraints.NONE;
        settings.add(new JLabel("Validity:"), constraints);
        constraints.gridx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        settings.add(validityCombo, constraints);
        return settings;
    }

    private JPanel createBottomPanel() {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JButton add = new JButton("Add band");
        add.addActionListener(event -> addSelectedBand());
        JButton remove = new JButton("Remove selected");
        remove.addActionListener(event -> removeSelectedBand());
        buttons.add(addBandCombo);
        buttons.add(add);
        buttons.add(remove);
        return buttons;
    }

    private void configureTable() {
        table.getColumnModel().getColumn(0).setPreferredWidth(180);
        table.getColumnModel().getColumn(1).setPreferredWidth(180);
        table.getColumnModel().getColumn(2).setPreferredWidth(70);
        table.getColumnModel().getColumn(3).setPreferredWidth(60);
        table.getColumnModel().getColumn(2).setCellEditor(new ColorTableCellEditor());
        table.getColumnModel().getColumn(2).setCellRenderer(new ColorRenderer());
        table.getColumnModel().getColumn(3).setCellEditor(new DefaultCellEditor(new JCheckBox()));
    }

    private static void configureChoiceLabels(JComboBox<?> comboBox) {
        comboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                String label = value == ClassBandGroupConfig.ValidityPolicy.STRICT_SOURCE_VALIDITY
                        ? "Use source validity masks (default)"
                        : value == ClassBandGroupConfig.ValidityPolicy.FINITE_AND_DECLARED_NODATA
                        ? "Ignore source masks; accept finite values (OWT zeros)"
                        : "";
                return super.getListCellRendererComponent(list, label, index, isSelected, cellHasFocus);
            }
        });
    }

    private void addSelectedBand() {
        Band band = (Band) addBandCombo.getSelectedItem();
        if (band != null) {
            controller.getModel().addBand(band);
            tableModel.fireTableDataChanged();
            refreshAvailableBands();
        }
    }

    private void removeSelectedBand() {
        int row = table.getSelectedRow();
        if (row >= 0) {
            controller.getModel().removeBand(row);
            tableModel.fireTableDataChanged();
            refreshAvailableBands();
        }
    }

    private void refreshAvailableBands() {
        addBandCombo.removeAllItems();
        for (Band band : controller.getModel().getAvailableBands()) {
            addBandCombo.addItem(band);
        }
    }

    private void updateModel() {
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
        ClassFractionRendererModel model = controller.getModel();
        model.setDisplayName(displayNameField.getText().trim());
        model.setDisplayScale(((Number) displayScaleSpinner.getValue()).doubleValue());
        model.setValidityPolicy((ClassBandGroupConfig.ValidityPolicy) validityCombo.getSelectedItem());
    }

    private static final class ClassBandTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {"Band", "Class name", "Color", "Show in View"};
        private final ClassFractionRendererModel model;

        private ClassBandTableModel(ClassFractionRendererModel model) {
            this.model = model;
        }

        @Override
        public int getRowCount() {
            return model.getRows().size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == 2 ? Color.class : columnIndex == 3 ? Boolean.class : Object.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex != 0;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            ClassFractionRendererModel.ClassBandRow row = model.getRows().get(rowIndex);
            switch (columnIndex) {
                case 0: return row.getBand().getName();
                case 1: return row.getDisplayName();
                case 2: return row.getColor();
                case 3: return row.isVisible();
                default: throw new IllegalArgumentException("Unknown column: " + columnIndex);
            }
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            ClassFractionRendererModel.ClassBandRow row = model.getRows().get(rowIndex);
            switch (columnIndex) {
                case 1: row.setDisplayName((String) value); break;
                case 2: row.setColor((Color) value); break;
                case 3: row.setVisible((Boolean) value); break;
                default: return;
            }
            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }

    private static final class ColorRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component component = super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
            component.setBackground((Color) value);
            return component;
        }
    }
}
