package org.esa.snap.rcp.actions.file;

import org.esa.snap.vfs.NioPaths;

import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Maintains a list of unique, existing file paths in a preferences store.
 *
 * @author Norman Fomferra
 */
class RecentPaths {
    private final Preferences preferences;
    private final String key;
    private final boolean filterExisting;

    public RecentPaths(Preferences preferences, String key, boolean filterExisting) {
        this.preferences = preferences;
        this.key = key;
        this.filterExisting = filterExisting;
    }

    public List<String> get() {
        return getAsStream().collect(Collectors.toList());
    }

    public void add(String path) {
        if (path.isEmpty() || !isPathValid(path)) {
            return;
        }
        String value = Stream
                .concat(Stream.of(path), getAsStream().filter(p -> !p.equals(path)))
                .collect(Collectors.joining(File.pathSeparator));
        preferences.put(key, value);
        flush();
    }

    private boolean isPathValid(String path) {
        try {
            Paths.get(path);
        } catch (InvalidPathException e) {
            return false;
        }
        return true;
    }

    public void clear() {
        preferences.remove(key);
        flush();
    }

    private Stream<String> getAsStream() {
        String value = preferences.get(key, null);
        if (value == null) {
            return Stream.empty();
        }
        return Arrays
                .stream(value.split(File.pathSeparator))
                .map(this::convertToPath)
                .filter(Objects::nonNull)
                .map(Path::toString);
    }

    private Path convertToPath(String pathAsString) {
        try {
            return NioPaths.get(pathAsString);
        } catch (java.nio.file.InvalidPathException e) {
            return null;
        }
    }

    private void flush() {
        try {
            preferences.flush();
        } catch (BackingStoreException e) {
            // ignored, may log later
        }
    }

}
