package org.esa.snap.ui.classfraction;

import com.bc.ceres.core.ProgressMonitor;
import eu.esa.snap.core.renderer.classfraction.ClassFractionRendererConfig;
import eu.esa.snap.core.renderer.classfraction.ClassFractionStatistics;
import eu.esa.snap.core.renderer.classfraction.ClassFractionStatisticsResult;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


/**
 * Class-specific bridge from a renderer configuration to original-band statistics.
 */
public final class ClassFractionStatisticsController {


    private final ClassFractionRendererConfig configuration;
    private final List<RasterDataNode> sources;
    private final Product product;


    /**
     * Creates a statistics context from an already configured renderer model.
     */
    public ClassFractionStatisticsController(ClassFractionRendererModel model) {
        this(model.createConfiguration(), rows(model), model.getProduct());
    }

    /**
     * Creates a statistics context for an existing restored renderer configuration.
     */
    public ClassFractionStatisticsController(ClassFractionRendererConfig configuration, List<RasterDataNode> sources, Product product) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.sources = Collections.unmodifiableList(new ArrayList<>(sources));
        this.product = Objects.requireNonNull(product, "product");
    }

    public ClassFractionRendererConfig getConfiguration() {
        return configuration;
    }

    public List<RasterDataNode> getSources() {
        return sources;
    }

    public List<Mask> getCompatibleRoiMasks() {
        List<Mask> masks = new ArrayList<>();
        RasterDataNode anchor = sources.get(0);

        for (int i = 0; i < product.getMaskGroup().getNodeCount(); i++) {
            Mask mask = product.getMaskGroup().get(i);

            if (mask.getRasterWidth() == anchor.getRasterWidth()
                    && mask.getRasterHeight() == anchor.getRasterHeight()
                    && mask.getImageToModelTransform().equals(anchor.getImageToModelTransform())) {
                masks.add(mask);
            }
        }
        return Collections.unmodifiableList(masks);
    }

    public ClassFractionStatisticsResult compute(Mask roiMask, int histogramBinCount, ProgressMonitor progressMonitor) {
        if (roiMask != null && !getCompatibleRoiMasks().contains(roiMask)) {
            throw new IllegalArgumentException("Selected ROI mask is not compatible with the configured class grid");
        }
        return ClassFractionStatistics.compute(configuration, sources, roiMask, histogramBinCount, progressMonitor);
    }

    private static List<RasterDataNode> rows(ClassFractionRendererModel model) {
        List<RasterDataNode> sources = new ArrayList<>();
        for (ClassFractionRendererModel.ClassBandRow row : model.getRows()) {
            sources.add(row.getBand());
        }
        return sources;
    }
}
