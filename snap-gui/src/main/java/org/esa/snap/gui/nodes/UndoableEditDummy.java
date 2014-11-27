package org.esa.snap.gui.nodes;

import org.openide.windows.WindowManager;

import javax.swing.JOptionPane;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;

/**
* Created by Norman on 27.11.2014.
*/
class UndoableEditDummy implements UndoableEdit {
    String id = Long.toHexString(Double.doubleToRawLongBits( Math.random()));

    @Override
    public void undo() throws CannotUndoException {
        JOptionPane.showMessageDialog(WindowManager.getDefault().getMainWindow(), "Undo: " + id);
    }

    @Override
    public boolean canUndo() {
        return true;
    }

    @Override
    public void redo() throws CannotRedoException {
        JOptionPane.showMessageDialog(WindowManager.getDefault().getMainWindow(), "Redo: " + id);
    }

    @Override
    public boolean canRedo() {
        return true;
    }

    @Override
    public void die() {
    }

    @Override
    public boolean addEdit(UndoableEdit anEdit) {
        return false;
    }

    @Override
    public boolean replaceEdit(UndoableEdit anEdit) {
        return false;
    }

    @Override
    public boolean isSignificant() {
        return true;
    }

    @Override
    public String getPresentationName() {
        return "Edit " + id;
    }

    @Override
    public String getUndoPresentationName() {
        return "Undo " + id;
    }

    @Override
    public String getRedoPresentationName() {
        return "Redo " + id;
    }
}
