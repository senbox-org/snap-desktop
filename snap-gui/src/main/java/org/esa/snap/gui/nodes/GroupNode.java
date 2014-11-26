/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.nodes;

import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;

/**
 * @author Norman
 */
class GroupNode extends AbstractNode {
    private final Group group;

    GroupNode(Group group) {
        super(Children.create(group.childFactory, false));
        this.group = group;
        setDisplayName(group.displayName);
    }

    Group getGroup() {
        return group;
    }
}
