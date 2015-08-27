package org.esa.snap.rcp.actions;

import org.junit.Assert;
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
 * @author Norman
 */
public class ProxyActionTest {

    @Test
    public void testConstructor() throws Exception {
        AbstractAction delegate = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        };
        String path = "Test/" + ProxyAction.genInstanceId();
        ProxyAction proxyAction = new ProxyAction(delegate, path);
        Assert.assertSame(delegate, proxyAction.getDelegate());
        assertEquals(path, proxyAction.getPath());

        try {
            new ProxyAction(null, path);
            Assert.fail();
        } catch (NullPointerException ignored) {
        }

        try {
            new ProxyAction(delegate, null);
            Assert.fail();
        } catch (NullPointerException ignored) {
        }

        try {
            new ProxyAction(delegate, "u");
            Assert.fail();
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Test
    public void testProxyDelegatesAllCalls() throws Exception {
        String[] actionCommand = new String[1];
        AbstractAction delegate = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                actionCommand[0] = e.getActionCommand();
            }
        };
        delegate.setEnabled(false);

        ProxyAction proxyAction = new ProxyAction(delegate, "Test/" + ProxyAction.genInstanceId());

        // Enables state
        assertEquals(false, delegate.isEnabled());
        assertEquals(false, proxyAction.isEnabled());
        proxyAction.setEnabled(true);
        assertEquals(true, delegate.isEnabled());
        assertEquals(true, proxyAction.isEnabled());

        // Property values
        assertEquals(null, delegate.getValue("XXX"));
        assertEquals(null, proxyAction.getValue("XXX"));
        proxyAction.putValue("XXX", 3456);
        assertEquals(3456, delegate.getValue("XXX"));
        assertEquals(3456, proxyAction.getValue("XXX"));

        // Property changes
        String[] name = new String[1];
        proxyAction.addPropertyChangeListener(evt -> {
            name[0] = evt.getPropertyName();
        });
        assertEquals(null, name[0]);
        proxyAction.putValue("XXX", 9954);
        assertEquals("XXX", name[0]);
        delegate.putValue("YYY", 9954);
        assertEquals("YYY", name[0]);

        // Action
        assertEquals(null, actionCommand[0]);
        delegate.actionPerformed(new ActionEvent(this, 0, "cmd1"));
        assertEquals("cmd1", actionCommand[0]);
        proxyAction.actionPerformed(new ActionEvent(this, 1, "cmd2"));
        assertEquals("cmd2", actionCommand[0]);
    }

    @Test
    public void testAddRemoveAction() throws Exception {
        AbstractAction realAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        };
        FileObject actionFile = ProxyAction.addAction(realAction, "Test/Action");
        assertNotNull(actionFile);
        assertNotNull(actionFile.getParent());
        assertEquals("application/x-nbsettings", actionFile.getMIMEType());
        assertEquals("Test/Action", actionFile.getParent().getPath());
        assertEquals("instance", actionFile.getExt());

        Action action = FileUtil.getConfigObject(actionFile.getPath(), Action.class);
        assertNotNull(action);
        assertEquals(ProxyAction.class, action.getClass());
        assertSame(realAction, ((ProxyAction) action).getDelegate());

        boolean ok = ProxyAction.removeAction(actionFile);
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
        FileObject actionFile = ProxyAction.addAction(realAction, "Test/Action");

        FileObject actionRef1File = ProxyAction.addActionReference(actionFile, "Test/Refs1", 10);
        assertNotNull(actionRef1File);

        assertNotNull(actionRef1File.getParent());
        assertEquals("Test/Refs1", actionRef1File.getParent().getPath());
        assertEquals("shadow", actionRef1File.getExt());

        assertEquals("content/unknown", actionRef1File.getMIMEType());

        Action refAction = FileUtil.getConfigObject(actionRef1File.getPath(), Action.class);
        assertNotNull(refAction);
        assertEquals(ProxyAction.class, refAction.getClass());
        assertSame(realAction, ((ProxyAction) refAction).getDelegate());

        boolean ok = ProxyAction.removeActionReference(actionFile);
        assertEquals(false, ok);

        ok = ProxyAction.removeActionReference(actionRef1File);
        assertEquals(true, ok);
        refAction = FileUtil.getConfigObject(actionRef1File.getPath(), Action.class);
        assertNull(refAction);
    }
}
