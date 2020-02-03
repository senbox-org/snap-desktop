package org.esa.snap.grapheditor.ui.components.utils;

import javafx.util.Pair;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.HashMap;

public class SettingManager {
    private enum SettingType {
        INT,
        BOOLEAN,
        STRING,
        DOUBLE
    }

    private class SettingValue  {
        private SettingType type;
        private Object value;

        public SettingValue(boolean val) {
            type = SettingType.BOOLEAN;
            value = val;
        }

        public SettingValue(int val) {
            type = SettingType.INT;
            value = val;
        }

        public SettingValue(double val) {
            type = SettingType.DOUBLE;
            value = val;
        }

        public SettingValue(String val) {
            type = SettingType.STRING;
            value = val;
        }

        public SettingType getType() {
            return type;
        }

        public boolean asBoolean() {
            if (type == SettingType.BOOLEAN)
                return (boolean) value;
            return false;
        }

        public int asInt() {
            if (type == SettingType.INT)
                return (int) value;
            return 0;
        }

        public double asDouble() {
            if (type == SettingType.DOUBLE)
                return (double) value;
            return 0;
        }

        public String asString() {
            if (type == SettingType.STRING)
                return (String) value;
            return "";
        }

        public void set(int val) {
            if (type == SettingType.INT)
                value = val;
        }

        public void set(double val) {
            if (type == SettingType.DOUBLE)
                value = val;
        }

        public void set(boolean val) {
            if (type == SettingType.BOOLEAN)
                value = val;
        }

        public void set(String val) {
            if (type == SettingType.STRING)
                value = val;
        }

        public void updateValue(Object component) {
            switch (type) {
                case BOOLEAN:
                    set(((JCheckBox)component).isSelected());
                    break;
                case STRING:
                    set(((JTextField)component).getText());
                    break;
                case INT:
                    set((int)((SpinnerNumberModel)component).getValue());
                    break;
                case DOUBLE:
                    set((double)((SpinnerNumberModel)component).getValue());
                    break;
            }
        }

        public Pair<JComponent, Object> getComponent(String title) {
            Object comp = null;
            JComponent cont = null;

            JPanel p = new JPanel();
            p.add(new JLabel(title), BorderLayout.LINE_START);
            if (type == SettingType.BOOLEAN) {
                comp = new JCheckBox("", this.asBoolean());
                p.add((JComponent)comp, BorderLayout.LINE_END);
            } else if (type == SettingType.STRING) {
                comp = new JTextField(this.asString());
                p.add((JComponent)comp, BorderLayout.LINE_END);
            } else {
                SpinnerModel model;

                if (type == SettingType.INT)
                    model = new SpinnerNumberModel(this.asInt(),
                                                   Integer.MIN_VALUE,
                                                   Integer.MAX_VALUE,
                                                   1);
                else
                    model = new SpinnerNumberModel(this.asDouble(),
                                                   Double.MIN_VALUE,
                                                   Double.MAX_VALUE,
                                                   0.01);

                comp = model;
                p.add(new JSpinner(model), BorderLayout.LINE_END);
            }
        cont = p;

            return new Pair<>(cont, comp);
        }

    }

    private HashMap<String, SettingValue> settings = new HashMap<>();
    static private SettingManager instance_ = null;

    static final private String TOOLTIPENABLED = "tooltipEnabled";
    static final private String TOOLTIPWHILECONNECTING = "tooltipWhileConnectingEnabled";
    static final private String COMMANDPANELENABLED =  "commandPanelEnabled";
    static final private String COMMANDPANELKEY = "commandPanelKey";

    private SettingManager(){
        settings.put(TOOLTIPENABLED, new SettingValue(true));
        settings.put(TOOLTIPWHILECONNECTING, new SettingValue(true));
        settings.put(COMMANDPANELENABLED, new SettingValue(true));
        settings.put(COMMANDPANELKEY, new SettingValue(KeyEvent.VK_TAB));
    }

    public boolean isShowToolipEnabled() {
        return settings.get(TOOLTIPENABLED).asBoolean();
    }

    public boolean isShowTooltipWhileConnectingEnabled() {
        return settings.get(TOOLTIPWHILECONNECTING).asBoolean();
    }

    public boolean isCommandPanelEnabled() {
        return settings.get(COMMANDPANELENABLED).asBoolean();
    }

    public int getCommandPanelKey() {
        return  settings.get(COMMANDPANELKEY).asInt();
    }

    public JDialog showSettingsDialog(Window parent) {
        JDialog dialog = new JDialog(parent, "Graph Editor Settings");
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        HashMap<String, Object> components = new HashMap<>();
        for (String title: settings.keySet()) {
            Pair<JComponent, Object> comps = settings.get(title).getComponent(title);
            panel.add(comps.getKey());
            components.put(title, comps.getValue());
        }

        JPanel buttonPanel = new JPanel();
        JButton cancelButton = new JButton("Cancel");
        JButton okButton = new JButton("Ok");
        buttonPanel.add(cancelButton, BorderLayout.LINE_START);
        buttonPanel.add(okButton, BorderLayout.LINE_END);
        panel.add(buttonPanel);

        cancelButton.addActionListener(e -> {
            dialog.setVisible(false);
        });

        okButton.addActionListener(e -> {
            for (String title: components.keySet()){
                Object c = components.get(title);
                settings.get(title).updateValue(c);
            }
            dialog.setVisible(false);
        });


        dialog.setContentPane(panel);
        dialog.setModal(true);

        dialog.validate();
        dialog.pack();
        dialog.setPreferredSize(new Dimension(600, 30 * (components.size() + 1)));

        dialog.setVisible(true);

        return dialog;
    }

    static final public SettingManager getInstance() {
        if (instance_ == null)
            instance_ = new SettingManager();
        return instance_;
    }
}
