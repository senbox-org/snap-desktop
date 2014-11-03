/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.node;

import java.awt.image.BufferedImage;
import java.beans.IntrospectionException;
import java.util.ArrayList;
import java.util.List;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.esa.snap.core.Band;
import org.esa.snap.core.Product;

public class ProductChildFactory extends ChildFactory<Product> {
    
    final static ProductChildFactory instance = new ProductChildFactory();

    List<Product> products;

    public static ProductChildFactory getInstance() {
        return instance;
    }
        

    private ProductChildFactory() {
        products = new ArrayList<Product>();
    }
    
    public void addProduct(Product product) {
        products.add(product);
        refresh(true);
    }

    @Override
    protected boolean createKeys(List<Product> list) {
        list.addAll(products);
        Product p1 = new Product("Product_1",
                new Band("Band_A", new BufferedImage(800, 600, BufferedImage.TYPE_4BYTE_ABGR)),
                new Band("Band_B", new BufferedImage(800, 600, BufferedImage.TYPE_4BYTE_ABGR)));
        Product p2 = new Product("Product_2",
                new Band("Band_1", new BufferedImage(640, 400, BufferedImage.TYPE_4BYTE_ABGR)),
                new Band("Band_2", new BufferedImage(640, 400, BufferedImage.TYPE_4BYTE_ABGR)),
                new Band("Band_3", new BufferedImage(640, 400, BufferedImage.TYPE_4BYTE_ABGR)));
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
