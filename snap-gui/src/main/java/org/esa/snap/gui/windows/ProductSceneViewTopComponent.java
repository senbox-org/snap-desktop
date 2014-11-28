/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.windows;

import com.bc.ceres.swing.selection.Selection;
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import com.bc.ceres.swing.selection.SelectionChangeListener;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeListenerAdapter;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.snap.gui.nodes.BNode;
import org.esa.snap.gui.nodes.MNode;
import org.esa.snap.gui.nodes.TPGNode;
import org.esa.snap.gui.util.DocumentTopComponent;
import org.esa.snap.gui.util.WindowUtilities;
import org.openide.awt.UndoRedo;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;

import java.awt.*;
import java.beans.IntrospectionException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * A document window which displays images
 *
 * @author Norman
 */
public class ProductSceneViewTopComponent extends DocumentTopComponent<ProductNode>
        implements UndoRedo.Provider, SelectionChangeListener {

    private static final Logger LOG = Logger.getLogger(ProductSceneViewTopComponent.class.getName());

    private final ProductSceneView view;
    private UndoRedo undoRedo;
    private final ProductNodeListenerAdapter nodeRenameHandler;
    private Selection selection;

    public ProductSceneViewTopComponent(ProductSceneView view, UndoRedo undoRedo) {
        super(view.getRaster());
        this.view = view;
        this.undoRedo = undoRedo;
        this.nodeRenameHandler = new NodeRenameHandler();
        initComponents();
        RasterDataNode raster = view.getRaster();
        setName(raster.getName());
        setDisplayName(WindowUtilities.getUniqueTitle(raster.getName(), ProductSceneViewTopComponent.class));
        setToolTipText(raster.getProduct().getName() + " - " + raster.getName());
        setIcon(ImageUtilities.loadImage("org/esa/snap/gui/icons/RsBandAsSwath16.gif"));

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
    }

    @Override
    public UndoRedo getUndoRedo() {
        return undoRedo;
    }

    public ProductSceneView getView() {
        return view;
    }

    @Override
    public void selectionChanged(SelectionChangeEvent event) {
        setSelection(event.getSelection());
    }

    @Override
    public void selectionContextChanged(SelectionChangeEvent event) {
    }

    @Override
    protected void componentOpened() {
        getDocument().getProduct().addProductNodeListener(nodeRenameHandler);
    }

    @Override
    protected void componentClosed() {
        getDocument().getProduct().removeProductNodeListener(nodeRenameHandler);
    }

    @Override
    protected void componentActivated() {
        getDynamicContent().add(view);
        setSelection(view.getFigureEditor().getSelectionContext().getSelection());
        view.getFigureEditor().getSelectionContext().addSelectionChangeListener(this);
    }

    @Override
    protected void componentDeactivated() {
        view.getFigureEditor().getSelectionContext().removeSelectionChangeListener(this);
        setSelection(null);
        getDynamicContent().remove(view);
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        add(view, BorderLayout.CENTER);
    }

    private void setSelection(Selection newSelection) {
        // todo - something is still wrong here!
        LOG.info(">>> setSelection: newSelection = " + newSelection);
        Selection oldSelection = this.selection;
        LOG.info(">>> setSelection: oldSelection = " + oldSelection);
        Set<Object> oldSet = oldSelection != null ? new HashSet<>(Arrays.asList(oldSelection.getSelectedValues())) : Collections.emptySet();
        Set<Object> newSet = newSelection != null ? new HashSet<>(Arrays.asList(newSelection.getSelectedValues())) : Collections.emptySet();
        Set<Object> tmpSet = new HashSet<>(oldSet);
        oldSet.removeAll(newSet);
        newSet.removeAll(tmpSet);
        for (Object o : oldSet) {
            LOG.info(">>> setSelection: removing o = " + o);
            getDynamicContent().remove(o);
        }
        for (Object o : newSet) {
            LOG.info(">>> setSelection: adding o = " + o);
            getDynamicContent().add(o);
        }
        if (oldSelection != null && oldSelection != Selection.EMPTY) {
            getDynamicContent().remove(oldSelection);
        }
        if (newSelection != null && newSelection != Selection.EMPTY) {
            getDynamicContent().add(newSelection);
        }
        this.selection = newSelection;
    }

    private class NodeRenameHandler extends ProductNodeListenerAdapter {
        @Override
        public void nodeChanged(final ProductNodeEvent event) {
            if (event.getSourceNode() == getDocument() &&
                    event.getPropertyName().equalsIgnoreCase(ProductNode.PROPERTY_NAME_NAME)) {
                setDisplayName(getDocument().getName());
            }
        }
    }
}
