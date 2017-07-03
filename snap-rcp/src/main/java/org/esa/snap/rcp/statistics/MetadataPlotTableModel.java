package org.esa.snap.rcp.statistics;

import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Marco Peters
 */
class MetadataPlotTableModel extends AbstractTableModel {

    private XYPlot plot;
    private List<String> columList;

    MetadataPlotTableModel(XYPlot plot) {
        this.plot = plot;
        columList = new ArrayList<>();
        columList.add(plot.getDomainAxis().getLabel());
        for(int i = 0; i < plot.getDatasetCount(); i++) {
            columList.add(String.valueOf(plot.getDataset(i).getSeriesKey(0)));
        }
    }

    @Override
    public int getRowCount() {
        return plot.getDataset().getItemCount(0);
    }

    @Override
    public int getColumnCount() {
        return columList.size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex == 0) {
            return plot.getDataset(columnIndex).getXValue(0, rowIndex);
        } else {
            XYDataset dataset = plot.getDataset(columnIndex - 1);
            int itemCount = dataset.getItemCount(0);
            if (rowIndex < itemCount) {
                return dataset.getYValue(0, rowIndex);
            }else {
                return null;
            }
        }
    }

    @Override
    public String getColumnName(int column) {
        return columList.get(column);
    }
}
