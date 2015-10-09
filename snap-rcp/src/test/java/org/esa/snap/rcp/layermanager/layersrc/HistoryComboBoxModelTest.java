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

package org.esa.snap.rcp.layermanager.layersrc;

import org.esa.snap.ui.UserInputHistory;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.NodeChangeListener;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

import static org.junit.Assert.*;

public class HistoryComboBoxModelTest {

    @Test
    public void testAddElement() {
        final HistoryComboBoxModel model = new HistoryComboBoxModel(new UserInputHistory(3, "historyItem"));
        assertEquals(0, model.getSize());

        model.setSelectedItem("one");
        assertEquals(1, model.getSize());
        model.setSelectedItem("two");
        model.setSelectedItem("three");
        assertEquals(3, model.getSize());
        assertEquals("three", model.getElementAt(0));
        assertEquals("two", model.getElementAt(1));
        assertEquals("one", model.getElementAt(2));

        model.setSelectedItem("four");
        assertEquals(3, model.getSize());
        assertEquals("four", model.getElementAt(0));
        assertEquals("three", model.getElementAt(1));
        assertEquals("two", model.getElementAt(2));

        model.setSelectedItem("five");
        assertEquals(3, model.getSize());
        assertEquals("five", model.getElementAt(0));
        assertEquals("four", model.getElementAt(1));
        assertEquals("three", model.getElementAt(2));
    }

    @Test
    public void testAddElementWithInnitilaizedProperties() {
        final Preferences preferences = new DummyPreferences();
        preferences.put("historyItem.0", "one");
        preferences.put("historyItem.1", "two");
        final UserInputHistory history = new UserInputHistory(3, "historyItem");
        history.initBy(preferences);
        final HistoryComboBoxModel model = new HistoryComboBoxModel(history);
        assertEquals(2, model.getSize());
        assertEquals("one", model.getElementAt(0));
        assertEquals("two", model.getElementAt(1));

        model.setSelectedItem("three");
        assertEquals(3, model.getSize());
        assertEquals("three", model.getElementAt(0));
        assertEquals("one", model.getElementAt(1));
        assertEquals("two", model.getElementAt(2));

        model.setSelectedItem("four");
        assertEquals(3, model.getSize());
        assertEquals("four", model.getElementAt(0));
        assertEquals("three", model.getElementAt(1));
        assertEquals("one", model.getElementAt(2));

    }


    @Test
    public void testValidation() {
        final Preferences preferences = new DummyPreferences();
        preferences.put("historyItem.0", "one");
        preferences.put("historyItem.1", "two");
        preferences.put("historyItem.2", "three");

        final UserInputHistory history = new UserInputHistory(3, "historyItem") {
            @Override
            protected boolean isValidItem(String item) {
                return "two".equals(item);

            }
        };
        history.initBy(preferences);
        final HistoryComboBoxModel model = new HistoryComboBoxModel(history);
        assertEquals(1, model.getSize());
        assertEquals("two", model.getElementAt(0));
    }

    @Test
    public void testSetSelected() {
        final Preferences preferences = new DummyPreferences();
        preferences.put("historyItem.0", "one");
        preferences.put("historyItem.1", "two");

        final UserInputHistory history = new UserInputHistory(3, "historyItem");
        history.initBy(preferences);
        final HistoryComboBoxModel model = new HistoryComboBoxModel(history);
        assertEquals(2, model.getSize());
        assertEquals("one", model.getElementAt(0));
        assertEquals("two", model.getElementAt(1));

        model.setSelectedItem("two");
        assertEquals(2, model.getSize());
        assertEquals("two", model.getElementAt(0));
        assertEquals("one", model.getElementAt(1));

        model.setSelectedItem("three");
        assertEquals(3, model.getSize());
        assertEquals("three", model.getElementAt(0));
        assertEquals("two", model.getElementAt(1));
        assertEquals("one", model.getElementAt(2));

        model.setSelectedItem("one");
        assertEquals(3, model.getSize());
        assertEquals("one", model.getElementAt(0));
        assertEquals("three", model.getElementAt(1));
        assertEquals("two", model.getElementAt(2));

        model.setSelectedItem("four");
        assertEquals(3, model.getSize());
        assertEquals("four", model.getElementAt(0));
        assertEquals("one", model.getElementAt(1));
        assertEquals("three", model.getElementAt(2));

    }

    @Test
    public void testSetSelectedOnEmptyHistory() {
        final HistoryComboBoxModel model = new HistoryComboBoxModel(new UserInputHistory(3, "historyItem"));
        assertEquals(0, model.getSize());

        model.setSelectedItem("one");
        assertEquals(1, model.getSize());
        assertEquals("one", model.getElementAt(0));

        model.setSelectedItem("two");
        assertEquals(2, model.getSize());
        assertEquals("two", model.getElementAt(0));
        assertEquals("one", model.getElementAt(1));
    }

    @Test
    public void testLoadHistory() {
        final Preferences preferences = new DummyPreferences();
        preferences.put("historyItem.0", "one");

        final UserInputHistory history = new UserInputHistory(3, "historyItem");
        history.initBy(preferences);
        final HistoryComboBoxModel model = new HistoryComboBoxModel(history);
        assertEquals(1, model.getSize());

        preferences.put("historyItem.1", "two");
        preferences.put("historyItem.2", "three");

        model.getHistory().initBy(preferences);
        assertEquals(3, model.getSize());
        assertEquals("one", model.getElementAt(0));
        assertEquals("two", model.getElementAt(1));
        assertEquals("three", model.getElementAt(2));
    }

    @Test
    public void testLoadHistoryOverwritesCurrentModel() {
        final Preferences preferences = new DummyPreferences();
        preferences.put("historyItem.0", "one");
        final UserInputHistory history = new UserInputHistory(3, "historyItem");
        history.initBy(preferences);
        final HistoryComboBoxModel model = new HistoryComboBoxModel(history);
        assertEquals(1, model.getSize());
        model.setSelectedItem("two");
        model.setSelectedItem("three");
        assertEquals(3, model.getSize());

        preferences.put("historyItem.1", "two2");
        preferences.put("historyItem.2", "three3");

        model.getHistory().initBy(preferences);
        assertEquals(3, model.getSize());
        assertEquals("one", model.getElementAt(0));
        assertEquals("two2", model.getElementAt(1));
        assertEquals("three3", model.getElementAt(2));
    }

    @Test
    public void testSaveHistory() {
        final HistoryComboBoxModel model = new HistoryComboBoxModel(new UserInputHistory(3, "historyItem"));
        model.setSelectedItem("one");
        model.setSelectedItem("two");

        final Preferences preferences = new DummyPreferences();
        model.getHistory().copyInto(preferences);
        assertEquals("two", preferences.get("historyItem.0", ""));
        assertEquals("one", preferences.get("historyItem.1", ""));
        assertEquals("", preferences.get("historyItem.2", ""));

        model.setSelectedItem("three");
        model.getHistory().copyInto(preferences);
        assertEquals("three", preferences.get("historyItem.0", ""));
        assertEquals("two", preferences.get("historyItem.1", ""));
        assertEquals("one", preferences.get("historyItem.2", ""));

    }

    private class DummyPreferences extends Preferences {

        Map<String, Object> propertyMap;

        DummyPreferences() {
            propertyMap = new HashMap<String, Object>();
        }

        @Override
        public void put(String key, String value) {
            propertyMap.put(key, value);
        }

        @Override
        public String get(String key, String def) {
            final Object value = propertyMap.get(key);
            if (value != null) {
                return value.toString();
            }
            return def;
        }

        @Override
        public void remove(String key) {

        }

        @Override
        public void clear() throws BackingStoreException {

        }

        @Override
        public void putInt(String key, int value) {
            propertyMap.put(key, value);
        }

        @Override
        public int getInt(String key, int def) {
            final Object value = propertyMap.get(key);
            if (value != null) {
                return Integer.parseInt(value.toString());
            }
            return def;
        }

        @Override
        public void putLong(String key, long value) {

        }

        @Override
        public long getLong(String key, long def) {
            return 0;
        }

        @Override
        public void putBoolean(String key, boolean value) {

        }

        @Override
        public boolean getBoolean(String key, boolean def) {
            return false;
        }

        @Override
        public void putFloat(String key, float value) {

        }

        @Override
        public float getFloat(String key, float def) {
            return 0;
        }

        @Override
        public void putDouble(String key, double value) {

        }

        @Override
        public double getDouble(String key, double def) {
            return 0;
        }

        @Override
        public void putByteArray(String key, byte[] value) {

        }

        @Override
        public byte[] getByteArray(String key, byte[] def) {
            return new byte[0];
        }

        @Override
        public String[] keys() throws BackingStoreException {
            return new String[0];
        }

        @Override
        public String[] childrenNames() throws BackingStoreException {
            return new String[0];
        }

        @Override
        public Preferences parent() {
            return null;
        }

        @Override
        public Preferences node(String pathName) {
            return null;
        }

        @Override
        public boolean nodeExists(String pathName) throws BackingStoreException {
            return false;
        }

        @Override
        public void removeNode() throws BackingStoreException {

        }

        @Override
        public String name() {
            return null;
        }

        @Override
        public String absolutePath() {
            return null;
        }

        @Override
        public boolean isUserNode() {
            return false;
        }

        @Override
        public String toString() {
            return null;
        }

        @Override
        public void flush() throws BackingStoreException {

        }

        @Override
        public void sync() throws BackingStoreException {

        }

        @Override
        public void addPreferenceChangeListener(PreferenceChangeListener pcl) {

        }

        @Override
        public void removePreferenceChangeListener(PreferenceChangeListener pcl) {

        }

        @Override
        public void addNodeChangeListener(NodeChangeListener ncl) {

        }

        @Override
        public void removeNodeChangeListener(NodeChangeListener ncl) {

        }

        @Override
        public void exportNode(OutputStream os) throws IOException, BackingStoreException {

        }

        @Override
        public void exportSubtree(OutputStream os) throws IOException, BackingStoreException {

        }
    }
}
