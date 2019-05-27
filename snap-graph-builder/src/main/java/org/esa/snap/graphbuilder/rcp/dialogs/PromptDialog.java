/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.graphbuilder.rcp.dialogs;

import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.GridBagUtils;
import org.esa.snap.ui.ModalDialog;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: lveci
 * Date: Jun 5, 2008
 * To change this template use File | Settings | File Templates.
 */
public class PromptDialog extends ModalDialog {

    private boolean ok = false;
    private final Map<String, JComponent> componentMap = new HashMap<>();

    public enum TYPE { TEXTFIELD, TEXTAREA, CHECKBOX, PASSWORD }

    public PromptDialog(final String title, final String label, final String defaultValue, final TYPE type) {
        this(title, new Descriptor[] {new Descriptor(label, defaultValue, type)});
    }

    public PromptDialog(final String title, final Descriptor[] descriptorList) {
        super(SnapApp.getDefault().getMainFrame(), title, ModalDialog.ID_OK_CANCEL, null);

        final JPanel content = GridBagUtils.createPanel();
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();
        gbc.insets.right = 4;
        gbc.insets.top = 2;

        for(Descriptor descriptor : descriptorList) {
            final JComponent prompt = addComponent(content, gbc, descriptor.label, descriptor.defaultValue, descriptor.type);
            componentMap.put(descriptor.label, prompt);
            gbc.gridy++;
        }

        getJDialog().setMinimumSize(new Dimension(400, 100));

        setContent(content);
    }

    private static JComponent addComponent(final JPanel content, final GridBagConstraints gbc,
                                           final String label, final String defaultValue, final TYPE type) {
        if (type.equals(TYPE.CHECKBOX)) {
            final JCheckBox checkBox = new JCheckBox(label);
            checkBox.setSelected(!defaultValue.isEmpty());
            content.add(checkBox, gbc);
            return checkBox;
        }
        JTextComponent textComp;
        if (type.equals(TYPE.TEXTAREA)) {
            final JTextArea textArea = new JTextArea(defaultValue);
            textArea.setColumns(50);
            textArea.setRows(7);
            textComp = textArea;
        } else {
            gbc.gridx = 0;
            content.add(new JLabel(label), gbc);
            gbc.weightx = 2;
            gbc.gridx = 1;
            if (type.equals(TYPE.PASSWORD)) {
                textComp = new  JPasswordField(defaultValue);
                ((JPasswordField)textComp).setEchoChar('*');
            } else {
                textComp = new JTextField(defaultValue);
            }
            gbc.weightx = 1;
        }
        textComp.setEditable(true);
        content.add(textComp, gbc);
        gbc.gridx = 0;
        return textComp;
    }

    public String getValue(final String label) throws Exception {
        final JComponent component = componentMap.get(label);
        if(component instanceof JTextComponent) {
            final JTextComponent textComponent = (JTextComponent) component;
            return textComponent.getText();
        }
        throw new Exception(label + " is not a JTextComponent");
    }

    public boolean isSelected(final String label) throws Exception {
        final JComponent component = componentMap.get(label);
        if(component instanceof JCheckBox) {
            final JCheckBox checkBox = (JCheckBox) component;
            return checkBox.isSelected();
        }
        throw new Exception(label + " is not a JCheckBox");
    }

    protected void onOK() {
        ok = true;
        hide();
    }

    public boolean IsOK() {
        return ok;
    }

    public static class Descriptor {
        public final String label;
        final String defaultValue;
        public final TYPE type;

        public Descriptor(final String label, final String defaultValue, final TYPE type) {
            this.label = label;
            this.defaultValue = defaultValue;
            this.type = type;
        }
    }
}
