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

package org.esa.snap.gui.preferences.uibehavior;

import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;

/**
 * TODO fill out or delete
 *
 * @author thomas
 */
@org.openide.util.NbBundle.Messages({
        "Options_DisplayName_GeoLocation=Geo-Location",
        "Options_Keywords_GeoLocation=geo, location, geo-location, compatibility, differ"
})
@OptionsPanelController.SubRegistration(location = "General",
        displayName = "#Options_DisplayName_GeoLocation",
        keywords = "#Options_Keywords_GeoLocation",
        keywordsCategory = "Geo-Location",
        id = "GeoLocation")
public final class GeoLocationPanelController extends DefaultConfigController {

    /**
     * Preferences key for geo-location epsilon
     */
    public static final String PROPERTY_KEY_GEOLOCATION_EPS = "geolocation.eps";

    /**
     * Default geo-location epsilon
     */
    public static final double DEFAULT_GEOLOCATION_EPS = 1.0e-4;

    protected Object createBean() {
        return new GeoLocationBean();
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("geo-location");
    }

    static class GeoLocationBean {

        @ConfigProperty(label = "Consider products as spatially compatible<br>if their geo-locations differ less than", key = PROPERTY_KEY_GEOLOCATION_EPS)
        double geolocationEps = DEFAULT_GEOLOCATION_EPS;
    }

}
