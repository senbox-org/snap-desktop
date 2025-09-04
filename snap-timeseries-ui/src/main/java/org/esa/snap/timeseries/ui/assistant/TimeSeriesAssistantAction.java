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

package org.esa.snap.timeseries.ui.assistant;

import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.esa.snap.ui.assistant.AssistantPane;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

import java.awt.event.ActionEvent;

@ActionID(
        category = "File",
        id = "TimeSeriesAssistantAction"
)
@ActionRegistration(
        displayName = "#CTL_TimeSeriesAssistantActionName",
        iconBase = "org/esa/snap/timeseries/ui/icons/timeseries-new.gif"
)
//@ActionReference(path = "Menu/Raster/Time Series", position = 20)
@ActionReferences({
        @ActionReference(path = "Menu/Raster/Time Series", position = 10),
        @ActionReference(path = "Toolbars/Time Series", position = 10)
})
@NbBundle.Messages({
        "CTL_TimeSeriesAssistantActionName=New Time Series",
        "CTL_TimeSeriesAssistantActionDescription=Create a new time series",
})
public class TimeSeriesAssistantAction extends AbstractSnapAction {

    public TimeSeriesAssistantAction() {
        putValue(LARGE_ICON_KEY, ImageUtilities.loadImageIcon("org/esa/snap/timeseries/ui/icons/timeseries-new24.gif", false));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final TimeSeriesAssistantModel assistantModel = new TimeSeriesAssistantModel();
        final AssistantPane assistant = new AssistantPane(getAppContext().getApplicationWindow(), "New Time Series");
        assistant.show(new TimeSeriesAssistantPage_SourceProducts(assistantModel));
    }


}
