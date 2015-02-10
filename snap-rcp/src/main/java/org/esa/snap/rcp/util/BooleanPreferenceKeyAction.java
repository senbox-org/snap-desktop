/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.util;

import org.esa.snap.rcp.SnapApp;
import org.openide.util.WeakListeners;
import org.openide.util.actions.Presenter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

/**
 * An action which sets a boolean preference value.
 *
 * @author Norman
 */
public class BooleanPreferenceKeyAction extends AbstractAction
        implements PreferenceChangeListener, Presenter.Toolbar, Presenter.Menu, Presenter.Popup {

    private final String preferenceKey;
    private final boolean defaultValue;

    protected BooleanPreferenceKeyAction(String preferenceKey) {
        this(preferenceKey, false);
    }

    protected BooleanPreferenceKeyAction(String preferenceKey, boolean defaultValue) {
        this.preferenceKey = preferenceKey;
        this.defaultValue = defaultValue;
        Preferences preferences = SnapApp.getDefault().getPreferences();
        preferences.addPreferenceChangeListener(WeakListeners.create(PreferenceChangeListener.class, this, preferences));
        setSelected(getPreferenceValue());
    }

    public String getPreferenceKey() {
        return preferenceKey;
    }

    public boolean isSelected() {
        return Boolean.TRUE.equals(getValue(SELECTED_KEY));
    }

    public void setSelected(boolean selected) {
        putValue(SELECTED_KEY, selected);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        setPreferenceValue(isSelected());
    }

    @Override
    public JMenuItem getMenuPresenter() {
        JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(this);
        menuItem.setIcon(null);
        return menuItem;
    }

    @Override
    public JMenuItem getPopupPresenter() {
        return getMenuPresenter();
    }

    @Override
    public Component getToolbarPresenter() {
        JToggleButton toggleButton = new JToggleButton(this);
        toggleButton.setText(null);
        return toggleButton;
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent evt) {
        if (getPreferenceKey().equals(evt.getKey())) {
            setSelected(getPreferenceValue());
        }
    }

    private boolean getPreferenceValue() {
        return SnapApp.getDefault().getPreferences().getBoolean(getPreferenceKey(), defaultValue);
    }

    private void setPreferenceValue(boolean selected) {
        SnapApp.getDefault().getPreferences().putBoolean(getPreferenceKey(), selected);
    }
}
