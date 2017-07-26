package org.esa.snap.worldwind;

import gov.nasa.worldwind.util.Logging;
import org.esa.snap.runtime.Activator;
import org.esa.snap.runtime.Config;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Marco Peters
 */
public class WWActivator implements Activator{
    // need to keep it as static field, otherwise it might be removed from logmanager, and therefor the configuration too
    // seems this is a bug and fixed in Java9 (http://bugs.java.com/view_bug.do?bug_id=8030192)
    private static final Logger logger = Logging.logger();
    private static final String WORLDWIND_LOGLEVEL_KEY = "snap.worldwind.logLevel";

    @Override
    public void start() {
//        Logger logger = Logger.getLogger(Configuration.DEFAULT_LOGGER_NAME);
        String level = Config.instance().preferences().get(WORLDWIND_LOGLEVEL_KEY, "OFF");
        logger.setLevel(Level.parse(level));
    }

    @Override
    public void stop() {

    }

}
