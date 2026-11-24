package org.esa.snap.ui.classfraction;

import com.bc.ceres.annotation.STTM;
import eu.esa.snap.core.renderer.classfraction.ClassFractionRendererConfig;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;


public class ClassFractionRendererConfigStoreTest {


    @Test
    @STTM("SNAP-4339")
    public void remembersAConfigurationOnlyForItsProduct() {
        Product configuredProduct = product("configured");
        Product otherProduct = product("other");
        ClassFractionRendererConfig configuration = new ClassFractionRendererModel(configuredProduct).createConfiguration();

        ClassFractionRendererConfigStore.remember(configuredProduct, configuration);

        assertSame(configuration, ClassFractionRendererConfigStore.getLastConfiguration(configuredProduct));
        assertNull(ClassFractionRendererConfigStore.getLastConfiguration(otherProduct));
    }

    private static Product product(String name) {
        Product product = new Product(name, "test", 2, 1);
        product.addBand("first", ProductData.TYPE_FLOAT32);
        product.addBand("second", ProductData.TYPE_FLOAT32);
        return product;
    }
}
