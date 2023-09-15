/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.ui;

import org.junit.Test;

import java.util.prefs.Preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class UserInputHistoryTest {

    @Test
    public void testFail() {
        final String propertyKey = "test.prop";
        final Preferences preferences = new DummyPreferences();

        preferences.putInt(propertyKey + ".length", 4);
        preferences.put(propertyKey + ".0", "0");
        preferences.put(propertyKey + ".1", "1");
        preferences.put(propertyKey + ".2", "2");
        preferences.put(propertyKey + ".3", "3");
        preferences.put(propertyKey + ".4", "4");

        final UserInputHistory history = new UserInputHistory(9, propertyKey);

        assertEquals(9, history.getMaxNumEntries());
        assertEquals(0, history.getNumEntries());
        assertNull(history.getEntries());

        history.initBy(preferences);

        assertEquals(4, history.getMaxNumEntries());
        assertEquals(4, history.getNumEntries());
        String[] entries = history.getEntries();
        assertEquals(4, entries.length);
        assertEquals("0", entries[0]);
        assertEquals("1", entries[1]);
        assertEquals("2", entries[2]);
        assertEquals("3", entries[3]);

        history.push("4");

        assertEquals(4, history.getMaxNumEntries());
        assertEquals(4, history.getNumEntries());
        entries = history.getEntries();
        assertEquals(4, entries.length);
        assertEquals("4", entries[0]);
        assertEquals("0", entries[1]);
        assertEquals("1", entries[2]);
        assertEquals("2", entries[3]);

        history.setMaxNumEntries(2);

        assertEquals(2, history.getMaxNumEntries());
        assertEquals(2, history.getNumEntries());
        entries = history.getEntries();
        assertEquals(2, entries.length);
        assertEquals("4", entries[0]);
        assertEquals("0", entries[1]);

        history.copyInto(preferences);

        assertEquals("4", preferences.get(propertyKey + ".0", null));
        assertEquals("0", preferences.get(propertyKey + ".1", null));
        assertNull(preferences.get(propertyKey + ".2", null));
        assertNull(preferences.get(propertyKey + ".3", null));
        assertNull(preferences.get(propertyKey + ".4", null));
    }
}
