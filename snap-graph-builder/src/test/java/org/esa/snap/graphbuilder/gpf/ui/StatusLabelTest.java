package org.esa.snap.graphbuilder.gpf.ui;

import com.bc.ceres.annotation.STTM;
import org.junit.Before;
import org.junit.Test;

import java.awt.*;

import static org.junit.Assert.assertEquals;

public class StatusLabelTest {

    private StatusLabel statusLabel;

    @Before
    public void setUp() {
        statusLabel = new StatusLabel();
    }

    @Test
    @STTM("SNAP-4019")
    public void testConstruction() {
        assertEquals("", statusLabel.getText());
        assertEquals(Color.RED, statusLabel.getForeground());
    }

    @Test
    @STTM("SNAP-4019")
    public void testSetOkMessage() {
        statusLabel.setOkMessage("fine_for_me");

        assertEquals("fine_for_me", statusLabel.getText());
        assertEquals(Color.GREEN, statusLabel.getForeground());
    }

    @Test
    @STTM("SNAP-4019")
    public void testSetWarningMessage() {
        statusLabel.setOkMessage("to_reset_from_default");

        statusLabel.setWarningMessage("ooopsi");

        assertEquals("ooopsi", statusLabel.getText());
        assertEquals(Color.ORANGE, statusLabel.getForeground());
    }

    @Test
    @STTM("SNAP-4019")
    public void testSetErrorMessage() {
        statusLabel.setOkMessage("to_reset_from_default");

        statusLabel.setErrorMessage("oha!");

        assertEquals("oha!", statusLabel.getText());
        assertEquals(Color.RED, statusLabel.getForeground());
    }
}
