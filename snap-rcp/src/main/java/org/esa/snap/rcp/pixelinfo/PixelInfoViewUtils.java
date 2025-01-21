package org.esa.snap.rcp.pixelinfo;

import org.esa.snap.ui.product.ProductSceneView;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.util.math.MathUtils;


import com.bc.ceres.multilevel.MultiLevelModel;

import java.awt.geom.Point2D;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;

import org.opengis.referencing.operation.TransformException;

/**
 * Utility class to compute pixel value in any raster
 */
public class PixelInfoViewUtils {

    // The following code is inspired from rcp.pixelinfo.PixelInfoViewModelUpdater

    /**
     * Compute the scene position according to the view raster
     *
     * @param view the current view
     * @param x    the current pixel's X coordinate
     * @param y    the current pixel's Y coordinate
     * @return the scene position
     */
    public static Point2D.Double computeScenePos(final ProductSceneView view,
                                                 final int x, final int y) {

        Point2D.Double scenePos;

        // Get the view raster (where the x and y where read)
        final RasterDataNode viewRaster = view.getRaster();

        // TBN: use the better resolution possible (level = 0)
        final AffineTransform i2mTransform = view.getBaseImageLayer().getImageToModelTransform(0);
        final Point2D modelP = i2mTransform.transform(new Point2D.Double(x + 0.5, y + 0.5), null);
        try {
            final Point2D sceneP = viewRaster.getModelToSceneTransform().transform(modelP, new Point2D.Double());
            scenePos = new Point2D.Double(sceneP.getX(), sceneP.getY());
        } catch (TransformException te) {
            scenePos = new Point2D.Double(Double.NaN, Double.NaN);
        }
        return scenePos;
    }

    /**
     * Get the pixel value for the current raster
     *
     * @param scenePos the scene position (in another raster)
     * @param raster   the current raster for which we want to find the pixel value
     * @return the pixel value for the current raster
     */
    public static String getPixelValue(final Point2D.Double scenePos, final RasterDataNode raster) {

        String pixelString;
        Point2D.Double modelPos = new Point2D.Double();
        try {
            raster.getSceneToModelTransform().transform(scenePos, modelPos);
            if (!(Double.isNaN(modelPos.getX()) || Double.isNaN(modelPos.getY()))) {

                final MultiLevelModel multiLevelModel = raster.getMultiLevelModel();
                final PixelPos rasterPos = (PixelPos) multiLevelModel.getModelToImageTransform(0).transform(modelPos, new PixelPos());
                final int pixelXForGrid = MathUtils.floorInt(rasterPos.getX());
                final int pixelYForGrid = MathUtils.floorInt(rasterPos.getY());
                if (coordinatesAreInRasterBounds(raster, pixelXForGrid, pixelYForGrid)) {
                    pixelString = raster.getPixelString(pixelXForGrid, pixelYForGrid);
                } else {
                    pixelString = RasterDataNode.INVALID_POS_TEXT;
                }
            } else {
                pixelString = RasterDataNode.INVALID_POS_TEXT;
            }
        } catch (TransformException e) {
            pixelString = RasterDataNode.INVALID_POS_TEXT;
        }
        return pixelString;
    }

    /**
     * Check if the (x,y) pixel coordinates are within the raster bounds
     *
     * @param raster the current raster
     * @param x      the pixel x in the raster resolution
     * @param y      the pixel y in the raster resolution
     * @return true if the pixel (x,y) belongs to the raster bounds, false otherwise
     */
    private static boolean coordinatesAreInRasterBounds(final RasterDataNode raster, final int x, final int y) {
        final RenderedImage levelImage = raster.getSourceImage().getImage(0);
        return x >= 0 && y >= 0 && x < levelImage.getWidth() && y < levelImage.getHeight();
    }
}
