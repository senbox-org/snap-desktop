package org.esa.snap.product.library.ui.v2.repository.output;

import org.esa.snap.remote.products.repository.RepositoryProduct;

import java.util.Comparator;

/**
 * Created by jcoravu on 30/1/2020.
 */
public interface OutputProductResultsCallback {

    public Comparator<RepositoryProduct> getProductsComparator();

    public OutputProductResults getOutputProductResults();
}
