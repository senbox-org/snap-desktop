/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.nodes;

import org.esa.beam.framework.datamodel.MetadataElement;
import org.openide.awt.UndoRedo;

import java.beans.IntrospectionException;

/**
 * A node that represents a {@link org.esa.beam.framework.datamodel.MetadataElement} (=ME).
 *
 * @author Norman
 */
public class MENode extends PNLeafNode<MetadataElement> {

    public MENode(MetadataElement element, UndoRedo undoRedo) throws IntrospectionException {
        super(element, undoRedo);
        setIconBaseWithExtension("org/esa/snap/gui/icons/RsMetaData16.gif");
    }
}
