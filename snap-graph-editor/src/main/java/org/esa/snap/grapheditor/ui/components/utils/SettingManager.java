package org.esa.snap.grapheditor.ui.components.utils;

import java.awt.event.KeyEvent;

public class SettingManager {
    private boolean showToolip = true;
    private boolean showTooltipWhileConnecting = true;
    private boolean enableCommandPanel = true;
    private int commandPanelKey = KeyEvent.VK_TAB;

    static private SettingManager instance_ = null;

    private SettingManager(){}

    public boolean isShowToolipEnabled() {
        return showToolip;
    }

    public boolean isShowTooltipWhileConnectingEnabled() {
        return showTooltipWhileConnecting;
    }

    public boolean isCommandPanelEnabled() {
        return enableCommandPanel;
    }

    public int getCommandPanelKey() {
        return commandPanelKey;
    }

    static final public SettingManager getInstance() {
        if (instance_ == null)
            instance_ = new SettingManager();
        return instance_;
    }
}
