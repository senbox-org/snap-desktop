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

import com.bc.ceres.core.runtime.internal.IOHelper;
import org.esa.snap.GlobalTestConfig;
import org.esa.snap.GlobalTestTools;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
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

    private Path _a;
    private Path _b;
    private Path _c;
    private Path _d;
    private Path _e;

    @Before
    public void setUp() {
        GlobalTestTools.deleteTestDataOutputDirectory();
        _a = GlobalTestConfig.getSnapTestDataOutputDirectory().resolve("A.dim");
        _b = GlobalTestConfig.getSnapTestDataOutputDirectory().resolve("B.dim");
        _c = GlobalTestConfig.getSnapTestDataOutputDirectory().resolve("C.dim");
        _d = GlobalTestConfig.getSnapTestDataOutputDirectory().resolve("D.dim");
        _e = GlobalTestConfig.getSnapTestDataOutputDirectory().resolve("E.dim");
        try {
            IOHelper.createDirectories(_a.getParent());
            IOHelper.createFile(_a);
            IOHelper.createFile(_b);
            //IOHelper.createFile(_c); should not be created
            IOHelper.createFile(_d);
            IOHelper.createFile(_e);
        } catch (Exception e) {
        }
    }

    @After
    public void tearDown() {
        GlobalTestTools.deleteTestDataOutputDirectory();
    }

    @Test
    public void testFileHistory() {
        assertTrue(_a.toAbsolutePath() + " does not exist", Files.exists(_a));
        assertTrue(_b.toAbsolutePath() + " does not exist", Files.exists(_b));
        assertTrue(_c.toAbsolutePath() + " does exist", !Files.exists(_c)); // should not exist
        assertTrue(_d.toAbsolutePath() + " deos not exist", Files.exists(_d));
        assertTrue(_e.toAbsolutePath() + " deos not exist", Files.exists(_e));
        final String propertyKey = "recent.files.";
        final Preferences preferences = new DummyPreferences();
        preferences.putInt(propertyKey + ".length", 3);
        preferences.put(propertyKey + ".0", _a.toAbsolutePath().toString());
        preferences.put(propertyKey + ".1", _b.toAbsolutePath().toString());
        preferences.put(propertyKey + ".2", _c.toAbsolutePath().toString());
        preferences.put(propertyKey + ".3", _d.toAbsolutePath().toString());
        preferences.put(propertyKey + ".4", _e.toAbsolutePath().toString());

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
        assertEquals(_a.toAbsolutePath().toString(), files[0]);
        assertEquals(_b.toAbsolutePath().toString(), files[1]);

        //Add new Path to existin two files
        history.push(_d.toAbsolutePath().toString());

        assertEquals(3, history.getNumEntries());
        files = history.getEntries();
        assertEquals(3, files.length);
        assertEquals(_d.toAbsolutePath().toString(), files[0]);
        assertEquals(_a.toAbsolutePath().toString(), files[1]);
        assertEquals(_b.toAbsolutePath().toString(), files[2]);

        //Add new Path to existing three files
        history.push(_e.toAbsolutePath().toString());

        assertEquals(3, history.getNumEntries());
        files = history.getEntries();
        assertEquals(3, files.length);
        assertEquals(_e.toAbsolutePath().toString(), files[0]);
        assertEquals(_d.toAbsolutePath().toString(), files[1]);
        assertEquals(_a.toAbsolutePath().toString(), files[2]);

        //decreace num max files
        history.setMaxNumEntries(2);

        assertEquals(2, history.getNumEntries());
        files = history.getEntries();
        assertEquals(2, files.length);
        assertEquals(_e.toAbsolutePath().toString(), files[0]);
        assertEquals(_d.toAbsolutePath().toString(), files[1]);

        //copy values to properties
        history.copyInto(preferences);

        assertEquals(2, preferences.getInt(propertyKey + ".length", -1));
        assertEquals(_e.toAbsolutePath().toString(), preferences.get(propertyKey + ".0", null));
        assertEquals(_d.toAbsolutePath().toString(), preferences.get(propertyKey + ".1", null));
        assertNull(preferences.get(propertyKey + ".2", null));
        assertNull(preferences.get(propertyKey + ".3", null));
        assertNull(preferences.get(propertyKey + ".4", null));
    }
}
