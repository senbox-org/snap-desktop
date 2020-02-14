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


/**

 */
public class WorldMapUI {

    private final NestWorldMapPaneDataModel worldMapDataModel;
    private final NestWorldMapPane worlMapPane;

    public WorldMapUI() {

        worldMapDataModel = new NestWorldMapPaneDataModel();
        worlMapPane = new NestWorldMapPane(worldMapDataModel);
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

    public NestWorldMapPane getWorlMapPane() {
        return worlMapPane;
    }

    public NestWorldMapPaneDataModel getModel() {
        return worldMapDataModel;
    }

}
