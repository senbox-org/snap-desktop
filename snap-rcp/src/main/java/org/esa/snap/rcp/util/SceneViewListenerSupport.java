package org.esa.snap.rcp.util;

import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.snap.rcp.windows.ProductSceneViewSelectionChangeListener;
import org.openide.util.Lookup;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.function.Predicate;

public class SceneViewListenerSupport {

    private final Lookup.Result<ProductSceneView> psvResult;
    private final HashMap<ProductSceneViewSelectionChangeListener, LookupListener> listenerMap;
    private Collection<? extends ProductSceneView> currentlySelectedViews;

    public SceneViewListenerSupport() {
        currentlySelectedViews = Collections.emptyList();
        psvResult = Utilities.actionsGlobalContext().lookupResult(ProductSceneView.class);
        listenerMap = new HashMap<>();
    }

    public Collection<? extends ProductSceneView> getCurrentlySelectedViews() {
        return Collections.unmodifiableCollection(currentlySelectedViews);
    }

    public void installSelectionChangeListener(ProductSceneViewSelectionChangeListener psvscl) {
        LookupListener lookupListener = createLookupListener(psvscl);
        psvResult.addLookupListener(WeakListeners.create(LookupListener.class, lookupListener, psvResult));
        listenerMap.put(psvscl, lookupListener);
    }

    public void uninstallSelectionChangeListener(ProductSceneViewSelectionChangeListener psvscl) {
        if (psvResult != null) {
            psvResult.removeLookupListener(WeakListeners.create(LookupListener.class, listenerMap.get(psvscl), psvResult));
        }
    }

    private LookupListener createLookupListener(ProductSceneViewSelectionChangeListener psvscl) {
        return ev -> {
            Collection<? extends ProductSceneView> allViews = psvResult.allInstances();

            ProductSceneView[] allDeselected = currentlySelectedViews.stream().filter((Predicate<ProductSceneView>) (o) -> !allViews.contains(o)).toArray(ProductSceneView[]::new);
            ProductSceneView firstDeselected = null;
            ProductSceneView[] moreDeselected = null;
            if (allDeselected.length > 0) {
                firstDeselected = allDeselected[0];
                moreDeselected = allDeselected.length > 1 ? Arrays.copyOfRange(allDeselected, 1, allDeselected.length) : new ProductSceneView[0];
            }

            ProductSceneView[] allNewlySelected = allViews.stream().filter((Predicate<ProductSceneView>) (o) -> !currentlySelectedViews.contains(o)).toArray(ProductSceneView[]::new);
            ProductSceneView firstSelected = null;
            ProductSceneView[] moreSelected = null;
            if (allNewlySelected.length > 0) {
                firstSelected = allNewlySelected[0];
                moreSelected = allNewlySelected.length > 1 ? Arrays.copyOfRange(allNewlySelected, 1, allNewlySelected.length) : new ProductSceneView[0];
            }

            currentlySelectedViews = allViews;

            if (firstDeselected != null) {
                psvscl.sceneViewDeselected(firstDeselected, moreDeselected);
            }
            if (firstSelected != null) {
                psvscl.sceneViewSelected(firstSelected, moreSelected);
            }

        };
    }
}
