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

package org.esa.snap.rcp.session;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;

import java.awt.geom.Point2D;

/**
 * A converter for {@link java.awt.Shape}s.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public class PointConverter implements Converter {
    private final GeometryFactory geometryFactory;

    public PointConverter() {
        geometryFactory = new GeometryFactory();
    }

    @Override
    public Class getValueType() {
        return Point2D.class;
    }

    @Override
    public Object parse(String text) throws ConversionException {
        try {
            Geometry geometry = new WKTReader(geometryFactory).read(text);
            if (geometry instanceof org.locationtech.jts.geom.Point) {
                org.locationtech.jts.geom.Point point = (org.locationtech.jts.geom.Point) geometry;
                return new Point2D.Double(point.getX(), point.getY());
            } else {
                throw new ConversionException("Failed to parse point geometry WKT.");
            }
        } catch (ParseException e) {
            throw new ConversionException("Failed to parse point geometry WKT.", e);
        }
    }

    @Override
    public String format(Object value) {
        Point2D point = (Point2D) value;
        return new WKTWriter().write(geometryFactory.createPoint(new Coordinate(point.getX(), point.getY())));
    }
}
