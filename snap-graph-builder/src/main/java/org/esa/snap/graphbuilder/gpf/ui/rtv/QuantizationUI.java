/*
 * Copyright (C) 2024 CS ROMANIA
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.graphbuilder.gpf.ui.rtv;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.media.jai.util.Range;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import org.esa.snap.graphbuilder.gpf.ui.BaseOperatorUI;
import org.esa.snap.graphbuilder.gpf.ui.UIValidation;
import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;
import org.esa.snap.ui.AppContext;

/**
 * User interface for Quantization
 */
public class QuantizationUI extends BaseOperatorUI {

	private final JComboBox<String> bandCombo = new JComboBox<>();

	private final IntervalsTablePanel intervalsTablePanel = new IntervalsTablePanel();

	
	@Override
	public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

		initializeOperatorUI(operatorName, parameterMap);

		final JComponent panel = createPanel();

		initParameters();

		bandCombo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (bandCombo.isEnabled()) {
					updateParametersBand();
				}
			}
		});
		
		return new JScrollPane(panel);
	}

	@Override
	public void initParameters() {
		if (sourceProducts == null)
			return;

		final String[] availableBands = getBandNames();
		final String  strSelectedBand = bandCombo.getSelectedItem() != null ?  (String)bandCombo.getSelectedItem()
											:paramMap.get("bandName") != null ?  (String)paramMap.get("bandName") : null;
		bandCombo.setEnabled(false);
		bandCombo.removeAllItems();
		for (String s : availableBands) {
			bandCombo.addItem(s);
		}
		bandCombo.setSelectedItem(null);
		bandCombo.setEnabled(true);
		if (strSelectedBand != null) {
			if (Arrays.stream(availableBands).filter(b-> b.equalsIgnoreCase(strSelectedBand)).count() > 0){
				bandCombo.setSelectedItem(strSelectedBand);
			}else{
				paramMap.remove("bandName");
			}
		}
		intervalsTablePanel.setEnabled(false);
		Map<Integer, Range>  lastIntervalsMap = intervalsTablePanel.getIntervalsMap();
		Map<Integer, Range> paramsIntervalsMap = (Map<Integer, Range>) paramMap.get("intervalsMap");
		if (lastIntervalsMap == null || lastIntervalsMap.isEmpty()) {
			if (paramsIntervalsMap != null && !paramsIntervalsMap.isEmpty()) {
				intervalsTablePanel.setIntervalsMap(paramsIntervalsMap);
			}
		}
		intervalsTablePanel.setEnabled(true);
	}

	@Override
	public UIValidation validateParameters() {
		if (getBandNames().length == 0) {
			// Graph not initialized
			return new UIValidation(UIValidation.State.OK, "");
		}
		
		if (!intervalsTablePanel.validateIntervals()) {
			String errorMessage = intervalsTablePanel.getLastErrorMessage();
			if (errorMessage == null || errorMessage.isBlank()) {
				errorMessage = "Invalid user defined intervals.";
			}
			return new UIValidation(UIValidation.State.ERROR, errorMessage);
		}
		
		
		return new UIValidation(UIValidation.State.OK, "");
	}

	@Override
	public void updateParameters() {
		if (this.hasSourceProducts()) {
			updateParametersBand();
			updateIntervalsMap();
		}
	}

	private void updateParametersBand() {
		String strSelectedBand = (String) bandCombo.getSelectedItem();
		if (strSelectedBand != null && !strSelectedBand.isBlank()) {
			paramMap.remove("bandName");
			paramMap.put("bandName", (String) bandCombo.getSelectedItem());
		}
	}
	private void updateIntervalsMap() {
		if(intervalsTablePanel.isEnabled()) {
			Map<Integer, Range> lastIntervalsMap = intervalsTablePanel.getIntervalsMap();
			if (lastIntervalsMap != null && !lastIntervalsMap.isEmpty()) {
				paramMap.remove("intervalsMap");
				paramMap.put("intervalsMap", lastIntervalsMap);
			}
		}
	}

	private JComponent createPanel() {

		final JPanel contentPane = new JPanel(new GridBagLayout());
		final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

		gbc.gridx = 0;
		contentPane.add(new JLabel("Source band:"), gbc);
		gbc.gridx = 1;
		contentPane.add(bandCombo, gbc);

		gbc.gridy = 1;
		gbc.gridx = 0;
		contentPane.add(new JLabel("Intervals Table:"), gbc);
		gbc.gridx = 1;
		contentPane.add(intervalsTablePanel, gbc);

		DialogUtils.fillPanel(contentPane, gbc);

		return contentPane;
	}

	private class IntervalsTablePanel extends JPanel {

		private final JTable intervalsTable = new JTable();

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
			final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

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
			this.intervalsMap.putAll(intervalsMap);
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
}
