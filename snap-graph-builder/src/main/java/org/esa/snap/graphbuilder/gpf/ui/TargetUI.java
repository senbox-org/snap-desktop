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
package org.esa.snap.graphbuilder.gpf.ui;

import org.esa.snap.core.gpf.ui.TargetProductSelector;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.actions.file.SaveProductAsAction;
import org.esa.snap.ui.AppContext;

import javax.swing.JComponent;
import java.io.File;
import java.util.Map;

/**
 * Writer OperatorUI
 */
public class TargetUI extends BaseOperatorUI {

    TargetProductSelector targetProductSelector = null;
    private static final String FILE_PARAMETER = "file";
    private static final String FORMAT_PARAMETER = "formatName";
    private static final String deafultFileName = "target";
    private AppContext appContext;

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        paramMap = parameterMap;
        targetProductSelector = new TargetProductSelector();
        this.appContext = appContext;

        File saveDir = null;
        final Object value = paramMap.get(FILE_PARAMETER);
        if (value != null) {
            final File file = (File) value;
            saveDir = file.getParentFile();
        }

        if (saveDir == null) {
            final String homeDirPath = SystemUtils.getUserHomeDir().getPath();
            final String savePath = SnapApp.getDefault().getPreferences().get(SaveProductAsAction.PREFERENCES_KEY_LAST_PRODUCT_DIR, homeDirPath);
            saveDir = new File(savePath);
        }
        targetProductSelector.getModel().setProductDir(saveDir);
        targetProductSelector.getOpenInAppCheckBox().setText("Open in " + appContext.getApplicationName());

        initParameters();

        return targetProductSelector.createDefaultPanel();
    }

    @Override
    public void initParameters() {
        assert (paramMap != null);
        String fileName = deafultFileName;
        String format = "BEAM-DIMAP";
        final Object fileValue = paramMap.get(FILE_PARAMETER);
        if (fileValue != null) {
            final File file = (File) fileValue;
            fileName = FileUtils.getFilenameWithoutExtension(file);
        }
        if (sourceProducts != null && sourceProducts.length > 0 && fileName.equals(deafultFileName)) {
            fileName = sourceProducts[0].getName();
        }
        final Object formatValue = paramMap.get(FORMAT_PARAMETER);
        if (formatValue != null) {
            format = (String) formatValue;
        }
        if (fileName != null) {
            targetProductSelector.getProductNameTextField().setText(fileName);
            targetProductSelector.getModel().setProductName(fileName);
            targetProductSelector.getModel().setFormatName(format);
        }
    }

    @Override
    public UIValidation validateParameters() {

        final String productName = targetProductSelector.getModel().getProductName();
        if (productName == null || productName.isEmpty())
            return new UIValidation(UIValidation.State.ERROR, "productName not specified");
        final File file = targetProductSelector.getModel().getProductFile();
        if (file == null)
            return new UIValidation(UIValidation.State.ERROR, "Target file not specified");

        final String productDir = targetProductSelector.getModel().getProductDir().getAbsolutePath();
        SnapApp.getDefault().getPreferences().put(SaveProductAsAction.PREFERENCES_KEY_LAST_PRODUCT_DIR, productDir);

        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {

        if (targetProductSelector.getModel().getProductName() != null) {
            paramMap.put("file", targetProductSelector.getModel().getProductFile());
            paramMap.put("formatName", targetProductSelector.getModel().getFormatName());
        }
    }
}
