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
package org.esa.snap.grapheditor.gpf.ui.worldmap;

import org.esa.snap.core.datamodel.GeoPos;

import javax.swing.event.MouseInputAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**

 */
public class WorldMapUI {

    private final NestWorldMapPaneDataModel worldMapDataModel;
    private final NestWorldMapPane worlMapPane;

    private final List<WorldMapUIListener> listenerList = new ArrayList<>(1);

    public WorldMapUI() {

        worldMapDataModel = new NestWorldMapPaneDataModel();
        worlMapPane = new NestWorldMapPane(worldMapDataModel);
        worlMapPane.getLayerCanvas().addMouseListener(new MouseHandler());
    }

    public GeoPos[] getSelectionBox() {
        return worldMapDataModel.getSelectionBox();
    }

    public void setSelectionStart(final double lat, final double lon) {
        worldMapDataModel.setSelectionBoxStart(lat, lon);
    }

    public void setSelectionEnd(final double lat, final double lon) {
        worldMapDataModel.setSelectionBoxEnd(lat, lon);
    }

    public void setAdditionalGeoBoundaries(final GeoPos[][] geoBoundaries) {
        worldMapDataModel.setAdditionalGeoBoundaries(geoBoundaries);
    }

    public void setSelectedGeoBoundaries(final GeoPos[][] geoBoundaries) {
        worldMapDataModel.setSelectedGeoBoundaries(geoBoundaries);
    }

    public NestWorldMapPane getWorlMapPane() {
        return worlMapPane;
    }

    public NestWorldMapPaneDataModel getModel() {
        return worldMapDataModel;
    }

    public void addListener(final WorldMapUIListener listener) {
        if (!listenerList.contains(listener)) {
            listenerList.add(listener);
        }
    }

    public void removeListener(final WorldMapUIListener listener) {
        listenerList.remove(listener);
    }

    private void notifyQuery() {
        for (final WorldMapUIListener listener : listenerList) {
            listener.notifyNewMapSelectionAvailable();
        }
    }

    public interface WorldMapUIListener {
        void notifyNewMapSelectionAvailable();
    }

    private class MouseHandler extends MouseInputAdapter {

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                notifyQuery();
            }
        }
    }
}
