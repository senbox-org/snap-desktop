/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.worldwind;

import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.util.BufferWrapper;
import gov.nasa.worldwindx.examples.analytics.AnalyticSurface;

import java.util.ArrayList;
import java.util.HashMap;

/**

 */
public class ProductRenderablesInfo {

    public ArrayList<AnalyticSurface> owiAnalyticSurfaces = null;
    public ArrayList<AnalyticSurface> oswAnalyticSurfaces = null;
    public ArrayList<AnalyticSurface> rvlAnalyticSurfaces = null;

    public ArrayList<BufferWrapper> owiAnalyticSurfaceValueBuffers = null;
    public ArrayList<BufferWrapper> oswAnalyticSurfaceValueBuffers = null;
    public ArrayList<BufferWrapper> rvlAnalyticSurfaceValueBuffers = null;

    public HashMap<String, ArrayList<Renderable>> theRenderableListHash;

    public ProductRenderablesInfo() {
        super();

        theRenderableListHash = new HashMap<>();
        theRenderableListHash.put("owi", new ArrayList<>());
        theRenderableListHash.put("osw", new ArrayList<>());
        theRenderableListHash.put("rvl", new ArrayList<>());

        owiAnalyticSurfaces = new ArrayList<>();
        oswAnalyticSurfaces = new ArrayList<>();
        rvlAnalyticSurfaces = new ArrayList<>();

        owiAnalyticSurfaceValueBuffers = new ArrayList<>();
        oswAnalyticSurfaceValueBuffers = new ArrayList<>();
        rvlAnalyticSurfaceValueBuffers = new ArrayList<>();
    }

    public void setAnalyticSurfaceAndBuffer(AnalyticSurface analyticSurface, BufferWrapper analyticSurfaceValueBuffer, String comp) {
        if (comp.equalsIgnoreCase("owi")) {
            //owiAnalyticSurface = analyticSurface;
            //owiAnalyticSurfaceValueBuffer = analyticSurfaceValueBuffer;

            owiAnalyticSurfaces.add(analyticSurface);
            owiAnalyticSurfaceValueBuffers.add(analyticSurfaceValueBuffer);
        } else if (comp.equalsIgnoreCase("osw")) {
            //oswAnalyticSurface = analyticSurface;
            //oswAnalyticSurfaceValueBuffer = analyticSurfaceValueBuffer;

            oswAnalyticSurfaces.add(analyticSurface);
            oswAnalyticSurfaceValueBuffers.add(analyticSurfaceValueBuffer);
        } else if (comp.equalsIgnoreCase("rvl")) {
            //rvlAnalyticSurface = analyticSurface;
            //rvlAnalyticSurfaceValueBuffer = analyticSurfaceValueBuffer;

            rvlAnalyticSurfaces.add(analyticSurface);
            rvlAnalyticSurfaceValueBuffers.add(analyticSurfaceValueBuffer);
        }
    }
}
