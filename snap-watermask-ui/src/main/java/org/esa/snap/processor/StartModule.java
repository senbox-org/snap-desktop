package org.esa.snap.processor;

import org.esa.snap.engine_utilities.util.ResourceUtils;
import org.openide.modules.OnStart;

/**
 * Handle OnStart for module
 */
public class StartModule {

    @OnStart
    public static class StartOp implements Runnable {

        @Override
        public void run() {
            ResourceUtils.installGraphs(this.getClass(), "org/esa/snap/processor/graphs/");
        }
    }
}
