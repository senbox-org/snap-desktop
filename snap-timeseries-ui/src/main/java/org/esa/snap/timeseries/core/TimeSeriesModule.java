package org.esa.snap.timeseries.core;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.snap.core.dataio.ProductIOPlugInManager;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.dataio.ProductWriterPlugIn;
import org.esa.snap.core.dataio.dimap.*;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.timeseries.core.timeseries.datamodel.AbstractTimeSeries;
import org.esa.snap.timeseries.core.timeseries.datamodel.TimeSeriesFactory;
import org.openide.modules.OnStart;
import org.openide.modules.OnStop;

import java.awt.*;
import java.io.File;
import java.net.URI;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

import static org.esa.snap.timeseries.core.timeseries.datamodel.AbstractTimeSeries.TIME_SERIES_PRODUCT_TYPE;

public class TimeSeriesModule {

    public static final String TIME_PROPERTY = "timeProperty";

    private static final ProductManager.Listener productManagerListener = createProductManagerListener();
    private static final DimapProductWriter.WriterExtender writerExtender = createWriterExtender();
    private static final DimapProductReader.ReaderExtender readerExtender = createReaderExtender();


    @OnStart
    public static class StartOp implements Runnable {

        @Override
        public void run() {
            final ProductIOPlugInManager ioPlugInManager = ProductIOPlugInManager.getInstance();

            final Iterator<ProductWriterPlugIn> allWriterPlugIns = ioPlugInManager.getWriterPlugIns(DimapProductConstants.DIMAP_FORMAT_NAME);
            while (allWriterPlugIns.hasNext()) {
                DimapProductWriterPlugIn writerPlugIn = (DimapProductWriterPlugIn) allWriterPlugIns.next();
                writerPlugIn.addWriterExtender(writerExtender);
            }

            final Iterator<ProductReaderPlugIn> readerPlugIns = ioPlugInManager.getReaderPlugIns(DimapProductConstants.DIMAP_FORMAT_NAME);
            while (readerPlugIns.hasNext()) {
                DimapProductReaderPlugIn readerPlugin = (DimapProductReaderPlugIn) readerPlugIns.next();
                readerPlugin.addReaderExtender(readerExtender);
            }

            SnapApp.getDefault().getProductManager().addListener(productManagerListener);
        }
    }

    @OnStop
    public static class StopOp implements Runnable {

        @Override
        public void run() {
            final ProductIOPlugInManager ioPlugInManager = ProductIOPlugInManager.getInstance();

            final Iterator<ProductWriterPlugIn> writerPlugIns = ioPlugInManager.getWriterPlugIns(DimapProductConstants.DIMAP_FORMAT_NAME);
            while (writerPlugIns.hasNext()) {
                DimapProductWriterPlugIn writerPlugIn = (DimapProductWriterPlugIn) writerPlugIns.next();
                writerPlugIn.removeWriterExtender(writerExtender);
            }

            final Iterator<ProductReaderPlugIn> readerPlugIns = ioPlugInManager.getReaderPlugIns(DimapProductConstants.DIMAP_FORMAT_NAME);
            while (readerPlugIns.hasNext()) {
                DimapProductReaderPlugIn readerPlugIn = (DimapProductReaderPlugIn) readerPlugIns.next();
                readerPlugIn.removeReaderExtender(readerExtender);
            }

            SnapApp.getDefault().getProductManager().removeListener(productManagerListener);
        }
    }

    static void convertAbsolutPathsToRelative(Product product, File outputDir) {
        final MetadataElement tsRootElem = product.getMetadataRoot().getElement(AbstractTimeSeries.TIME_SERIES_ROOT_NAME);

        final MetadataElement productLocations = tsRootElem.getElement(AbstractTimeSeries.PRODUCT_LOCATIONS);
        final MetadataElement sourceProductPaths = tsRootElem.getElement(AbstractTimeSeries.SOURCE_PRODUCT_PATHS);

        replaceWithRelativePaths(productLocations.getElements(), outputDir);
        replaceWithRelativePaths(sourceProductPaths.getElements(), outputDir);
    }


    private static ProductManager.Listener createProductManagerListener() {
        return new ProductManager.Listener() {
            @Override
            public void productAdded(ProductManager.Event event) {
            }

            @Override
            public void productRemoved(ProductManager.Event event) {
                final Product product = event.getProduct();
                if (product.getProductType().equals(TIME_SERIES_PRODUCT_TYPE)) {
                    final TimeSeriesMapper timeSeriesMapper = TimeSeriesMapper.getInstance();
                    final AbstractTimeSeries timeSeries = timeSeriesMapper.getTimeSeries(product);
                    final Product[] sourceProducts = timeSeries.getSourceProducts();
                    final ProductManager productManager = SnapApp.getDefault().getProductManager();
                    for (Product sourceProduct : sourceProducts) {
                        if (!productManager.contains(sourceProduct)) {
                            sourceProduct.dispose();
                        }
                    }
                    timeSeriesMapper.remove(product);
                }
            }
        };
    }

    private static DimapProductWriter.WriterExtender createWriterExtender() {
        return new DimapProductWriter.WriterExtender() {
            @Override
            public boolean vetoableShouldWrite(ProductNode node) {
                if (!isTimeSerisProduct(node.getProduct())) {
                    return true;
                } else if (node instanceof RasterDataNode) {
                    return false;
                }
                return true;
            }

            @Override
            public void intendToWriteDimapHeaderTo(File outputDir, Product product) {
                if (isTimeSerisProduct(product)) {
                    convertAbsolutPathsToRelative(product, outputDir);
                }
            }

            private boolean isTimeSerisProduct(Product product) {
                return product.getProductType().equals(AbstractTimeSeries.TIME_SERIES_PRODUCT_TYPE);
            }
        };
    }

    private static void replaceWithRelativePaths(MetadataElement[] elements, File outputDir) {
        for (MetadataElement element : elements) {
            final String pathName = element.getAttributeString(AbstractTimeSeries.PL_PATH);
            final URI relativeUri = FileUtils.getRelativeUri(outputDir.toURI(), new File(pathName));

            final MetadataAttribute pathAttr = element.getAttribute(AbstractTimeSeries.PL_PATH);
            final MetadataAttribute typeAttr = element.getAttribute(AbstractTimeSeries.PL_TYPE);

            element.removeAttribute(pathAttr);
            element.removeAttribute(typeAttr);

            pathAttr.dispose();
            final MetadataAttribute newPathAttr = new MetadataAttribute(AbstractTimeSeries.PL_PATH, ProductData.createInstance(relativeUri.toString()), true);

            element.addAttribute(newPathAttr);
            element.addAttribute(typeAttr);
        }
    }

    private static DimapProductReader.ReaderExtender createReaderExtender() {
        return new DimapProductReader.ReaderExtender() {
            @Override
            public void completeProductNodesReading(final Product product) {
                if (product.getProductType().equals(TIME_SERIES_PRODUCT_TYPE)) {

                    final SnapApp snapApp = SnapApp.getDefault();
                    final Window appWindow = snapApp.getMainFrame();
                    final ProgressMonitorSwingWorker<AbstractTimeSeries, Void> pm = new ProgressMonitorSwingWorker<AbstractTimeSeries, Void>(appWindow, "Creating Time Series...") {
                        @Override
                        protected void done() {
                            try {
                                get();
                            } catch (InterruptedException e) {
                                handleError(e);
                            } catch (ExecutionException e) {
                                handleError(e.getCause());
                            }
                            super.done();
                        }

                        private void handleError(Throwable theCause) {
                            snapApp.getLogger().log(Level.SEVERE, theCause.getMessage());
                            Dialogs.showError("Could not load time series", theCause.getMessage());
                        }

                        @Override
                        protected AbstractTimeSeries doInBackground(ProgressMonitor pm) throws Exception {
                            return TimeSeriesFactory.create(product, pm);
                        }
                    };
                    pm.executeWithBlocking();
                }
            }
        };
    }

}
