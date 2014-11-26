/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.nodes;

/**
 * @author Norman
 */
class Group {

    final String displayName;
    final PNChildFactory childFactory;

    Group(String displayName, PNChildFactory childFactory) {
        this.displayName = displayName;
        this.childFactory = childFactory;
    }
}
