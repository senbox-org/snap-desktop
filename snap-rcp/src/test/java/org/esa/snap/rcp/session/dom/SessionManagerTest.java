package org.esa.snap.rcp.session.dom;

import org.esa.snap.rcp.session.SessionManager;
import org.esa.snap.util.io.SnapFileFilter;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by muhammad.bc on 7/17/2015.
 */
public class SessionManagerTest {
    @Test
    public void testFileFilter() throws Exception {
        SnapFileFilter filter = SessionManager.getDefault().getSessionFileFilter();
        assertTrue(filter.accept(new File("test.snap")));
        assertFalse(filter.accept(new File("test.nc")));
        assertArrayEquals(new String[]{".snap"}, filter.getExtensions());
    }
}
