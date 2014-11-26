/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.nodes;

import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;

/**
 * A node that represents a group of some elements.
 *
 * @author Norman
 */
class GroupNode extends AbstractNode {

    GroupNode(Group group) {
        super(Children.create(group, false));
        setDisplayName(group.getDisplayName());
        // todo
        //setIconBaseWithExtension("org/esa/snap/gui/icons/xxx.gif");
    }
}
