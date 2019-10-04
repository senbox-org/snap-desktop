package org.esa.snap.product.library.ui.v2.repository.local;

import gov.nasa.worldwind.geom.Angle;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.product.library.ui.v2.thread.AbstractProgressTimerRunnable;
import org.esa.snap.product.library.ui.v2.thread.ProgressBarHelper;
import org.esa.snap.product.library.v2.database.ProductLibraryDAL;
import org.esa.snap.product.library.v2.database.SaveProductData;
import org.esa.snap.remote.products.repository.Polygon2D;
import org.esa.snap.ui.loading.GenericRunnable;

import javax.swing.SwingUtilities;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Created by jcoravu on 3/10/2019.
 */
public class AddLocalRepositoryTimerRunnable extends AbstractProgressTimerRunnable<Void> {

    private static final Logger logger = Logger.getLogger(AddLocalRepositoryTimerRunnable.class.getName());

    private final Path localRepositoryFolderPath;

    public AddLocalRepositoryTimerRunnable(ProgressBarHelper progressPanel, int threadId, Path localRepositoryFolderPath) {
        super(progressPanel, threadId, 500);

        this.localRepositoryFolderPath = localRepositoryFolderPath;
    }

    @Override
    protected Void execute() throws Exception {
        updateProgressBarTextLater("");

        try (Stream<Path> stream = Files.list(this.localRepositoryFolderPath)) {
            Iterator<Path> it = stream.iterator();
            while (it.hasNext()) {
                Path path = it.next();
                try {
                    Product product = ProductIO.readProduct(path.toFile());
                    if (product == null) {
                        if (logger.isLoggable(Level.FINE)) {
                            logger.log(Level.FINE, "The path '"+path.toString()+"' does not represent a valid product.");
                        }
                    } else {
                        if (product.getSceneGeoCoding() == null) {
                            if (logger.isLoggable(Level.FINE)) {
                                logger.log(Level.FINE, "The local product from the path '"+path.toString()+"' does not contain the geo-coding associated with the scene raster.");
                            }
                        } else {
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
                            for (int i=0; i<pixelPositions.length; i++) {
                                geographicalPositions[i] = sceneGeoCoding.getGeoPos(pixelPositions[i], null);
                            }
                            ProductUtils.normalizeGeoPolygon(geographicalPositions);

                            Polygon2D polygon2D = new Polygon2D();
                            for (int i=0; i<geographicalPositions.length; i++) {
                                Angle latitude = Angle.fromDegreesLatitude(geographicalPositions[i].getLat());
                                Angle longitude = Angle.fromDegreesLongitude(geographicalPositions[i].getLon());
                                polygon2D.append(longitude.getDegrees(), latitude.getDegrees());
                            }
                            BufferedImage quickLookImage = null;
//                        try {
//                            QuicklookGenerator quicklookGenerator = new QuicklookGenerator();
//                            quickLookImage = quicklookGenerator.createQuickLookFromBrowseProduct(product);
//                        } catch (Exception exception) {
//                            logger.log(Level.SEVERE, "Failed to create the quick look image for product '" + product.getName() + "'.", exception);
//                        }

                            SaveProductData saveProductData = ProductLibraryDAL.saveProduct(product, quickLookImage, polygon2D, path, this.localRepositoryFolderPath);

                            updateFinishSavingProductDataLater(saveProductData);
                        }
                    }

                } catch (Exception exception) {
                    logger.log(Level.SEVERE, "Failed to add the local product from the path '" + path.toString() + "'.", exception);
                }
            }
        }
        return null;
    }

    @Override
    protected String getExceptionLoggingMessage() {
        return "Failed to add the local repository folder '"+this.localRepositoryFolderPath.toString()+"'.";
    }

    protected void onFinishSavingProduct(SaveProductData saveProductData) {
    }

    private void updateFinishSavingProductDataLater(SaveProductData saveProductData) {
        GenericRunnable<SaveProductData> runnable = new GenericRunnable<SaveProductData>(saveProductData) {
            @Override
            protected void execute(SaveProductData item) {
                onFinishSavingProduct(item);
            }
        };
        SwingUtilities.invokeLater(runnable);
    }
}
