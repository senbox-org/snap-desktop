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
package org.esa.snap.rcp.actions.file;

import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import java.util.Map;

/**
 * Generic configurable action for importing data products.
 *
 * @author Marco Peters
 * @author Norman Fomferra
 */
@NbBundle.Messages({
        "CTL_ImportProductActionName=Import Product",
        "CTL_ImportProductActionMenuText=Import Product..."
})
public class ImportProductAction extends AbstractAction implements HelpCtx.Provider {

    /**
     * Action factory method used in NetBeans {@code layer.xml} file, e.g.
     *
     * <pre>
     * &lt;file name="org-esa-snap-dataio-ceos-ImportAvnir2Product.instance"&gt;
     *     &lt;attr name="instanceCreate"
     *         methodvalue="org.openide.awt.Actions.alwaysEnabled"/&gt;
     *     &lt;attr name="delegate"
     *         methodvalue="ImportProductAction.create"/&gt;
     *     &lt;attr name="displayName"
     *         stringvalue="ALOS/AVNIR-2 Product"/&gt;
     *     &lt;attr name="formatName"
     *         stringvalue="AVNIR-2"/&gt;
     *     &lt;attr name="useAllFileFilter"
     *         boolvalue="true"/&gt;
     *     &lt;attr name="helpId"
     *         stringvalue="importAvnir2Product"/&gt;
     *     &lt;attr name="ShortDescription"
     *         stringvalue="Import an ALOS/AVNIR-2 data product."/&gt;
     * &lt;/file&gt;
     * </pre>
     *
     * @param configuration Configuration attributes from layer.xml.
     * @return The action.
     * @since SNAP 2
     */
    public static ImportProductAction create(Map<String, Object> configuration) {
        ImportProductAction importProductAction = new ImportProductAction();
        importProductAction.setFormatName((String) configuration.get("formatName"));
        importProductAction.setHelpCtx((String) configuration.get("helpId"));
        importProductAction.setUseAllFileFilter((Boolean) configuration.get("useAllFileFilter"));
        return importProductAction;
    }

    @Override
    public HelpCtx getHelpCtx() {
        return (HelpCtx) getValue("helpCtx");
    }

    public void setHelpCtx(String helpId) {
        putValue("helpCtx", helpId != null ? new HelpCtx(helpId) : null);
    }

    public void setFormatName(String formatName) {
        putValue("formatName", formatName);
    }

    String getFormatName() {
        return (String) getValue("formatName");
    }

    public void setUseAllFileFilter(boolean useAllFileFilter) {
        putValue("useAllFileFilter", useAllFileFilter);
    }

    boolean getUseAllFileFilter() {
        return Boolean.TRUE.equals(getValue("useAllFileFilter"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final ProductOpener opener = new ProductOpener();
        opener.setFileFormat(getFormatName());
        opener.setUseAllFileFilter(getUseAllFileFilter());
        opener.setMultiSelectionEnabled(false);
        opener.setSubsetImportEnabled(true);
        opener.openProduct();
    }

}
