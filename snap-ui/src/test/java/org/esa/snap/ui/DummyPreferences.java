package org.esa.snap.ui;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.NodeChangeListener;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

/**
 * @author Tonio Fincke
 */
public class DummyPreferences extends Preferences {

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
        if(value != null && !value.equals("")) {
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
        if(value != null) {
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
