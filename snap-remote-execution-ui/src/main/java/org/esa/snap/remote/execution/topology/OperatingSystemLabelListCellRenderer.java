package org.esa.snap.remote.execution.topology;

import org.esa.snap.remote.execution.machines.RemoteMachineProperties;
import org.esa.snap.ui.loading.LabelListCellRenderer;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

/**
 * Created by jcoravu on 17/1/2019.
 */
public class OperatingSystemLabelListCellRenderer extends LabelListCellRenderer<RemoteMachineProperties> {

    private final Icon windowsIcon;
    private final Icon linuxIcon;

    public OperatingSystemLabelListCellRenderer(Insets margins) {
        super(margins);

        this.windowsIcon = loadImageIcon("org/esa/snap/remote/execution/windows16.png");
        this.linuxIcon = loadImageIcon("org/esa/snap/remote/execution/linux16.png");
    }

    @Override
    public JLabel getListCellRendererComponent(JList<? extends RemoteMachineProperties> list, RemoteMachineProperties value, int index, boolean isSelected, boolean cellHasFocus) {
        JLabel component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        if (value.isWindows()) {
            component.setIcon(this.windowsIcon);
        } else if (value.isLinux()) {
            component.setIcon(this.linuxIcon);
        } else {
            throw new IllegalArgumentException("Uknown operating system '" + value.getOperatingSystemName() + "'.");
        }
        return component;
    }

    @Override
    protected String getItemDisplayText(RemoteMachineProperties value) {
        return value.getHostName() + ":" + value.getPortNumber();
    }

    private static ImageIcon loadImageIcon(String imagePath) {
        URL imageURL = OperatingSystemLabelListCellRenderer.class.getClassLoader().getResource(imagePath);
        return (imageURL == null) ? null : new ImageIcon(imageURL);
    }
}
