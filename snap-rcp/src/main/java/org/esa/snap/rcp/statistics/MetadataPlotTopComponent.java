/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.rcp.statistics;

import org.esa.snap.ui.PackageDefaults;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.awt.BorderLayout;

/**
 * The tool view containing a density plot
 *
 */
@TopComponent.Description(
        preferredID = "MetadataPlotTopComponent",
        iconBase = "org/esa/snap/rcp/icons/MetadataPlot.png"
)
@TopComponent.Registration(
        mode = "MetadataPlot",
        openAtStartup = false,
        position = 50
)
@ActionID(category = "Window", id = "org.esa.snap.rcp.statistics.MetadataPlotTopComponent")
@ActionReferences({
        @ActionReference(path = "Menu/Analysis",position = 70),
        @ActionReference(
                path = "Toolbars/" + PackageDefaults.METADATA_PLOT_TOOLBAR_NAME,
                position = PackageDefaults.METADATA_PLOT_TOOLBAR_POSITION
        )
})
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_MetadataPlotTopComponent_Name",
        preferredID = "MetadataPlotTopComponent"
)
@NbBundle.Messages({
        "CTL_MetadataPlotTopComponent_Name=" + PackageDefaults.METADATA_PLOT_NAME,
        "CTL_MetadataPlotTopComponent_HelpId=metadataPlotDialog"
})
public class MetadataPlotTopComponent extends AbstractStatisticsTopComponent {

    @Override
    protected PagePanel createPagePanel() {
        final Icon largeIcon = new ImageIcon(MetadataPlotTopComponent.class.getResource("/org/esa/snap/rcp/icons/" + PackageDefaults.METADATA_PLOT_ICON));
        MetadataPlotPanel metadataPlotPanel = new MetadataPlotPanel(this, Bundle.CTL_MetadataPlotTopComponent_HelpId());
        final TableViewPagePanel tableViewPanel = new MetadataTableViewPagePanel(metadataPlotPanel, largeIcon);
        metadataPlotPanel.setAlternativeView(tableViewPanel);
        tableViewPanel.setAlternativeView(metadataPlotPanel);
        return metadataPlotPanel;
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(Bundle.CTL_MetadataPlotTopComponent_HelpId());
    }

    @Override
    protected void componentOpened() {
        super.componentOpened();
    }

    private class MetadataTableViewPagePanel extends TableViewPagePanel {
        private MetadataTableViewPagePanel(MetadataPlotPanel metadataPlotPanel, Icon largeIcon) {
            super(MetadataPlotTopComponent.this, Bundle.CTL_MetadataPlotTopComponent_HelpId(), metadataPlotPanel.getTitle(), largeIcon);
        }

        @Override
        protected void showAlternativeView() {
            // this is overridden to avoid the clearance of the MetadataPlotPanel when
            // switching back from the MetadataTableViewPagePanel
            final TopComponent parent = (TopComponent) this.getParent();
            parent.remove(this);
            this.setVisible(false);
            final PagePanel alternativeView = getAlternativeView();
            alternativeView.handleLayerContentChanged();
            parent.add(alternativeView, BorderLayout.CENTER);
            alternativeView.setVisible(true);
            parent.revalidate();

        }

    }
}
