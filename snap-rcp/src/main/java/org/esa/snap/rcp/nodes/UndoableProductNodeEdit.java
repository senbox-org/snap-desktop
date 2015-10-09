package org.esa.snap.rcp.nodes;

import org.esa.snap.core.datamodel.ProductNode;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * An undoable edit of a {@code ProductNode}.
 *
 * @param <T> The product node type.
 * @author Norman Fomferra
 */
public class UndoableProductNodeEdit<T extends ProductNode> extends AbstractUndoableEdit {
    private final String name;
    private final T node;
    private final Edit<T> undo;
    private final Edit<T> redo;

    public UndoableProductNodeEdit(String name, T node, Edit<T> undo, Edit<T> redo) {
        this.name = name;
        this.node = node;
        this.undo = undo;
        this.redo = redo;
    }

    @Override
    public String getPresentationName() {
        return name;
    }

    @Override
    public void undo() throws CannotUndoException {
        if (undo != null) {
            undo.edit(node);
        }
    }

    @Override
    public void redo() throws CannotRedoException {
        if (redo != null) {
            redo.edit(node);
        }
    }

    @Override
    public boolean canUndo() {
        return undo != null;
    }

    @Override
    public boolean canRedo() {
        return redo != null;
    }

    interface Edit<T extends ProductNode> {
        void edit(T node);
    }
}
