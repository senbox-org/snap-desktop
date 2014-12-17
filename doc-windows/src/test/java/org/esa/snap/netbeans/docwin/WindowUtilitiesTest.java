package org.esa.snap.netbeans.docwin;

import org.junit.After;
import org.junit.Test;
import org.openide.windows.TopComponent;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Norman
 */
public class WindowUtilitiesTest {

    @After
    public void tearDown() throws Exception {
        WindowUtilities.tcProvider = WindowUtilities.DEFAULT_TC_PROVIDER;
    }

    @Test
    public void testGetUniqueTitle() throws Exception {
        WindowUtilities.tcProvider = () -> Arrays.asList(createTC("A"),
                                                         createMyTC("B"),
                                                         createTC("C"));

        assertEquals("A (2)", WindowUtilities.getUniqueTitle("A", TopComponent.class));
        assertEquals("B (2)", WindowUtilities.getUniqueTitle("B", TopComponent.class));
        assertEquals("C (2)", WindowUtilities.getUniqueTitle("C", TopComponent.class));
        assertEquals("D", WindowUtilities.getUniqueTitle("D", TopComponent.class));

        assertEquals("A", WindowUtilities.getUniqueTitle("A", MyTopComponent.class));
        assertEquals("B (2)", WindowUtilities.getUniqueTitle("B", MyTopComponent.class));
        assertEquals("C", WindowUtilities.getUniqueTitle("C", MyTopComponent.class));
        assertEquals("D", WindowUtilities.getUniqueTitle("D", MyTopComponent.class));
    }

    @Test
    public void testGetOpened() throws Exception {
        WindowUtilities.tcProvider = () -> Arrays.asList(createMyOtherTC("A"),
                                                         createMyTC("B"),
                                                         createTC("C"),
                                                         createTC("D"),
                                                         createMyOtherTC("E"));
        assertEquals(5, WindowUtilities.getOpened(TopComponent.class).count());
        assertEquals(3, WindowUtilities.getOpened(MyTopComponent.class).count());
        assertEquals(2, WindowUtilities.getOpened(MyOtherTopComponent.class).count());
        assertEquals(2, WindowUtilities.getOpened(Runnable.class).count());
    }

    @Test
    public void testGetOpenedWithContainer() throws Exception {
        WindowUtilities.tcProvider = () -> Arrays.asList(createMyOtherTC("A"),
                                                         createMyTC("B"),
                                                         createTC("C"),
                                                         createMyContainerTC("D",
                                                                             createTC("Da"),
                                                                             createMyTC("Db")),
                                                         createTC("E"),
                                                         createMyOtherTC("F"),
                                                         createMyContainerTC("G",
                                                                             createTC("Ga"),
                                                                             createMyOtherTC("Gb"),
                                                                             createMyTC("Gc")));
        assertEquals(12, WindowUtilities.getOpened(TopComponent.class).count());
        assertEquals(6, WindowUtilities.getOpened(MyTopComponent.class).count());
        assertEquals(3, WindowUtilities.getOpened(MyOtherTopComponent.class).count());
        assertEquals(3, WindowUtilities.getOpened(Runnable.class).count());
        assertEquals(2, WindowUtilities.getOpened(WindowContainer.class).count());
    }

    TopComponent createTC(String displayName) {
        TopComponent topComponent = new TopComponent();
        topComponent.setDisplayName(displayName);
        return topComponent;
    }

    TopComponent createMyTC(String displayName) {
        TopComponent topComponent = new MyTopComponent();
        topComponent.setDisplayName(displayName);
        return topComponent;
    }

    TopComponent createMyOtherTC(String displayName) {
        TopComponent topComponent = new MyOtherTopComponent();
        topComponent.setDisplayName(displayName);
        return topComponent;
    }

    TopComponent createMyContainerTC(String displayName, TopComponent... topComponents) {
        TopComponent topComponent = new MyContainerTopComponent(Arrays.asList(topComponents));
        topComponent.setDisplayName(displayName);
        return topComponent;
    }

    static class MyTopComponent extends TopComponent {
    }

    static class MyOtherTopComponent extends MyTopComponent implements Runnable {
        @Override
        public void run() {
        }
    }

    static class MyContainerTopComponent extends TopComponent implements WindowContainer<TopComponent> {
        final List<TopComponent> topComponents;

        public MyContainerTopComponent(List<TopComponent> topComponents) {
            this.topComponents = topComponents;
        }

        @Override
        public TopComponent getSelectedWindow() {
            return null;
        }

        @Override
        public List<TopComponent> getOpenedWindows() {
            return topComponents;
        }
    }
}
