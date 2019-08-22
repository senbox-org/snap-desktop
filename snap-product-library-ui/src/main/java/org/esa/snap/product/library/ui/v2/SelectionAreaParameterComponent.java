package org.esa.snap.product.library.ui.v2;

import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.graphbuilder.gpf.ui.worldmap.NestWorldMapPane;
import org.esa.snap.graphbuilder.gpf.ui.worldmap.WorldMapUI;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

/**
 * Created by jcoravu on 7/8/2019.
 */
public class SelectionAreaParameterComponent extends AbstractParameterComponent<Rectangle2D.Double> {

    private final WorldMapUI worldMapUI;

    public SelectionAreaParameterComponent(String parameterName, String parameterLabelText) {
        super(parameterName, parameterLabelText);

        this.worldMapUI = new WorldMapUI();
        this.worldMapUI.getWorlMapPane().getLayerCanvas().setBackground(Color.WHITE);
        this.worldMapUI.getWorlMapPane().getLayerCanvas().setOpaque(true);
    }

    @Override
    public NestWorldMapPane getComponent() {
        return this.worldMapUI.getWorlMapPane();
    }

//    private String getFootprint() {
//        final Rectangle.Double rect = dbQuery.getSelectionRectangle();
//        if(rect != null && rect.width != 0 && rect.height != 0) {
//            final StringBuilder str = new StringBuilder();
//            str.append("( footprint:\"Intersects(POLYGON((");
//
//            str.append(rect.y); // lat
//            str.append(' ');
//            str.append(rect.x); // lon
//            str.append(", ");
//
//            str.append(rect.y);
//            str.append(' ');
//            str.append(rect.x  + rect.width);
//            str.append(", ");
//
//            str.append(rect.y + rect.height);
//            str.append(' ');
//            str.append(rect.x + rect.width);
//            str.append(", ");
//
//            str.append(rect.y + rect.height);
//            str.append(' ');
//            str.append(rect.x);
//            str.append(", ");
//
//            str.append(rect.y);
//            str.append(' ');
//            str.append(rect.x);
//
//            str.append(")))\" )");
//            return str.toString();
//        } else if(rect != null) {
//            final StringBuilder str = new StringBuilder();
//            str.append("( footprint:\"Intersects(");
//
//            str.append(rect.y); // lat
//            str.append(", ");
//            str.append(rect.x); // lon
//
//            str.append(")\" )");
//            return str.toString();
//        }
//        return "";
//    }
    @Override
    public Rectangle2D.Double getParameterValue() {
        GeoPos[] geoPositions = this.worldMapUI.getSelectionBox();
        return getBoundingRect(geoPositions);
    }

    private static Rectangle2D.Double getBoundingRect(GeoPos[] geoPositions) {
        double minX = Float.MAX_VALUE;
        double maxX = -Float.MAX_VALUE;
        double minY = Float.MAX_VALUE;
        double maxY = -Float.MAX_VALUE;

        for (GeoPos pos : geoPositions) {
            double x = pos.getLat();
            double y = pos.getLon();
            if (x < minX) {
                minX = x;
            }
            if (x > maxX) {
                maxX = x;
            }
            if (y < minY) {
                minY = y;
            }
            if (y > maxY) {
                maxY = y;
            }
        }
        if (minX >= maxX || minY >= maxY) {
            return new Rectangle.Double(minX, minY, 0.0d, 0.0d);
        }
        return new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
    }
}
