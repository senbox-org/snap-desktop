package org.esa.snap.opendap;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductManager;
import org.esa.snap.core.ui.AppContext;
import org.esa.snap.core.ui.application.ApplicationPage;
import org.esa.snap.core.ui.product.ProductSceneView;
import org.esa.snap.opendap.ui.OpendapAccessPanel;
import org.esa.snap.util.DefaultPropertyMap;
import org.esa.snap.util.PropertyMap;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import java.awt.Window;

/**
 * @author Tonio Fincke
 */
public class OpendapAccess {

    public static void main(String[] args) {

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        final OpendapAccessPanel opendapAccessPanel = new OpendapAccessPanel(new DefaultAppContext(""), "");
        final JFrame mainFrame = new JFrame("OPeNDAP Access");
        mainFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        mainFrame.setContentPane(opendapAccessPanel);
        mainFrame.pack();
        mainFrame.setVisible(true);
    }


    /**
     * This trivial implementation of the {@link org.esa.snap.core.ui.AppContext} class
     * is only for testing.
     */
    private static class DefaultAppContext implements AppContext {

        private Window applicationWindow;
        private String applicationName;
        private ProductManager productManager;
        private Product selectedProduct;
        private PropertyMap preferences;
        private ProductSceneView selectedSceneView;

        public DefaultAppContext(String applicationName) {
            this(applicationName,
                 new JFrame(applicationName),
                 new ProductManager(),
                 new DefaultPropertyMap());
        }


        public DefaultAppContext(String applicationName,
                                 Window applicationWindow,
                                 ProductManager productManager,
                                 PropertyMap preferences) {
            this.applicationWindow = applicationWindow;
            this.applicationName = applicationName;
            this.productManager = productManager;
            this.preferences = preferences;
        }

        @Override
        public String getApplicationName() {
            return applicationName;
        }

        public void setApplicationName(String applicationName) {
            this.applicationName = applicationName;
        }

        @Override
        public Window getApplicationWindow() {
            return applicationWindow;
        }

        @Override
        public ApplicationPage getApplicationPage() {
            return null;
        }

        public void setApplicationWindow(Window applicationWindow) {
            this.applicationWindow = applicationWindow;
        }

        @Override
        public PropertyMap getPreferences() {
            return preferences;
        }

        public void setPreferences(PropertyMap preferences) {
            this.preferences = preferences;
        }

        @Override
        public ProductManager getProductManager() {
            return productManager;
        }

        public void setProductManager(ProductManager productManager) {
            this.productManager = productManager;
        }

        @Override
        public Product getSelectedProduct() {
            return selectedProduct;
        }

        public void setSelectedProduct(Product selectedProduct) {
            this.selectedProduct = selectedProduct;
        }

        @Override
        public void handleError(String message, Throwable t) {
            if (t != null) {
                t.printStackTrace();
            }
            JOptionPane.showMessageDialog(getApplicationWindow(), message);
        }

        @Override
        public ProductSceneView getSelectedProductSceneView() {
            return selectedSceneView;
        }

        public void setSelectedSceneView(ProductSceneView selectedView) {
            this.selectedSceneView = selectedView;
        }
    }

}
