package org.esa.snap.rcp.scripting;

import org.junit.Assert;
import org.junit.Test;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * @author Norman
 */
public class TransientActionTest {

    @Test
    public void testConstructor() throws Exception {
        AbstractAction delegate = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        };
        String path = "Test/X.instance";
        TransientAction transientAction = new TransientAction(delegate, path);
        assertSame(delegate, transientAction.getDelegate());
        assertEquals(path, transientAction.getPath());

        try {
            new TransientAction(null, path);
            Assert.fail();
        } catch (NullPointerException ignored) {
        }

        try {
            new TransientAction(delegate, null);
            Assert.fail();
        } catch (NullPointerException ignored) {
        }

        try {
            new TransientAction(delegate, "Test/u");
            Assert.fail();
        } catch (IllegalArgumentException ignored) {
        }

        try {
            new TransientAction(delegate, "u.instance");
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

        TransientAction transientAction = new TransientAction(delegate, "Test/MyAction.instance");

        // Enables state
        assertEquals(false, delegate.isEnabled());
        assertEquals(false, transientAction.isEnabled());
        transientAction.setEnabled(true);
        assertEquals(true, delegate.isEnabled());
        assertEquals(true, transientAction.isEnabled());

        // Property values
        assertEquals(null, delegate.getValue("XXX"));
        assertEquals(null, transientAction.getValue("XXX"));
        transientAction.putValue("XXX", 3456);
        assertEquals(3456, delegate.getValue("XXX"));
        assertEquals(3456, transientAction.getValue("XXX"));

        // Property changes
        String[] name = new String[1];
        transientAction.addPropertyChangeListener(evt -> {
            name[0] = evt.getPropertyName();
        });
        assertEquals(null, name[0]);
        transientAction.putValue("XXX", 9954);
        assertEquals("XXX", name[0]);
        delegate.putValue("YYY", 9954);
        assertEquals("YYY", name[0]);

        // Action
        assertEquals(null, actionCommand[0]);
        delegate.actionPerformed(new ActionEvent(this, 0, "cmd1"));
        assertEquals("cmd1", actionCommand[0]);
        transientAction.actionPerformed(new ActionEvent(this, 1, "cmd2"));
        assertEquals("cmd2", actionCommand[0]);
    }

}
