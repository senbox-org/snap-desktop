/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.nodes;

import org.esa.beam.framework.datamodel.FlagCoding;

import java.beans.IntrospectionException;

/**
 * A node that represents a {@link org.esa.beam.framework.datamodel.FlagCoding} (=FC).
 *
 * @author Norman
 */
public class FCNode extends PNLeafNode<FlagCoding> {

    public FCNode(FlagCoding flagCoding) throws IntrospectionException {
        super(flagCoding);
        // todo
        //setIconBaseWithExtension("org/esa/snap/gui/icons/xxx.gif");
    }
}
