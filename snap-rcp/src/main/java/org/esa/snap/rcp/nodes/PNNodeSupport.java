/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.nodes;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.ProductNodeEvent;
import org.esa.snap.core.datamodel.ProductNodeListener;
import org.esa.snap.rcp.SnapApp;
import org.openide.awt.UndoRedo;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.Utilities;

import javax.swing.Action;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author Norman
 */
abstract class PNNodeSupport implements ProductNodeListener {

    static UndoRedo getUndoRedo(Node node) {
        return node instanceof UndoRedo.Provider ? ((UndoRedo.Provider) node).getUndoRedo() : null;
    }

    static boolean isDirectChild(Children children, ProductNode productNode) {
        return children != Children.LEAF
                && children.snapshot().stream()
                .filter(node -> node instanceof PNNode)
                .anyMatch(node -> ((PNNode) node).getProductNode() == productNode);
    }

    static final PNNodeSupport NONE = new PNNodeSupport() {
        @Override
        public void nodeChanged(ProductNodeEvent event) {
        }

        @Override
        public void nodeDataChanged(ProductNodeEvent event) {
        }

        @Override
        public void nodeAdded(ProductNodeEvent event) {
        }

        @Override
        public void nodeRemoved(ProductNodeEvent event) {
        }
    };

    static Action[] getContextActions(ProductNode productNode) {
        ArrayList<Action> actionList = new ArrayList<>();
        Class<?> type = productNode.getClass();
        do {
            List<? extends Action> actions = Utilities.actionsForPath("Context/Product/" + type.getSimpleName());
            actionList.addAll(actions);
            type = type.getSuperclass();
        } while (type != null && ProductNode.class.isAssignableFrom(type));

        return actionList.toArray(new Action[actionList.size()]);
    }

    static <T extends ProductNode> void performUndoableProductNodeEdit(String name, T productNode, UndoableProductNodeEdit.Edit<T> action, UndoableProductNodeEdit.Edit<T> undo) {
        action.edit(productNode);
        Product product = productNode.getProduct();
        // be paranoid
        if (product != null) {
            UndoRedo.Manager undoManager = SnapApp.getDefault().getUndoManager(product);
            // be paranoid again
            if (undoManager != null) {
                undoManager.addEdit(new UndoableProductNodeEdit<>(name, productNode, undo, action));
            }
        }
    }

    static class PNNodeSupportImpl extends PNNodeSupport {
        private final PNNodeBase node;
        private final PNGroupBase group;

        public PNNodeSupportImpl(PNNodeBase node, PNGroupBase group) {
            this.node = node;
            this.group = group;
        }

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            if (group.shallReactToPropertyChange(event.getPropertyName())) {
                final boolean nodeExpanded = NodeExpansionManager.isNodeExpanded(node);
                group.refresh();
                if (nodeExpanded) {
                    NodeExpansionManager.expandNode(node);
                }
            }
            delegateProductNodeEvent(l -> l.nodeChanged(event));
        }

        @Override
        public void nodeDataChanged(ProductNodeEvent event) {
            delegateProductNodeEvent(l -> l.nodeDataChanged(event));
        }

        @Override
        public void nodeAdded(ProductNodeEvent event) {
            refreshChildrenAfterAdd(event.getSourceNode());
            delegateProductNodeEvent(l -> l.nodeAdded(event));
        }

        @Override
        public void nodeRemoved(ProductNodeEvent event) {
            refreshChildrenAfterRemove(event.getSourceNode());
            delegateProductNodeEvent(l -> l.nodeRemoved(event));
        }

        private void refreshChildrenAfterAdd(ProductNode productNode) {
            if (group.isDirectChild(productNode)) {
                group.refresh();
            }
        }

        private void refreshChildrenAfterRemove(ProductNode productNode) {
            if (node.isDirectChild(productNode)) {
                group.refresh();
            }
        }

        private void delegateProductNodeEvent(Consumer<ProductNodeListener> action) {
            node.getChildren()
                    .snapshot()
                    .stream()
                    .filter(node -> node instanceof ProductNodeListener)
                    .map(node -> (ProductNodeListener) node)
                    .forEach(action);
        }
    }

    public static PNNodeSupport create(PNNodeBase tpnNode, PNGroupBase childFactory) {
        return childFactory != null ? new PNNodeSupportImpl(tpnNode, childFactory) : NONE;
    }
}
