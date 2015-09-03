package org.esa.snap.netbeans.docwin;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;

/**
 * Manages global opening, closing, and selection of {@link DocumentWindow}s.
 *
 * @author Norman Fomferra
 * @since 2.0
 */
public class DocumentWindowManagerTest {
    @Test
    public void testListeners() throws Exception {

        Listener1 listener1 = new Listener1();
        Listener2 listener2 = new Listener2();
        Listener3 listener3 = new Listener3();
        Listener4 listener4 = new Listener4();

        DocumentWindowManager.getDefault().addListener(listener1);
        DocumentWindowManager.getDefault().addListener(String.class, listener2);
        DocumentWindowManager.getDefault().addListener(File.class, listener3);
        DocumentWindowManager.getDefault().addListener(File2.class, listener4);

        DocumentTopComponent<String> window1 = new DocumentTopComponent<>("string");
        DocumentTopComponent<File> window2 = new DocumentTopComponent<>(new File("file"));
        DocumentTopComponent<File2> window3 = new DocumentTopComponent<>(new File2("file2"));

        DocumentWindowManager.getDefault().openWindow(window1);
        DocumentWindowManager.getDefault().openWindow(window2);
        DocumentWindowManager.getDefault().openWindow(window3);
        DocumentWindowManager.getDefault().closeWindow(window1);
        DocumentWindowManager.getDefault().closeWindow(window2);
        DocumentWindowManager.getDefault().closeWindow(window3);

        assertEquals("windowOpened(string);windowOpened(file);windowOpened(file2);" +
                             "windowClosed(string);windowClosed(file);windowClosed(file2);", listener1.trace);
        assertEquals("windowOpened(string);" +
                             "windowClosed(string);", listener2.trace);
        assertEquals("windowOpened(file);windowOpened(file2);" +
                             "windowClosed(file);windowClosed(file2);", listener3.trace);
        assertEquals("windowOpened(file2);" +
                             "windowClosed(file2);", listener4.trace);

        assertEquals(4, DocumentWindowManager.getDefault().getListeners().length);
        assertEquals(2, DocumentWindowManager.getDefault().getListeners(String.class).length);
        assertEquals(2, DocumentWindowManager.getDefault().getListeners(File.class).length);
        assertEquals(3, DocumentWindowManager.getDefault().getListeners(File2.class).length);

        DocumentWindowManager.getDefault().removeListener(listener1);
        assertEquals(3, DocumentWindowManager.getDefault().getListeners().length);
        assertEquals(1, DocumentWindowManager.getDefault().getListeners(String.class).length);
        assertEquals(1, DocumentWindowManager.getDefault().getListeners(File.class).length);
        assertEquals(2, DocumentWindowManager.getDefault().getListeners(File2.class).length);

        DocumentWindowManager.getDefault().removeListener(String.class, listener2);
        assertEquals(2, DocumentWindowManager.getDefault().getListeners().length);
        assertEquals(0, DocumentWindowManager.getDefault().getListeners(String.class).length);
        assertEquals(1, DocumentWindowManager.getDefault().getListeners(File.class).length);
        assertEquals(2, DocumentWindowManager.getDefault().getListeners(File2.class).length);

        DocumentWindowManager.getDefault().removeListener(File2.class, listener4);
        assertEquals(1, DocumentWindowManager.getDefault().getListeners().length);
        assertEquals(0, DocumentWindowManager.getDefault().getListeners(String.class).length);
        assertEquals(1, DocumentWindowManager.getDefault().getListeners(File.class).length);
        assertEquals(1, DocumentWindowManager.getDefault().getListeners(File2.class).length);

        DocumentWindowManager.getDefault().removeListener(File.class, listener3);
        assertEquals(0, DocumentWindowManager.getDefault().getListeners().length);
        assertEquals(0, DocumentWindowManager.getDefault().getListeners(String.class).length);
        assertEquals(0, DocumentWindowManager.getDefault().getListeners(File.class).length);
        assertEquals(0, DocumentWindowManager.getDefault().getListeners(File2.class).length);
    }

    private static class Listener1 implements DocumentWindowManager.Listener {
        String trace = "";

        @Override
        public void windowOpened(DocumentWindowManager.Event e) {
            trace += String.format("windowOpened(%s);", e.getDocumentWindow().getDocument());
        }

        @Override
        public void windowClosed(DocumentWindowManager.Event e) {
            trace += String.format("windowClosed(%s);", e.getDocumentWindow().getDocument());
        }

        @Override
        public void windowSelected(DocumentWindowManager.Event e) {
        }

        @Override
        public void windowDeselected(DocumentWindowManager.Event e) {
        }
    }

    private static class Listener2 implements DocumentWindowManager.Listener<String> {
        String trace = "";

        @Override
        public void windowOpened(DocumentWindowManager.Event<String> e) {
            trace += String.format("windowOpened(%s);", e.getDocumentWindow().getDocument());
        }

        @Override
        public void windowClosed(DocumentWindowManager.Event<String> e) {
            trace += String.format("windowClosed(%s);", e.getDocumentWindow().getDocument());
        }

        @Override
        public void windowSelected(DocumentWindowManager.Event<String> e) {
        }

        @Override
        public void windowDeselected(DocumentWindowManager.Event<String> e) {
        }
    }

    private static class Listener3 implements DocumentWindowManager.Listener<File> {
        String trace = "";

        @Override
        public void windowOpened(DocumentWindowManager.Event<File> e) {
            trace += String.format("windowOpened(%s);", e.getDocumentWindow().getDocument());
        }

        @Override
        public void windowClosed(DocumentWindowManager.Event<File> e) {
            trace += String.format("windowClosed(%s);", e.getDocumentWindow().getDocument());
        }

        @Override
        public void windowSelected(DocumentWindowManager.Event<File> e) {
        }

        @Override
        public void windowDeselected(DocumentWindowManager.Event<File> e) {
        }
    }

    private static class Listener4 implements DocumentWindowManager.Listener<File2> {
        String trace = "";

        @Override
        public void windowOpened(DocumentWindowManager.Event<File2> e) {
            trace += String.format("windowOpened(%s);", e.getDocumentWindow().getDocument());
        }

        @Override
        public void windowClosed(DocumentWindowManager.Event<File2> e) {
            trace += String.format("windowClosed(%s);", e.getDocumentWindow().getDocument());
        }

        @Override
        public void windowSelected(DocumentWindowManager.Event<File2> e) {
        }

        @Override
        public void windowDeselected(DocumentWindowManager.Event<File2> e) {
        }
    }

    public static class File2 extends File {
        public File2(String pathname) {
            super(pathname);
        }
    }
}
