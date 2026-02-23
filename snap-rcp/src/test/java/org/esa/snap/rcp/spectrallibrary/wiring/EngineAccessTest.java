package org.esa.snap.rcp.spectrallibrary.wiring;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.speclib.api.SpectralLibraryService;
import org.esa.snap.speclib.io.SpectralLibraryIO;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


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

    @Test
    @STTM("SNAP-4128")
    public void test_libraryService_returnsCachedInstance_withoutServiceLoader() throws Exception {
        SpectralLibraryService mockService = mock(SpectralLibraryService.class);

        setStaticField(EngineAccess.class, "libraryService", mockService);
        SpectralLibraryService s1 = EngineAccess.libraryService();
        SpectralLibraryService s2 = EngineAccess.libraryService();

        assertSame(mockService, s1);
        assertSame(mockService, s2);
    }

    @Test
    @STTM("SNAP-4128")
    public void test_libraryIO_returnsCachedInstance_withoutServiceLoader() throws Exception {
        SpectralLibraryIO mockIO = mock(SpectralLibraryIO.class);

        setStaticField(EngineAccess.class, "libraryIO", mockIO);
        SpectralLibraryIO io1 = EngineAccess.libraryIO();
        SpectralLibraryIO io2 = EngineAccess.libraryIO();

        assertSame(mockIO, io1);
        assertSame(mockIO, io2);
    }

    @Test
    @STTM("SNAP-4128")
    public void test_libraryService_triesSpiOrThrowsHelpfulMessage() throws Exception {
        setStaticField(EngineAccess.class, "libraryService", null);

        try {
            SpectralLibraryService s = EngineAccess.libraryService();
            assertNotNull(s);
        } catch (IllegalStateException ex) {
            String msg = ex.getMessage();
            assertNotNull(msg);
            assertTrue(msg.contains(SpectralLibraryService.class.getName()));
            assertTrue(msg.toLowerCase().contains("spi"));
        }
    }

    @Test
    @STTM("SNAP-4128")
    public void test_libraryIO_triesSpiOrThrowsHelpfulMessage() throws Exception {
        setStaticField(EngineAccess.class, "libraryIO", null);

        try {
            SpectralLibraryIO io = EngineAccess.libraryIO();
            assertNotNull(io);
        } catch (IllegalStateException ex) {
            String msg = ex.getMessage();
            assertNotNull(msg);
            assertTrue(msg.contains(SpectralLibraryIO.class.getName()));
            assertTrue(msg.toLowerCase().contains("spi"));
        }
    }

    private static void setStaticField(Class<?> clazz, String fieldName, Object value) throws Exception {
        Field f = clazz.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(null, value);
    }
}