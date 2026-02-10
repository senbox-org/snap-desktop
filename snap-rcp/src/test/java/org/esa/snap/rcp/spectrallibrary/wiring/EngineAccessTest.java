package org.esa.snap.rcp.spectrallibrary.wiring;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.speclib.api.SpectralLibraryService;
import org.junit.Test;

import static org.junit.Assert.*;


public class EngineAccessTest {


    @Test
    @STTM("SNAP-4128")
    public void test_libraryService_isPresentOrThrowsHelpfulMessage() {
        try {
            SpectralLibraryService s = EngineAccess.libraryService();
            assertNotNull(s);
        } catch (IllegalStateException ex) {
            assertTrue(ex.getMessage().toLowerCase().contains("spectrallibraryservice"));
        }
    }
}