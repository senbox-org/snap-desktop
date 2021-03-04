package org.esa.snap.raster;

 /**
 * Created by fdouziech on 25/02/2021.
 */

import org.esa.snap.engine_utilities.util.ResourceUtils;
import org.openide.modules.OnStart;

public class RasterModule {
    @OnStart
    public static class StartOp implements Runnable {

        @Override
        public void run() {
            ResourceUtils.installGraphs(this.getClass(), "org/esa/snap/raster/graphs/");
        }
    }
}
