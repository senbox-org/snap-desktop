/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.landcover;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.util.ResourceInstaller;
import org.esa.snap.core.util.SystemUtils;
import org.openide.modules.OnStart;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Handle OnStart for module
 */
public class LandCoverModule {

    @OnStart
    public static class StartOp implements Runnable {

        @Override
        public void run() {
            installColorPalettes(this.getClass(), "org/esa/snap/landcover/auxdata/color_palettes/");
        }
    }

    public static void installColorPalettes(final Class callingClass, final String path) {
        final Path moduleBasePath = ResourceInstaller.findModuleCodeBasePath(callingClass);
        final Path auxdataDir = getColorPalettesDir();
        Path sourcePath = moduleBasePath.resolve(path);
        final ResourceInstaller resourceInstaller = new ResourceInstaller(sourcePath, auxdataDir);

        try {
            resourceInstaller.install(".*.cpd", ProgressMonitor.NULL);
        } catch (IOException e) {
            SystemUtils.LOG.severe("Unable to install colour palettes "+moduleBasePath+" to "+auxdataDir+" "+e.getMessage());
        }
    }

    private static Path getColorPalettesDir() {
        return SystemUtils.getAuxDataPath().resolve("color_palettes");
    }
}
