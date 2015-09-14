package org.esa.snap.rcp.scripting;

import org.junit.Test;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

import javax.swing.AbstractAction;
import javax.swing.Action;
import java.awt.event.ActionEvent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * @author Norman Fomferra
 */
public class SnapUtilsTest {
    @Test
    public void testAddRemoveAction() throws Exception {
        AbstractAction realAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        };
        FileObject actionFile = SnapUtils.addAction(realAction, "Test/Action");
        assertNotNull(actionFile);
        assertNotNull(actionFile.getParent());
        assertEquals("application/x-nbsettings", actionFile.getMIMEType());
        assertEquals("Test/Action", actionFile.getParent().getPath());
        assertEquals("instance", actionFile.getExt());

        Action action = FileUtil.getConfigObject(actionFile.getPath(), Action.class);
        assertNotNull(action);
        assertEquals(TransientAction.class, action.getClass());
        assertSame(realAction, ((TransientAction) action).getDelegate());

        boolean ok = SnapUtils.removeAction(actionFile);
        assertEquals(true, ok);
        action = FileUtil.getConfigObject(actionFile.getPath(), Action.class);
        assertNull(action);
    }

    @Test
    public void testAddRemoveActionReference() throws Exception {
        AbstractAction realAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        };
        FileObject actionFile = SnapUtils.addAction(realAction, "Test/Action");

        FileObject actionRef1File = SnapUtils.addActionReference(actionFile, "Test/Refs1", 10);
        assertNotNull(actionRef1File);

        assertNotNull(actionRef1File.getParent());
        assertEquals("Test/Refs1", actionRef1File.getParent().getPath());
        assertEquals("shadow", actionRef1File.getExt());

        assertEquals("content/unknown", actionRef1File.getMIMEType());

        Action refAction = FileUtil.getConfigObject(actionRef1File.getPath(), Action.class);
        assertNotNull(refAction);
        assertEquals(TransientAction.class, refAction.getClass());
        assertSame(realAction, ((TransientAction) refAction).getDelegate());

        boolean ok = SnapUtils.removeActionReference(actionFile);
        assertEquals(false, ok);

        ok = SnapUtils.removeActionReference(actionRef1File);
        assertEquals(true, ok);
        refAction = FileUtil.getConfigObject(actionRef1File.getPath(), Action.class);
        assertNull(refAction);
    }

}
