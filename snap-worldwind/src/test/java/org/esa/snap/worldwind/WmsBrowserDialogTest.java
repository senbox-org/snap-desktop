package org.esa.snap.worldwind;

import gov.nasa.worldwind.WorldWindow;
import org.junit.Test;

import java.awt.Dimension;
import java.net.ServerSocket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class WmsBrowserDialogTest {

    @Test
    public void testUnreachableServerIsReportedAsWithoutLayers() throws Exception {
        // a local port nobody listens on
        final int port;
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        final WmsBrowserDialog.SnapWMSLayersPanel panel = new WmsBrowserDialog.SnapWMSLayersPanel(
                mock(WorldWindow.class), "http://127.0.0.1:" + port + "/wms", new Dimension(300, 300), layer -> { });

        final CountDownLatch loaded = new CountDownLatch(1);
        final AtomicBoolean hasLayers = new AtomicBoolean(true);
        panel.whenLoaded(() -> {
            hasLayers.set(panel.hasLayers());
            loaded.countDown();
        });

        // WorldWind's panel itself only prints the failure; the dialog is told when loading has ended
        assertTrue("loading ends", loaded.await(60, TimeUnit.SECONDS));
        assertFalse(hasLayers.get());
    }
}
