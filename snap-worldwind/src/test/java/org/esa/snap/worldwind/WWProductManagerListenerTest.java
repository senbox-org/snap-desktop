package org.esa.snap.worldwind;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductManager;
import org.junit.Test;

import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class WWProductManagerListenerTest {

    private static class RecordingView implements WWView {
        final List<String> calls = Collections.synchronizedList(new ArrayList<>());
        final List<String> threads = Collections.synchronizedList(new ArrayList<>());
        Product selected;

        private void record(String call) {
            calls.add(call);
            threads.add(SwingUtilities.isEventDispatchThread() ? "EDT" : Thread.currentThread().getName());
        }

        @Override
        public Product getSelectedProduct() {
            return selected;
        }

        @Override
        public void setSelectedProduct(Product product) {
            selected = product;
            record("select:" + (product != null ? product.getName() : null));
        }

        @Override
        public void setProducts(Product[] products) {
            record("add:" + products.length + ":" + products[0].getName());
        }

        @Override
        public void removeProduct(Product product) {
            record("remove:" + product.getName());
        }
    }

    @Test
    public void testEventsFromWorkerThreadAreHandledOnTheEDTInOrder() throws Exception {
        final ProductManager productManager = new ProductManager();
        final RecordingView view = new RecordingView();
        productManager.addListener(new WWProductManagerListener(view, () -> productManager));
        final Product product = new Product("target", "T", 10, 10);

        // what SingleTargetProductDialog does with its target product
        final Thread worker = new Thread(() -> {
            productManager.addProduct(product);
            productManager.removeProduct(product);
        });
        worker.start();
        worker.join();
        SwingUtilities.invokeAndWait(() -> { });

        // the product was already gone when the add was processed, so it is never added
        assertEquals(Collections.singletonList("remove:target"), view.calls);
        for (String thread : view.threads) {
            assertEquals("EDT", thread);
        }
        assertNull(view.selected);
    }

    @Test
    public void testAddedProductIsAddedAloneAndSelected() throws Exception {
        final ProductManager productManager = new ProductManager();
        final RecordingView view = new RecordingView();
        productManager.addListener(new WWProductManagerListener(view, () -> productManager));
        productManager.addProduct(new Product("a", "T", 10, 10));
        SwingUtilities.invokeAndWait(() -> { });
        view.calls.clear();

        final Product product = new Product("b", "T", 10, 10);
        productManager.addProduct(product);
        SwingUtilities.invokeAndWait(() -> { });

        // only the new product is passed, not all open products again
        assertEquals(List.of("add:1:b", "select:b"), view.calls);
    }

    @Test
    public void testRemovingTheSelectedProductClearsTheSelection() throws Exception {
        final ProductManager productManager = new ProductManager();
        final RecordingView view = new RecordingView();
        productManager.addListener(new WWProductManagerListener(view, () -> productManager));
        final Product product = new Product("a", "T", 10, 10);
        productManager.addProduct(product);
        SwingUtilities.invokeAndWait(() -> { });
        assertTrue(view.selected == product);

        SwingUtilities.invokeAndWait(() -> productManager.removeProduct(product));

        assertNull(view.selected);
        assertEquals("remove:a", view.calls.get(view.calls.size() - 1));
    }
}
