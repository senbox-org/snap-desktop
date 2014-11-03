/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.esa.snap.core;

import java.awt.image.RenderedImage;

/**
 * @author Norman
 */
public class Band {
    Product product;
    String name;
    RenderedImage data;

    public Band(String name, RenderedImage data) {
        this.name = name;
        this.data = data;
    }

    public Band(Product product) {
        this.product = product;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RenderedImage getData() {
        return data;
    }

    public void setData(RenderedImage data) {
        this.data = data;
    }


}
