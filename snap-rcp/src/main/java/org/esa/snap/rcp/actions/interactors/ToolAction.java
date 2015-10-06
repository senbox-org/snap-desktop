/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.rcp.actions.interactors;

import com.bc.ceres.core.Assert;
import com.bc.ceres.swing.figure.AbstractInteractorListener;
import com.bc.ceres.swing.figure.Interactor;
import com.bc.ceres.swing.figure.InteractorListener;
import com.bc.ceres.swing.figure.interactions.NullInteractor;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.ui.product.ProductSceneView;
import org.openide.util.ContextAwareAction;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;
import org.openide.util.actions.Presenter;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JToggleButton;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;

/**
 * Tool actions are used to interact with a {@link com.bc.ceres.swing.figure.FigureEditor FigureEditor},
 * such as the VISAT product scene view.
 * <p>
 * Derived tool actions must at least provide two public constructors:
 * <ol>
 * <li>a default constructor</li>
 * <li>a constructor that takes a single {@code Lookup} argument</li>
 * </ol>
 * <p>
 * Derived actions must also tell the system to load the actions eagerly, that is, use the action registration
 * as follows: {@code @ActionRegistration(displayName = "not-used", lazy = false)}.
 *
 * @author Norman Fomferra
 */
public abstract class ToolAction extends AbstractAction
        implements ContextAwareAction, LookupListener, Presenter.Toolbar, Presenter.Menu, Presenter.Popup, HelpCtx.Provider {

    public static final String INTERACTOR_KEY = "interactor";

    private static final Logger LOG = Logger.getLogger(ToolAction.class.getName());

    private final InteractorListener interactorListener;
    private final Lookup lookup;
    private final Lookup.Result<ProductSceneView> viewResult;

    protected ToolAction() {
        this(null);
    }

    protected ToolAction(Lookup lookup) {
        this(lookup, NullInteractor.INSTANCE);
    }

    protected ToolAction(Lookup lookup, Interactor interactor) {
        putValue(ACTION_COMMAND_KEY, getClass().getName());
        putValue(SELECTED_KEY, false);
        interactorListener = new InternalInteractorListener();
        setInteractor(interactor);
        this.lookup = lookup != null ? lookup : Utilities.actionsGlobalContext();
        this.viewResult = this.lookup.lookupResult(ProductSceneView.class);
        this.viewResult.addLookupListener(WeakListeners.create(LookupListener.class, this, viewResult));
        updateEnabledState();
    }

    public ProductSceneView getProductSceneView() {
        return lookup.lookup(ProductSceneView.class);
    }

    @Override
    public Component getToolbarPresenter() {
        JToggleButton toggleButton = new JToggleButton(this);
        toggleButton.setText(null);
        return toggleButton;
    }

    @Override
    public JMenuItem getMenuPresenter() {
        return new JCheckBoxMenuItem(this);
    }

    @Override
    public JMenuItem getPopupPresenter() {
        return getMenuPresenter();
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        try {
            Constructor<? extends ToolAction> constructor = getClass().getConstructor(Lookup.class);
            return constructor.newInstance(actionContext);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void resultChanged(LookupEvent ignored) {
        updateEnabledState();

        if (isEnabled() && isSelected()) {
            ProductSceneView productSceneView = getProductSceneView();
            if (productSceneView != null) {
                Interactor oldInteractor = productSceneView.getFigureEditor().getInteractor();
                Interactor newInteractor = getInteractor();
                if (oldInteractor != newInteractor) {
                    oldInteractor.deactivate();
                    newInteractor.activate();
                    productSceneView.getFigureEditor().setInteractor(newInteractor);
                }
            }
        }
    }

    @Override
    public final void actionPerformed(ActionEvent actionEvent) {
        onSelectionStateChanged();
    }

    public Lookup getLookup() {
        return lookup;
    }

    public boolean isSelected() {
        return Boolean.TRUE.equals(getValue(SELECTED_KEY));
    }

    public void setSelected(boolean selected) {
        putValue(SELECTED_KEY, selected);
    }

    public Interactor getInteractor() {
        return (Interactor) getValue(INTERACTOR_KEY);
    }

    public final void setInteractor(Interactor interactor) {
        Guardian.assertNotNull("interactor", interactor);
        Interactor oldInteractor = getInteractor();
        if (interactor == oldInteractor) {
            return;
        }
        if (oldInteractor != null) {
            oldInteractor.removeListener(interactorListener);
        }
        interactor.addListener(interactorListener);
        putValue(INTERACTOR_KEY, interactor);
    }

    protected void updateEnabledState() {
        super.setEnabled(!viewResult.allInstances().isEmpty());
    }

    private void onSelectionStateChanged() {
        LOG.fine(String.format(">>> %s#onSelectionStateChanged: selected = %s, interactor = %s%n", getClass().getName(), isSelected(), getInteractor()));
        ProductSceneView productSceneView = getProductSceneView();
        if (productSceneView != null && isSelected()) {
            Interactor oldInteractor = productSceneView.getFigureEditor().getInteractor();
            Interactor newInteractor = getInteractor();
            Assert.notNull(newInteractor, "No interactor set on action " + getClass());
            if (oldInteractor != newInteractor && oldInteractor.isActive()) {
                oldInteractor.deactivate();
            }
            if (!newInteractor.isActive()) {
                newInteractor.activate();
            }
            productSceneView.getFigureEditor().setInteractor(newInteractor);
        }
    }

    private class InternalInteractorListener extends AbstractInteractorListener {

        @Override
        public void interactorActivated(Interactor interactor) {
            LOG.fine(String.format(">>> %s#interactorActivated: interactor = %s%n", getClass().getName(), interactor));
            if (interactor == getInteractor() && !isSelected()) {
                setSelected(true);
            }
        }

        @Override
        public void interactorDeactivated(Interactor interactor) {
            LOG.fine(String.format(">>> %s#interactorDeactivated: interactor = %s%n", getClass().getName(), interactor));
            if (interactor == getInteractor() && isSelected()) {
                setSelected(false);
            }
        }

    }
}
