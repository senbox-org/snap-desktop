package org.esa.snap.product.library.ui.v2.repository.local;

import gov.nasa.worldwind.geom.Angle;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.product.library.v2.database.ProductLibraryDAL;
import org.esa.snap.product.library.v2.database.SaveProductData;
import org.esa.snap.remote.products.repository.Polygon2D;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Created by jcoravu on 4/10/2019.
 */
public class SaveLocalProductsHelper {

    private static final Logger logger = Logger.getLogger(SaveLocalProductsHelper.class.getName());

    public SaveLocalProductsHelper() {
    }

    protected void finishSavingProduct(SaveProductData saveProductData) {
    }

    protected void invalidProduct(Path path) throws IOException {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "The path '"+path.toString()+"' does not represent a valid product.");
        }
    }

    protected void missingProductGeoCoding(Path path, Product product) throws IOException {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "The local product from the path '"+path.toString()+"' does not contain the geo-coding associated with the scene raster.");
        }
    }

    public List<SaveProductData> saveProductsFromFolder(Path localRepositoryFolderPath) throws IOException {
        List<SaveProductData> savedProducts = null;
        if (Files.exists(localRepositoryFolderPath)) {
            // the local repository folder exists on the disk
            try (Stream<Path> stream = Files.list(localRepositoryFolderPath)) {
                savedProducts = new ArrayList<>();
                Iterator<Path> it = stream.iterator();
                while (it.hasNext()) {
                    Path path = it.next();
                    try {
                        Product product = ProductIO.readProduct(path.toFile());
                        if (product == null) {
                            invalidProduct(path);
                        } else if (product.getSceneGeoCoding() == null) {
                            try {
                                product.dispose();
                            } finally {
                                missingProductGeoCoding(path, product);
                            }
                        } else {
                            try {
                                Polygon2D polygon2D = buildProductPolygon(product);
                                BufferedImage quickLookImage = null;
//                        try {
//                            QuicklookGenerator quicklookGenerator = new QuicklookGenerator();
//                            quickLookImage = quicklookGenerator.createQuickLookFromBrowseProduct(product);
//                        } catch (Exception exception) {
//                            logger.log(Level.SEVERE, "Failed to create the quick look image for product '" + product.getName() + "'.", exception);
//                        }

                                SaveProductData saveProductData = ProductLibraryDAL.saveProduct(product, quickLookImage, polygon2D, path, localRepositoryFolderPath);
                                savedProducts.add(saveProductData);

                                finishSavingProduct(saveProductData);
                            } finally {
                                product.dispose();
                            }
                        }
                    } catch (Exception exception) {
                        logger.log(Level.SEVERE, "Failed to save the local product from the path '" + path.toString() + "'.", exception);
                    }
                }
            }
        } else {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "The local repository folder '"+localRepositoryFolderPath.toString()+"' does not exist.");
            }
        }
        return savedProducts;
    }

    private static Polygon2D buildProductPolygon(Product product) {
        int productWidth = product.getSceneRasterWidth();
        int productHeight = product.getSceneRasterHeight();
        PixelPos[] pixelPositions = new PixelPos[5];
        pixelPositions[0] = new PixelPos(0.0d, 0.0d);
        pixelPositions[1] = new PixelPos(productWidth, 0.0d);
        pixelPositions[2] = new PixelPos(productWidth, productHeight);
        pixelPositions[3] = new PixelPos(0.0d, productHeight);
        pixelPositions[4] = new PixelPos(0.0d, 0.0d);

        GeoPos[] geographicalPositions = new GeoPos[pixelPositions.length];
        GeoCoding sceneGeoCoding = product.getSceneGeoCoding();
        for (int i = 0; i < pixelPositions.length; i++) {
            geographicalPositions[i] = sceneGeoCoding.getGeoPos(pixelPositions[i], null);
        }
        ProductUtils.normalizeGeoPolygon(geographicalPositions);

        Polygon2D polygon2D = new Polygon2D();
        for (int i = 0; i < geographicalPositions.length; i++) {
            Angle latitude = Angle.fromDegreesLatitude(geographicalPositions[i].getLat());
            Angle longitude = Angle.fromDegreesLongitude(geographicalPositions[i].getLon());
            polygon2D.append(longitude.getDegrees(), latitude.getDegrees());
        }
        return polygon2D;
    }
}
