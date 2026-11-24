package org.esa.snap.ui.classfraction;

import eu.esa.snap.core.renderer.classfraction.ClassBandConfig;
import eu.esa.snap.core.renderer.classfraction.ClassBandGroupConfig;
import eu.esa.snap.core.renderer.classfraction.ClassFractionRendererConfig;
import eu.esa.snap.core.renderer.classfraction.ClassFractionRendererValidator;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


/**
 * Editable, class-renderer-specific UI state.  It deliberately has no RGB or ImageInfo state.
 */
public final class ClassFractionRendererModel {


    private static final Color[] DEFAULT_COLORS = {
            new Color(228, 26, 28), new Color(55, 126, 184), new Color(77, 175, 74),
            new Color(152, 78, 163), new Color(255, 127, 0), new Color(166, 86, 40),
            new Color(247, 129, 191), new Color(153, 153, 153)
    };

    private final Product product;
    private final List<ClassBandRow> rows;
    private String displayName;
    private double displayScale;
    private ClassBandGroupConfig.ValidityPolicy validityPolicy;


    public ClassFractionRendererModel(Product product) {
        this.product = Objects.requireNonNull(product, "product");
        this.rows = new ArrayList<>();
        this.displayName = product.getName() + " Class Fractions";
        this.displayScale = 1.0;
        this.validityPolicy = ClassBandGroupConfig.ValidityPolicy.STRICT_SOURCE_VALIDITY;
        Arrays.stream(product.getBands()).limit(2).forEach(this::addBand);
    }


    public Product getProduct() {
        return product;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public double getDisplayScale() {
        return displayScale;
    }

    public void setDisplayScale(double displayScale) {
        this.displayScale = displayScale;
    }

    public ClassBandGroupConfig.ValidityPolicy getValidityPolicy() {
        return validityPolicy;
    }

    public void setValidityPolicy(ClassBandGroupConfig.ValidityPolicy validityPolicy) {
        this.validityPolicy = Objects.requireNonNull(validityPolicy, "validityPolicy");
    }

    public List<ClassBandRow> getRows() {
        return Collections.unmodifiableList(rows);
    }

    public List<Band> getAvailableBands() {
        List<Band> available = new ArrayList<>();
        for (Band band : product.getBands()) {
            if (!contains(band)) {
                available.add(band);
            }
        }
        return available;
    }

    public void addBand(Band band) {
        if (band == null || band.getProduct() != product) {
            throw new IllegalArgumentException("A band from the selected product is required");
        }
        if (contains(band)) {
            throw new IllegalArgumentException("Band is already selected: " + band.getName());
        }

        rows.add(new ClassBandRow(band, band.getName(), DEFAULT_COLORS[(rows.size()) % DEFAULT_COLORS.length], true));
    }

    public void removeBand(int row) {
        rows.remove(row);
    }

    /**
     * Replaces this editable state with a compatible saved class-renderer configuration.
     *
     * @return {@code true} if every configured source can be restored from this product
     */
    public boolean applyConfiguration(ClassFractionRendererConfig configuration) {
        Objects.requireNonNull(configuration, "configuration");
        List<ClassBandRow> restoredRows = new ArrayList<>();
        List<RasterDataNode> sources = new ArrayList<>();

        for (ClassBandConfig configuredBand : configuration.getClassBandGroup().getBands()) {
            if (!product.getName().equals(configuredBand.getProductName())) {
                return false;
            }
            Band band = product.getBand(configuredBand.getBandName());
            if (band == null || contains(restoredRows, band)) {
                return false;
            }
            restoredRows.add(new ClassBandRow(band, configuredBand.getDisplayName(), new Color(configuredBand.getArgb(), true), configuredBand.isVisible()));
            sources.add(band);
        }

        try {
            ClassFractionRendererValidator.validate(configuration, sources);
        } catch (IllegalArgumentException ignored) {
            return false;
        }

        rows.clear();
        rows.addAll(restoredRows);
        displayName = configuration.getDisplayName();
        displayScale = configuration.getClassBandGroup().getDisplayScale();
        validityPolicy = configuration.getClassBandGroup().getValidityPolicy();
        return true;
    }

    public ClassFractionRendererConfig createConfiguration() {
        List<ClassBandConfig> bands = new ArrayList<>();
        List<RasterDataNode> sources = new ArrayList<>();

        for (ClassBandRow row : rows) {
            bands.add(new ClassBandConfig(product.getName(), row.getBand().getName(), row.getDisplayName(), row.getColor().getRGB(), row.isVisible()));
            sources.add(row.getBand());
        }

        ClassBandGroupConfig group = new ClassBandGroupConfig(bands, displayScale, validityPolicy);
        ClassFractionRendererConfig config = new ClassFractionRendererConfig(displayName, group);
        ClassFractionRendererValidator.validate(config, sources);
        return config;
    }

    private boolean contains(Band candidate) {
        return contains(rows, candidate);
    }

    private static boolean contains(List<ClassBandRow> rows, Band candidate) {
        for (ClassBandRow row : rows) {
            if (row.getBand() == candidate) {
                return true;
            }
        }
        return false;
    }

    public static final class ClassBandRow {

        private final Band band;
        private String displayName;
        private Color color;
        private boolean visible;

        private ClassBandRow(Band band, String displayName, Color color, boolean visible) {
            this.band = band;
            this.displayName = displayName;
            this.color = color;
            this.visible = visible;
        }

        public Band getBand() {
            return band;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public Color getColor() {
            return color;
        }

        public void setColor(Color color) {
            this.color = Objects.requireNonNull(color, "color");
        }

        public boolean isVisible() {
            return visible;
        }

        public void setVisible(boolean visible) {
            this.visible = visible;
        }

    }
}
