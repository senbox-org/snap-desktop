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

package org.esa.snap.gui.preferences;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertyDescriptorFactory;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.Validator;
import com.bc.ceres.binding.ValueRange;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.core.Assert;
import com.bc.ceres.swing.binding.BindingContext;
import org.esa.beam.util.StringUtils;
import org.esa.snap.gui.SnapApp;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.Lookup;
import org.openide.util.NbPreferences;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Field;
import java.util.prefs.Preferences;

/**
 * Abstract superclass for preferences pages. Subclasses need to be annotated with either
 * {@link OptionsPanelController.TopLevelRegistration} or {@link OptionsPanelController.SubRegistration}.
 *
 * @author thomas
 */
public abstract class DefaultConfigController extends OptionsPanelController {

    private PreferencesPanel panel;
    private Preferences preferences;
    private BindingContext bindingContext;
    private Object originalState;

    /**
     * Create a bean object instance that holds all parameters. The parameters need to annotated with
     * {@link ConfigProperty}. Clients that want to maintain properties need to overwrite this method.
     *
     * @return A bean object instance.
     */
    protected Object createBean() {
        return new Object();
    }

    /**
     * Create a panel that allows the user to set the parameters in the given {@link BindingContext}. Clients that want
     * to create their own panel representation on the given properties need to overwrite this method.
     *
     * @param context    The {@link BindingContext} for the panel.
     * @return           A JPanel instance for the given {@link BindingContext}. If <code>null</code>, a default panel
     *                   will be used.
     */
    protected JPanel createPanel(BindingContext context) {
        return null;
    };

    @Override
    public void update() {
        // Called when the panel is visited, so used to store the original state.
        setOriginalState();
    }

    @Override
    public void applyChanges() {
        panel.setChanged(false);
        originalState = null;
    }

    @Override
    public void cancel() {
        panel.setChanged(false);
        if (originalState != null) {
            restoreOriginalState();
        }
        originalState = null;
    }

    @Override
    public boolean isValid() {
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
        return panel != null && panel.isChanged();
    }

    @Override
    public JComponent getComponent(Lookup lookup) {
        if (!isInitialised()) {
            init();
        }
        return panel.getComponent();
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener propertyChangeListener) {
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener propertyChangeListener) {
    }

    private boolean isInitialised() {
        return panel != null;
    }

    private void init() {
        preferences = NbPreferences.forModule(SnapApp.class);
        setupPanel(createBean());
        initiallyFillPreferences();
        setupChangeListeners();
    }

    private void setOriginalState() {
        if (originalState == null) {
            originalState = createBean();
            for (Property property : bindingContext.getPropertySet().getProperties()) {
                String key = property.getName();
                for (Field field : originalState.getClass().getDeclaredFields()) {
                    if (field.getAnnotation(ConfigProperty.class).key().equals(key)) {
                        try {
                            field.setAccessible(true);
                            field.set(originalState, property.getValue());
                        } catch (IllegalAccessException e) {
                            throw new IllegalStateException(e);
                        }
                        break;
                    }
                }
            }
        }
    }

    private void restoreOriginalState() {
        try {
            for (Field origField : originalState.getClass().getDeclaredFields()) {
                String key = origField.getAnnotation(ConfigProperty.class).key();
                origField.setAccessible(true);
                Object value = origField.get(originalState);
                Property property = bindingContext.getPropertySet().getProperty(key);
                property.setValue(value);
            }
            bindingContext.adjustComponents();
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (ValidationException e) {
            throw new IllegalStateException("Must never come here.", e);
        }
    }

    private void setupChangeListeners() {
        for (Property property : bindingContext.getPropertySet().getProperties()) {
            property.addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    preferences.put(property.getDescriptor().getAttribute("key").toString(), evt.getNewValue().toString());
                }
            });
        }
    }

    private void initiallyFillPreferences() {
        for (Property property : bindingContext.getPropertySet().getProperties()) {
            preferences.put(property.getDescriptor().getAttribute("key").toString(), property.getValueAsText());
        }
    }

    private void setupPanel(Object bean) {
        bindingContext = new BindingContext(PropertyContainer.createObjectBacked(bean, new PropertyDescriptorFactory() {
            @Override
            public PropertyDescriptor createValueDescriptor(Field field) {
                Class<ConfigProperty> annotationClass = ConfigProperty.class;
                ConfigProperty annotation = field.getAnnotation(annotationClass);
                if (annotation == null) {
                    throw new IllegalStateException("Field '" + field.getName() + "' must be annotated with '" +
                                                    annotationClass.getSimpleName() + "'.");
                }
                String label = annotation.label();
                String key = annotation.key();
                String[] valueSet = annotation.valueSet();
                String valueRange = annotation.interval();
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
                valueDescriptor.setAttribute("propertyValidator", validator);

                if (valueSet.length > 0) {
                    valueDescriptor.setValueSet(new ValueSet(valueSet));
                }
                if (StringUtils.isNotNullAndNotEmpty(valueRange)) {
                    valueDescriptor.setValueRange(ValueRange.parseValueRange(valueRange));
                }
                return valueDescriptor;
            }
        }));
        panel = new PreferencesPanel(createPanel(bindingContext), bindingContext);
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
