/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.rcp.mask;

import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.ui.tool.ToolButtonFactory;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import java.awt.Dimension;
import java.net.URL;
import java.util.prefs.Preferences;

/**
 * @author Marco Peters
 * @since BEAM 4.7
 */
abstract class MaskAction extends AbstractAction {

    private MaskForm maskForm;
    private static final String DEFAULT_MASK_NAME_PREFIX = "new_mask_";

    MaskAction(MaskForm maskForm, String iconPath, String buttonName, String description) {
        this.maskForm = maskForm;
        putValue(ACTION_COMMAND_KEY, getClass().getName());
        if (!iconPath.isEmpty()) {
            putValue(LARGE_ICON_KEY, loadIcon(iconPath));
        }
        putValue(SHORT_DESCRIPTION, description);
        putValue("componentName", buttonName);
    }

    protected MaskForm getMaskForm() {
        return maskForm;
    }

    private ImageIcon loadIcon(String iconPath) {
        final ImageIcon icon;
        URL resource = MaskManagerForm.class.getResource(iconPath);
        if (resource != null) {
            icon = new ImageIcon(resource);
        } else {
            icon = UIUtils.loadImageIcon(iconPath);
        }
        return icon;
    }

    JComponent createComponent() {
        AbstractButton button = ToolButtonFactory.createButton(this, false);
        button.setName((String) getValue("componentName"));
        return button;
    }

    void updateState() {
    }

    protected Mask createNewMask(Mask.ImageType type) {
        String maskName = getNewMaskName(getMaskForm().getProduct().getMaskGroup());
        Dimension maskSize = getMaskForm().getTargetMaskSize();
        Mask mask = new Mask(maskName, maskSize.width, maskSize.height, type);
        Preferences preferences = SnapApp.getDefault().getPreferences();
        mask.setImageColor(
                StringUtils.parseColor(preferences.get("mask.color", StringUtils.formatColor(Mask.ImageType.DEFAULT_COLOR))));
        mask.setImageTransparency(preferences.getDouble("mask.transparency", Mask.ImageType.DEFAULT_TRANSPARENCY));
        return mask;
    }

    private String getNewMaskName(ProductNodeGroup<Mask> maskGroup) {
        String possibleName = DEFAULT_MASK_NAME_PREFIX + maskGroup.getNodeCount();
        for (int i = 0; i <= maskGroup.getNodeCount(); i++) {
            possibleName = DEFAULT_MASK_NAME_PREFIX + (maskGroup.getNodeCount() + i + 1);
            if (!maskGroup.contains(possibleName)) {
                break;
            }
        }
        return possibleName;
    }
}
