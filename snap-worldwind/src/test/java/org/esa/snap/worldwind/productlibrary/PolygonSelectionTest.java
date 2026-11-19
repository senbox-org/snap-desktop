package org.esa.snap.worldwind.productlibrary;

import com.bc.ceres.glayer.swing.LayerCanvas;
import gov.nasa.worldwind.Model;
import gov.nasa.worldwind.View;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.event.InputHandler;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.LayerList;
import org.esa.snap.remote.products.repository.geometry.PolygonArea;
import org.junit.Test;

import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PolygonSelectionTest {

    private final List<String> messages = new java.util.ArrayList<>();
    private java.util.function.Consumer<String> originalHandler;

    @org.junit.Before
    public void collectMessages() {
        originalHandler = Map2DMouseHandler.polygonRejected;
        Map2DMouseHandler.polygonRejected = messages::add;
    }

    @org.junit.After
    public void restoreHandler() {
        Map2DMouseHandler.polygonRejected = originalHandler;
    }

    private static MouseEvent event(final Component source, final int id, final int x, final int y, final int clickCount) {
        return new MouseEvent(source, id, 0, MouseEvent.BUTTON1_DOWN_MASK, x, y, clickCount, false, MouseEvent.BUTTON1);
    }

    private static List<Point.Double> points(final double... xy) {
        final Point.Double[] points = new Point.Double[xy.length / 2];
        for (int i = 0; i < points.length; i++) {
            points[i] = new Point.Double(xy[2 * i], xy[2 * i + 1]);
        }
        return Arrays.asList(points);
    }

    @Test
    public void testBuildPolygonArea() {
        final PolygonArea triangle = Map2DMouseHandler.buildPolygonArea(points(10, 45, 14, 45, 12, 48));
        assertEquals(3, triangle.getVertexCount());
        // repeated vertices of a double click and a closing vertex are dropped
        assertEquals(triangle, Map2DMouseHandler.buildPolygonArea(points(10, 45, 14, 45, 14, 45, 12, 48, 10, 45)));
        // not a polygon
        assertNull(Map2DMouseHandler.buildPolygonArea(points(10, 45, 14, 45)));
        assertNull(Map2DMouseHandler.buildPolygonArea(points(10, 45, 14, 45, 14, 45)));
        assertNull("collinear", Map2DMouseHandler.buildPolygonArea(points(10, 45, 11, 46, 12, 47)));
        assertNull("bow tie", Map2DMouseHandler.buildPolygonArea(points(10, 45, 14, 48, 10, 48, 14, 45)));
    }

    @Test
    public void test2DCrossingEdgesKeepDrawingWithAMessage() {
        final LayerCanvas canvas = new LayerCanvas();
        final Map2DMouseHandler handler = new Map2DMouseHandler(canvas);
        handler.enablePolygon();
        // a bow tie
        for (int[] click : new int[][]{{10, 10}, {60, 60}, {10, 60}, {60, 10}}) {
            handler.mousePressed(event(canvas, MouseEvent.MOUSE_PRESSED, click[0], click[1], 1));
        }
        handler.mousePressed(event(canvas, MouseEvent.MOUSE_PRESSED, 60, 10, 2));
        assertTrue(handler.isDrawingPolygon());
        assertNull(handler.getSelectedArea());
        assertEquals(java.util.Collections.singletonList(Map2DMouseHandler.INVALID_POLYGON_MESSAGE), messages);
    }

    @Test
    public void testAntimeridianCrossing() {
        assertTrue(Polygon3DSelection.crossesAntimeridian(points(170, 10, -170, 10, -170, 20, 170, 20)));
        assertFalse(Polygon3DSelection.crossesAntimeridian(points(10, 45, 14, 45, 12, 48)));
        // the closing edge counts too
        assertTrue(Polygon3DSelection.crossesAntimeridian(points(175, 10, 179, 15, -178, 20)));
    }

    @Test
    public void test2DPolygonDrawing() {
        final LayerCanvas canvas = new LayerCanvas();
        final Map2DMouseHandler handler = new Map2DMouseHandler(canvas);
        handler.enablePolygon();
        assertTrue(handler.isDrawingPolygon());
        assertNull(handler.getSelectedArea());

        final int[][] clicks = {{10, 10}, {60, 10}, {35, 50}};
        for (int[] click : clicks) {
            final MouseEvent press = event(canvas, MouseEvent.MOUSE_PRESSED, click[0], click[1], 1);
            handler.mousePressed(press);
            assertTrue("drawing presses are consumed", press.isConsumed());
            handler.mouseReleased(event(canvas, MouseEvent.MOUSE_RELEASED, click[0], click[1], 1));
            final MouseEvent clicked = event(canvas, MouseEvent.MOUSE_CLICKED, click[0], click[1], 1);
            handler.mouseClicked(clicked);
            assertTrue("drawing clicks do not select footprints", clicked.isConsumed());
        }
        handler.mouseMoved(event(canvas, MouseEvent.MOUSE_MOVED, 40, 40, 0));
        assertTrue(handler.getPolygonInProgress() != null);

        // the second press of a double click closes the polygon
        handler.mousePressed(event(canvas, MouseEvent.MOUSE_PRESSED, 35, 50, 2));
        assertFalse(handler.isDrawingPolygon());
        assertNull(handler.getPolygonInProgress());

        final Rectangle2D area = handler.getSelectedArea();
        assertTrue(area instanceof PolygonArea);
        final PolygonArea polygon = (PolygonArea) area;
        assertEquals(3, polygon.getVertexCount());
        for (int i = 0; i < clicks.length; i++) {
            final Point2D expected = canvas.getViewport().getViewToModelTransform()
                    .transform(new Point(clicks[i][0], clicks[i][1]), null);
            assertEquals(expected.getX(), polygon.getLongitude(i), 1e-9);
            assertEquals(expected.getY(), polygon.getLatitude(i), 1e-9);
        }

        // the click of the closing press is consumed, a later click on the map is a normal click again
        final MouseEvent closingClick = event(canvas, MouseEvent.MOUSE_CLICKED, 35, 50, 2);
        handler.mouseClicked(closingClick);
        assertTrue(closingClick.isConsumed());
        final MouseEvent later = event(canvas, MouseEvent.MOUSE_CLICKED, 5, 5, 1);
        handler.mouseClicked(later);
        assertFalse(later.isConsumed());
    }

    @Test
    public void test2DDoubleClickWithTooFewVerticesKeepsDrawing() {
        final LayerCanvas canvas = new LayerCanvas();
        final Map2DMouseHandler handler = new Map2DMouseHandler(canvas);
        handler.enablePolygon();
        handler.mousePressed(event(canvas, MouseEvent.MOUSE_PRESSED, 10, 10, 1));
        handler.mousePressed(event(canvas, MouseEvent.MOUSE_PRESSED, 10, 10, 2));
        assertTrue(handler.isDrawingPolygon());
        assertNull(handler.getSelectedArea());
        assertTrue("no message before three vertices", messages.isEmpty());
    }

    @Test
    public void test2DSetAndClearSelectedArea() {
        final LayerCanvas canvas = new LayerCanvas();
        final Map2DMouseHandler handler = new Map2DMouseHandler(canvas);
        final PolygonArea triangle = new PolygonArea(new double[]{10, 14, 12}, new double[]{45, 45, 48});

        handler.setSelectedArea(triangle);
        assertSame(triangle, handler.getSelectedArea());

        final Rectangle2D rectangle = new Rectangle2D.Double(1, 2, 3, 4);
        handler.setSelectedArea(rectangle);
        assertEquals(rectangle, handler.getSelectedArea());
        assertFalse(handler.getSelectedArea() instanceof PolygonArea);

        handler.setSelectedArea(triangle);
        handler.disable();
        assertNull(handler.getSelectedArea());
    }

    @Test
    public void test3DPolygonDrawing() {
        final WorldWindow wwd = mock(WorldWindow.class);
        final Model model = mock(Model.class);
        final View view = mock(View.class);
        when(wwd.getModel()).thenReturn(model);
        when(model.getLayers()).thenReturn(new LayerList());
        when(wwd.getInputHandler()).thenReturn(mock(InputHandler.class));
        when(wwd.getView()).thenReturn(view);
        // screen x, y -> lon x / 10, lat y / 10
        when(view.computePositionFromScreenPoint(anyDouble(), anyDouble())).thenAnswer(invocation ->
                Position.fromDegrees((Double) invocation.getArgument(1) / 10, (Double) invocation.getArgument(0) / 10, 0));

        final Polygon3DSelection selection = new Polygon3DSelection(wwd);
        final JPanel source = new JPanel();
        selection.start();
        assertTrue(selection.isDrawing());
        for (int[] click : new int[][]{{100, 450}, {140, 450}, {120, 480}}) {
            final MouseEvent press = event(source, MouseEvent.MOUSE_PRESSED, click[0], click[1], 1);
            selection.mousePressed(press);
            assertTrue(press.isConsumed());
            selection.mouseMoved(event(source, MouseEvent.MOUSE_MOVED, click[0] + 5, click[1] + 5, 0));
        }
        selection.mousePressed(event(source, MouseEvent.MOUSE_PRESSED, 120, 480, 2));

        assertFalse(selection.isDrawing());
        assertEquals(new PolygonArea(new double[]{10, 14, 12}, new double[]{45, 45, 48}), selection.getSelectedArea());
        assertEquals("the polygon is shown", 1, selection.getLayer().getNumRenderables());

        selection.setSelectedArea(new Rectangle2D.Double(0, 0, 1, 1));
        assertNull("a rectangle removes the polygon", selection.getSelectedArea());
        assertEquals(0, selection.getLayer().getNumRenderables());
    }

    @Test
    public void test3DAntimeridianPolygonRejected() {
        final WorldWindow wwd = mock(WorldWindow.class);
        final Model model = mock(Model.class);
        final View view = mock(View.class);
        when(wwd.getModel()).thenReturn(model);
        when(model.getLayers()).thenReturn(new LayerList());
        when(wwd.getInputHandler()).thenReturn(mock(InputHandler.class));
        when(wwd.getView()).thenReturn(view);
        // screen x -> lon: 0..10 is 170..180, above 10 wraps to -180..
        when(view.computePositionFromScreenPoint(anyDouble(), anyDouble())).thenAnswer(invocation -> {
            double lon = 170 + (Double) invocation.getArgument(0);
            if (lon > 180) {
                lon -= 360;
            }
            return Position.fromDegrees((Double) invocation.getArgument(1), lon, 0);
        });

        final Polygon3DSelection selection = new Polygon3DSelection(wwd);
        final JPanel source = new JPanel();
        selection.start();
        for (int[] click : new int[][]{{0, 10}, {20, 10}, {20, 20}, {0, 20}}) {
            selection.mousePressed(event(source, MouseEvent.MOUSE_PRESSED, click[0], click[1], 1));
        }
        selection.mousePressed(event(source, MouseEvent.MOUSE_PRESSED, 0, 20, 2));
        assertTrue("keeps drawing", selection.isDrawing());
        assertNull(selection.getSelectedArea());
        assertEquals(java.util.Collections.singletonList(Polygon3DSelection.ANTIMERIDIAN_MESSAGE), messages);
    }

    @Test
    public void test3DCrossingEdgesKeepDrawingWithAMessage() {
        final WorldWindow wwd = mock(WorldWindow.class);
        final Model model = mock(Model.class);
        final View view = mock(View.class);
        when(wwd.getModel()).thenReturn(model);
        when(model.getLayers()).thenReturn(new LayerList());
        when(wwd.getInputHandler()).thenReturn(mock(InputHandler.class));
        when(wwd.getView()).thenReturn(view);
        when(view.computePositionFromScreenPoint(anyDouble(), anyDouble())).thenAnswer(invocation ->
                Position.fromDegrees((Double) invocation.getArgument(1) / 10, (Double) invocation.getArgument(0) / 10, 0));

        final Polygon3DSelection selection = new Polygon3DSelection(wwd);
        final JPanel source = new JPanel();
        selection.start();
        // a bow tie
        for (int[] click : new int[][]{{100, 450}, {140, 480}, {100, 480}, {140, 450}}) {
            selection.mousePressed(event(source, MouseEvent.MOUSE_PRESSED, click[0], click[1], 1));
        }
        selection.mousePressed(event(source, MouseEvent.MOUSE_PRESSED, 140, 450, 2));
        assertTrue("keeps drawing", selection.isDrawing());
        assertNull(selection.getSelectedArea());
        assertEquals(java.util.Collections.singletonList(Map2DMouseHandler.INVALID_POLYGON_MESSAGE), messages);
    }

    @Test
    public void test3DShapesHaveStraightLonLatEdges() {
        final WorldWindow wwd = mock(WorldWindow.class);
        final Model model = mock(Model.class);
        when(wwd.getModel()).thenReturn(model);
        when(model.getLayers()).thenReturn(new LayerList());
        when(wwd.getInputHandler()).thenReturn(mock(InputHandler.class));
        final Polygon3DSelection selection = new Polygon3DSelection(wwd);
        selection.setSelectedArea(new PolygonArea(new double[]{-60, 60, 0}, new double[]{50, 50, 70}));
        final gov.nasa.worldwind.render.SurfacePolygon shape =
                (gov.nasa.worldwind.render.SurfacePolygon) selection.getLayer().getRenderables().iterator().next();
        // as searched: straight in longitude and latitude, not great circles bowing towards the pole
        assertEquals(gov.nasa.worldwind.avlist.AVKey.LINEAR, shape.getPathType());
    }

    @Test
    public void testWideEdgesFromThe2DMapGoTheLongWayOnTheGlobe() {
        // from the 2D map: an edge from -170 to 170 spans 340 degrees, not 20 across the antimeridian
        final PolygonArea wide = new PolygonArea(new double[]{-170, 170, 170, -170}, new double[]{-10, -10, 10, 10});
        final List<gov.nasa.worldwind.geom.LatLon> locations = Polygon3DSelection.displayLocations(wide);
        assertEquals(4 + 3 + 3, locations.size());
        for (int i = 0; i < locations.size(); i++) {
            final double lon = locations.get(i).getLongitude().degrees;
            final double nextLon = locations.get((i + 1) % locations.size()).getLongitude().degrees;
            assertTrue("no edge WorldWind would take across 180", Math.abs(nextLon - lon) <= 90.0 + 1e-9);
        }
        assertEquals(-85.0, locations.get(1).getLongitude().degrees, 1e-9);
        // edges within 90 degrees are kept as they are
        final PolygonArea small = new PolygonArea(new double[]{10, 14, 12}, new double[]{45, 45, 48});
        assertEquals(3, Polygon3DSelection.displayLocations(small).size());
    }

    @Test
    public void test3DIgnoresClicksWhenNotDrawing() {
        final WorldWindow wwd = mock(WorldWindow.class);
        when(wwd.getInputHandler()).thenReturn(mock(InputHandler.class));
        final Polygon3DSelection selection = new Polygon3DSelection(wwd);
        final MouseEvent press = event(new JPanel(), MouseEvent.MOUSE_PRESSED, 5, 5, 1);
        selection.mousePressed(press);
        // the globe navigation must still receive the events
        assertFalse(press.isConsumed());
    }
}
