package org.esa.snap.rcp.spectrallibrary.wiring;

import org.esa.snap.speclib.api.SpectralLibraryService;
import org.esa.snap.speclib.io.SpectralLibraryIO;

import java.util.ServiceLoader;


public class EngineAccess {


    private static volatile SpectralLibraryService libraryService;
    private static volatile SpectralLibraryIO libraryIO;


    private EngineAccess() {}


    public static SpectralLibraryService libraryService() {
        SpectralLibraryService local = libraryService;
        if (local != null) {
            return local;
        }
        synchronized (EngineAccess.class) {
            if (libraryService == null) {
                libraryService = loadOrThrow(SpectralLibraryService.class);
            }
            return libraryService;
        }
    }

    public static SpectralLibraryIO libraryIO() {
        SpectralLibraryIO local = libraryIO;
        if (local != null) {
            return local;
        }
        synchronized (EngineAccess.class) {
            if (libraryIO == null) {
                libraryIO = loadOrThrow(SpectralLibraryIO.class);
            }
            return libraryIO;
        }
    }

    private static <T> T loadOrThrow(Class<T> type) {
        try {
            for (T impl : ServiceLoader.load(type)) {
                if (impl != null) {
                    return impl;
                }
            }
        } catch (Throwable ignored) {}
        throw new IllegalStateException("No implementation found via SPI for: " + type.getName());
    }
}
