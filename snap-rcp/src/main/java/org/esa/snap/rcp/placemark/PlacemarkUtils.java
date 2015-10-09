package org.esa.snap.rcp.placemark;


import com.bc.ceres.swing.figure.Figure;
import com.bc.ceres.swing.figure.FigureCollection;
import com.bc.ceres.swing.figure.support.DefaultFigureStyle;
import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.product.ProductSceneView;
import org.esa.snap.ui.product.SimpleFeaturePointFigure;
import org.opengis.feature.simple.SimpleFeature;

import java.awt.Color;

public class PlacemarkUtils {

    public static Color getPlacemarkColor(Placemark placemark) {
        return getPlacemarkColor(placemark, SnapApp.getDefault().getSelectedProductSceneView());
    }

    public static Color getPlacemarkColor(Placemark placemark, ProductSceneView view) {
        final String styleCss = placemark.getStyleCss();
        if (styleCss.contains(DefaultFigureStyle.FILL_COLOR.getName())) {
            return DefaultFigureStyle.createFromCss(styleCss).getFillColor();
        }
        final Figure[] figures = getFigures(view);
        for (Figure figure : figures) {
            if (figure instanceof SimpleFeaturePointFigure) {
                final SimpleFeature simpleFeature = ((SimpleFeaturePointFigure) figure).getSimpleFeature();
                if (simpleFeature.getID().equals(placemark.getName())) {
                    return figure.getNormalStyle().getFillColor();
                }
            }
        }
        return Color.BLUE;
    }

    private static Figure[] getFigures(ProductSceneView view) {
        if (view == null) {
            return new Figure[0];
        }
        final FigureCollection figureCollection = view.getFigureEditor().getFigureCollection();
        return figureCollection.getFigures();
    }

}
