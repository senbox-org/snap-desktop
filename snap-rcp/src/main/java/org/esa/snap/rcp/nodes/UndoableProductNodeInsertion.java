package org.esa.snap.rcp.nodes;

import com.bc.ceres.core.Assert;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.openide.util.NbBundle;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * An undoable deletion of a {@code ProductNode}.
 *
 * @param <T> The product node type.
 * @author Norman Fomferra
 */
@NbBundle.Messages("LBL_UndoableProductNodeInsertionName=Add ''{0}''")
public class UndoableProductNodeInsertion<T extends ProductNode> extends AbstractUndoableEdit {

    private ProductNodeGroup<T> group;
    private T productNode;
    private final int index;

    public UndoableProductNodeInsertion(ProductNodeGroup<T> productNodeGroup, T productNode) {
        Assert.notNull(productNodeGroup, "group");
        Assert.notNull(productNode, "node");
        this.group = productNodeGroup;
        this.productNode = productNode;
        this.index = productNodeGroup.indexOf(productNode);
    }

    public T getProductNode() {
        return productNode;
    }

    @Override
    public String getPresentationName() {
        return Bundle.LBL_UndoableProductNodeInsertionName(productNode.getName());
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();
        // todo - close all open document windows
        group.remove(productNode);
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();
        if (index < group.getNodeCount()) {
            group.add(productNode);
        } else {
            group.add(index, productNode);
        }
    }

    @Override
    public void die() {
        group = null;
        productNode = null;
    }
}
