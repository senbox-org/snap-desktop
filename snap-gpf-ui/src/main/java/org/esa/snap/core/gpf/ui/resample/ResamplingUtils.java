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

    public static void setUpUpsamplingColumn(JTable table,
                                              TableColumn upsamplingColumn) {
        //Set up the editor for the upsampling
        JComboBox comboBox = new JComboBox();
        comboBox.addItem("Nearest");
        comboBox.addItem("Bilinear");
        comboBox.addItem("Cubic");
        upsamplingColumn.setCellEditor(new DefaultCellEditor(comboBox));


        DefaultTableCellRenderer renderer =
                new DefaultTableCellRenderer();
        renderer.setToolTipText("Click for combo box");
        upsamplingColumn.setCellRenderer(renderer);
    }

    public static void setUpDownsamplingColumn(JTable table,
                                                TableColumn upsamplingColumn) {
        //Set up the editor for the downsampling
        JComboBox comboBox = new JComboBox();
        for ( String alias : (Set<String>) GPF.getDefaultInstance().getDownsamplerSpiRegistry().getAliases()) {
            comboBox.addItem(alias);
        }
        upsamplingColumn.setCellEditor(new DefaultCellEditor(comboBox));

        DefaultTableCellRenderer renderer =
                new DefaultTableCellRenderer();
        renderer.setToolTipText("Click for combo box");
        upsamplingColumn.setCellRenderer(renderer);
    }
}
