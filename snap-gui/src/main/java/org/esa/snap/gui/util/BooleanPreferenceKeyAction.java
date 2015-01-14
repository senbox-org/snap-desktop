/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.util;

import org.esa.snap.gui.SnapApp;
import org.openide.util.WeakListeners;
import org.openide.util.actions.Presenter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

/**
 * @author Norman
 */
public class BooleanPreferenceKeyAction extends AbstractAction
        implements PreferenceChangeListener, Presenter.Toolbar, Presenter.Menu, Presenter.Popup {

    private final String preferenceKey;

    protected BooleanPreferenceKeyAction(String preferenceKey) {
        this.preferenceKey = preferenceKey;
        Preferences preferences = SnapApp.getDefault().getPreferences();
        preferences.addPreferenceChangeListener(WeakListeners.create(PreferenceChangeListener.class, this, preferences));
        setSelected(getPreferenceValue());
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
        if (preferenceKey.equals(evt.getKey())) {
            setSelected(getPreferenceValue());
        }
    }

    private boolean getPreferenceValue() {
        return SnapApp.getDefault().getPreferences().getBoolean(preferenceKey, false);
    }

    private void setPreferenceValue(boolean selected) {
        SnapApp.getDefault().getPreferences().putBoolean(preferenceKey, selected);
    }
}
