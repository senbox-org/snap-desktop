/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.nodes;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeListener;
import org.openide.awt.UndoRedo;
import org.openide.nodes.Node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A factory for nodes that represent {@link Product}s.
 *
 * @author Norman
 */
public class PNodeFactory extends PNGroup<Product> {

    private final static PNodeFactory instance = new PNodeFactory();

    private final List<Product> productList;
    private final Map<Product, UndoRedo.Manager> undoManagerMap;
    private final Map<Product, ProductNodeListener> productNodeListenerMap;

    public static PNodeFactory getInstance() {
        return instance;
    }

    PNodeFactory() {
        productList = new ArrayList<>();
        undoManagerMap = new HashMap<>();
        productNodeListenerMap = new HashMap<>();
    }

    @Override
    public String getDisplayName() {
        return "Products";
    }

    public List<Product> getOpenedProducts() {
        return new ArrayList<>(productList);
    }

    public void addProduct(Product newProduct) {
        productList.add(newProduct);
        refresh(true);
    }

    public void addProducts(Product... newProducts) {
        productList.addAll(Arrays.asList(newProducts));
        refresh(true);
    }

    public void removeProduct(Product oldProduct) {
        productList.remove(oldProduct);
        uninstallListener(oldProduct);
        refresh(true);
    }

    public void removeProducts(Product... oldProducts) {
        productList.removeAll(Arrays.asList(oldProducts));
        uninstallListener(oldProducts);
        refresh(true);
    }

    private void installListener(PNode node) {
        Product newProduct = node.getProduct();
        newProduct.addProductNodeListener(node);
        productNodeListenerMap.put(newProduct, node);
    }

    private void uninstallListener(Product... oldProducts) {
        for (Product oldProduct : oldProducts) {
            ProductNodeListener productNodeListener = productNodeListenerMap.remove(oldProduct);
            if (productNodeListener != null) {
                oldProduct.removeProductNodeListener(productNodeListener);
            }
        }
    }

    public UndoRedo.Manager getUndoManager(Product product) {
        return undoManagerMap.get(product);
    }

    @Override
    protected boolean createKeys(List<Product> list) {
        list.addAll(productList);
        return true;
    }

    @Override
    protected Node createNodeForKey(Product key) {
        UndoRedo.Manager undoManager = undoManagerMap.get(key);
        if (undoManager == null) {
            undoManager = new UndoRedo.Manager();
            undoManagerMap.put(key, undoManager);
        }
        PNode node = new PNode(key, undoManager);
        installListener(node);
        return node;
    }
}
