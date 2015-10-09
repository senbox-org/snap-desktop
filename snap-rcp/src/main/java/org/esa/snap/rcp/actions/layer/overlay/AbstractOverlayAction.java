package org.esa.snap.rcp.actions.layer.overlay;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.AbstractLayerListener;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.SelectionSupport;
import org.esa.snap.ui.product.ProductSceneView;
import org.netbeans.api.annotations.common.NullAllowed;
import org.openide.util.actions.Presenter;

import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JToggleButton;
import java.awt.Component;
import java.awt.event.ActionEvent;

/**
 * Monitor the state of overlays to either be enable or disable.
 *
 * @author Muhammad.bc
 * @author Marco Peters
 * @author Norman Fomferra
 */
public abstract class AbstractOverlayAction extends AbstractAction
        implements Presenter.Toolbar, Presenter.Menu, Presenter.Popup, SelectionSupport.Handler<ProductSceneView>{

    private final AbstractLayerListener layerListener = new AbstractLayerListener() {
        @Override
        public void handleLayersAdded(Layer parentLayer, Layer[] childLayers) {
            updateActionState();
        }

        @Override
        public void handleLayersRemoved(Layer parentLayer, Layer[] childLayers) {
            updateActionState();
        }
    };

    protected AbstractOverlayAction() {
        SnapApp.getDefault().getSelectionSupport(ProductSceneView.class).addHandler(this);
        initActionProperties();
        updateActionState();
    }


    protected void updateActionState() {
        ProductSceneView view = getSelectedProductSceneView();
        if (view != null) {
            setEnabled(getActionEnabledState(view));
            setSelected(getActionSelectionState(view));
        } else {
            setEnabled(false);
            setSelected(false);
        }
    }



    @Override
    public void selectionChange(@NullAllowed ProductSceneView oldValue, @NullAllowed ProductSceneView newValue) {
        if (oldValue != null) {
            oldValue.getRootLayer().removeListener(layerListener);
        }
        if (newValue != null) {
            newValue.getRootLayer().addListener(layerListener);
        }
        updateActionState();
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        setOverlayEnableState(getSelectedProductSceneView());
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

    /**
     * Compute the state of a ProductSceneView that is selected.
     *
     * @param view // get the selected productSceneView
     * @return // return the state of the Overlay within the ProductSceneView
     */
    protected abstract boolean getActionSelectionState(ProductSceneView view);

    protected abstract boolean getActionEnabledState(ProductSceneView view);

    protected abstract void setOverlayEnableState(ProductSceneView view);

    protected ProductSceneView getSelectedProductSceneView() {
        return SnapApp.getDefault().getSelectedProductSceneView();
    }

    protected boolean isSelected() {
        return Boolean.TRUE.equals(getValue(SELECTED_KEY));
    }

    private void setSelected(boolean selected) {
        putValue(SELECTED_KEY, selected);
    }

}
