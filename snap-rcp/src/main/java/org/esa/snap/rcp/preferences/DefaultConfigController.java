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
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.Validator;
import com.bc.ceres.binding.ValueRange;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.core.Assert;
import com.bc.ceres.swing.binding.BindingContext;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.runtime.Config;
import org.esa.snap.util.StringUtils;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.Lookup;
import org.openide.util.NbPreferences;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.beans.PropertyChangeListener;
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
    private PropertyContainer originalState;

    /**
     * Create a {@link PropertyContainer} object instance that holds all parameters.
     * Clients that want to maintain properties need to overwrite this method. If
     * the parameters are stored within a bean, use {@link #createPropertyContainer(Object)}
     * in order to create a <code>PropertyContainer</code> instance.
     *
     * @return An instance of {@link PropertyContainer}, holding all configuration parameters.
     *
     * @see #createPropertyContainer(Object)
     */
    protected PropertyContainer createPropertyContainer() {
        return new PropertyContainer();
    }

    /**
     * Create a panel that allows the user to set the parameters in the given {@link BindingContext}. Clients that want
     * to create their own panel representation on the given properties need to overwrite this method.
     *
     * @param context The {@link BindingContext} for the panel.
     *
     * @return A JPanel instance for the given {@link BindingContext}, never <code>null</code>.
     */
    protected JPanel createPanel(BindingContext context) {
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
     * {@link #createPropertyContainer()}.
     */
    protected final PropertyContainer createPropertyContainer(Object bean) {
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
        setupPanel(createPropertyContainer());
        initiallyFillPreferences();
        setupChangeListeners();
        panel.getComponent(); // trigger component initialisation
        configure(bindingContext);
    }

    Preferences getPreferences(PropertyDescriptor propertyDescriptor) {
        Object configNameValue = propertyDescriptor.getAttribute("configName");
        String configName = configNameValue != null ? configNameValue.toString().trim() : null;
        if (configName == null || configName.isEmpty()) {
            return SnapApp.getDefault().getPreferences();
        }
        return Config.instance(configName).load().preferences();
    }

    private void setOriginalState() {
        if (originalState == null) {
            originalState = createPropertyContainer();
            for (Property property : bindingContext.getPropertySet().getProperties()) {
                String key = property.getName();
                try {
                    originalState.getProperty(key).setValue(property.getValue());
                } catch (ValidationException e) {
                    e.printStackTrace(); // very basic exception handling because exception is not expected to be thrown
                }
            }
        }
    }

    private void restoreOriginalState() {
        try {
            for (Property originalProperty : originalState.getProperties()) {
                Property property = bindingContext.getPropertySet().getProperty(originalProperty.getName());
                property.setValue(originalProperty.getValue());
            }
            bindingContext.adjustComponents();
        } catch (ValidationException e) {
            e.printStackTrace();  // very basic exception handling because exception is not expected to be thrown
        }
    }

    private void setupChangeListeners() {
        for (Property property : bindingContext.getPropertySet().getProperties()) {
            property.addPropertyChangeListener(evt -> {
                String key = property.getDescriptor().getAttribute("key").toString();
                String value = evt.getNewValue().toString();
                getPreferences(property.getDescriptor()).put(key, value);
            });
        }
    }

    private void initiallyFillPreferences() {
        for (Property property : bindingContext.getPropertySet().getProperties()) {
            PropertyDescriptor descriptor = property.getDescriptor();
            getPreferences(descriptor).put(descriptor.getAttribute("key").toString(), property.getValueAsText());
        }
    }

    private void setupPanel(PropertyContainer propertyContainer) {
        bindingContext = new BindingContext(propertyContainer);
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
