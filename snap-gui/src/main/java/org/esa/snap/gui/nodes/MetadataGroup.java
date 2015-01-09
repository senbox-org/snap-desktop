/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.nodes;

import org.esa.beam.framework.datamodel.MetadataElement;
import org.openide.awt.UndoRedo;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;

import java.beans.IntrospectionException;
import java.util.Collections;
import java.util.List;

/**
 * A group representing the metadata root element of a {@link org.esa.beam.framework.datamodel.Product}.
 *
 * @author Norman
 */
class MetadataGroup extends Group<MetadataElement> {

    private final MetadataElement root;
    private final UndoRedo undoRedo;

    MetadataGroup(MetadataElement root, UndoRedo undoRedo) {
        this.root = root;
        this.undoRedo = undoRedo;
    }

    @Override
    public UndoRedo getUndoRedo() {
        return undoRedo;
    }

    @Override
    public String getDisplayName() {
        return "Metadata";
    }

    @Override
    protected boolean createKeys(List<MetadataElement> toPopulate) {
        Collections.addAll(toPopulate, root.getElements());
        return true;
    }

    @Override
    protected Node createNodeForKey(MetadataElement key) {
        try {
            return new PNLeafNode.ME(key, getUndoRedo());
        } catch (IntrospectionException e) {
            Exceptions.printStackTrace(e);
            return null;
        }
    }
}
