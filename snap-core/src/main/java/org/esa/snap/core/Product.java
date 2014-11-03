/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.esa.snap.core;

import org.esa.snap.core.io.ProductReader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Norman
 */
public class Product {
    String name;
    List<Band> bands;
    ProductReader productReader;

    public Product(String name, Band... bands) {
        this.name = name;
        this.bands = new ArrayList<>(bands.length);
        for (Band band : bands) {
            addBand(band);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getBandCount() {
        return bands.size();
    }

    public List<Band> getBands() {
        return bands;
    }

    public void addBand(Band band) {
        if (this.bands.add(band)) {
            band.setProduct(this);
        }
    }

    public void addBands(Collection<Band> bands) {
        for (Band band : bands) {
            addBand(band);
        }
    }

    public void removeBand(Band band) {
        if (this.bands.remove(band)) {
            band.setProduct(null);
        }
    }

    public void removeBands(Collection<Band> bands) {
        for (Band band : bands) {
            removeBand(band);
        }
    }

    public ProductReader getProductReader() {
        return productReader;
    }

    public void setProductReader(ProductReader productReader) {
        this.productReader = productReader;
    }

}
