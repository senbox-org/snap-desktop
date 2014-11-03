/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.node;

import org.esa.snap.core.Band;
import org.esa.snap.gui.view.BandImagePanel;
import org.esa.snap.gui.window.WorkspaceTopComponent;
import org.openide.nodes.BeanNode;
import org.openide.nodes.Children;
import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.Lookups;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JScrollPane;
import java.awt.event.ActionEvent;
import java.beans.IntrospectionException;

/**
 * @author Norman
 */
public class BandNode extends BeanNode<Band> {

    @Messages("MSG_IMG_SIZE=Size: ")
    public BandNode(Band band) throws IntrospectionException {
        super(band, Children.LEAF, Lookups.singleton(band));
        setDisplayName(band.getName());
        setShortDescription(Bundle.MSG_IMG_SIZE() + band.getData().getWidth() + " x " + band.getData().getHeight());
        //setIconBaseWithExtension("org/fully/qualified/name/myicon.png");
    }

    @Override
    public Action[] getActions(boolean context) {
        return new Action[]{new OpenBandImageAction()};
    }

    @Override
    public Action getPreferredAction() {
        return new OpenBandImageAction();
    }

    private class OpenBandImageAction extends AbstractAction {

        private OpenBandImageAction() {
            super("Open Band Image");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            /*
            Mode editor = WindowManager.getDefault().findMode("editor");
            BandImageTopComponent topComponent = new BandImageTopComponent(getBean());
            editor.dockInto(topComponent);
            topComponent.open();
            */

            WorkspaceTopComponent.getInstance().addComponent(getBean().getName(), new JScrollPane(new BandImagePanel(getBean())));
        }
    }
}
