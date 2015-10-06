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

package org.esa.snap.rcp.preferences;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.Validator;
import com.bc.ceres.binding.ValueRange;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.core.Assert;
import com.bc.ceres.swing.binding.BindingContext;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.runtime.Config;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.Lookup;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.beans.PropertyChangeListener;
import java.util.HashSet;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Abstract superclass for preferences pages. Subclasses need to be annotated with either
 * {@link OptionsPanelController.TopLevelRegistration} or {@link OptionsPanelController.SubRegistration}.
 *
 * @author thomas
 */
public abstract class DefaultConfigController extends OptionsPanelController {

    private PreferencesPanel panel;
    private BindingContext bindingContext;

    /**
     * Create a {@link PropertySet} object instance that holds all parameters.
     * Clients that want to maintain properties need to overwrite this method.
     *
     * @return An instance of {@link PropertySet}, holding all configuration parameters.
     *
     * @see #createPropertySet(Object)
     */
    protected abstract PropertySet createPropertySet();

    /**
     * Create a panel that allows the user to set the parameters in the given {@link BindingContext}. Clients that want
     * to create their own panel representation on the given properties need to overwrite this method.
     *
     * @param context The {@link BindingContext} for the panel.
     *
     * @return A JPanel instance for the given {@link BindingContext}, never <code>null</code>.
     */
    protected JPanel createPanel(BindingContext context) {
        Assert.state(isInitialised());
        return new PreferencesPanel(null, bindingContext).getComponent();
    }

    /**
     * Configure the passed binding context. This is intended to be used to create <code>enablements</code> in order to
     * add dependencies between property states. The default implementation does nothing.
     *
     * @param context The {@link BindingContext} to configure.
     *
     * @see com.bc.ceres.swing.binding.Enablement
     * @see com.bc.ceres.swing.binding.BindingContext#bindEnabledState(String, boolean, com.bc.ceres.swing.binding.Enablement.Condition)
     * @see com.bc.ceres.swing.binding.BindingContext#bindEnabledState(String, boolean, String, Object)
     */
    protected void configure(BindingContext context) {
    }

    /**
     * Creates a PropertyContainer for any bean. The bean parameters need to be annotated with {@link Preference}.
     *
     * @param bean a bean with fields annoted with {@link Preference}.
     *
     * @return an instance of {@link PropertyContainer}, fit for passing within overridden
     * {@link #createPropertySet()}.
     */
    protected final PropertyContainer createPropertySet(Object bean) {
        return PropertyContainer.createObjectBacked(bean, field -> {
            Class<Preference> annotationClass = Preference.class;
            Preference annotation = field.getAnnotation(annotationClass);
            if (annotation == null) {
                throw new IllegalStateException("Field '" + field.getName() + "' must be annotated with '" +
                                                annotationClass.getSimpleName() + "'.");
            }
            String label = annotation.label();
            String key = annotation.key();
            String[] valueSet = annotation.valueSet();
            String valueRange = annotation.interval();
            String description = annotation.description();

            Validator validator = createValidator(annotation.validatorClass());
            Assert.state(StringUtils.isNotNullAndNotEmpty(label),
                         "Label of field '" + field.getName() + "' must not be null or empty.");
            Assert.state(StringUtils.isNotNullAndNotEmpty(key),
                         "Key of field '" + field.getName() + "' must not be null or empty.");
            boolean isDeprecated = field.getAnnotation(Deprecated.class) != null;

            PropertyDescriptor valueDescriptor = new PropertyDescriptor(key, field.getType());
            valueDescriptor.setDeprecated(isDeprecated);
            valueDescriptor.setAttribute("key", key);
            valueDescriptor.setAttribute("displayName", label);
            valueDescriptor.setAttribute("configName", annotation.config());
            valueDescriptor.setAttribute("propertyValidator", validator);
            valueDescriptor.setDescription(description);


            if (valueSet.length > 0) {
                valueDescriptor.setValueSet(new ValueSet(valueSet));
            }
            if (StringUtils.isNotNullAndNotEmpty(valueRange)) {
                valueDescriptor.setValueRange(ValueRange.parseValueRange(valueRange));
            }
            return valueDescriptor;
        });
    }

    @Override
    public void update() {
        if (isInitialised()) {
            for (Property property : bindingContext.getPropertySet().getProperties()) {
                String key = property.getDescriptor().getAttribute("key").toString();
                String preferencesValue = getPreferences(property.getDescriptor()).get(key, null);
                if (preferencesValue != null) {
                    try {
                        property.setValueFromText(preferencesValue);
                        SystemUtils.LOG.fine(String.format("Bean property value change: %s = %s", property.getName(), property.getValueAsText()));
                    } catch (ValidationException e) {
                        SystemUtils.LOG.severe("Failed to set bean value from preferences: " + e.getMessage());
                    }
                }
            }
        }
    }

    @Override
    public void applyChanges() {
        if (isInitialised()) {
            HashSet<Preferences> set = new HashSet<Preferences>();
            for (Property property : bindingContext.getPropertySet().getProperties()) {
                String key = property.getDescriptor().getAttribute("key").toString();
                String value = property.getValueAsText();
                Preferences preferences = getPreferences(property.getDescriptor());
                preferences.put(key, value);
                set.add(preferences);
                SystemUtils.LOG.fine(String.format("Preferences value change: %s = %s", key, preferences.get(key, null)));
            }
            for (Preferences preferences : set) {
                try {
                    preferences.flush();
                } catch (BackingStoreException e) {
                    SnapApp.getDefault().handleError("Failed to store user preferences.", e);
                }
            }
            panel.setChanged(false);
        }
    }

    @Override
    public void cancel() {
        if (isInitialised()) {
            panel.setChanged(false);
        }
    }

    @Override
    public boolean isValid() {
        if (!isInitialised()) {
            return false;
        }
        for (Property property : bindingContext.getPropertySet().getProperties()) {
            Validator validator = (Validator) property.getDescriptor().getAttribute("propertyValidator");
            try {
                validator.validateValue(property, property.getValue());
            } catch (ValidationException e) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isChanged() {
        return isInitialised() && panel.isChanged();
    }

    @Override
    public JComponent getComponent(Lookup lookup) {
        if (!isInitialised()) {
            initialize();
        }
        return panel.getComponent();
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener propertyChangeListener) {
        if (bindingContext != null) {
            bindingContext.addPropertyChangeListener(propertyChangeListener);
        }
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener propertyChangeListener) {
        if (bindingContext != null) {
            bindingContext.removePropertyChangeListener(propertyChangeListener);
        }
    }

    private boolean isInitialised() {
        return bindingContext != null;
    }

    private void initialize() {
        bindingContext = new BindingContext(createPropertySet());
        panel = new PreferencesPanel(createPanel(bindingContext), bindingContext);
        panel.getComponent(); // trigger component initialisation
        configure(bindingContext);
    }

    private Preferences getPreferences(PropertyDescriptor propertyDescriptor) {
        Object configNameValue = propertyDescriptor.getAttribute("configName");
        String configName = configNameValue != null ? configNameValue.toString().trim() : null;
        if (configName == null || configName.isEmpty()) {
            return SnapApp.getDefault().getPreferences();
        }
        return Config.instance(configName).load().preferences();
    }

    private Validator createValidator(Class<? extends Validator> validatorClass) {
        Validator validator;
        try {
            validator = validatorClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        return validator;
    }
}
