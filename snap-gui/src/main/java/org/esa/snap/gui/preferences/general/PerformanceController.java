/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.gui.preferences.general;

import com.bc.ceres.binding.PropertyContainer;
import org.esa.snap.gui.preferences.DefaultConfigController;
import org.esa.snap.gui.preferences.Preference;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;

/**
 * The top-level controller for performance preferences.
 *
 * @author thomas
 */
@OptionsPanelController.TopLevelRegistration(
        categoryName = "#Options_DisplayName_Performance",
        iconBase = "org/esa/snap/gui/icons/Performance32.png",
        keywords = "#Options_Keywords_Performance",
        keywordsCategory = "Performance",
        id = "performance")
@org.openide.util.NbBundle.Messages({
        "Options_DisplayName_Performance=Performance",
        "Options_Keywords_Performance=performance"
})
public final class PerformanceController extends DefaultConfigController {

    /**
     * Preferences key for the memory capacity of the JAI tile cache in megabytes
     */
    public static final String PROPERTY_KEY_JAI_TILE_CACHE_CAPACITY = "jai.tileCache.memoryCapacity";
    /**
     * Preferences key for the number of processors which may be employed for JAI image processing.
     */
    public static final String PROPERTY_KEY_JAI_PARALLELISM = "snap.jai.parallelism";
    /**
     * Preferences key for the number of jobs that can be run in parallel.
     */
    public static final String PROPERTY_KEY_PARALLEL_JOBS_COUNT = "snap.parallelism.jobscount";

    @Override
    protected PropertyContainer createPropertyContainer() {
        return createPropertyContainer(new PerformanceBean());
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("performance");
    }

    static class PerformanceBean {

        @Preference(label = "Tile cache capacity (MB)",
                key = PROPERTY_KEY_JAI_TILE_CACHE_CAPACITY)
        int tileCacheCapacity = 512;

        @Preference(label = "Number of cores which may be employed for JAI image processing",
                key = PROPERTY_KEY_JAI_PARALLELISM)
        int jaiParallelism = Runtime.getRuntime().availableProcessors();

        @Preference(label = "Number of cores which may be employed for JAI image processing",
                key = PROPERTY_KEY_PARALLEL_JOBS_COUNT)
        int jobsCount = 1;
    }

}
