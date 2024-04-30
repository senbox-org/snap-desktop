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

import org.esa.snap.GlobalTestConfig;
import org.esa.snap.GlobalTestTools;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.prefs.Preferences;

import static org.esa.snap.core.util.Guardian.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * <code>FileHistory</code> is a fixed-size array for the pathes of files opened/saved by a user. If a new file is added
 * and the file history is full, the list of registered files is shifted so that the oldest file path is beeing
 * skipped..
 *
 * @author Norman Fomferra
 * @version $Revision$  $Date$
 */
public class FileHistoryTest {

    private File _a;
    private File _b;
    private File _c;
    private File _d;
    private File _e;

    @Before
    public void setUp() {
        GlobalTestTools.deleteTestDataOutputDirectory();
        _a = new File(GlobalTestConfig.getSnapTestDataOutputDirectory(), "A.dim");
        _b = new File(GlobalTestConfig.getSnapTestDataOutputDirectory(), "B.dim");
        _c = new File(GlobalTestConfig.getSnapTestDataOutputDirectory(), "C.dim");
        _d = new File(GlobalTestConfig.getSnapTestDataOutputDirectory(), "D.dim");
        _e = new File(GlobalTestConfig.getSnapTestDataOutputDirectory(), "E.dim");
        try {
            _a.getParentFile().mkdirs();
            _a.createNewFile();
            _b.createNewFile();
            //_c.createNewFile(); should not be created
            _d.createNewFile();
            _e.createNewFile();
        } catch (IOException e) {
        }
    }

    @After
    public void tearDown() {
        GlobalTestTools.deleteTestDataOutputDirectory();
    }

    @Test
    public void testFileHistory() {
        assertTrue(_a.getAbsolutePath() + " does not exist", _a.exists());
        assertTrue(_b.getAbsolutePath() + " does not exist", _b.exists());
        assertTrue(_c.getAbsolutePath() + " does exist", !_c.exists()); // should not exist
        assertTrue(_d.getAbsolutePath() + " deos not exist", _d.exists());
        assertTrue(_e.getAbsolutePath() + " deos not exist", _e.exists());
        final String propertyKey = "recent.files.";
        final Preferences preferences = new DummyPreferences();
        preferences.putInt(propertyKey + ".length", 3);
        preferences.put(propertyKey + ".0", _a.getAbsolutePath());
        preferences.put(propertyKey + ".1", _b.getAbsolutePath());
        preferences.put(propertyKey + ".2", _c.getAbsolutePath());
        preferences.put(propertyKey + ".3", _d.getAbsolutePath());
        preferences.put(propertyKey + ".4", _e.getAbsolutePath());

        //create and init new FileHistory
        final FileHistory history = new FileHistory(9, propertyKey);

        assertEquals(9, history.getMaxNumEntries());
        assertEquals(0, history.getNumEntries());
        assertNull(history.getEntries());

        //init by Properties
        history.initBy(preferences);

        assertEquals(3, history.getMaxNumEntries());
        assertEquals(2, history.getNumEntries());
        String[] files = history.getEntries();
        assertEquals(2, files.length);
        assertEquals(_a.getAbsolutePath(), files[0]);
        assertEquals(_b.getAbsolutePath(), files[1]);

        //Add new File to existin two files
        history.push(_d.getAbsolutePath());

        assertEquals(3, history.getNumEntries());
        files = history.getEntries();
        assertEquals(3, files.length);
        assertEquals(_d.getAbsolutePath(), files[0]);
        assertEquals(_a.getAbsolutePath(), files[1]);
        assertEquals(_b.getAbsolutePath(), files[2]);

        //Add new File to existing three files
        history.push(_e.getAbsolutePath());

        assertEquals(3, history.getNumEntries());
        files = history.getEntries();
        assertEquals(3, files.length);
        assertEquals(_e.getAbsolutePath(), files[0]);
        assertEquals(_d.getAbsolutePath(), files[1]);
        assertEquals(_a.getAbsolutePath(), files[2]);

        //decreace num max files
        history.setMaxNumEntries(2);

        assertEquals(2, history.getNumEntries());
        files = history.getEntries();
        assertEquals(2, files.length);
        assertEquals(_e.getAbsolutePath(), files[0]);
        assertEquals(_d.getAbsolutePath(), files[1]);

        //copy values to properties
        history.copyInto(preferences);

        assertEquals(2, preferences.getInt(propertyKey + ".length", -1));
        assertEquals(_e.getAbsolutePath(), preferences.get(propertyKey + ".0", null));
        assertEquals(_d.getAbsolutePath(), preferences.get(propertyKey + ".1", null));
        assertNull(preferences.get(propertyKey + ".2", null));
        assertNull(preferences.get(propertyKey + ".3", null));
        assertNull(preferences.get(propertyKey + ".4", null));
    }
}
