/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.nodes;

import org.esa.beam.framework.datamodel.Product;
import org.openide.awt.UndoRedo;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;

import java.beans.IntrospectionException;
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
public class PNodeFactory extends ChildFactory<Product> {

    private final static PNodeFactory instance = new PNodeFactory();

    private final List<Product> productList;
    private final Map<Product, UndoRedo.Manager> undoManagerMap;

    public static PNodeFactory getInstance() {
        return instance;
    }

    PNodeFactory() {
        productList = new ArrayList<>();
        undoManagerMap = new HashMap<>();
    }

    public void addProduct(Product newProduct) {
        productList.add(newProduct);
        refresh(true);
    }

    public void addProducts(Product... newProducts) {
        productList.addAll(Arrays.asList(newProducts));
        refresh(true);
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

            // todo - remove test
            // test, test
            undoManager.addEdit(new UndoableEditDummy());
            undoManager.addEdit(new UndoableEditDummy());
            undoManager.addEdit(new UndoableEditDummy());

            undoManagerMap.put(key, undoManager);
        }
        PNode node = null;
        try {
            node = new PNode(key, undoManager);
        } catch (IntrospectionException ex) {
            Exceptions.printStackTrace(ex);
        }
        return node;
    }
}
