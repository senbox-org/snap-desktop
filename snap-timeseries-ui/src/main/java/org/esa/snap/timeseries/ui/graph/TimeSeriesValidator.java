/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.timeseries.ui.graph;

import org.esa.snap.core.jexp.*;
import org.esa.snap.core.jexp.impl.DefaultNamespace;
import org.esa.snap.core.jexp.impl.ParserImpl;
import org.esa.snap.core.jexp.impl.SymbolFactory;
import org.esa.snap.core.util.DefaultPropertyMap;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.timeseries.core.timeseries.datamodel.AxisMapping;
import org.esa.snap.ui.ExpressionPane;
import org.esa.snap.ui.ModalDialog;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesDataItem;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;

/**
 * @author Sabine Embacher
 * @author Thomas Storm
 */
class TimeSeriesValidator implements TimeSeriesGraphForm.ValidatorUI, TimeSeriesGraphModel.Validation {

    private static final String QUALIFIER_RASTER = "r.";
    private static final String QUALIFIER_INSITU = "i.";

    private final Map<Object, Map<String, String>> timeSeriesExpressionsMap = new HashMap<>();
    private final Set<TimeSeriesGraphModel.ValidationListener> validationListeners = new HashSet<>();
    private final Parser parser = new ParserImpl();

    private Map<String, String> currentExpressionMap;
    private List<String> qualifiedSourceNames;
    private DefaultNamespace namespace;
    private JComboBox<String> sourceNamesDropDown;
    private JTextField expressionTextField;

    private boolean hasUI = false;

    @Override
    public JComponent createUI() {

        expressionTextField = new JTextField("");
        expressionTextField.setEditable(false);
        expressionTextField.setEnabled(false);
        expressionTextField.setColumns(30);
        expressionTextField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (expressionTextField.isEnabled()) {
                    showExpressionEditor();
                }
            }
        });

        sourceNamesDropDown = new JComboBox<>();
        sourceNamesDropDown.setPreferredSize(new Dimension(120, 20));
        sourceNamesDropDown.addItemListener(e -> {
            if (ItemEvent.SELECTED == e.getStateChange()) {
                final String selectedSourceName = e.getItem().toString();
                final String expression = getExpressionFor(selectedSourceName);
                expressionTextField.setText(expression == null ? "" : expression);
            }
        });
        sourceNamesDropDown.setEnabled(false);

        final JButton editExpressionButton = new JButton("...");
        editExpressionButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showExpressionEditor();
            }

        });

        JPanel uiPanel = new JPanel();
        uiPanel.add(new JLabel("Valid expression:"));
        uiPanel.add(sourceNamesDropDown);
        uiPanel.add(expressionTextField);
        uiPanel.add(editExpressionButton);
        hasUI = true;

        final JPanel stretchablePanel = new JPanel(new BorderLayout());
        stretchablePanel.add(uiPanel, BorderLayout.CENTER);
        return stretchablePanel;
    }

    @Override
    public void adaptTo(Object timeSeriesKey, AxisMapping axisMapping) {
        if (timeSeriesExpressionsMap.containsKey(timeSeriesKey)) {
            currentExpressionMap = timeSeriesExpressionsMap.get(timeSeriesKey);
        } else {
            currentExpressionMap = new HashMap<>();
            timeSeriesExpressionsMap.put(timeSeriesKey, currentExpressionMap);
        }
        qualifiedSourceNames = extractQualifiedSourceNames(axisMapping);

        namespace = new DefaultNamespace();
        for (String qualifiedSourceName : qualifiedSourceNames) {
            namespace.registerSymbol(SymbolFactory.createVariable(qualifiedSourceName, 0.0));
        }

        if (qualifiedSourceNames.size() > 0 && hasUI) {
            expressionTextField.setEnabled(true);
            sourceNamesDropDown.setEnabled(true);
            sourceNamesDropDown.setModel(new DefaultComboBoxModel<>(getSourceNames()));
            final String expression = getExpressionFor(getSelectedSourceName());
            expressionTextField.setText(expression == null ? "" : expression);
        }
    }

    @Override
    public TimeSeries validate(TimeSeries timeSeries, String sourceName, TimeSeriesType type) throws ParseException {
        String qualifiedSourceName = createQualifiedSourcename(sourceName, type);
        final Symbol symbol = namespace.resolveSymbol(qualifiedSourceName);
        if (symbol == null) {
            throw new ParseException("No variable for identifier '" + qualifiedSourceName + "' registered.");
        }
        final String expression = getExpressionFor(qualifiedSourceName);
        if (expression == null || expression.trim().isEmpty()) {
            return timeSeries;
        }
        final Variable variable = (Variable) symbol;
        final Term term = parser.parse(expression, namespace);

        final int seriesCount = timeSeries.getItemCount();
        final TimeSeries validatedSeries = new TimeSeries(timeSeries.getKey());
        for (int i = 0; i < seriesCount; i++) {
            final TimeSeriesDataItem dataItem = timeSeries.getDataItem(i);
            final Number value = dataItem.getValue();
            variable.assignD(null, value.doubleValue());
            if (term.evalB(null)) {
                validatedSeries.add(dataItem);
            }
        }

        return validatedSeries;
    }

    @Override
    public void addValidationListener(TimeSeriesGraphModel.ValidationListener listener) {
        validationListeners.add(listener);
    }

    boolean setExpression(String qualifiedSourceName, String expression) {
        final Symbol symbol = namespace.resolveSymbol(qualifiedSourceName);
        if (symbol == null) {
            return false;
        }
        if (isExpressionValid(expression, qualifiedSourceName)) {
            currentExpressionMap.put(qualifiedSourceName, expression);
            fireExpressionChanged();
            return true;
        }
        return false;
    }

    private void showExpressionEditor() {
        final MyExpressionPane expressionPane = new MyExpressionPane();
        expressionPane.setEmptyExpressionAllowed(true);
        expressionPane.setCode(expressionTextField.getText());
        final String sourceName = getSelectedSourceName();
        final int status = expressionPane.showModalDialog(SnapApp.getDefault().getMainFrame(), "Valid Expression for Source '" + sourceName + "'");
        if (ModalDialog.ID_OK == status) {
            final String expression = expressionPane.getCode();
            expressionTextField.setText(expression);
            setExpression(sourceName, expression);
        }
    }

    private boolean isExpressionValid(String expression, String qualifiedSourceName) {
        if (expression == null || expression.trim().isEmpty()) {
            return true;
        }
        if (expression.trim().equals(qualifiedSourceName.trim())) {
            return false;
        }
        try {
            final DefaultNamespace expressionValidationNamespace = new DefaultNamespace();
            expressionValidationNamespace.registerSymbol(SymbolFactory.createVariable(qualifiedSourceName, 0.0));
            final Term term = parser.parse(expression, expressionValidationNamespace);
            return term != null && term.isB();
        } catch (ParseException ignored) {
            return false;
        }
    }

    private void fireExpressionChanged() {
        validationListeners.forEach(TimeSeriesGraphModel.ValidationListener::expressionChanged);
    }

    private String getSelectedSourceName() {
        return sourceNamesDropDown.getSelectedItem().toString();
    }

    private String[] getSourceNames() {
        return qualifiedSourceNames.toArray(new String[qualifiedSourceNames.size()]);
    }

    private void collectSourceNames(ArrayList<String> names, List<String> sourceNames, String qualifier) {
        for (String sourceName : sourceNames) {
            final String qualifiedSourceName = qualifier + sourceName;
            names.add(qualifiedSourceName);
        }
    }

    private List<String> extractQualifiedSourceNames(AxisMapping axisMapping) {
        final ArrayList<String> names = new ArrayList<>();
        for (String alias : axisMapping.getAliasNames()) {
            collectSourceNames(names, axisMapping.getInsituNames(alias), QUALIFIER_INSITU);
            collectSourceNames(names, axisMapping.getRasterNames(alias), QUALIFIER_RASTER);
        }
        return names;
    }

    private String createQualifiedSourcename(String sourceName, TimeSeriesType type) {
        String qualifiedSourceName;
        if (TimeSeriesType.INSITU.equals(type)) {
            qualifiedSourceName = QUALIFIER_INSITU + sourceName;
        } else {
            qualifiedSourceName = QUALIFIER_RASTER + sourceName;
        }
        return qualifiedSourceName;
    }

    private String getExpressionFor(String qualifiedSourceName) {
        return currentExpressionMap.get(qualifiedSourceName);
    }

    private class MyExpressionPane extends ExpressionPane {

        public MyExpressionPane() {
            super(true, null, new DefaultPropertyMap());
            initParser();
            initLeftAccessory();
        }

        private void initParser() {
            final String sourceName = getSelectedSourceName();
            final Variable sourceVariable = SymbolFactory.createVariable(sourceName, 0.0);
            final DefaultNamespace namespace = new DefaultNamespace();
            namespace.registerSymbol(sourceVariable);
            setParser(new ParserImpl(namespace, true));
        }

        private void initLeftAccessory() {

            final String sourceName = getSelectedSourceName();
            final JButton insertButton = createInsertButton(sourceName);

            final JPanel sourcePane = new JPanel(new BorderLayout());
            sourcePane.add(new JLabel("Data Source:"), BorderLayout.NORTH);
            sourcePane.add(insertButton);

            final JPanel patternInsertionPane = createPatternInsertionPane();

            final JPanel leftAccessory = new JPanel(new BorderLayout(4, 4));
            leftAccessory.add(sourcePane, BorderLayout.NORTH);
            leftAccessory.add(patternInsertionPane);

            setLeftAccessory(leftAccessory);
        }
    }
}
