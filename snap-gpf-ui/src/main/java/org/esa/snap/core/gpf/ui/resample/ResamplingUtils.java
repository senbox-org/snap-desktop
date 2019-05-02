package org.esa.snap.core.gpf.ui.resample;

import org.esa.snap.core.gpf.GPF;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.util.Set;

/**
 * Created by obarrile on 22/04/2019.
 */
public class ResamplingUtils {

    public static String RESAMPLING_PRESET_FOLDER = "resampling_presets";

    public static void setUpUpsamplingColumn(JTable table,
                                              TableColumn upsamplingColumn, String defaultUpsampling) {
        //Set up the editor for the upsampling
        JComboBox comboBox = new JComboBox();
        //TODO use registry when agreement with other teams. By the moment, they are available only the options in the old resampling
        comboBox.addItem("Nearest");
        comboBox.addItem("Bilinear");
        comboBox.addItem("Bicubic");
        if(defaultUpsampling != null) {
            comboBox.setSelectedItem(defaultUpsampling);
        }
        upsamplingColumn.setCellEditor(new DefaultCellEditor(comboBox));


        DefaultTableCellRenderer renderer =
                new DefaultTableCellRenderer();
        renderer.setToolTipText("Click for combo box");
        upsamplingColumn.setCellRenderer(renderer);
    }

    public static void setUpDownsamplingColumn(JTable table,
                                                TableColumn downsamplingColumn, String defaultDownsampling) {
        //Set up the editor for the downsampling
        JComboBox comboBox = new JComboBox();
        for ( String alias : (Set<String>) GPF.getDefaultInstance().getDownsamplerSpiRegistry().getAliases()) {
            comboBox.addItem(alias);
        }

        if(defaultDownsampling != null) {
            comboBox.setSelectedItem(defaultDownsampling);
        }
        downsamplingColumn.setCellEditor(new DefaultCellEditor(comboBox));

        DefaultTableCellRenderer renderer =
                new DefaultTableCellRenderer();
        renderer.setToolTipText("Click for combo box");
        downsamplingColumn.setCellRenderer(renderer);
    }
}
