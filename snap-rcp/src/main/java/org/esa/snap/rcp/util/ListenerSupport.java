package org.esa.snap.rcp.util;

import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.snap.netbeans.docwin.DocumentWindowManager;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.windows.ProductSceneViewTopComponent;
import org.openide.util.Lookup;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;
import org.openide.windows.TopComponent;

import java.util.prefs.PreferenceChangeListener;

public class ListenerSupport {

    private ListenerSupport() {
    }

    public static void installSceneViewListener(SceneViewListener svl) {
        DocumentWindowManager.Listener listener = new DocumentWindowManager.Listener() {
            @Override
            public void windowOpened(DocumentWindowManager.Event e) {
                ProductSceneView view = getView(e);
                if (view != null) {
                    svl.opened(view);
                }
            }

            @Override
            public void windowClosed(DocumentWindowManager.Event e) {
                ProductSceneView view = getView(e);
                if (view != null) {
                    svl.closed(view);
                }
            }

            @Override
            public void windowSelected(DocumentWindowManager.Event e) {
                ProductSceneView view = getView(e);
                if (view != null) {
                    svl.selected(view);
                }
            }

            @Override
            public void windowDeselected(DocumentWindowManager.Event e) {
                ProductSceneView view = getView(e);
                if (view != null) {
                    svl.deselected(view);
                }
            }

            private ProductSceneView getView(DocumentWindowManager.Event e) {
                TopComponent topComponent = e.getDocumentWindow().getTopComponent();
                if (topComponent instanceof ProductSceneViewTopComponent) {
                    return ((ProductSceneViewTopComponent) topComponent).getView();
                }
                return null;
            }
        };
        DocumentWindowManager.getDefault().addListener(listener);
        svl.docListener = listener;
    }

    public static void uninstallSceneViewListener(SceneViewListener svl) {
        DocumentWindowManager.getDefault().removeListener(svl.docListener);
    }

    public static <T extends ProductNode> void installProductNodeSelectionListener(ProductNodeSelectionListener<T> pnsl, Class<T> nodeType) {
        Lookup.Result<T> productResult = Utilities.actionsGlobalContext().lookupResult(nodeType);
        LookupListener lookupListener = ev -> {
            Lookup lookup = Utilities.actionsGlobalContext();
            T productNode = lookup.lookup(nodeType);
            pnsl.selectionChanged(productNode);
        };
        pnsl.listener = lookupListener;
        pnsl.result = productResult;
        productResult.addLookupListener(WeakListeners.create(LookupListener.class, lookupListener, productResult));
    }

    public static void uninstallProductNodeSelectionListener(ProductNodeSelectionListener pnsl) {
        pnsl.result.removeLookupListener(pnsl.listener);
    }

    public static void installProductManagerListener(ProductManager.Listener pml) {
        SnapApp.getDefault().getProductManager().addListener(pml);
    }

    public static void uninstallProductManagerListener(ProductManager.Listener pml) {
        SnapApp.getDefault().getProductManager().removeListener(pml);
    }

    public static void installPreferenceChangeListener(PreferenceChangeListener pcl) {
        SnapApp.getDefault().getPreferences().addPreferenceChangeListener(pcl);
    }

    public static void uninstallPreferenceChangeListener(PreferenceChangeListener pcl) {
        SnapApp.getDefault().getPreferences().removePreferenceChangeListener(pcl);
    }

    public static abstract class SceneViewListener {

        private DocumentWindowManager.Listener docListener;


        public void opened(ProductSceneView view) {

        }

        public void closed(ProductSceneView view) {

        }

        public void selected(ProductSceneView view){

        }

        public void deselected(ProductSceneView view){

        }

    }

    public static abstract class ProductNodeSelectionListener<T extends ProductNode> {

        private Lookup.Result<T> result;
        private LookupListener listener;

        public abstract void selectionChanged(T p);

    }


}
