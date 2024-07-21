package org.esa.snap.ui.product.spectrum;

import com.bc.ceres.annotation.STTM;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BandSelectionStateTest {

    @Test
    @STTM("SNAP-3709")
    public void testConstruction() {
        final BandSelectionState state = new BandSelectionState();

        assertFalse(state.isSelected());
    }

    @Test
    @STTM("SNAP-3709")
    public void testSetSelected() {
        final BandSelectionState state = new BandSelectionState();
        assertFalse(state.isSelected());

        state.setSelected(true);
        assertTrue(state.isSelected());

        state.setSelected(false);
        assertFalse(state.isSelected());
    }

    @Test
    @STTM("SNAP-3709")
    public void testSetSelected_tracksSelectionCount() {
        final BandSelectionState state = new BandSelectionState();
        assertFalse(state.isSelected());

        state.setSelected(true);
        assertTrue(state.isSelected());

        state.setSelected(true);
        assertTrue(state.isSelected());

        // we have a reference count of 2 here
        state.setSelected(false);
        assertTrue(state.isSelected());

        state.setSelected(false);
        assertFalse(state.isSelected());
    }
}
