package org.esa.snap.rcp.placemark;


import com.bc.ceres.swing.figure.Figure;
import com.bc.ceres.swing.figure.FigureCollection;
import com.bc.ceres.swing.figure.support.DefaultFigureStyle;
import java.awt.Color;
import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.core.datamodel.PlacemarkGroup;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.colormanip.DistinctColorGenerator;
import org.esa.snap.ui.product.ProductSceneView;

public class PlacemarkUtils {

    private static final DistinctColorGenerator COLOR_GENERATOR = new DistinctColorGenerator();

    public static Color getPlacemarkColor(Placemark placemark) {
        return getPlacemarkColor(placemark, SnapApp.getDefault().getSelectedProductSceneView());
    }

    public static Color getPlacemarkColor(Placemark placemark, ProductSceneView view) {
        final String styleCss = placemark.getStyleCss();
        if (styleCss.contains(DefaultFigureStyle.FILL_COLOR.getName())) {
            return DefaultFigureStyle.createFromCss(styleCss).getFillColor();
        }
        return COLOR_GENERATOR.getNextDistinctColor();
    }

    private static Figure[] getFigures(ProductSceneView view) {
        if (view == null) {
            return new Figure[0];
        }
        final FigureCollection figureCollection = view.getFigureEditor().getFigureCollection();
        return figureCollection.getFigures();
    }

    public static void rotatePinColor(Placemark placemark, Product product) {
        boolean isPin = placemark.getDescriptor().getRoleName().equalsIgnoreCase("pin");
        if (isPin && placemark.getStyleCss().isEmpty()) {
            // rotate the color for pins
            PlacemarkGroup placemarkGroup = product.getPinGroup();
            String defaultStyleCss = placemarkGroup.getVectorDataNode().getDefaultStyleCss();
            DefaultFigureStyle placemarkStyle = new DefaultFigureStyle(defaultStyleCss);
            placemarkStyle.setFillColor(COLOR_GENERATOR.getNextDistinctColor());
            placemark.setStyleCss(placemarkStyle.toCssString());
        }
    }
}
