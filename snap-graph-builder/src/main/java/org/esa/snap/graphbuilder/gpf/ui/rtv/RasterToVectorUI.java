/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Map;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.esa.snap.graphbuilder.gpf.ui.BaseOperatorUI;
import org.esa.snap.graphbuilder.gpf.ui.UIValidation;
import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;
import org.esa.snap.ui.AppContext;

/**
 * User interface for Raster To Vector
 */
public class RasterToVectorUI extends BaseOperatorUI {

	private final JComboBox<String> bandCombo = new JComboBox<>();

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
	}

	@Override
	public UIValidation validateParameters() {
		return new UIValidation(UIValidation.State.OK, "");
	}

	@Override
	public void updateParameters() {
		updateParametersBand();
	}

	private void updateParametersBand() {
		String strSelectedBand = (String) bandCombo.getSelectedItem();
		if (strSelectedBand != null && !strSelectedBand.isBlank()) {
			paramMap.remove("bandName");
			paramMap.put("bandName", strSelectedBand);
		}
	}

	private JComponent createPanel() {

		final JPanel contentPane = new JPanel(new GridBagLayout());
		final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

		gbc.gridx = 0;
		contentPane.add(new JLabel("Source band:"), gbc);
		gbc.gridx = 1;
		contentPane.add(bandCombo, gbc);

		DialogUtils.fillPanel(contentPane, gbc);

		return contentPane;
	}
}
