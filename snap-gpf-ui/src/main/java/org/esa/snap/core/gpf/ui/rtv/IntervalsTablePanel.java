package org.esa.snap.core.gpf.ui.rtv;

import org.esa.snap.ui.GridBagUtils;

import javax.media.jai.util.Range;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class IntervalsTablePanel extends JPanel {

    public final JTable intervalsTable = new JTable();

    private final JScrollPane tableContainer;

    private final JButton addButton = new JButton("Add");

    private final JButton deleteButton = new JButton("Delete");

    private Map<Integer, Range> intervalsMap = new HashMap<>();

    private String lastErrorMessage;

    /**
     * Constructor.
     *
     */
    public IntervalsTablePanel() {
        setLayout(new GridBagLayout());
        final GridBagConstraints gbc = GridBagUtils.createDefaultConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets.top = 1;
        gbc.insets.bottom = 1;
        gbc.insets.right = 1;
        gbc.insets.left = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;

        initTable();

        gbc.gridx = 0;
        gbc.gridwidth = 1;
        tableContainer = new JScrollPane(intervalsTable);
        tableContainer.setPreferredSize(new Dimension(100, 100));
        this.add(tableContainer, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.weightx = 1;
        final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(addButton);
        buttonPanel.add(deleteButton);
        this.add(buttonPanel, gbc);

        addButton.addActionListener(e -> {
            final DefaultTableModel tm = (DefaultTableModel) intervalsTable.getModel();
            tm.addRow(new Object[] {"", "", ""});
        });

        deleteButton.addActionListener(e -> {
            int r = intervalsTable.getSelectedRow();
            if (r == -1) {
                return;
            }
            final DefaultTableModel tm = (DefaultTableModel) intervalsTable.getModel();
            tm.removeRow(r);
        });
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        intervalsTable.setEnabled(enabled);
        tableContainer.setEnabled(enabled);
        addButton.setEnabled(enabled);
        if (intervalsTable.getSelectedRow() == -1) {
            deleteButton.setEnabled(false);
        } else {
            deleteButton.setEnabled(enabled);
        }
    }

    private void initTable() {

        DefaultTableModel tm = new DefaultTableModel(new Object[] {"Interval start","Interval end","Associated value"}, 0);
        intervalsTable.setModel(tm);

        // show black grid lines
        intervalsTable.setShowGrid(true);
        intervalsTable.setGridColor(Color.BLACK);

        // allow only one row to be selected
        intervalsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        intervalsTable.getSelectionModel().addListSelectionListener(e -> {
            if (intervalsTable.getSelectedRow() == -1) {
                deleteButton.setEnabled(false);
            } else {
                deleteButton.setEnabled(true);
            }
        });
    }

    /**
     * Get the current value of the intervalsMap.
     *
     * @return the intervalsMap
     */
    public Map<Integer, Range> getIntervalsMap() {
        validateIntervals();
        return this.intervalsMap;
    }

    public void setIntervalsMap(Map<Integer, Range> intervalsMap){
        this.intervalsMap.clear();
        if(intervalsMap != null) {
            this.intervalsMap.putAll(intervalsMap);
        }
        loadIntervalsTable();
    }

    private void loadIntervalsTable(){
        final DefaultTableModel tm = (DefaultTableModel) intervalsTable.getModel();
        int nbRows = tm.getRowCount();
        for (int i = nbRows - 1; i >=0; i--) {
            tm.removeRow(i);
        }
        this.intervalsMap. forEach((value, range) -> tm.addRow(new Object[]{range.getMinValue(), range.getMaxValue(), value}));
    }

    /**
     * Get the current value of the lastErrorMessage.
     *
     * @return the lastErrorMessage
     */
    public String getLastErrorMessage() {
        return this.lastErrorMessage;
    }

    /**
     * Validate the intervals
     *
     * @return true if the intervals are valid, false if not.
     */
    public boolean validateIntervals() {
        if (intervalsTable.getRowCount() == 0) {
            lastErrorMessage = "At least one interval is required";
            return false;
        }
        this.intervalsMap.clear();
        for (int i = 0; i < intervalsTable.getRowCount(); i++) {
            try {
                final double min = Double.parseDouble(intervalsTable.getValueAt(i, 0).toString());
                final double max = Double.parseDouble(intervalsTable.getValueAt(i, 1).toString());
                final int value = Integer.parseInt(intervalsTable.getValueAt(i, 2).toString());

                // min must be smaller or equal to max
                if (min > max) {
                    lastErrorMessage = "The interval [" + min + ", " + max + "] is not valid. The first value must be smaller or equal to the second value";
                    return false;
                }

                // The 0 value is reserved for no data
                if (value == 0) {
                    lastErrorMessage = "The value 0 is reserved for no-data pixels";
                    return false;
                }

                // create a range with min excluded and max included
                final Range r = new Range(Double.class, min, false, max, true);

                // check that the value is unique
                if (intervalsMap.containsKey(value)) {
                    // value already exists
                    lastErrorMessage = "The value " + value + " is used more than once";
                    return false;
                }

                // check if the range overlaps other range
                if (intervalsMap.entrySet().stream().filter(e -> e.getValue().intersects(r)).count() > 0) {
                    // interval overlaps
                    lastErrorMessage = "The interval [" + r.getMinValue() + ", " + r.getMaxValue() + "] overlaps other interval";
                    return false;
                }

                intervalsMap.put(value, r);

            } catch(NumberFormatException ex) {
                lastErrorMessage = "Invalid number: " + ex.getMessage();
                return false;
            }
        }

        return true;
    }
}
