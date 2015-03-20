package org.esa.snap.rcp.statistics;

import com.bc.ceres.swing.TableLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

/**
 * @author Tonio Fincke
 */
public class TitledSeparator extends JPanel {

    private final JLabel labelComponent;

    public TitledSeparator(String title) {
        final TableLayout tableLayout = new TableLayout(3);
        tableLayout.setTableAnchor(TableLayout.Anchor.CENTER);
        tableLayout.setTableWeightY(0.0);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTablePadding(2, 0);
        tableLayout.setColumnWeightX(0, 1.0);
        tableLayout.setColumnWeightX(1, 0.0);
        tableLayout.setColumnWeightX(2, 1.0);
        setLayout(tableLayout);
        add(new JSeparator());
        labelComponent = new JLabel(title);
        add(labelComponent);
        add(new JSeparator());
    }

    JLabel getLabelComponent() {
        return labelComponent;
    }

}
