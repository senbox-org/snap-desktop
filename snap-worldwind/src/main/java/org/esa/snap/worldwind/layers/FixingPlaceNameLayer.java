/*
 * Copyright (c) 2021.  Brockmann Consult GmbH (info@brockmann-consult.de)
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
 *
 *
 */

package org.esa.snap.worldwind.layers;

import gov.nasa.worldwind.layers.Earth.NASAWFSPlaceNameLayer;
import gov.nasa.worldwind.render.DeclutteringTextRenderer;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.DrawContextImpl;
import gov.nasa.worldwind.render.GeographicText;
import org.esa.snap.runtime.Engine;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author Marco Peters
 * @since 8.0.6
 */
public class FixingPlaceNameLayer extends NASAWFSPlaceNameLayer {

    private static final Logger LOGGER = Engine.getInstance().getLogger();
    private final DeclutteringTextRenderer fixedTxtRenderer;
    private final Field rendererField;

    public FixingPlaceNameLayer() {
        super();
        rendererField = getTextRendererField();
        final HashMap<String, String> nameMap = new HashMap<>();
        nameMap.put("Sea of Japan", "Sea of Japan/East Sea");
        fixedTxtRenderer = new NamesFixingTextRenderer(nameMap);
    }

    @Override
    public void render(DrawContext dc) {
        if (rendererField != null) {
            fixedTextRendering(dc, rendererField, fixedTxtRenderer);
        } else {
            super.render(dc);
        }
    }

    private void fixedTextRendering(DrawContext dc, Field field, DeclutteringTextRenderer fixedDtr) {
        DeclutteringTextRenderer origRenderer = dc.getDeclutteringTextRenderer();
        try {
            exchangeRenderer(dc, field, fixedDtr);
            super.render(dc);
        } finally {
            exchangeRenderer(dc, field, origRenderer);
        }
    }

    private void exchangeRenderer(DrawContext dc, Field field, DeclutteringTextRenderer fixedDtr) {
        try {
            field.setAccessible(true);
            field.set(dc, fixedDtr);
        } catch (IllegalAccessException e) {
            LOGGER.warning("Could not change text renderer. Labels in WordWind view might not be correct.");
        } finally {
            field.setAccessible(false);
        }
    }

    private static Field getTextRendererField() {
        Field field = null;
        try {
            field = DrawContextImpl.class.getDeclaredField("declutteringTextRenderer");
        } catch (NoSuchFieldException e) {
            LOGGER.warning("Could not retrieve declutteringTextRenderer. Labels in WordWind view are not corrected.");
        }
        return field;
    }

    private static class NamesFixingTextRenderer extends DeclutteringTextRenderer {
        private final Map<String, String> nameMap;

        public NamesFixingTextRenderer(Map<String, String> nameMap) {
            this.nameMap = nameMap;
        }

        @Override
        public void render(DrawContext dc, Iterable<? extends GeographicText> textIterable) {
            final Iterator<? extends GeographicText> iterator = textIterable.iterator();
            List<GeographicText> geoTextList = new ArrayList<>();
            while (iterator.hasNext()) {
                GeographicText geoText = iterator.next();
                final String currentName = geoText.getText().toString();
                if (nameMap.containsKey(currentName)) {
                    geoText.setText(nameMap.get(currentName));
                }
                geoTextList.add(geoText);
            }
            super.render(dc, geoTextList);
        }
    }
}
