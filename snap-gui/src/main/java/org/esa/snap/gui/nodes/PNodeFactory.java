/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.nodes;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;

import java.beans.IntrospectionException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class PNodeFactory extends ChildFactory<Product> {

    private final static PNodeFactory instance = new PNodeFactory();

    private final List<Product> products;

    public static PNodeFactory getInstance() {
        return instance;
    }

    private PNodeFactory() {
        products = new ArrayList<>();
        addTestProducts();
    }

    public void addProduct(Product product) {
        products.add(product);
        refresh(true);
    }

    @Override
    protected boolean createKeys(List<Product> list) {
        list.addAll(products);
        return true;
    }

    @Override
    protected Node createNodeForKey(Product key) {
        PNode node = null;
        try {
            node = new PNode(key);
        } catch (IntrospectionException ex) {
            Exceptions.printStackTrace(ex);
        }
        return node;
    }

    private void addTestProducts() {
        Product p1 = new Product("Product_1", "Type_1", 2048, 1024);
        p1.addTiePointGrid(new TiePointGrid("Grid_A", 32, 16, 0, 0, 2048f / 32, 1024f / 16, createRandomPoints(32 * 16)));
        p1.addTiePointGrid(new TiePointGrid("Grid_B", 32, 16, 0, 0, 2048f / 32, 1024f / 16, createRandomPoints(32 * 16)));
        p1.addBand("Band_A", "sin(0.01 * (X*X - Y*Y))");
        p1.addBand("Band_B", "cos(0.01 * 2 * X*Y)");

        Product p2 = new Product("Product_2", "Type_2", 1024, 2048);
        p2.addTiePointGrid(new TiePointGrid("Grid_1", 16, 32, 0, 0, 1024f / 16, 2048f / 32, createRandomPoints(32 * 16)));
        p2.addTiePointGrid(new TiePointGrid("Grid_2", 16, 32, 0, 0, 1024f / 16, 2048f / 32, createRandomPoints(32 * 16)));
        p2.addBand("Band_1", "cos(X/100)-sin(Y/100)");
        p2.addBand("Band_2", "sin(X/100)+cos(Y/100)");
        p2.addBand("Band_3", "cos(X/100)*cos(Y/100)");

        products.add(p1);
        products.add(p2);
    }

    private float[] createRandomPoints(int n) {
        Random random = new Random();
        float[] floats = new float[n];
        for (int i = 0; i < floats.length; i++) {
            floats[i] = (float) random.nextGaussian();
        }
        return floats;
    }

}
