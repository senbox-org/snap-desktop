/*
 *
 *  * Copyright (C) 2016 CS ROMANIA
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  *  with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package org.esa.snap.ui.tooladapter.dialogs;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.PropertyEditor;
import com.bc.ceres.swing.binding.PropertyEditorRegistry;
import org.esa.snap.core.gpf.descriptor.SystemVariable;
import org.esa.snap.core.gpf.descriptor.dependency.Bundle;
import org.esa.snap.core.gpf.descriptor.dependency.BundleLocation;
import org.esa.snap.core.gpf.descriptor.dependency.BundleType;
import org.esa.snap.ui.GridBagUtils;

import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.List;

import static org.esa.snap.utils.SpringUtilities.DEFAULT_PADDING;

/**
 * Form for editing the bundle properties for an adapter
 *
 * @author Cosmin Cara
 */
public class BundleForm extends JPanel {
    private Bundle original;
    private Bundle modified;
    private final PropertyContainer propertyContainer;
    private final BindingContext bindingContext;
    private JRadioButton rbLocal;
    private JRadioButton rbRemote;
    private JComponent winUrl;
    private JComponent linUrl;
    private JComponent macUrl;
    private JComponent arguments;
    private JComboBox variable;
    private List<SystemVariable> variables;
    private PropertyChangeListener listener;

    public BundleForm(Bundle bundle, List<SystemVariable> variables) {
        this.original = bundle;
        try {
            this.modified = copy(bundle);
            if (this.modified.getLocation() == null) {
                this.modified.setLocation(BundleLocation.REMOTE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.variables = variables;
        propertyContainer = PropertyContainer.createObjectBacked(this.modified);
        bindingContext = new BindingContext(propertyContainer);
        buildUI();
        addChangeListeners();
        toggleControls();
    }

    /**
     * Updates the source bundle object and returns the modified bundle object.
     */
    public Bundle applyChanges() {
        try {
            this.original = copy(this.modified);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this.original;
    }

    public void setPropertyChangeListener(PropertyChangeListener listener) {
        this.listener = listener;
    }

    private void buildUI() {

        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING);
        c.weighty = 0.01;
        GridBagUtils.addToPanel(this, new JLabel("Bundle Type:"), c, "gridx=0, gridy=0, gridwidth=1, weightx=0");
        c.weighty = 0;
        PropertyDescriptor propertyDescriptor = propertyContainer.getDescriptor("bundleType");
        PropertyEditor editor = PropertyEditorRegistry.getInstance().findPropertyEditor(propertyDescriptor);
        JComboBox bundleCombo = (JComboBox) editor.createEditorComponent(propertyDescriptor, bindingContext);
        GridBagUtils.addToPanel(this, bundleCombo, c, "gridx=1, gridy=0, gridwidth=6, weightx=1");

        GridBagUtils.addToPanel(this, new JLabel("Bundle Location:"), c, "gridx=0, gridy=1, gridwidth=1, weightx=0");
        ButtonGroup rbGroup = new ButtonGroup();
        rbLocal = new JRadioButton(BundleLocation.LOCAL.toString());
        rbGroup.add(rbLocal);
        rbRemote = new JRadioButton(BundleLocation.REMOTE.toString());
        rbGroup.add(rbRemote);
        rbRemote.setSelected(true);
        rbLocal.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                modified.setLocation(BundleLocation.LOCAL);
                toggleControls();
            }
        });
        rbRemote.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                modified.setLocation(BundleLocation.REMOTE);
                toggleControls();
            }
        });
        GridBagUtils.addToPanel(this, rbLocal, c, "gridx=1, gridy=1, gridwidth=3, weightx=1");
        GridBagUtils.addToPanel(this, rbRemote, c, "gridx=4, gridy=1, gridwidth=3, weightx=1");

        GridBagUtils.addToPanel(this, new JLabel("Source File:"), c, "gridx=1, gridy=2, gridwidth=1, weightx=0");
        propertyDescriptor = propertyContainer.getDescriptor("source");
        PropertyEditor propertyEditor = PropertyEditorRegistry.getInstance().findPropertyEditor(propertyDescriptor);
        JComponent localFilePanel = propertyEditor.createEditorComponent(propertyDescriptor, bindingContext);
        GridBagUtils.addToPanel(this, localFilePanel, c, "gridx=2, gridy=2, gridwidth=2, weightx=1");

        GridBagUtils.addToPanel(this, new JLabel("URL for Windows:"), c, "gridx=4, gridy=2, gridwidth=1, weightx=0");
        propertyDescriptor = propertyContainer.getDescriptor("windowsURL");
        propertyEditor = PropertyEditorRegistry.getInstance().findPropertyEditor(propertyDescriptor);
        winUrl = propertyEditor.createEditorComponent(propertyDescriptor, bindingContext);
        winUrl.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                try {
                    String url = ((JTextField) winUrl).getText();
                    URL checkedURL = new URL(url);
                    modified.setWindowsURL(checkedURL.toString());
                    super.focusLost(e);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        GridBagUtils.addToPanel(this, winUrl, c, "gridx=5, gridy=2, gridwidth=2, weightx=1");

        GridBagUtils.addToPanel(this, new JLabel("URL for Linux:"), c, "gridx=4, gridy=3, gridwidth=1, weightx=0");
        propertyDescriptor = propertyContainer.getDescriptor("linuxURL");
        propertyEditor = PropertyEditorRegistry.getInstance().findPropertyEditor(propertyDescriptor);
        linUrl = propertyEditor.createEditorComponent(propertyDescriptor, bindingContext);
        linUrl.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                try {
                    String url = ((JTextField) winUrl).getText();
                    URL checkedURL = new URL(url);
                    modified.setLinuxURL(checkedURL.toString());
                    super.focusLost(e);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        GridBagUtils.addToPanel(this, linUrl, c, "gridx=5, gridy=3, gridwidth=2, weightx=1");

        GridBagUtils.addToPanel(this, new JLabel("URL for MacOSX:"), c, "gridx=4, gridy=4, gridwidth=1, weightx=0");
        propertyDescriptor = propertyContainer.getDescriptor("macosxURL");
        propertyEditor = PropertyEditorRegistry.getInstance().findPropertyEditor(propertyDescriptor);
        macUrl = propertyEditor.createEditorComponent(propertyDescriptor, bindingContext);
        macUrl.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                try {
                    String url = ((JTextField) winUrl).getText();
                    URL checkedURL = new URL(url);
                    modified.setMacosxURL(checkedURL.toString());
                    super.focusLost(e);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        GridBagUtils.addToPanel(this, macUrl, c, "gridx=5, gridy=4, gridwidth=2, weightx=1");

        GridBagUtils.addToPanel(this, new JLabel("Installation Folder:"), c, "gridx=0, gridy=5, gridwidth=1, weightx=0");
        propertyDescriptor = propertyContainer.getDescriptor("targetLocation");
        propertyDescriptor.setAttribute("directory", true);
        propertyEditor = PropertyEditorRegistry.getInstance().findPropertyEditor(propertyDescriptor);
        JComponent targetLocationPanel = propertyEditor.createEditorComponent(propertyDescriptor, bindingContext);
        GridBagUtils.addToPanel(this, targetLocationPanel, c, "gridx=1, gridy=5, gridwidth=6, weightx=1");

        GridBagUtils.addToPanel(this, new JLabel("Installer Arguments:"), c, "gridx=0, gridy=6, gridwidth=1, weightx=0");
        propertyDescriptor = propertyContainer.getDescriptor("arguments");
        propertyEditor = PropertyEditorRegistry.getInstance().findPropertyEditor(propertyDescriptor);
        arguments = propertyEditor.createEditorComponent(propertyDescriptor, bindingContext);
        GridBagUtils.addToPanel(this, arguments, c, "gridx=1, gridy=6, gridwidth=6, weightx=0");

        GridBagUtils.addToPanel(this, new JLabel("Reflect in Variable:"), c, "gridx=0, gridy=7, gridwidth=1, weightx=0");
        propertyDescriptor = propertyContainer.getDescriptor("updateVariable");
        propertyDescriptor.setValueSet(new ValueSet(this.variables.stream().map(SystemVariable::getKey).toArray()));
        propertyEditor = PropertyEditorRegistry.getInstance().findPropertyEditor(propertyDescriptor);
        variable = (JComboBox) propertyEditor.createEditorComponent(propertyDescriptor, bindingContext);
        GridBagUtils.addToPanel(this, variable, c, "gridx=1, gridy=7, gridwidth=6, weightx=0");

        GridBagUtils.addToPanel(this, new JLabel(" "), c, "gridx=0, gridy=8, gridwidth=1, weightx=0, weighty=1");
    }

    public void setVariables(List<SystemVariable> variables) {
        this.variables = variables;
        PropertyDescriptor propertyDescriptor = propertyContainer.getDescriptor("updateVariable");
        propertyDescriptor.setValueSet(new ValueSet(this.variables.stream().map(SystemVariable::getKey).toArray()));
        PropertyEditor propertyEditor = PropertyEditorRegistry.getInstance().findPropertyEditor(propertyDescriptor);
        variable = (JComboBox) propertyEditor.createEditorComponent(propertyDescriptor, bindingContext);
        repaint();
    }

    private void addChangeListeners() {
        final Property bundleTypeProperty = propertyContainer.getProperty("bundleType");
        bundleTypeProperty.addPropertyChangeListener(evt -> {
            modified.setBundleType((BundleType) evt.getNewValue());
            toggleControls();
        });

        final Property variableProperty = propertyContainer.getProperty("updateVariable");
        variableProperty.addPropertyChangeListener(evt -> {
            Object value = evt.getNewValue();
            if (value != null) {
                modified.setUpdateVariable(value.toString());
            }
        });

        Property property = propertyContainer.getProperty("bundleLocation");
        property.addPropertyChangeListener(evt -> {
            modified.setLocation(Enum.valueOf(BundleLocation.class, (String) evt.getNewValue()));
            toggleControls();
        });

        property = propertyContainer.getProperty("source");
        property.addPropertyChangeListener(evt -> {
            modified.setSource((File) evt.getNewValue());
            if (this.listener != null) {
                this.listener.propertyChange(evt);
            }
        });

        property = propertyContainer.getProperty("targetLocation");
        property.addPropertyChangeListener(evt -> modified.setTargetLocation((File) evt.getNewValue()));

        property = propertyContainer.getProperty("arguments");
        property.addPropertyChangeListener(evt -> modified.setArguments((String) evt.getNewValue()));
    }

    private void toggleControls() {
        boolean canSelect = modified.getBundleType() != BundleType.NONE;
        BundleLocation location = modified.getLocation();
        boolean remoteCondition = canSelect && location == BundleLocation.REMOTE;
        boolean localCondition = canSelect && location == BundleLocation.LOCAL;
        rbLocal.setEnabled(canSelect);
        rbRemote.setEnabled(canSelect);
        JComponent[] components = bindingContext.getBinding("source").getComponents();
        for (JComponent component : components) {
            component.setEnabled(localCondition);
        }
        winUrl.setEnabled(remoteCondition);
        linUrl.setEnabled(remoteCondition);
        macUrl.setEnabled(remoteCondition);
        components = bindingContext.getBinding("targetLocation").getComponents();
        for (JComponent component : components) {
            component.setEnabled(canSelect);
        }
        arguments.setEnabled(canSelect);
        variable.setEnabled(canSelect);
        repaint();
    }

    private Bundle copy(Bundle source) throws Exception {
        if (source == null) {
            throw new IllegalArgumentException("source cannot be null");
        }
        Bundle target = new Bundle(source.getBundleType(), source.getTargetLocation());
        for (Field field : Bundle.class.getDeclaredFields()) {
            field.setAccessible(true);
            field.set(target, field.get(source));
        }
        return target;
    }
}
