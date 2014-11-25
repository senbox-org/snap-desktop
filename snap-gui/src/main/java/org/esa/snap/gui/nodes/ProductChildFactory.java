/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.nodes;

import org.esa.beam.framework.datamodel.Product;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;

import java.beans.IntrospectionException;
import java.util.ArrayList;
import java.util.List;


public class ProductChildFactory extends ChildFactory<Product> {

    final static ProductChildFactory instance = new ProductChildFactory();

    List<Product> products;

    public static ProductChildFactory getInstance() {
        return instance;
    }


    private ProductChildFactory() {
        products = new ArrayList<>();
    }

    public void addProduct(Product product) {
        products.add(product);
        refresh(true);
    }

    @Override
    protected boolean createKeys(List<Product> list) {
        list.addAll(products);
        Product p1 = new Product("Product_1", "Type_1", 2048, 1024);
        p1.addBand("Band_A", "X*X - Y*Y");
        p1.addBand("Band_B", "2*X*Y");
        Product p2 = new Product("Product_2", "Type_2", 1024, 2048);
        p2.addBand("Band_1", "cos(X/100)-sin(Y/100)");
        p2.addBand("Band_2", "sin(X/100)+cos(Y/100)");
        p2.addBand("Band_3", "cos(X/100)*cos(Y/100)");
        list.add(p1);
        list.add(p2);
        return true;
    }

    @Override
    protected Node createNodeForKey(Product key) {
        ProductNode node = null;
        try {
            node = new ProductNode(key);
        } catch (IntrospectionException ex) {
            Exceptions.printStackTrace(ex);
        }
        return node;
    }
}
