package org.esa.snap.netbeans.docwin;

import org.esa.snap.netbeans.docwin.DocumentWindowManager.Predicate;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.io.File;

import static org.junit.Assert.*;

/**
 * Manages global opening, closing, and selection of {@link DocumentWindow}s.
 *
 * @author Norman Fomferra
 * @since 2.0
 */
@RunWith(HeadlessTestRunner.class)
public class DocumentWindowManagerTest {
    @Test
    public void testListeners() throws Exception {

        Listener1 listener1 = new Listener1();
        Listener2 listener2 = new Listener2();
        Listener3 listener3 = new Listener3();
        Listener4 listener4 = new Listener4();
        Listener5 listener5 = new Listener5();

        DocumentWindowManager.getDefault().addListener(listener1);
        DocumentWindowManager.getDefault().addListener(Predicate.doc(String.class), listener2);
        DocumentWindowManager.getDefault().addListener(Predicate.doc(File.class), listener3);
        DocumentWindowManager.getDefault().addListener(Predicate.doc(File2.class), listener4);
        DocumentWindowManager.getDefault().addListener(Predicate.docView(File2.class, MyPanel.class), listener5);

        MyDocumentTopComponent<String, JLabel> window1 = MyDocumentTopComponent.create("string");
        MyDocumentTopComponent<File, JLabel> window2 = MyDocumentTopComponent.create(new File("file"));
        MyDocumentTopComponent<File2, MyPanel> window3 = new MyDocumentTopComponent<>(new File2("file2"), new MyPanel());

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
        assertEquals("windowOpened(file2, MyPanel);" +
                             "windowClosed(file2, MyPanel);", listener5.trace);

        assertEquals(5, DocumentWindowManager.getDefault().getListeners().length);
        assertEquals(2, DocumentWindowManager.getDefault().getListeners(window1).length);
        assertEquals(2, DocumentWindowManager.getDefault().getListeners(window2).length);
        assertEquals(4, DocumentWindowManager.getDefault().getListeners(window3).length);

        DocumentWindowManager.getDefault().removeListener(listener1);
        assertEquals(4, DocumentWindowManager.getDefault().getListeners().length);
        assertEquals(1, DocumentWindowManager.getDefault().getListeners(window1).length);
        assertEquals(1, DocumentWindowManager.getDefault().getListeners(window2).length);
        assertEquals(3, DocumentWindowManager.getDefault().getListeners(window3).length);

        DocumentWindowManager.getDefault().removeListener(Predicate.doc(String.class), listener2);
        assertEquals(3, DocumentWindowManager.getDefault().getListeners().length);
        assertEquals(0, DocumentWindowManager.getDefault().getListeners(window1).length);
        assertEquals(1, DocumentWindowManager.getDefault().getListeners(window2).length);
        assertEquals(3, DocumentWindowManager.getDefault().getListeners(window3).length);

        DocumentWindowManager.getDefault().removeListener(Predicate.doc(File2.class), listener4);
        assertEquals(2, DocumentWindowManager.getDefault().getListeners().length);
        assertEquals(0, DocumentWindowManager.getDefault().getListeners(window1).length);
        assertEquals(1, DocumentWindowManager.getDefault().getListeners(window2).length);
        assertEquals(2, DocumentWindowManager.getDefault().getListeners(window3).length);

        DocumentWindowManager.getDefault().removeListener(Predicate.doc(File.class), listener3);
        assertEquals(1, DocumentWindowManager.getDefault().getListeners().length);
        assertEquals(0, DocumentWindowManager.getDefault().getListeners(window1).length);
        assertEquals(0, DocumentWindowManager.getDefault().getListeners(window2).length);
        assertEquals(1, DocumentWindowManager.getDefault().getListeners(window3).length);

        DocumentWindowManager.getDefault().removeListener(Predicate.docView(File2.class, MyPanel.class), listener5);
        assertEquals(0, DocumentWindowManager.getDefault().getListeners().length);
        assertEquals(0, DocumentWindowManager.getDefault().getListeners(window1).length);
        assertEquals(0, DocumentWindowManager.getDefault().getListeners(window2).length);
        assertEquals(0, DocumentWindowManager.getDefault().getListeners(window3).length);
    }

    private static class Listener1 implements DocumentWindowManager.Listener {
        String trace = "";

        @Override
        public void windowOpened(DocumentWindowManager.Event e) {
            trace += String.format("windowOpened(%s);", e.getWindow().getDocument());
        }

        @Override
        public void windowClosed(DocumentWindowManager.Event e) {
            trace += String.format("windowClosed(%s);", e.getWindow().getDocument());
        }

        @Override
        public void windowSelected(DocumentWindowManager.Event e) {
        }

        @Override
        public void windowDeselected(DocumentWindowManager.Event e) {
        }
    }

    private static class Listener2 implements DocumentWindowManager.Listener<String, Object> {
        String trace = "";

        @Override
        public void windowOpened(DocumentWindowManager.Event<String, Object> e) {
            trace += String.format("windowOpened(%s);", e.getWindow().getDocument());
        }

        @Override
        public void windowClosed(DocumentWindowManager.Event<String, Object> e) {
            trace += String.format("windowClosed(%s);", e.getWindow().getDocument());
        }

        @Override
        public void windowSelected(DocumentWindowManager.Event<String, Object> e) {
        }

        @Override
        public void windowDeselected(DocumentWindowManager.Event<String, Object> e) {
        }
    }

    private static class Listener3 implements DocumentWindowManager.Listener<File, Object> {
        String trace = "";

        @Override
        public void windowOpened(DocumentWindowManager.Event<File, Object> e) {
            trace += String.format("windowOpened(%s);", e.getWindow().getDocument());
        }

        @Override
        public void windowClosed(DocumentWindowManager.Event<File, Object> e) {
            trace += String.format("windowClosed(%s);", e.getWindow().getDocument());
        }

        @Override
        public void windowSelected(DocumentWindowManager.Event<File, Object> e) {
        }

        @Override
        public void windowDeselected(DocumentWindowManager.Event<File, Object> e) {
        }
    }

    private static class Listener4 implements DocumentWindowManager.Listener<File2, Object> {
        String trace = "";

        @Override
        public void windowOpened(DocumentWindowManager.Event<File2, Object> e) {
            trace += String.format("windowOpened(%s);", e.getWindow().getDocument());
        }

        @Override
        public void windowClosed(DocumentWindowManager.Event<File2, Object> e) {
            trace += String.format("windowClosed(%s);", e.getWindow().getDocument());
        }

        @Override
        public void windowSelected(DocumentWindowManager.Event<File2, Object> e) {
        }

        @Override
        public void windowDeselected(DocumentWindowManager.Event<File2, Object> e) {
        }
    }

    private static class Listener5 implements DocumentWindowManager.Listener<File2, MyPanel> {
        String trace = "";

        @Override
        public void windowOpened(DocumentWindowManager.Event<File2, MyPanel> e) {
            trace += String.format("windowOpened(%s, %s);", e.getWindow().getDocument(), e.getWindow().getView());
        }

        @Override
        public void windowClosed(DocumentWindowManager.Event<File2, MyPanel> e) {
            trace += String.format("windowClosed(%s, %s);", e.getWindow().getDocument(), e.getWindow().getView());
        }

        @Override
        public void windowSelected(DocumentWindowManager.Event<File2, MyPanel> e) {
        }

        @Override
        public void windowDeselected(DocumentWindowManager.Event<File2, MyPanel> e) {
        }
    }

    public static class File2 extends File {
        public File2(String pathname) {
            super(pathname);
        }
    }

    public static class MyPanel extends JPanel {
        @Override
        public String toString() {
            return "MyPanel";
        }
    }

    private static class MyDocumentTopComponent<D, V> extends DocumentTopComponent<D, V> {
        V view;

        static <D> MyDocumentTopComponent<D, JLabel> create(D doc) {
            return new MyDocumentTopComponent<>(doc, new JLabel(doc + ""));
        }

        public MyDocumentTopComponent(D doc, V view) {
            super(doc);
            this.view = view;
        }

        @Override
        public V getView() {
            return view;
        }
    }
}
