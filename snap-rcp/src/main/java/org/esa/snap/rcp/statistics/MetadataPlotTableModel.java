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
        XYDataset dataset = plot.getDataset();
        for(int i = 0; i < dataset.getSeriesCount(); i++) {
            columList.add(String.valueOf(dataset.getSeriesKey(i)));
        }
//        for( int i = 0; i < plot.getRangeAxisCount(); i++) {
//            columList.add(plot.getRangeAxis(i).getLabel());
//        }
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
            return plot.getDataset().getXValue(columnIndex, rowIndex);
        } else {
            return plot.getDataset().getYValue(columnIndex-1, rowIndex);
        }
    }

    @Override
    public String getColumnName(int column) {
        return columList.get(column);
    }
}
