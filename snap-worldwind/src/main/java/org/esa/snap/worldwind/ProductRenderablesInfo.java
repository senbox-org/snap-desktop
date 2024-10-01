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

    public final ArrayList<AnalyticSurface> owiAnalyticSurfaces = new ArrayList<>();
    public final ArrayList<AnalyticSurface> oswAnalyticSurfaces = new ArrayList<>();
    public final ArrayList<AnalyticSurface> rvlAnalyticSurfaces = new ArrayList<>();

    public final ArrayList<BufferWrapper> owiAnalyticSurfaceValueBuffers = new ArrayList<>();
    public final ArrayList<BufferWrapper> oswAnalyticSurfaceValueBuffers = new ArrayList<>();
    public final ArrayList<BufferWrapper> rvlAnalyticSurfaceValueBuffers = new ArrayList<>();

    public final HashMap<String, ArrayList<Renderable>> theRenderableListHash = new HashMap<>();

    public ProductRenderablesInfo() {
        super();

        theRenderableListHash.put("owi", new ArrayList<>());
        theRenderableListHash.put("osw", new ArrayList<>());
        theRenderableListHash.put("rvl", new ArrayList<>());
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
