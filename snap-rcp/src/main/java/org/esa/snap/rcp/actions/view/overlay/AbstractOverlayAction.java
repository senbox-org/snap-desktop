package org.esa.snap.rcp.actions.view.overlay;

import org.esa.snap.framework.ui.product.ProductSceneView;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.WeakListeners;
import org.openide.util.actions.Presenter;

import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JToggleButton;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.Collection;

/**
 * @author Marco Peters
 */
public abstract class AbstractOverlayAction extends AbstractAction implements ContextAwareAction, LookupListener,
                                                                              Presenter.Toolbar, Presenter.Menu, Presenter.Popup {

    private final Lookup.Result<ProductSceneView> result;

    protected AbstractOverlayAction(Lookup lkp) {
        result = lkp.lookupResult(ProductSceneView.class);
        result.addLookupListener(WeakListeners.create(LookupListener.class, this, result));
        initActionProperties();
        updateActionState();

    }

    protected void updateActionState() {
        ProductSceneView view = getSelectedView();
        if (view != null) {
            setEnabled(getActionEnableState(view));
            setSelected(getActionSelectionState(view));
        } else {
            setEnabled(false);
            setSelected(false);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        setOverlayEnableState(getSelectedView());
        updateActionState();
    }


    @Override
    public void resultChanged(LookupEvent le) {
        updateActionState();
    }

    @Override
    public JMenuItem getMenuPresenter() {
        JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(this);
        menuItem.setIcon(null);
        return menuItem;
    }

    @Override
    public JMenuItem getPopupPresenter() {
        return getMenuPresenter();
    }

    @Override
    public Component getToolbarPresenter() {
        JToggleButton toggleButton = new JToggleButton(this);
        toggleButton.setText(null);
        return toggleButton;
    }

    protected abstract void initActionProperties();

    protected abstract boolean getActionSelectionState(ProductSceneView view);

    protected abstract boolean getActionEnableState(ProductSceneView view);

    protected abstract void setOverlayEnableState(ProductSceneView view);

    protected ProductSceneView getSelectedView() {
        Collection<? extends ProductSceneView> views = result.allInstances();
        return !views.isEmpty() ? views.stream().findFirst().get() : null;
    }

    protected boolean isSelected() {
        return Boolean.TRUE.equals(getValue(SELECTED_KEY));
    }

    private void setSelected(boolean selected) {
        putValue(SELECTED_KEY, selected);
    }

}
