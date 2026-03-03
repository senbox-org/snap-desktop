package org.esa.snap.rcp.spectrallibrary.ui;

import org.esa.snap.rcp.spectrallibrary.util.AttributDialogUtils;
import org.esa.snap.speclib.model.AttributeType;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.*;
import java.util.List;


public class AddPreviewAttributeDialog {


    public record AttributeSpec(String key, AttributeType type, String valueText) {}

    private static final int COL_TYPE = 0;
    private static final int COL_KEY = 1;
    private static final int COL_VALUE = 2;


    public static Optional<List<AttributeSpec>> show(Component parent) {
        JCheckBox enable = new JCheckBox("Assign Attributes to the Profiles");
        enable.setSelected(false);

        int rc0 = JOptionPane.showConfirmDialog(
                parent,
                enable,
                "Add to Library",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        if (rc0 != JOptionPane.OK_OPTION) {
            return Optional.empty();
        }
        if (!enable.isSelected()) {
            return Optional.of(List.of());
        }

        return showTableDialog(parent);
    }

    private static Optional<List<AttributeSpec>> showTableDialog(Component parent) {
        JLabel hint = new JLabel(" ");
        hint.setForeground(Color.GRAY);

        DefaultTableModel model = new DefaultTableModel(new Object[]{"Type", "Key", "Value"}, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return switch (columnIndex) {
                    case COL_TYPE -> AttributeType.class;
                    default -> String.class;
                };
            }
            @Override
            public boolean isCellEditable(int row, int column) {
                return true;
            }
        };

        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(Math.max(table.getRowHeight(), 22));

        JComboBox<AttributeType> typeCombo = new JComboBox<>(AttributDialogUtils.ALLOWED_TYPES);
        typeCombo.setSelectedItem(AttributeType.STRING);
        TableColumn typeCol = table.getColumnModel().getColumn(COL_TYPE);
        typeCol.setCellEditor(new DefaultCellEditor(typeCombo));

        int visibleRows = 8;
        table.setPreferredScrollableViewportSize(new Dimension(560, table.getRowHeight() * visibleRows));
        JScrollPane scroll = new JScrollPane(table);

        JButton addBtn = new JButton("Add new attribute");
        JButton removeBtn = new JButton("Remove selected attribute");

        addRow(model);
        table.setRowSelectionInterval(0, 0);

        Runnable updateRemoveEnabled = () -> {
            boolean hasSel = table.getSelectedRow() >= 0;
            removeBtn.setEnabled(hasSel && model.getRowCount() > 1);
        };

        addBtn.addActionListener(e -> {
            int row = addRow(model);
            table.requestFocusInWindow();
            table.setRowSelectionInterval(row, row);
            updateRemoveEnabled.run();
            updateHintFromSelection(table, model, hint);
        });

        removeBtn.addActionListener(e -> {
            int viewRow = table.getSelectedRow();
            if (viewRow < 0 || model.getRowCount() <= 1) return;

            int row = table.convertRowIndexToModel(viewRow);
            if (table.isEditing()) {
                table.getCellEditor().stopCellEditing();
            }

            model.removeRow(row);

            int newRow = Math.min(row, model.getRowCount() - 1);
            if (newRow >= 0) {
                int viewNewRow = table.convertRowIndexToView(newRow);
                table.setRowSelectionInterval(viewNewRow, viewNewRow);
            }

            updateRemoveEnabled.run();
            updateHintFromSelection(table, model, hint);
        });

        updateRemoveEnabled.run();
        updateHintFromSelection(table, model, hint);

        table.getSelectionModel().addListSelectionListener((ListSelectionEvent e) -> {
            if (e.getValueIsAdjusting()) return;
            updateRemoveEnabled.run();
            updateHintFromSelection(table, model, hint);
        });

        model.addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE && e.getColumn() == COL_TYPE) {
                updateHintFromSelection(table, model, hint);
            }
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        buttons.add(addBtn);
        buttons.add(removeBtn);

        JPanel south = new JPanel();
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));
        south.add(hint);
        south.add(Box.createVerticalStrut(4));
        south.add(buttons);

        JPanel root = new JPanel(new BorderLayout(6, 6));
        root.add(scroll, BorderLayout.CENTER);
        root.add(south, BorderLayout.SOUTH);

        int rc = JOptionPane.showConfirmDialog(
                parent,
                root,
                "Add to Library",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        if (rc != JOptionPane.OK_OPTION) {
            return Optional.empty();
        }

        LinkedHashMap<String, AttributeSpec> out = new LinkedHashMap<>();
        for (int r = 0; r < model.getRowCount(); r++) {
            String key = asTrimmedString(model.getValueAt(r, COL_KEY));
            if (key.isEmpty()) continue;

            AttributeType type = (AttributeType) model.getValueAt(r, COL_TYPE);
            if (type == null) type = AttributeType.STRING;

            String value = asString(model.getValueAt(r, COL_VALUE));
            out.put(key, new AttributeSpec(key, type, value));
        }
        return Optional.of(List.copyOf(out.values()));
    }

    private static int addRow(DefaultTableModel model) {
        int r = model.getRowCount();
        model.addRow(new Object[]{AttributeType.STRING, "", ""});
        return r;
    }

    private static void updateHintFromSelection(JTable table, DefaultTableModel model, JLabel hint) {
        if (!table.isEnabled() || model.getRowCount() == 0 || model.getColumnCount() <= COL_TYPE) {
            hint.setText(" ");
            return;
        }

        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            hint.setText(" ");
            return;
        }

        int row = table.convertRowIndexToModel(viewRow);
        if (row < 0 || row >= model.getRowCount()) {
            hint.setText(" ");
            return;
        }

        Object tObj = model.getValueAt(row, COL_TYPE);
        AttributeType t = (tObj instanceof AttributeType at) ? at : AttributeType.STRING;

        String ex = AttributDialogUtils.exampleFor(t);
        hint.setText(ex.isBlank() ? " " : ("Example: " + ex));
    }

    private static String asTrimmedString(Object v) {
        return v == null ? "" : v.toString().trim();
    }

    private static String asString(Object v) {
        return v == null ? "" : v.toString();
    }
}
