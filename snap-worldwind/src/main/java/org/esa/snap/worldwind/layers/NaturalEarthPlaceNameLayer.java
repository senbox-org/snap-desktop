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
package org.esa.snap.worldwind.layers;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.GeographicText;
import gov.nasa.worldwind.render.GeographicTextRenderer;
import gov.nasa.worldwind.render.UserFacingText;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.worldwind.model.PlaceNames;

import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Labels the bundled Natural Earth populated places, more of them the closer the eye is to the ground. Replaces
 * WorldWind's place name layer, whose NASA server no longer answers.
 */
public class NaturalEarthPlaceNameLayer extends AbstractLayer {

    public static final String NAME = "Place Names (Natural Earth)";

    private static final Font PLACE_FONT = Font.decode("Arial-PLAIN-12");
    private static final Font CAPITAL_FONT = Font.decode("Arial-BOLD-13");

    private final List<PlaceNames.Place> places;
    private final Map<PlaceNames.Place, UserFacingText> texts = new IdentityHashMap<>();
    private final GeographicTextRenderer renderer = new GeographicTextRenderer();

    public NaturalEarthPlaceNameLayer() {
        this(loadPlaces());
    }

    NaturalEarthPlaceNameLayer(final List<PlaceNames.Place> places) {
        this.places = places;
        setName(NAME);
        setPickEnabled(false);
        renderer.setCullTextEnabled(true);
        renderer.setCullTextMargin(2);
        renderer.setEffect(AVKey.TEXT_EFFECT_OUTLINE);
        for (PlaceNames.Place place : places) {
            final UserFacingText text = new UserFacingText(place.name, Position.fromDegrees(place.lat, place.lon, 0));
            text.setFont(place.capital ? CAPITAL_FONT : PLACE_FONT);
            text.setColor(Color.WHITE);
            text.setBackgroundColor(Color.BLACK);
            // culling keeps the text of higher priority when labels overlap
            text.setPriority(place.capital ? Double.MAX_VALUE : place.population);
            texts.put(place, text);
        }
    }

    private static List<PlaceNames.Place> loadPlaces() {
        try {
            return PlaceNames.getBundledPlaces();
        } catch (IOException e) {
            SystemUtils.LOG.log(Level.WARNING, "WorldWind: unable to read the bundled place names", e);
            return Collections.emptyList();
        }
    }

    List<GeographicText> getTexts(final double eyeAltitude, final Sector visibleSector) {
        final List<PlaceNames.Place> selected = PlaceNames.select(places, eyeAltitude,
                visibleSector.getMinLatitude().degrees, visibleSector.getMaxLatitude().degrees,
                visibleSector.getMinLongitude().degrees, visibleSector.getMaxLongitude().degrees);
        final List<GeographicText> visible = new ArrayList<>(selected.size());
        for (PlaceNames.Place place : selected) {
            visible.add(texts.get(place));
        }
        return visible;
    }

    @Override
    protected void doRender(final DrawContext dc) {
        final Sector visibleSector = dc.getVisibleSector();
        final Position eye = dc.getView() != null ? dc.getView().getEyePosition() : null;
        if (visibleSector == null || eye == null) {
            return;
        }
        final List<GeographicText> visible = getTexts(eye.getElevation(), visibleSector);
        if (!visible.isEmpty()) {
            renderer.render(dc, visible);
        }
    }

    @Override
    public String toString() {
        return NAME;
    }
}
