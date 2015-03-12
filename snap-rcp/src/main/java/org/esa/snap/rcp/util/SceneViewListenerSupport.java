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
import java.util.LinkedList;
import java.util.function.Predicate;

public class SceneViewListenerSupport {

    private final Lookup.Result<ProductSceneView> psvResult;
    private final LinkedList<ProductSceneViewSelectionChangeListener> psvsclList;
    private final LookupListener psvLookupListener;
    private Collection<? extends ProductSceneView> currentlySelectedViews;

    public SceneViewListenerSupport() {
        currentlySelectedViews = Collections.emptyList();
        psvResult = Utilities.actionsGlobalContext().lookupResult(ProductSceneView.class);
        psvsclList = new LinkedList<>();
        psvLookupListener = createLookupListener();
    }

    public Collection<? extends ProductSceneView> getCurrentlySelectedViews() {
        return Collections.unmodifiableCollection(currentlySelectedViews);
    }

    public void installSelectionChangeListener(ProductSceneViewSelectionChangeListener psvscl) {
        if (psvsclList.isEmpty()) { // first listener added --> add LookupListener
            psvResult.addLookupListener(WeakListeners.create(LookupListener.class, psvLookupListener, psvResult));
        }
        psvsclList.add(psvscl);
    }

    public void uninstallSelectionChangeListener(ProductSceneViewSelectionChangeListener psvscl) {
        psvsclList.remove(psvscl);
        if (psvsclList.isEmpty()) { // last listener removed --> remove LookupListener
            psvResult.removeLookupListener(WeakListeners.create(LookupListener.class, psvLookupListener, psvResult));
        }
    }

    private LookupListener createLookupListener() {
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
                for (ProductSceneViewSelectionChangeListener listener : psvsclList) {
                    listener.sceneViewDeselected(firstDeselected, moreDeselected);
                }
            }
            if (firstSelected != null) {
                for (ProductSceneViewSelectionChangeListener listener : psvsclList) {
                    listener.sceneViewSelected(firstSelected, moreSelected);
                }
            }

        };
    }
}
