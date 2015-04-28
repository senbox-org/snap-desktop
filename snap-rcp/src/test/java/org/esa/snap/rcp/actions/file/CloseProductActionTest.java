package org.esa.snap.rcp.actions.file;

import org.esa.snap.framework.dataio.ProductSubsetDef;
import org.esa.snap.framework.datamodel.Product;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 */
public class CloseProductActionTest {

    @Test
    public void testFindFirstSourceProduct() throws Exception {
        Product toBeClosedProduct = new Product("ToBeClosed", "Type", 100, 100);
        Product dependentProduct = createDependentProduct(toBeClosedProduct);
        HashSet<Product> stillOpen = new HashSet<>(Collections.singletonList(dependentProduct));

        Product firstSourceProduct = CloseProductAction.findFirstSourceProduct(toBeClosedProduct, stillOpen);
        assertEquals(dependentProduct, firstSourceProduct);
    }

    private Product createDependentProduct(Product toBeClosedProduct) throws IOException {
        ProductSubsetDef subset = new ProductSubsetDef("subset");
        subset.setSubSampling(2,2);
        return toBeClosedProduct.createSubset(subset, "dependent", "none");
    }

}