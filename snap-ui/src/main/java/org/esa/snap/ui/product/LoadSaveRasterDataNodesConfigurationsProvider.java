package org.esa.snap.ui.product;

import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.ui.tool.ToolButtonFactory;
import org.openide.util.ImageUtilities;

import javax.swing.AbstractButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LoadSaveRasterDataNodesConfigurationsProvider {

    private final LoadSaveRasterDataNodesConfigurationsComponent component;
    private AbstractButton loadButton;
    private AbstractButton saveButton;

    public LoadSaveRasterDataNodesConfigurationsProvider(LoadSaveRasterDataNodesConfigurationsComponent component) {
        this.component = component;
    }

    public AbstractButton getLoadButton() {
        if(loadButton == null) {
            loadButton = createButton("tango/22x22/actions/document-open.png");
            loadButton.setToolTipText("Load configuration");
            loadButton.addActionListener(new LoadConfigurationActionListener());
        }
        return loadButton;
    }

    public AbstractButton getSaveButton() {
        if(saveButton == null) {
            saveButton = createButton("tango/22x22/actions/document-save-as.png");
            saveButton.setToolTipText("Save configuration");
            saveButton.addActionListener(new SaveConfigurationActionListener());
        }
        return saveButton;
    }

    private static AbstractButton createButton(String s) {
        return ToolButtonFactory.createButton(ImageUtilities.loadImageIcon(s, false), false);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static File getBandSetsDataDir() {
        File file = new File(SystemUtils.getAuxDataPath().toFile(), "band_sets");
        if (!file.exists()) {
            file.mkdirs();
        }
        return file;
    }

    private class LoadConfigurationActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            File currentDirectory = getBandSetsDataDir();
            JFileChooser fileChooser = new JFileChooser(currentDirectory);
            if (fileChooser.showOpenDialog(component.getParent()) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                try {
                    BufferedReader reader = new BufferedReader(new FileReader(file));
                    List<String> bandNameList = new ArrayList<>();
                    String readBandName;
                    while ((readBandName = reader.readLine()) != null) {
                        bandNameList.add(readBandName);
                    }
                    reader.close();
                    String[] bandNames = bandNameList.toArray(new String[bandNameList.size()]);
                    component.setReadRasterDataNodeNames(bandNames);
                } catch (IOException e1) {
                    JOptionPane.showMessageDialog(component.getParent(), "Could not load configuration");
                }
            }
        }
    }

    private class SaveConfigurationActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            File currentDirectory = getBandSetsDataDir();
            JFileChooser fileChooser = new JFileChooser(currentDirectory);
            File suggestedFile = new File(currentDirectory + File.separator + "config.txt");
            int fileCounter = 1;
            while (suggestedFile.exists()) {
                suggestedFile = new File("config" + fileCounter + ".txt");
            }
            fileChooser.setSelectedFile(suggestedFile);
            if (fileChooser.showSaveDialog(component.getParent()) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                try {
                    BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                    String[] bandNames = component.getRasterDataNodeNamesToWrite();
                    for (String bandName : bandNames) {
                        writer.write(bandName + "\n");
                    }
                    writer.close();
                } catch (IOException e1) {
                    JOptionPane.showMessageDialog(component.getParent(), "Could not save configuration");
                }
            }
        }
    }

}
