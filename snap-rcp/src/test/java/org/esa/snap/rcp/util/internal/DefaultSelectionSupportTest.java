package org.esa.snap.rcp.util.internal;

import org.esa.snap.rcp.util.ContextGlobalExtender;
import org.esa.snap.rcp.util.SelectionSupport;
import org.esa.snap.ui.product.ProductSceneView;
import org.junit.Assume;
import org.junit.Test;
import org.mockito.Mockito;
import org.openide.util.Utilities;

import java.awt.GraphicsEnvironment;

import static org.junit.Assert.*;

public class DefaultSelectionSupportTest {

    @Test
    public void testSingleSelection() throws Exception {
        Assume.assumeFalse(GraphicsEnvironment.isHeadless());
        ContextGlobalExtender globalExtender = Utilities.actionsGlobalContext().lookup(ContextGlobalExtender.class);
        DefaultSelectionSupport<ProductSceneView> selectionChangeSupport = new DefaultSelectionSupport<>(ProductSceneView.class);
        MySelectionChangeHandler changeListener = new MySelectionChangeHandler();
        selectionChangeSupport.addHandler(changeListener);

        ProductSceneView sceneView1 = Mockito.mock(ProductSceneView.class);
        globalExtender.put("view", sceneView1);
        assertEquals(1, changeListener.count);
        globalExtender.remove("view");
        assertEquals(0, changeListener.count);
    }

    @Test
    public void testMultiSelection() throws Exception {
        Assume.assumeFalse(GraphicsEnvironment.isHeadless());
        ContextGlobalExtender globalExtender = Utilities.actionsGlobalContext().lookup(ContextGlobalExtender.class);
        DefaultSelectionSupport<ProductSceneView> selectionChangeSupport = new DefaultSelectionSupport<>(ProductSceneView.class);
        MySelectionChangeHandler changeListener = new MySelectionChangeHandler();
        selectionChangeSupport.addHandler(changeListener);

        ProductSceneView sceneView1 = Mockito.mock(ProductSceneView.class);
        ProductSceneView sceneView2 = Mockito.mock(ProductSceneView.class);
        ProductSceneView sceneView3 = Mockito.mock(ProductSceneView.class);
        globalExtender.put("view1", sceneView1);
        assertEquals(1, changeListener.count);
        globalExtender.put("view2", sceneView2);
        assertEquals(2, changeListener.count);
        globalExtender.put("view3", sceneView3);
        assertEquals(3, changeListener.count);

        globalExtender.remove("view2");
        assertEquals(2, changeListener.count);
    }

    private static class MySelectionChangeHandler implements SelectionSupport.Handler<ProductSceneView> {

        volatile int count = 0;

        @Override
        public void selectionChange(ProductSceneView oldValue, ProductSceneView newValue) {
            if (oldValue != null) {
                count--;
            }
            if (newValue != null) {
                count++;
            }
        }
    }
}
