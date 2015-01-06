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

package org.esa.snap.gui.preferences;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.Enablement;
import org.esa.beam.util.SystemUtils;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;

/**
 * The top-level controller for logging preferences.
 *
 * @author thomas
 */
@org.openide.util.NbBundle.Messages({
        "Options_DisplayName_Logging=Logging",
        "Options_Keywords_Logging=Logging, Logger, Log"
})
@OptionsPanelController.TopLevelRegistration(
        categoryName = "#Options_DisplayName_Logging",
        iconBase = "org/esa/snap/gui/icons/Logging32.gif",
        keywords = "#Options_Keywords_Logging",
        keywordsCategory = "Logging",
        id = "Logging")
public final class LoggingPanelController extends DefaultConfigController {

    public static final String PROPERTY_KEY_APP_LOG_ENABLED = "app.log.enabled";
    public static final String PROPERTY_KEY_APP_LOG_PREFIX = "app.log.prefix";
    public static final String PROPERTY_KEY_APP_LOG_LEVEL = "app.log.level";
    public static final String PROPERTY_KEY_APP_LOG_ECHO = "app.log.echo";
    public static final String PROPERTY_KEY_APP_LOG_DEBUG = "app.debug.enabled";

    @Override
    protected Object createBean() {
        return new LoggingBean();
    }

    @Override
    protected void configure(BindingContext context) {
        Enablement enablementLogPrefix = context.bindEnabledState(PROPERTY_KEY_APP_LOG_PREFIX, true,
                                                                  PROPERTY_KEY_APP_LOG_ENABLED, true);
        Enablement enablementLogEcho = context.bindEnabledState(PROPERTY_KEY_APP_LOG_ECHO, true,
                                                                PROPERTY_KEY_APP_LOG_ENABLED, true);
        Enablement enablementLogDebug = context.bindEnabledState(PROPERTY_KEY_APP_LOG_DEBUG, true,
                                                                 PROPERTY_KEY_APP_LOG_ENABLED, true);
        // always disabled
        context.bindEnabledState(PROPERTY_KEY_APP_LOG_LEVEL, false, new Enablement.Condition() {
            @Override
            public boolean evaluate(BindingContext bindingContext) {
                return true;
            }
        }).apply();


        context.getPropertySet().getProperty(PROPERTY_KEY_APP_LOG_ENABLED).addPropertyChangeListener(evt -> {
            enablementLogPrefix.apply();
            enablementLogEcho.apply();
            enablementLogDebug.apply();
        });

        context.getPropertySet().getProperty(PROPERTY_KEY_APP_LOG_DEBUG).addPropertyChangeListener(evt -> {
            Property logLevelProperty = context.getPropertySet().getProperty(PROPERTY_KEY_APP_LOG_LEVEL);
            boolean isLogDebug = (Boolean) evt.getNewValue();
            try {
                if (isLogDebug) {
                    logLevelProperty.setValue(SystemUtils.LLS_DEBUG);
                } else {
                    logLevelProperty.setValue(SystemUtils.LLS_INFO);
                }
            } catch (ValidationException e) {
                e.printStackTrace(); // very basic exception handling because exception is not expected to be thrown
            }
        });

    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("logging");
    }

    @SuppressWarnings("UnusedDeclaration")
    static class LoggingBean {

        @ConfigProperty(label = "Enable logging", key = PROPERTY_KEY_APP_LOG_ENABLED)
        boolean logEnabled;

        @ConfigProperty(label = "Log filename prefix", key = PROPERTY_KEY_APP_LOG_PREFIX)
        String logPrefix;

        @ConfigProperty(label = "Echo log output (effective only with console)", key = PROPERTY_KEY_APP_LOG_ECHO)
        boolean logEcho;

        @ConfigProperty(label = "Log extra debugging information", key = PROPERTY_KEY_APP_LOG_DEBUG)
        boolean logDebug;

        @ConfigProperty(label = "Log level", key = PROPERTY_KEY_APP_LOG_LEVEL, valueSet = {SystemUtils.LLS_DEBUG,
                SystemUtils.LLS_INFO, SystemUtils.LLS_ERROR, SystemUtils.LLS_WARNING})
        String logLevel = SystemUtils.LLS_INFO;

    }
}
