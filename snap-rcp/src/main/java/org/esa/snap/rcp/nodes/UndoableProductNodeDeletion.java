package org.esa.snap.rcp.nodes;

import com.bc.ceres.core.Assert;
import org.esa.snap.framework.datamodel.ProductNode;
import org.esa.snap.framework.datamodel.ProductNodeGroup;
import org.openide.util.NbBundle;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * An undoable insertion of a {@code ProductNode}.
 *
 * @param <T> The product node type.
 * @author Norman Fomferra
 */
@NbBundle.Messages("LBL_UndoableProductNodeDeletionName=Delete ''{0}''")
public class UndoableProductNodeDeletion<T extends ProductNode> extends AbstractUndoableEdit {

    private ProductNodeGroup<T>[] productNodeGroups;
    private T productNode;
    private final int[] indexes;

    public UndoableProductNodeDeletion(ProductNodeGroup<T>[] productNodeGroups, T productNode, int[] indexes) {
        Assert.notNull(productNodeGroups, "group");
        Assert.notNull(productNode, "node");
        this.productNodeGroups = productNodeGroups;
        this.productNode = productNode;
        this.indexes = indexes;
    }

    public T getProductNode() {
        return productNode;
    }

    @Override
    public String getPresentationName() {
        return Bundle.LBL_UndoableProductNodeDeletionName(productNode.getName());
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();
        for (int i = 0; i < productNodeGroups.length; i++) {
            if (indexes[i] < productNodeGroups[i].getNodeCount()) {
                productNodeGroups[i].add(indexes[i], productNode);
            } else {
                productNodeGroups[i].add(productNode);
            }
        }
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();
        // todo - close all open document windows
        for(ProductNodeGroup<T> group : productNodeGroups) {
            group.remove(productNode);
        }
    }

    @Override
    public void die() {
        productNodeGroups = null;
        productNode = null;
    }
}
