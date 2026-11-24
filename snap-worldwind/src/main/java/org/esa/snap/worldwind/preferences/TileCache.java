/*
 * Copyright (C) 2026 by SkyWatch Space Applications Inc. http://www.skywatch.com
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.worldwind.preferences;

import gov.nasa.worldwind.WorldWind;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Locale;

/**
 * Size and clearing of WorldWind's tile cache. WorldWind does not limit the size of the cache itself.
 */
final class TileCache {

    private TileCache() {
    }

    /**
     * The folder WorldWind writes downloaded tiles to.
     */
    static File getLocation() {
        return WorldWind.getDataFileStore().getWriteLocation();
    }

    static long sizeOf(final File folder) {
        final long[] size = {0};
        if (folder == null || !folder.isDirectory()) {
            return 0;
        }
        try {
            Files.walkFileTree(folder.toPath(), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                    size[0] += attributes.size();
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException e) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            // partial size
        }
        return size[0];
    }

    /**
     * Deletes the content of a tile cache folder. Refuses any folder not named like WorldWind's cache folder, so that
     * a wrong location can never empty another folder.
     */
    static void clear(final File folder) throws IOException {
        if (folder == null || !WorldWindPreferences.WORLDWIND_DATA_FOLDER.equals(folder.getName())) {
            throw new IOException("Not a WorldWind tile cache: " + folder);
        }
        final Path root = folder.toPath();
        if (!Files.isDirectory(root)) {
            return;
        }
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
                if (!dir.equals(root)) {
                    Files.deleteIfExists(dir);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    static String formatSize(final long bytes) {
        if (bytes < 1024 * 1024) {
            return String.format(Locale.ENGLISH, "%d KB", bytes / 1024);
        }
        if (bytes < 1024L * 1024 * 1024) {
            return String.format(Locale.ENGLISH, "%.1f MB", bytes / (1024.0 * 1024));
        }
        return String.format(Locale.ENGLISH, "%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
