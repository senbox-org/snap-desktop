/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.windows;

import com.bc.ceres.swing.selection.Selection;
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import com.bc.ceres.swing.selection.SelectionChangeListener;
import eu.esa.snap.netbeans.docwin.DocumentTopComponent;
import eu.esa.snap.netbeans.docwin.WindowUtilities;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.ProductNodeEvent;
import org.esa.snap.core.datamodel.ProductNodeListenerAdapter;
import org.esa.snap.rcp.actions.edit.SelectionActions;
import org.esa.snap.rcp.util.ContextGlobalExtender;
import org.esa.snap.ui.product.ProductSceneView;
import org.openide.awt.UndoRedo;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.Utilities;

import javax.swing.*;
import javax.swing.plaf.LayerUI;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * A document window which displays images.
 *
 * @author Norman Fomferra
 */
public class ProductSceneViewTopComponent extends DocumentTopComponent<ProductNode, ProductSceneView>
        implements UndoRedo.Provider, SelectionChangeListener {

    private static final Logger LOG = Logger.getLogger(ProductSceneViewTopComponent.class.getName());

    private final ProductSceneView view;
    private final UndoRedo undoRedo;
    private final ProductNodeListenerAdapter nodeRenameHandler;
    private Selection selection;

    public ProductSceneViewTopComponent(ProductSceneView view, UndoRedo undoRedo) {
        super(view.getRaster());
        this.view = view;
        this.undoRedo = undoRedo != null ? undoRedo : UndoRedo.NONE;
        this.nodeRenameHandler = new NodeRenameHandler();
        this.selection = Selection.EMPTY;
        setToolTipText(view.getRaster().getDescription());
        setIcon(ImageUtilities.loadImage("org/esa/snap/rcp/icons/RsBandAsSwath.gif"));
        updateDisplayName();
        setName(getDisplayName());
/*
        // checkme - this is ugly and not wanted (nf), the node will either passed in or we'll have
        // a central node factory, e.g. via an ExtensionObject
        Node node = null;
        try {
            if (raster instanceof Mask) {
                node = new MNode((Mask) raster, undoRedo);
            } else if (raster instanceof Band) {
                node = new BNode((Band) raster, undoRedo);
            } else if (raster instanceof TiePointGrid) {
                node = new TPGNode((TiePointGrid) raster, undoRedo);
            }
        } catch (IntrospectionException e) {
            Exceptions.printStackTrace(e);
        }
        if (node != null) {
            setActivatedNodes(new Node[]{node});
        }
*/
        setLayout(new BorderLayout());
        add(new JLayer<>(this.view, new ProductSceneViewLayerUI()), BorderLayout.CENTER);
    }

    /**
     * Retrieves the ProductSceneView displayed.
     *
     * @return the scene view, never null
     */
    public ProductSceneView getView() {
        return view;
    }

    @Override
    public UndoRedo getUndoRedo() {
        return undoRedo;
    }

    @Override
    public void selectionChanged(SelectionChangeEvent event) {
        setSelection(event.getSelection());
    }

    @Override
    public void selectionContextChanged(SelectionChangeEvent event) {
    }

    @Override
    public void componentOpened() {
        LOG.info(">> componentOpened");
        final Product product = getDocument().getProduct();
        if (product != null) {
            product.addProductNodeListener(nodeRenameHandler);
        }
    }

    @Override
    public void componentClosed() {
        LOG.info(">> componentClosed");
        final Product product = getDocument().getProduct();
        if (product != null) {
            product.removeProductNodeListener(nodeRenameHandler);
        }
    }

    @Override
    public void componentSelected() {
        LOG.info(">> componentSelected");

        updateSelectedState();

        ContextGlobalExtender contextGlobalExtender = Utilities.actionsGlobalContext().lookup(ContextGlobalExtender.class);
        if (contextGlobalExtender != null) {
            contextGlobalExtender.put("view", getView());
        }

        setSelection(getView().getFigureEditor().getSelectionContext().getSelection());
        getView().getFigureEditor().getSelectionContext().addSelectionChangeListener(this);
    }

    @Override
    public void componentDeselected() {
        LOG.info(">> componentDeselected");

        updateSelectedState();

        getView().getFigureEditor().getSelectionContext().removeSelectionChangeListener(this);
        setSelection(Selection.EMPTY);

        ContextGlobalExtender contextGlobalExtender = Utilities.actionsGlobalContext().lookup(ContextGlobalExtender.class);
        if (contextGlobalExtender != null) {
            contextGlobalExtender.remove("view");
        }
    }

    @Override
    public void documentClosing() {
        super.documentClosing();
        getView().disposeLayers();
        getView().dispose();
    }

    private class ProductSceneViewLayerUI extends LayerUI<ProductSceneView> {
        @Override
        public void paint(Graphics g, JComponent c) {
            super.paint(g, c);

            if (isSelected()) {
                final int N = 6;
                final int A = 220;
                final Color C = new Color(255, 213, 79);

                for (int i = 0; i < N; i++) {
                    g.setColor(new Color(C.getRed(), C.getGreen(), C.getBlue(), A - i * A / N));
                    g.drawRect(i, i, getWidth() - 2 * i, getHeight() - 2 * i);
                }
            }
        }
    }

    private void updateDisplayName() {
        setDisplayName(WindowUtilities.getUniqueTitle(getView().getSceneName(), ProductSceneViewTopComponent.class));
    }

    private void setSelection(Selection newSelection) {
        Selection oldSelection = this.selection;
        getDynamicContent().remove(oldSelection);
        if (!newSelection.isEmpty()) {
            this.selection = newSelection.clone();
            getDynamicContent().add(newSelection);
        }
        updateActionMap(newSelection);
    }

    private void updateActionMap(Selection newSelection) {
        ActionMap actionMap = getActionMap();
        actionMap.put(SelectionActions.SELECT_ALL, new SelectAllAction());
        actionMap.put(DefaultEditorKit.pasteAction, new PasteAction());
        if (!newSelection.isEmpty()) {
            actionMap.put(DefaultEditorKit.cutAction, new CutAction());
            actionMap.put(DefaultEditorKit.copyAction, new CopyAction());
            actionMap.put("delete", new DeleteAction());
            actionMap.put(SelectionActions.DESELECT_ALL, new DeselectAllAction());
        } else {
            actionMap.remove(DefaultEditorKit.cutAction);
            actionMap.remove(DefaultEditorKit.copyAction);
            actionMap.remove("delete");
            actionMap.remove(SelectionActions.DESELECT_ALL);
        }
        getDynamicContent().remove(actionMap);
        getDynamicContent().add(actionMap);
    }

    private class CutAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            Selection selection = getLookup().lookup(Selection.class);
            if (selection != null && !selection.isEmpty()) {
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                Transferable transferable = getView().getFigureEditor().getFigureSelection().createTransferable(false);
                clipboard.setContents(transferable, selection);
                getView().getFigureEditor().deleteSelection();
                //JOptionPane.showMessage(WindowManager.getDefault().getMainWindow(), "Cut: " + transferable);
            }
        }
    }

    private class CopyAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            Selection selection = getLookup().lookup(Selection.class);
            if (selection != null && !selection.isEmpty()) {
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                // todo - when we copy, we actually don't clone the SimpleFeature. Then, if we paste, two figures refer
                // to the same SimpleFeature.
                Transferable transferable = getView().getFigureEditor().getFigureSelection().createTransferable(true);
                clipboard.setContents(transferable, selection);
                //JOptionPane.showMessage(WindowManager.getDefault().getMainWindow(), "Copy: " + transferable);
            }
        }
    }

    private class PasteAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            Transferable contents = clipboard.getContents(getView());
            //JOptionPane.showMessage(WindowManager.getDefault().getMainWindow(), "Paste: " + contents);

            try {
                getView().getSelectionContext().insert(contents);
            } catch (IOException | UnsupportedFlavorException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    private class DeleteAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            getView().getFigureEditor().deleteSelection();
        }
    }

    private class SelectAllAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            getView().getFigureEditor().selectAll();
        }
    }

    private class DeselectAllAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            getView().getFigureEditor().setSelection(Selection.EMPTY);
        }
    }

    private class NodeRenameHandler extends ProductNodeListenerAdapter {
        @Override
        public void nodeChanged(final ProductNodeEvent event) {
            if (event.getSourceNode() == getDocument() &&
                    event.getPropertyName().equalsIgnoreCase(ProductNode.PROPERTY_NAME_NAME)) {
                updateDisplayName();
            }
        }
    }
}
