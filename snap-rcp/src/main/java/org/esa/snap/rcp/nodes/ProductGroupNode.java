/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.nodes;

import com.bc.ceres.core.Assert;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductManager;
import org.esa.snap.core.datamodel.ProductNodeListener;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A node that represents a {@link Product} (=P).
 * Every {@code PNode} holds a dedicated undo/redo context.
 *
 * @author Norman
 */
public class ProductGroupNode extends AbstractNode {

    private final ProductManager productManager;
    private final PGroup group;
    private final Map<Product, ProductNodeListener> productNodeListenerMap;

    public ProductGroupNode(ProductManager productManager) {
        super(Children.LEAF);
        Assert.notNull(productManager, "productManager");
        this.productManager = productManager;
        this.group = new PGroup();
        this.productNodeListenerMap = new HashMap<>();

        setChildren(Children.create(group, false));
        setDisplayName("Products");

        productManager.addListener(new ProductManager.Listener() {
            @Override
            public void productAdded(ProductManager.Event event) {
                group.refresh();
            }

            @Override
            public void productRemoved(ProductManager.Event event) {
                uninstallListener(event.getProduct());
                group.refresh();
            }
        });
    }

    private void installListener(PNode node) {
        Product newProduct = node.getProduct();
        newProduct.addProductNodeListener(node);
        productNodeListenerMap.put(newProduct, node);
    }

    private void uninstallListener(Product oldProduct) {
        ProductNodeListener productNodeListener = productNodeListenerMap.remove(oldProduct);
        if (productNodeListener != null) {
            oldProduct.removeProductNodeListener(productNodeListener);
        }
    }

    class PGroup extends PNGroupBase<Product> {

        @Override
        protected boolean createKeys(List<Product> list) {
            list.addAll(Arrays.asList(productManager.getProducts()));
            return true;
        }

        @Override
        protected Node createNodeForKey(Product key) {
            PNode node = new PNode(key);
            installListener(node);
            return node;
        }
    }
}
