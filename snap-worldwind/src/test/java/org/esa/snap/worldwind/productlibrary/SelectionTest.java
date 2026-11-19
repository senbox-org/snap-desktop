package org.esa.snap.worldwind.productlibrary;

import gov.nasa.worldwind.Model;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.event.InputHandler;
import gov.nasa.worldwind.layers.LayerList;
import org.junit.Test;

import javax.swing.JPanel;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Rectangle2D;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SelectionTest {

    @Test
    public void testSelectedAreaIsNormalisedForEveryDragDirection() {
        final Rectangle2D expected = new Rectangle2D.Double(10, 20, 5, 3);
        final Point.Double[][] drags = {
                {new Point.Double(10, 20), new Point.Double(15, 23)},
                {new Point.Double(15, 23), new Point.Double(10, 20)},
                {new Point.Double(15, 20), new Point.Double(10, 23)},
                {new Point.Double(10, 23), new Point.Double(15, 20)},
        };
        for (Point.Double[] drag : drags) {
            assertEquals(expected, Map2DMouseHandler.buildSelectedArea(drag[0], drag[1]));
        }
    }

    @Test
    public void testEmptySelectionIsNull() {
        assertNull(Map2DMouseHandler.buildSelectedArea(new Point.Double(10, 20), new Point.Double(10, 25)));
        assertNull(Map2DMouseHandler.buildSelectedArea(new Point.Double(10, 20), null));
    }

    @Test
    public void testSelectionPointsAreClampedToTheWorld() {
        final Point.Double point = new Point.Double(230, 110);
        Map2DMouseHandler.clampToWorld(point);
        assertEquals(180.0, point.x, 0.0);
        assertEquals(90.0, point.y, 0.0);

        final Point.Double negative = new Point.Double(-200, -95);
        Map2DMouseHandler.clampToWorld(negative);
        assertEquals(-180.0, negative.x, 0.0);
        assertEquals(-90.0, negative.y, 0.0);
    }

    @Test
    public void testEnablingTwiceRegistersListenersOnce() {
        final WorldWindow wwd = mock(WorldWindow.class);
        final Model model = mock(Model.class);
        final InputHandler inputHandler = mock(InputHandler.class);
        when(wwd.getModel()).thenReturn(model);
        when(model.getLayers()).thenReturn(new LayerList());
        when(wwd.getInputHandler()).thenReturn(inputHandler);

        final Rectangle3DSelection selection = new Rectangle3DSelection(wwd) {
            @Override
            protected void setCursor(Cursor cursor) {
            }
        };

        selection.enable();
        selection.enable();
        verify(inputHandler, times(1)).addMouseListener(any(MouseListener.class));
        assertTrue(selection.isSelectionEnabled());

        selection.disable();
        selection.disable();
        verify(inputHandler, times(1)).removeMouseListener(any(MouseListener.class));
        assertFalse(selection.isSelectionEnabled());
    }

    @Test
    public void testReleaseOutsideAnInteractionIsNotConsumed() {
        final WorldWindow wwd = mock(WorldWindow.class);
        final Rectangle3DSelection selection = new Rectangle3DSelection(wwd) {
            @Override
            protected void setCursor(Cursor cursor) {
            }
        };
        final MouseEvent release = new MouseEvent(new JPanel(), MouseEvent.MOUSE_RELEASED, 0, 0, 5, 5, 1, false,
                                                  MouseEvent.BUTTON1);

        selection.mouseReleased(release);

        // the globe navigation must still receive left button releases
        assertFalse(release.isConsumed());
    }
}
