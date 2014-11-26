/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.nodes;

import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.IndexCoding;

import java.beans.IntrospectionException;

/**
 * @author Norman
 */
public class FCNode extends PNLeafNode<FlagCoding> {

    public FCNode(FlagCoding flagCoding) throws IntrospectionException {
        super(flagCoding);
        //setIconBaseWithExtension("org/esa/snap/gui/icons/RsBandAsSwath16.gif");
    }
}
