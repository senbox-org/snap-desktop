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

import org.esa.snap.core.util.Guardian;

import java.util.LinkedList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * <code>UserInputHistory</code> is a fixed-size array for {@code String} entries edited by a user. If a new entry is added
 * and the history is full, the list of registered entries is shifted so that the oldest entry is beeing
 * skipped.
 *
 * @author Norman Fomferra
 * @version $Revision$  $Date$
 */
public class UserInputHistory {

    private String propertyKey;
    private int maxNumEntries;
    private List<String> entriesList;

    public UserInputHistory(int maxNumEntries, String propertyKey) {
        Guardian.assertNotNullOrEmpty("propertyKey", propertyKey);
        this.propertyKey = propertyKey;
        setMaxNumEntries(maxNumEntries);
    }

    public int getNumEntries() {
        if (entriesList != null) {
            return entriesList.size();
        }
        return 0;
    }

    public int getMaxNumEntries() {
        return maxNumEntries;
    }

    public String getPropertyKey() {
        return propertyKey;
    }

    public String[] getEntries() {
        if (entriesList != null) {
            return entriesList.toArray(new String[entriesList.size()]);
        }
        return null;
    }

    public void initBy(final Preferences preferences) {
        int maxNumEntries = preferences.getInt(getLengthKey(), getMaxNumEntries());
        setMaxNumEntries(maxNumEntries);

        for (int i = maxNumEntries - 1; i >= 0; i--) {
            String entry = preferences.get(getNumKey(i), null);
            if (entry != null && isValidItem(entry)) {
                push(entry);
            }
        }
    }

    protected boolean isValidItem(String item) {
        return true;
    }

    public void push(String entry) {
        if (entry != null && isValidItem(entry)) {
            if (entriesList == null) {
                entriesList = new LinkedList<String>();
            }
            for (String anEntry : entriesList) {
                if (anEntry.equals(entry)) {
                    entriesList.remove(anEntry);
                    break;
                }
            }
            if (entriesList.size() == maxNumEntries) {
                entriesList.remove(entriesList.size() - 1);
            }
            entriesList.add(0, entry);
        }
    }

    public void copyInto(Preferences preferences) {
        preferences.putInt(getLengthKey(), maxNumEntries);
        for (int i = 0; i < 100; i++) {
            preferences.put(getNumKey(i), "");
        }
        final String[] entries = getEntries();
        if (entries != null) {
            for (int i = 0; i < entries.length; i++) {
                preferences.put(getNumKey(i), entries[i]);
            }
        }
    }

    private String getLengthKey() {
        return propertyKey + ".length";
    }

    private String getNumKey(int index) {
        return propertyKey + "." + index;
    }

    public void setMaxNumEntries(int maxNumEntries) {
        this.maxNumEntries = maxNumEntries > 0 ? maxNumEntries : 16;
        if (entriesList != null) {
            while (this.maxNumEntries < entriesList.size()) {
                entriesList.remove(entriesList.size() - 1);
            }
        }
    }
}
