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

import gov.nasa.worldwindx.examples.util.DirectedPath;

/**

 */
public class ArrowInfo {

    public DirectedPath theDirectedPath = null;
    public double theAvgIncAngle;
    public double theAvgWindSpeed;
    public double theAvgWindDir;
    public double theArrowLength;

    public ArrowInfo(DirectedPath directedPath, double avgIncAngle, double avgWindSpeed, double avgWindDir, double arrowLength_deg) {
        super();
        theDirectedPath = directedPath;
        theAvgIncAngle = avgIncAngle;
        theAvgWindSpeed = avgWindSpeed;
        theAvgWindDir = avgWindDir;
        theArrowLength = arrowLength_deg;
    }

}
