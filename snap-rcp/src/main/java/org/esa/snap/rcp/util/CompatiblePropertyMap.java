package org.esa.snap.rcp.util;

import com.bc.ceres.core.Assert;
import org.esa.beam.util.PropertyMap;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

/**
 * Only use this in cases where you need an old-style {@code PropertyMap}.
 * Actually, don't use this class.
 *
 * @author Norman
 * @author Marco
 * @deprecated By default, since this class shall be replaced by direct usage of {@code java.util.prefs.Preferences},
 */
@Deprecated
public class CompatiblePropertyMap extends PropertyMap {
    private boolean adjusting;

    public CompatiblePropertyMap(Preferences preferences) {
        Assert.notNull(preferences, "preferences");
        addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                synchronized (this) {
                    if (!adjusting) {
                        adjusting = true;
                        preferences.put(evt.getPropertyName(), String.valueOf(evt.getNewValue()));
                        adjusting = false;
                    }
                }
            }
        });
        preferences.addPreferenceChangeListener(new PreferenceChangeListener() {
            @Override
            public void preferenceChange(PreferenceChangeEvent evt) {
                synchronized (this) {
                    if (!adjusting) {
                        adjusting = true;
                        setPropertyString(evt.getKey(), evt.getNewValue());
                        adjusting = false;
                    }
                }
            }
        });
    }
}
