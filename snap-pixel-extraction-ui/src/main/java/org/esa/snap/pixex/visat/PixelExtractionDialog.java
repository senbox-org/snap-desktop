/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.pixex.visat;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.ParameterDescriptorFactory;
import org.esa.snap.core.gpf.ui.OperatorMenu;
import org.esa.snap.core.gpf.ui.OperatorParameterSupport;
import org.esa.snap.core.gpf.ui.ParameterUpdater;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.WildcardMatcher;
import org.esa.snap.pixex.Coordinate;
import org.esa.snap.pixex.PixExOp;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.ModelessDialog;

import javax.swing.AbstractButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

class PixelExtractionDialog extends ModelessDialog implements ParameterUpdater {

    private static final String OPERATOR_NAME = "PixEx";

    private final Map<String, Object> parameterMap;
    private final AppContext appContext;
    private final PixelExtractionIOForm ioForm;
    private final PixelExtractionParametersForm parametersForm;

    PixelExtractionDialog(AppContext appContext, String title, String helpID) {
        super(appContext.getApplicationWindow(), title, ID_OK | ID_CLOSE | ID_HELP, helpID);

        this.appContext = appContext;

        AbstractButton button = getButton(ID_OK);
        button.setText("Extract");
        button.setMnemonic('E');

        parameterMap = new HashMap<>();
        final OperatorSpi operatorSpi = GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi(OPERATOR_NAME);

        final PropertyContainer propertyContainer = createParameterMap(parameterMap);

        final OperatorParameterSupport parameterSupport = new OperatorParameterSupport(operatorSpi.getOperatorDescriptor(),
                                                                                       propertyContainer,
                                                                                       parameterMap,
                                                                                       this);
        final OperatorMenu operatorMenu = new OperatorMenu(this.getJDialog(),
                                                           operatorSpi.getOperatorDescriptor(),
                                                           parameterSupport,
                                                           appContext,
                                                           getHelpID());
        getJDialog().setJMenuBar(operatorMenu.createDefaultMenu());

        ListDataListener changeListener = new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent e) {
                contentsChanged(e);
            }

            @Override
            public void intervalRemoved(ListDataEvent e) {
                contentsChanged(e);
            }

            @Override
            public void contentsChanged(ListDataEvent e) {
                final Product[] sourceProducts = ioForm.getSourceProducts();
                if (sourceProducts.length > 0) {
                    parametersForm.setActiveProduct(sourceProducts[0]);
                    return;
                } else {
                    if (parameterMap.containsKey("sourceProductPaths")) {
                        final String[] inputPaths = (String[]) parameterMap.get("sourceProductPaths");
                        if (inputPaths.length > 0) {
                            Product firstProduct = openFirstProduct(inputPaths);
                            if (firstProduct != null) {
                                parametersForm.setActiveProduct(firstProduct);
                                return;
                            }
                        }
                    }
                }
                parametersForm.setActiveProduct(null);
            }
        };
        ioForm = new PixelExtractionIOForm(appContext, propertyContainer, changeListener);
        parametersForm = new PixelExtractionParametersForm(appContext, propertyContainer);

        final JPanel ioPanel = ioForm.getPanel();
        ioPanel.setBorder(new EmptyBorder(4, 4, 4, 4));
        final JPanel parametersPanel = parametersForm.getPanel();
        parametersPanel.setBorder(new EmptyBorder(4, 4, 4, 4));

        JTabbedPane tabbedPanel = new JTabbedPane();
        tabbedPanel.addTab("Input/Output", ioPanel);
        tabbedPanel.addTab("Parameters", parametersPanel);

        setContent(tabbedPanel);
    }

    private Product openFirstProduct(String[] inputPaths) {
        if (inputPaths != null) {

            final Logger logger = SystemUtils.LOG;

            for (String inputPath : inputPaths) {
                if (inputPath == null || inputPath.trim().length() == 0) {
                    continue;
                }
                try {
                    final TreeSet<File> fileSet = new TreeSet<>();
                    WildcardMatcher.glob(inputPath, fileSet);
                    for (File file : fileSet) {
                        final Product product = ProductIO.readProduct(file);
                        if (product != null) {
                            return product;
                        }
                    }
                } catch (IOException e) {
                    logger.severe("I/O problem occurred while scanning source product files: " + e.getMessage());
                }
            }
        }
        return null;
    }

    @Override
    protected void onOK() {
        handleParameterSaveRequest(parameterMap);
        ProgressMonitorSwingWorker worker = new MyProgressMonitorSwingWorker(getParent(), "Creating output file(s)...");
        worker.executeWithBlocking();
    }

    @Override
    public void close() {
        super.close();
        ioForm.clear();
    }

    @Override
    public int show() {
        ioForm.addProduct(appContext.getSelectedProduct());
        return super.show();
    }

    private static PropertyContainer createParameterMap(Map<String, Object> map) {
        ParameterDescriptorFactory parameterDescriptorFactory = new ParameterDescriptorFactory();
        final PropertyContainer container = PropertyContainer.createMapBacked(map, PixExOp.class,
                                                                              parameterDescriptorFactory);
        container.setDefaultValues();
        return container;
    }

    @Override
    public void handleParameterSaveRequest(Map<String, Object> parameterMap) {
        parameterMap.put("expression", parametersForm.getExpression());
        parameterMap.put("exportExpressionResult", parametersForm.isExportExpressionResultSelected());
        parameterMap.put("timeDifference", parametersForm.getAllowedTimeDifference());
        parameterMap.put("coordinates", parametersForm.getCoordinates());
    }

    @Override
    public void handleParameterLoadRequest(Map<String, Object> parameterMap) {
        Object expressionObject = parameterMap.get("expression");
        String expression = "";
        if (expressionObject instanceof String) {
            expression = (String) expressionObject;
        }
        parametersForm.setExpression(expression);
        Object outputDirObject = parameterMap.get("outputDir");
        if (outputDirObject instanceof  File) {
            ioForm.setOutputDirPath(outputDirObject.toString());
        }
        Object exportExpressionResultObject = parameterMap.get("exportExpressionResult");
        if (exportExpressionResultObject instanceof Boolean) {
            parametersForm.setExportExpressionResultSelected((Boolean) exportExpressionResultObject);
        }
        Object timeDifferenceObject = parameterMap.get("timeDifference");
        String timeDifference = null;
        if (timeDifferenceObject instanceof String) {
            timeDifference = (String) timeDifferenceObject;
        }
        parametersForm.setAllowedTimeDifference(timeDifference);
        Object coordinatesObject = parameterMap.get("coordinates");
        Coordinate[] coordinates = new Coordinate[0];
        if (coordinatesObject instanceof Coordinate[]) {
            coordinates = (Coordinate[]) coordinatesObject;
        }
        parametersForm.setCoordinates(coordinates);
        parametersForm.updateUi();
    }

    private class MyProgressMonitorSwingWorker extends ProgressMonitorSwingWorker<Void, Void> {

        protected MyProgressMonitorSwingWorker(Component parentComponent, String title) {
            super(parentComponent, title);
        }

        @Override
        protected Void doInBackground(ProgressMonitor pm) throws Exception {
            pm.beginTask("Computing pixel values...", -1);
            AbstractButton runButton = getButton(ID_OK);
            runButton.setEnabled(false);
            try {
                GPF.createProduct("PixEx", parameterMap, ioForm.getSourceProducts());
                pm.worked(1);
            } finally {
                pm.done();
            }
            return null;
        }

        @Override
        protected void done() {
            try {
                get();
                Object outputDir = parameterMap.get("outputDir");
                String message;
                if (outputDir != null) {
                    message = String.format(
                                "The pixel extraction tool has run successfully and written the result file(s) to %s.",
                                outputDir.toString());
                } else {
                    message = "The pixel extraction tool has run successfully and written the result file to to std.out.";
                }
                showInformationDialog(message);
            } catch (InterruptedException ignore) {
            } catch (ExecutionException e) {
                appContext.handleError(e.getMessage(), e);
            } finally {
                AbstractButton runButton = getButton(ID_OK);
                runButton.setEnabled(true);
            }
        }
    }
}
