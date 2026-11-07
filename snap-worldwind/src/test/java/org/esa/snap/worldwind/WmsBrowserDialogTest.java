package org.esa.snap.worldwind;

import com.sun.net.httpserver.HttpServer;
import gov.nasa.worldwind.Model;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import org.esa.snap.worldwind.preferences.WorldWindPreferences;
import org.junit.Test;

import javax.swing.JCheckBox;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WmsBrowserDialogTest {

    private static final String CAPABILITIES =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<WMS_Capabilities version=\"1.3.0\" xmlns=\"http://www.opengis.net/wms\" " +
            "xmlns:xlink=\"http://www.w3.org/1999/xlink\">\n" +
            "<Service><Name>WMS</Name><Title>Test</Title>" +
            "<OnlineResource xlink:href=\"http://127.0.0.1/wms\"/></Service>\n" +
            "<Capability><Request>" +
            "<GetCapabilities><Format>text/xml</Format><DCPType><HTTP><Get>" +
            "<OnlineResource xlink:href=\"http://127.0.0.1/wms?\"/></Get></HTTP></DCPType></GetCapabilities>" +
            "<GetMap><Format>image/png</Format><DCPType><HTTP><Get>" +
            "<OnlineResource xlink:href=\"http://127.0.0.1/wms?\"/></Get></HTTP></DCPType></GetMap>" +
            "</Request><Exception><Format>XML</Format></Exception>\n" +
            "<Layer><Title>Root</Title><CRS>EPSG:4326</CRS>" +
            "<EX_GeographicBoundingBox><westBoundLongitude>-180</westBoundLongitude>" +
            "<eastBoundLongitude>180</eastBoundLongitude><southBoundLatitude>-90</southBoundLatitude>" +
            "<northBoundLatitude>90</northBoundLatitude></EX_GeographicBoundingBox>" +
            "<Layer queryable=\"0\"><Name>test</Name><Title>Test layer</Title></Layer>" +
            "</Layer></Capability></WMS_Capabilities>\n";

    private static boolean awaitLoaded(final WmsBrowserDialog.SnapWMSLayersPanel panel, final AtomicBoolean hasLayers)
            throws InterruptedException {
        final CountDownLatch loaded = new CountDownLatch(1);
        panel.whenLoaded(() -> {
            hasLayers.set(panel.hasLayers());
            loaded.countDown();
        });
        return loaded.await(60, TimeUnit.SECONDS);
    }

    private static JCheckBox findCheckBox(final Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JCheckBox) {
                return (JCheckBox) component;
            }
            if (component instanceof Container) {
                final JCheckBox found = findCheckBox((Container) component);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    @Test
    public void testUnreachableServerIsReportedAsWithoutLayers() throws Exception {
        // a local port nobody listens on
        final int port;
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        final WmsBrowserDialog.SnapWMSLayersPanel panel = new WmsBrowserDialog.SnapWMSLayersPanel(
                mock(WorldWindow.class), "http://127.0.0.1:" + port + "/wms", new Dimension(300, 300),
                layer -> { }, layer -> { });

        final AtomicBoolean hasLayers = new AtomicBoolean(true);
        // WorldWind's panel itself only prints the failure; the dialog is told when loading has ended
        assertTrue("loading ends", awaitLoaded(panel, hasLayers));
        assertFalse(hasLayers.get());
    }

    @Test
    public void testLayerRemovedElsewhereCanBeCheckedAgain() throws Exception {
        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/wms", exchange -> {
            final byte[] body = CAPABILITIES.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/xml");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            final LayerList layers = new LayerList();
            final Model model = mock(Model.class);
            when(model.getLayers()).thenReturn(layers);
            final WorldWindow wwd = mock(WorldWindow.class);
            when(wwd.getModel()).thenReturn(model);
            final List<Layer> added = new ArrayList<>();
            final List<Layer> removed = new ArrayList<>();
            final WmsBrowserDialog.SnapWMSLayersPanel panel = new WmsBrowserDialog.SnapWMSLayersPanel(
                    wwd, "http://127.0.0.1:" + server.getAddress().getPort() + "/wms", new Dimension(300, 300),
                    added::add, removed::add);

            final AtomicBoolean hasLayers = new AtomicBoolean(false);
            assertTrue("loading ends", awaitLoaded(panel, hasLayers));
            assertTrue(hasLayers.get());

            SwingUtilities.invokeAndWait(() -> {
                final JCheckBox checkBox = findCheckBox(panel);
                assertNotNull(checkBox);
                checkBox.doClick();
                assertEquals(1, added.size());
                final Layer layer = added.get(0);
                assertTrue(layers.contains(layer));

                // removed from the globe elsewhere (the external data list), then unchecked
                layers.remove(layer);
                panel.deselect(layer);
                assertFalse(checkBox.isSelected());

                // checking it again adds it again
                checkBox.doClick();
                assertEquals(2, added.size());
                assertTrue(layers.contains(layer));

                // unchecking tells the list
                checkBox.doClick();
                assertEquals(List.of(layer), removed);
                assertFalse(layers.contains(layer));
            });
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testCancelledServerIsNeitherReportedNorSaved() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        final int port;
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        final String server = "http://127.0.0.1:" + port + "/cancelled";
        final WmsBrowserDialog.SnapWMSLayersPanel panel = new WmsBrowserDialog.SnapWMSLayersPanel(
                mock(WorldWindow.class), server, new Dimension(300, 300), layer -> { }, layer -> { });
        final AtomicBoolean hasLayers = new AtomicBoolean(true);
        assertTrue(awaitLoaded(panel, hasLayers));

        SwingUtilities.invokeAndWait(() -> {
            final WmsBrowserDialog dialog = new WmsBrowserDialog(null, mock(WorldWindow.class), l -> { }, l -> { });
            try {
                // the panel is not in the dialog's tabs, as after WorldWind's Cancel button: no error dialog
                dialog.loaded(server, panel);
                assertFalse(WorldWindPreferences.getWmsServers().contains(server));
            } finally {
                dialog.dispose();
            }
        });
    }
}
