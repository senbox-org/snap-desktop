package org.esa.snap.grapheditor.ui.components.utils;

import javafx.util.Pair;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.List;

/**
 * Simple setting manager for the GraphBuilder.
 *
 * @author Martino Ferrari (CS Group)
 */
public class SettingManager {
    /**
     * Internal enum to manage different type of values
     */
    private enum SettingType {
        INT,
        // COLOR, future
        BOOLEAN,
        STRING,
        DOUBLE,
        ENUMS,
    }

    /**
     * Internal class to contain multiple type of settings, and create the correct setting component.
     */
    private static class SettingValue  {
        private final SettingType type;
        private Object value;
        private Object data = null;

        private SettingValue(boolean val) {
            type = SettingType.BOOLEAN;
            value = val;
        }

        private SettingValue(String[] options, String val) {
            type = SettingType.ENUMS;
            value = val;
            data = options;
        }

        /**
         * For future use
         * @param val integer value
         */
        private SettingValue(int val) {
            type = SettingType.INT;
            value = val;
        }

        /**
         * For future use
         * @param val double value
         */
        private SettingValue(double val) {
            type = SettingType.DOUBLE;
            value = val;
        }

        /**
         * For future use
         * @param val string value
         */
        private SettingValue(String val) {
            type = SettingType.STRING;
            value = val;
        }

        private boolean asBoolean() {
            if (type == SettingType.BOOLEAN)
                return (boolean) value;
            return false;
        }

        private int asInt() {
            if (type == SettingType.INT)
                return (int) value;
            return 0;
        }

        private double asDouble() {
            if (type == SettingType.DOUBLE)
                return (double) value;
            return 0;
        }

        private String asString() {
            if (type == SettingType.STRING)
                return (String) value;
            return "";
        }

        private void set(int val) {
            if (type == SettingType.INT)
                value = val;
        }

        private void set(double val) {
            if (type == SettingType.DOUBLE)
                value = val;
        }

        private void set(boolean val) {
            if (type == SettingType.BOOLEAN)
                value = val;
        }

        private void set(String val) {
            if (type == SettingType.STRING)
                value = val;
        }

        private void updateValue(Object component) {
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

        private Pair<JComponent, Object> getComponent(String title) {
            Object comp;
            JComponent cont;

            JPanel p = new JPanel();
            p.add(new JLabel(title), BorderLayout.LINE_START);
            if (type == SettingType.BOOLEAN) {
                comp = new JCheckBox("", this.asBoolean());
                p.add((JComponent)comp, BorderLayout.LINE_END);
            } else if (type == SettingType.STRING) {
                comp = new JTextField(this.asString());
                p.add((JComponent)comp, BorderLayout.LINE_END);
            } else if (type == SettingType.ENUMS) {
                JComboBox<String> cb = new JComboBox<>((String[])data);
                cb.setSelectedItem(value);
                comp = cb;
                p.add((JComponent) comp, BorderLayout.LINE_END);
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

    private final HashMap<String, SettingValue> settings = new HashMap<>();
    static private SettingManager instance_ = null;

    static final private String TOOLTIPENABLED = "tooltip enabled";
    static final private String COMMANDPANELENABLED =  "command panel enabled";
    static final private String AUTOVALIDATEKEY = "auto validate enabled";
    static final private String BGGRIDVISIBLEKEY = "background grid visible";
    static final private String LAYOUTMODE = "layout mode";

    /**
     * Initialize SettingManager with default values.
     */
    private SettingManager(){
        //TODO load previous settings.
        settings.put(TOOLTIPENABLED, new SettingValue(true));
        settings.put(COMMANDPANELENABLED, new SettingValue(true));
        settings.put(AUTOVALIDATEKEY, new SettingValue(true));
        settings.put(BGGRIDVISIBLEKEY, new SettingValue(true));
        String[] options = {"classic", "modern"};
        settings.put(LAYOUTMODE, new SettingValue(options, options[0]));
    }

    /**
     * Is tooltip enabled?
     * @return tooltip enabled preference
     */
    public boolean isShowToolipEnabled() {
        return settings.get(TOOLTIPENABLED).asBoolean();
    }

    /**
     * Is command panel enabled?
     * @return command panel enabled preference
     */
    public boolean isCommandPanelEnabled() {
        return settings.get(COMMANDPANELENABLED).asBoolean();
    }

    /**
     * Is background grid visible?
     * @return background grid visibility preference
     */
    public boolean isBgGridVisible() {
        return settings.get(BGGRIDVISIBLEKEY).asBoolean();
    }

    /**
     * Get command panel short-cut (CONSTANT = TAB)
     * @return command panel key
     */
    public int getCommandPanelKey() {
        return KeyEvent.VK_TAB;
    }

    /**
     * Display the setting dialog
     * @param parent owner of the dialog
     */
    public void showSettingsDialog(Window parent) {
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

        cancelButton.addActionListener(e -> dialog.setVisible(false));

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

    }

    /**
     * Get SettingManager instance.
     * @return instance
     */
    static public SettingManager getInstance() {
        if (instance_ == null)
            instance_ = new SettingManager();
        return instance_;
    }
}
