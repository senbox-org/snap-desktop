package org.esa.snap.rcp.util;

import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.snap.rcp.windows.ProductSceneViewSelectionChangeListener;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.openide.util.Utilities;

import static org.junit.Assert.*;

public class SceneViewListenerSupportTest {

    private static ContextGlobalExtender globalExtender;

    @BeforeClass
    public static void setUp() throws Exception {
        globalExtender = Utilities.actionsGlobalContext().lookup(ContextGlobalExtender.class);
    }

    @Test
    public void testSingleSelection() throws Exception {
        SceneViewListenerSupport sceneViewListenerSupport = new SceneViewListenerSupport();
        MySelectionChangeListener changeListener = new MySelectionChangeListener();
        sceneViewListenerSupport.installSelectionChangeListener(changeListener);

        ProductSceneView sceneView1 = Mockito.mock(ProductSceneView.class);
        globalExtender.put("view", sceneView1);
        assertEquals(1, changeListener.count);
        globalExtender.remove("view");
        assertEquals(0, changeListener.count);
    }

    @Test
    public void testMultiSelection() throws Exception {
        SceneViewListenerSupport sceneViewListenerSupport = new SceneViewListenerSupport();
        MySelectionChangeListener changeListener = new MySelectionChangeListener();
        sceneViewListenerSupport.installSelectionChangeListener(changeListener);

        ProductSceneView sceneView1 = Mockito.mock(ProductSceneView.class);
        ProductSceneView sceneView2 = Mockito.mock(ProductSceneView.class);
        ProductSceneView sceneView3 = Mockito.mock(ProductSceneView.class);
        globalExtender.put("view1", sceneView1);
        assertEquals(1, changeListener.count);
        assertEquals(0, changeListener.moreCount);
        globalExtender.put("view2", sceneView2);
        assertEquals(2, changeListener.count);
        assertEquals(0, changeListener.moreCount);
        globalExtender.put("view3", sceneView3);
        assertEquals(3, changeListener.count);
        assertEquals(0, changeListener.moreCount);

        globalExtender.remove("view2");
        assertEquals(2, changeListener.count);
        assertEquals(0, changeListener.moreCount);
    }

    private static class MySelectionChangeListener implements ProductSceneViewSelectionChangeListener {

        volatile int count = 0;
        volatile int moreCount = 0;

        @Override
        public void sceneViewSelected(ProductSceneView first, ProductSceneView... more) {
            count++;
            moreCount = more.length;
        }

        @Override
        public void sceneViewDeselected(ProductSceneView first, ProductSceneView... more) {
            count--;
            moreCount = more.length;
        }
    }
}