package org.esa.snap.gui.nodes;

import com.bc.ceres.core.Assert;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
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
@NbBundle.Messages("LBL_UndoableProductNodeDeletionName=Delete ''{0}''")
class UndoableProductNodeDeletion<T extends ProductNode> extends AbstractUndoableEdit {

    private ProductNodeGroup<T> group;
    private T node;
    private int index;

    public UndoableProductNodeDeletion(ProductNodeGroup<T> group, T node, int index) {
        Assert.notNull(group, "group");
        Assert.notNull(node, "node");
        this.group = group;
        this.node = node;
        this.index = index;
    }

    @Override
    public String getPresentationName() {
        return Bundle.LBL_UndoableProductNodeDeletionName(node.getName());
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();
        group.add(index, node);
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();
        group.remove(node);
    }

    @Override
    public void die() {
        group = null;
        node = null;
    }
}
