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
import org.esa.snap.core.gpf.descriptor.OSFamily;
import org.esa.snap.core.gpf.descriptor.SystemVariable;
import org.esa.snap.core.gpf.descriptor.TemplateParameterDescriptor;
import org.esa.snap.core.gpf.descriptor.dependency.Bundle;
import org.esa.snap.core.gpf.descriptor.dependency.BundleLocation;
import org.esa.snap.core.gpf.descriptor.dependency.BundleType;
import org.esa.snap.ui.AbstractDialog;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.GridBagUtils;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.esa.snap.utils.SpringUtilities.DEFAULT_PADDING;

/**
 * Form for editing the bundle properties for an adapter
 *
 * @author Cosmin Cara
 */
public class BundleForm extends JPanel {
    private Map<OSFamily, Bundle> original;
    private Map<OSFamily, Bundle> modified;
    private final Map<OSFamily, PropertyContainer> propertyContainers;
    private final Map<OSFamily, BindingContext> bindingContexts;
    private final Map<OSFamily, List<JComponent>> controls;
    private List<SystemVariable> variables;
    private PropertyChangeListener listener;
    private AppContext appContext;
    private JTabbedPane bundleTabPane;
    private JComponent variablesCombo;

    public BundleForm(AppContext appContext, Bundle windowsBundle, Bundle linuxBundle, Bundle macosxBundle, List<SystemVariable> variables) {
        this.original = new HashMap<OSFamily, Bundle>() {{
            put(OSFamily.windows, windowsBundle);
            put(OSFamily.linux, linuxBundle);
            put(OSFamily.macosx, macosxBundle); }};
        this.appContext = appContext;
        try {
            this.modified = copy(this.original);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.variables = variables;
        propertyContainers = new HashMap<>();
        bindingContexts = new HashMap<>();

        this.bundleTabPane = new JTabbedPane(JTabbedPane.TOP);
        this.bundleTabPane.setBorder(BorderFactory.createEmptyBorder());

        this.controls = new HashMap<>();

        Bundle bundle = modified.get(OSFamily.windows);
        PropertyContainer propertyContainer = PropertyContainer.createObjectBacked(bundle);
        propertyContainers.put(OSFamily.windows, propertyContainer);
        bindingContexts.put(OSFamily.windows, new BindingContext(propertyContainer));
        controls.put(OSFamily.windows, new ArrayList<>());

        bundle = modified.get(OSFamily.linux);
        propertyContainer = PropertyContainer.createObjectBacked(bundle);
        propertyContainers.put(OSFamily.linux, propertyContainer);
        bindingContexts.put(OSFamily.linux, new BindingContext(propertyContainer));
        controls.put(OSFamily.linux, new ArrayList<>());

        bundle = modified.get(OSFamily.macosx);
        propertyContainer = PropertyContainer.createObjectBacked(bundle);
        propertyContainers.put(OSFamily.macosx, propertyContainer);
        bindingContexts.put(OSFamily.macosx, new BindingContext(propertyContainer));
        controls.put(OSFamily.macosx, new ArrayList<>());

        buildUI();
        addChangeListeners();
        toggleControls(OSFamily.windows);
        toggleControls(OSFamily.linux);
        toggleControls(OSFamily.macosx);
    }

    /**
     * Updates the source bundle object and returns the modified bundle object.
     */
    public Map<OSFamily, Bundle> applyChanges() {
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
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING);
        gbc.weighty = 0.01;
        addTab("Windows Bundle", createTab(OSFamily.windows));
        addTab("Linux Bundle", createTab(OSFamily.linux));
        addTab("MacOSX Bundle", createTab(OSFamily.macosx));
        GridBagUtils.addToPanel(this, this.bundleTabPane, gbc, "gridx=0, gridy=0, gridwidth=11, weightx=1");
        GridBagUtils.addToPanel(this, new JLabel("Reflect in Variable:"), gbc, "gridx=0, gridy=1, gridwidth=1, weightx=0");
        variablesCombo = getEditorComponent(OSFamily.all, "updateVariable", this.variables.stream().map(SystemVariable::getKey).toArray());
        GridBagUtils.addToPanel(this, variablesCombo, gbc, "gridx=1, gridy=1, gridwidth=10, weightx=0");
        GridBagUtils.addToPanel(this, new JLabel(" "), gbc, "gridx=0, gridy=2, gridwidth=11, weighty=1");
        int selected = 0;
        switch (Bundle.getCurrentOS()) {
            case windows:
                selected = 0;
                break;
            case linux:
                selected = 1;
                break;
            case macosx:
                selected = 2;
                break;
        }
        this.bundleTabPane.setSelectedIndex(selected);
        this.bundleTabPane.setUI(new BasicTabbedPaneUI());
    }

    public void setVariables(List<SystemVariable> variables) {
        this.variables = variables;
        PropertyContainer propertyContainer = propertyContainers.get(OSFamily.windows);
        BindingContext bindingContext = bindingContexts.get(OSFamily.windows);
        PropertyDescriptor propertyDescriptor = propertyContainer.getDescriptor("updateVariable");
        propertyDescriptor.setValueSet(new ValueSet(this.variables.stream().map(SystemVariable::getKey).toArray()));
        PropertyEditor propertyEditor = PropertyEditorRegistry.getInstance().findPropertyEditor(propertyDescriptor);
        variablesCombo = propertyEditor.createEditorComponent(propertyDescriptor, bindingContext);
        repaint();
    }

    private JPanel createTab(OSFamily osFamily) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING);
        c.weighty = 0.01;
        List<JComponent> controlGroup = controls.get(osFamily);
        final Bundle bundle = modified.get(osFamily);
        GridBagUtils.addToPanel(panel, new JLabel("Type:"), c, "gridx=0, gridy=0, gridwidth=1, weightx=0");
        JComponent type = getEditorComponent(osFamily, "bundleType");
        GridBagUtils.addToPanel(panel, type, c, "gridx=1, gridy=0, gridwidth=10, weightx=1");

        GridBagUtils.addToPanel(panel, new JLabel("Location:"), c, "gridx=0, gridy=1, gridwidth=1, weightx=0");
        ButtonGroup rbGroup = new ButtonGroup();
        JRadioButton rbLocal = new JRadioButton(BundleLocation.LOCAL.toString());
        rbGroup.add(rbLocal);
        JRadioButton rbRemote = new JRadioButton(BundleLocation.REMOTE.toString());
        rbGroup.add(rbRemote);
        if (BundleLocation.LOCAL.equals(bundle.getLocation())) {
            rbLocal.setSelected(true);
        } else if (BundleLocation.REMOTE.equals(bundle.getLocation())) {
            rbRemote.setSelected(true);
        }
        rbLocal.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                //this is first because, for some reason, otherwise is not triggering the editing event
                propertyContainers.get(osFamily).setValue("downloadURL", "");
                bundle.setLocation(BundleLocation.LOCAL);
                propertyContainers.get(osFamily).setValue("bundleLocation", BundleLocation.LOCAL);
                toggleControls(osFamily);
                firePropertyChange(new PropertyChangeEvent(bundle, "bundleLocation", null, BundleLocation.LOCAL));
            }
        });
        rbRemote.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                //this is first because, for some reason, otherwise is not triggering the editing event
                propertyContainers.get(osFamily).setValue("source", null);
                bundle.setLocation(BundleLocation.REMOTE);
                propertyContainers.get(osFamily).setValue("bundleLocation", BundleLocation.REMOTE);
                toggleControls(osFamily);
                firePropertyChange(new PropertyChangeEvent(bundle, "bundleLocation", null, BundleLocation.REMOTE));
            }
        });
        GridBagUtils.addToPanel(panel, rbLocal, c, "gridx=1, gridy=1, gridwidth=4, weightx=1");
        GridBagUtils.addToPanel(panel, rbRemote, c, "gridx=5, gridy=1, gridwidth=5, weightx=1");
        controlGroup.add(rbLocal);
        controlGroup.add(rbRemote);

        GridBagUtils.addToPanel(panel, new JLabel("Source File:"), c, "gridx=0, gridy=2, gridwidth=1, weightx=0");
        JComponent component = getEditorComponent(osFamily, "source");
        GridBagUtils.addToPanel(panel, component, c, "gridx=1, gridy=2, gridwidth=10, weightx=1");
        controlGroup.add(component);

        GridBagUtils.addToPanel(panel, new JLabel("URL:"), c, "gridx=0, gridy=3, gridwidth=1, weightx=0");
        final JComponent downloadURL = getEditorComponent(osFamily, "downloadURL", "url");

        ((JTextField) downloadURL).getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                try {
                    String url = ((JTextField) downloadURL).getText();
                    URL checkedURL = new URL(url);
                    bundle.setDownloadURL(checkedURL.toString());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        component.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                try {
                    String url = ((JTextField) downloadURL).getText();
                    URL checkedURL = new URL(url);
                    bundle.setDownloadURL(checkedURL.toString());
                    super.focusLost(e);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        GridBagUtils.addToPanel(panel, downloadURL, c, "gridx=1, gridy=3, gridwidth=10, weightx=1");
        controlGroup.add(downloadURL);

        GridBagUtils.addToPanel(panel, new JLabel("Arguments:"), c, "gridx=0, gridy=4, gridwidth=1, weightx=0");
        final JTextField argumentsParam = new JTextField(bundle.getCommandLine());
        GridBagUtils.addToPanel(panel, argumentsParam, c, "gridx=1, gridy=4, gridwidth=9, weightx=0");
        controlGroup.add(argumentsParam);
        argumentsParam.setEditable(false);
        JButton argumentDetailsButton = new JButton("...");
        argumentDetailsButton.setMaximumSize(new Dimension(32, argumentDetailsButton.getHeight()));
        argumentDetailsButton.addActionListener(e -> {
            TemplateParameterEditorDialog parameterEditorDialog =
                    new TemplateParameterEditorDialog(appContext,
                                                      bundle.getArgumentsParameter(),
                                                      bundle.getParent());
            int returnCode = parameterEditorDialog.show();
            if (returnCode == AbstractDialog.ID_OK) {
                argumentsParam.setText(bundle.getCommandLine());
            }
        });
        GridBagUtils.addToPanel(panel, argumentDetailsButton, c, "gridx=10, gridy=4, gridwidth=1, weightx=0");
        controlGroup.add(argumentDetailsButton);

        GridBagUtils.addToPanel(panel, new JLabel("Target Folder:"), c, "gridx=0, gridy=5, gridwidth=1, weightx=0");
        component = getEditorComponent(osFamily, "targetLocation", true);
        GridBagUtils.addToPanel(panel, component, c, "gridx=1, gridy=5, gridwidth=10, weightx=1");
        controlGroup.add(component);
        toggleControls(osFamily);
        return panel;
    }

    private void addTab(String text, JPanel contents) {
        JLabel tabText = new JLabel(text, JLabel.LEFT);
        Border titledBorder = BorderFactory.createEmptyBorder();
        contents.setBorder(titledBorder);
        this.bundleTabPane.addTab(null, contents);
        this.bundleTabPane.setTabComponentAt(this.bundleTabPane.getTabCount() - 1, tabText);
    }

    private void addChangeListeners() {
        propertyContainers.entrySet().forEach(entry -> {
            OSFamily osFamily = entry.getKey();
            Bundle bundle = modified.get(osFamily);
            PropertyContainer propertyContainer = entry.getValue();
            Property property = propertyContainer.getProperty("bundleType");
            property.addPropertyChangeListener(evt -> {
                bundle.setBundleType((BundleType) evt.getNewValue());
                toggleControls(osFamily);
            });
            property = propertyContainer.getProperty("updateVariable");
            property.addPropertyChangeListener(evt -> {
                Object value = evt.getNewValue();
                if (value != null) {
                    modified.values().forEach(b -> b.setUpdateVariable(value.toString()));
                }
            });
            property = propertyContainer.getProperty("bundleLocation");
            property.addPropertyChangeListener(evt -> {
                bundle.setLocation(Enum.valueOf(BundleLocation.class, (String) evt.getNewValue()));
                toggleControls(osFamily);
            });
            property = propertyContainer.getProperty("source");
            property.addPropertyChangeListener(evt -> {
                bundle.setSource((File) evt.getNewValue());
                firePropertyChange(new PropertyChangeEvent(bundle, "source",
                                                           evt.getOldValue(), evt.getNewValue()));
            });
            property = propertyContainer.getProperty("downloadURL");
            property.addPropertyChangeListener(evt -> {
                try {
                    String url = String.valueOf(evt.getNewValue());
                    if(url != null && url.length() > 0) {
                        URL checkedURL = new URL(url);
                        bundle.setDownloadURL(checkedURL.toString());
                    }else {
                        bundle.setDownloadURL(null);
                    }
                    firePropertyChange(new PropertyChangeEvent(bundle, "source",
                            evt.getOldValue(), evt.getNewValue()));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            property = propertyContainer.getProperty("targetLocation");
            property.addPropertyChangeListener(evt -> {
                bundle.setTargetLocation(String.valueOf(evt.getNewValue()));
                firePropertyChange(new PropertyChangeEvent(bundle, "source",
                        evt.getOldValue(), evt.getNewValue()));
            });

            property = propertyContainer.getProperty("templateparameter");
            property.addPropertyChangeListener(evt -> bundle.setArgumentsParameter((TemplateParameterDescriptor) evt.getNewValue()));
        });
    }

    private void firePropertyChange(PropertyChangeEvent event) {
        if (this.listener != null) {
            this.listener.propertyChange(event);
        }
    }

    private void toggleControls(OSFamily osFamily) {
        Bundle bundle = modified.get(osFamily);
        boolean canSelect = bundle.getBundleType() != BundleType.NONE;
        BundleLocation location = bundle.getLocation();
        boolean remoteCondition = canSelect && location == BundleLocation.REMOTE;
        boolean localCondition = canSelect && location == BundleLocation.LOCAL;
        for (JComponent component : controls.get(osFamily)) {
            if ("url".equals(component.getName())) {
                component.setEnabled(remoteCondition);
            } else {
                component.setEnabled(canSelect);
            }
        }

        BindingContext bindingContext = bindingContexts.get(osFamily);
        JComponent[] components = bindingContext.getBinding("source").getComponents();
        for (JComponent component : components) {
            component.setEnabled(localCondition);
        }
        for (Component jcomponent : components[0].getParent().getComponents()) {
            jcomponent.setEnabled(localCondition);
        }
        components = bindingContext.getBinding("targetLocation").getComponents();
        for (JComponent component : components) {
            component.setEnabled(canSelect);
        }
        repaint();
    }

    private JComponent getEditorComponent(OSFamily osFamily, String propertyName) {
        return getEditorComponent(osFamily, propertyName, null, false);
    }

    private JComponent getEditorComponent(OSFamily osFamily, String propertyName, boolean isFolder) {
        return getEditorComponent(osFamily, propertyName, null, isFolder);
    }

    private JComponent getEditorComponent(OSFamily osFamily, String propertyName, String controlName) {
        return getEditorComponent(osFamily, propertyName, controlName, false);
    }

    private JComponent getEditorComponent(OSFamily osFamily, String propertyName, Object[] items) {
        if (osFamily == OSFamily.all) {
            osFamily = OSFamily.windows;
        }
        PropertyContainer propertyContainer = propertyContainers.get(osFamily);
        BindingContext bindingContext = bindingContexts.get(osFamily);
        PropertyDescriptor propertyDescriptor = propertyContainer.getDescriptor(propertyName);
        if (items != null) {
            propertyDescriptor.setValueSet(new ValueSet(items));
        }
        PropertyEditor propertyEditor = PropertyEditorRegistry.getInstance().findPropertyEditor(propertyDescriptor);
        return propertyEditor.createEditorComponent(propertyDescriptor, bindingContext);
    }

    private JComponent getEditorComponent(OSFamily osFamily, String propertyName,String controlName, boolean isFolder) {
        if (osFamily == OSFamily.all) {
            osFamily = OSFamily.windows;
        }
        PropertyContainer propertyContainer = propertyContainers.get(osFamily);
        BindingContext bindingContext = bindingContexts.get(osFamily);
        PropertyDescriptor propertyDescriptor = propertyContainer.getDescriptor(propertyName);
        if (isFolder) {
            propertyDescriptor.setAttribute("directory", true);
        }
        PropertyEditor propertyEditor = PropertyEditorRegistry.getInstance().findPropertyEditor(propertyDescriptor);
        JComponent editorComponent = propertyEditor.createEditorComponent(propertyDescriptor, bindingContext);
        if (controlName != null) {
            editorComponent.setName(controlName);
        }
        return editorComponent;
    }

    private Map<OSFamily, Bundle> copy(Map<OSFamily, Bundle> sources) throws Exception {
        if (sources == null) {
            throw new IllegalArgumentException("source cannot be null");
        }
        Map<OSFamily, Bundle> bundles = new HashMap<>();
        for (Map.Entry<OSFamily, Bundle> entry : sources.entrySet()) {
            Bundle source = entry.getValue();
            Bundle target = new Bundle(source);
            bundles.put(entry.getKey(), target);
        }
        return bundles;
    }
}
