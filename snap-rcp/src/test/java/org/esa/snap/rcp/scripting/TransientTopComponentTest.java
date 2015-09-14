package org.esa.snap.rcp.scripting;

import org.junit.Assert;
import org.junit.Test;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.windows.TopComponent;

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
public class TransientTopComponentTest {

    @Test
    public void testThatItNeverPersists() throws Exception {
        TransientTopComponent tc = new TransientTopComponent() {
        };
        assertEquals(TopComponent.PERSISTENCE_NEVER, tc.getPersistenceType());
    }
}
