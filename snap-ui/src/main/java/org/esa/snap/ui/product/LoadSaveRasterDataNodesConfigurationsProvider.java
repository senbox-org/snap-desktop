package org.esa.snap.ui.product;

import com.bc.ceres.core.runtime.internal.IOHelper;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.ui.AbstractDialog;
import org.esa.snap.ui.tool.ToolButtonFactory;
import org.openide.util.ImageUtilities;

import javax.swing.AbstractButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
        if (loadButton == null) {
            loadButton = createButton("tango/22x22/actions/document-open.png");
            loadButton.setToolTipText("Load configuration");
            loadButton.addActionListener(new LoadConfigurationActionListener());
        }
        return loadButton;
    }

    public AbstractButton getSaveButton() {
        if (saveButton == null) {
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
    private static Path getBandSetsDataDir() {
        final Path file = SystemUtils.getAuxDataPath().resolve("band_sets");
        if (!Files.exists(file)) {
            IOHelper.createDirectories(file);
        }
        return file;
    }

    private class LoadConfigurationActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            final Path currentDirectory = getBandSetsDataDir();
            JFileChooser fileChooser = new JFileChooser(currentDirectory.toFile());
            if (fileChooser.showOpenDialog(component.getParent()) == JFileChooser.APPROVE_OPTION) {
                final Path file = fileChooser.getSelectedFile().toPath();
                try {
                    BufferedReader reader = Files.newBufferedReader(file);
                    List<String> bandNameList = new ArrayList<>();
                    String readBandName;
                    while ((readBandName = reader.readLine()) != null) {
                        bandNameList.add(readBandName);
                    }
                    reader.close();
                    String[] bandNames = bandNameList.toArray(new String[bandNameList.size()]);
                    component.setReadRasterDataNodeNames(bandNames);
                } catch (IOException e1) {
                    AbstractDialog.showInformationDialog(component.getParent(), "Could not load configuration", "Information");
                }
            }
        }
    }

    private class SaveConfigurationActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            final Path currentDirectory = getBandSetsDataDir();
            JFileChooser fileChooser = new JFileChooser(currentDirectory.toFile());
            Path suggestedFile = Path.of(currentDirectory + IOHelper.PATH_SEPARATOR + "config.txt");
            int fileCounter = 1;
            while (Files.exists(suggestedFile)) {
                suggestedFile = Path.of("config" + fileCounter + ".txt");
            }
            fileChooser.setSelectedFile(suggestedFile.toFile());
            if (fileChooser.showSaveDialog(component.getParent()) == JFileChooser.APPROVE_OPTION) {
                final Path file = fileChooser.getSelectedFile().toPath();
                try {
                    BufferedWriter writer = new BufferedWriter(Files.newBufferedWriter(file));
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
