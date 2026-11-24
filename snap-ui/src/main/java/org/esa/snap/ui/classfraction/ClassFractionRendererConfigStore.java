package org.esa.snap.ui.classfraction;

import eu.esa.snap.core.renderer.classfraction.ClassFractionImageLayerType;
import eu.esa.snap.core.renderer.classfraction.ClassFractionRendererConfig;
import eu.esa.snap.core.renderer.classfraction.ClassFractionRendererConfigJson;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.ui.product.ProductSceneView;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;


/**
 * Holds the most recently used configuration per open product for the current application run.
 */
public final class ClassFractionRendererConfigStore {


    private static final Map<Product, ClassFractionRendererConfig> LAST_CONFIGURATIONS = Collections.synchronizedMap(new WeakHashMap<>());


    private ClassFractionRendererConfigStore() {
    }

    public static void remember(Product product, ClassFractionRendererConfig configuration) {
        LAST_CONFIGURATIONS.put(product, configuration);
    }

    public static ClassFractionRendererConfig getLastConfiguration(Product product) {
        return LAST_CONFIGURATIONS.get(product);
    }

    /**
     * Returns the configuration embedded in the selected matching class-fraction view, if any.
     */
    public static ClassFractionRendererConfig getViewConfiguration(Product product, ProductSceneView view) {
        if (view == null
                || view.getNumRasters() == 0
                || view.getRasters()[0].getProduct() != product
                || view.getBaseImageLayer() == null
                || !(view.getBaseImageLayer().getLayerType() instanceof ClassFractionImageLayerType)) {
            return null;
        }

        String serialized = view.getBaseImageLayer().getConfiguration().getValue(ClassFractionImageLayerType.PROPERTY_NAME_CONFIG_JSON);
        if (serialized == null) {
            return null;
        }

        try {
            return ClassFractionRendererConfigJson.decode(serialized);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
