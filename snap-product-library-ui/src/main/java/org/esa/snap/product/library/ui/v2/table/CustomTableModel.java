//package org.esa.snap.product.library.ui.v2.table;
//
//import ro.cs.tao.eodata.EOProduct;
//
//import javax.swing.table.AbstractTableModel;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.Comparator;
//import java.util.List;
//
///**
// * Created by jcoravu on 8/8/2019.
// */
//public class CustomTableModel<RecordType> extends AbstractTableModel {
//
//    private final List<AbstractTableColumn<RecordType>> columnNames;
//    private List<RecordType> records;
//
//    public CustomTableModel(List<AbstractTableColumn<RecordType>> columnNames) {
//        this.columnNames = columnNames;
//        this.records = new ArrayList<RecordType>();
//    }
//
//    @Override
//    public int getRowCount() {
//        return this.records.size();
//    }
//
//    @Override
//    public Class<?> getColumnClass(int columnIndex) {
//        AbstractTableColumn<RecordType> tableColumn = this.columnNames.get(columnIndex);
//        return tableColumn.getColumnClass();
//    }
//
//    @Override
//    public int getColumnCount() {
//        return this.columnNames.size();
//    }
//
//    @Override
//    public String getColumnName(int columnIndex) {
//        return this.columnNames.get(columnIndex).getDisplayName();
//    }
//
//    @Override
//    public Object getValueAt(int rowIndex, int columnIndex) {
//        AbstractTableColumn<RecordType> tableColumn = this.columnNames.get(columnIndex);
//        RecordType record = this.records.get(rowIndex);
//        return tableColumn.getCellValue(record, rowIndex, columnIndex);
//    }
//
//    public void clearRecordsAndFireEvent() {
//        this.records = new ArrayList<RecordType>();
//        fireTableDataChanged();
//    }
//
//    public void setRecordsAndFireEvent(List<RecordType> records) {
//        this.records = new ArrayList<RecordType>(records);
//        fireTableDataChanged();
//    }
//
//    public void addRecordsAndFireEvent(List<RecordType> records) {
//        this.records.addAll(records);
//        fireTableDataChanged();
//    }
//
//    public void sortRecordsAndFireEvent(Comparator<RecordType> comparator) {
//        if (this.records.size() > 1) {
//            Collections.sort(this.records, comparator);
//            fireTableDataChanged();
//        }
//    }
//
//    public RecordType getRecordAt(int rowIndex) {
//        return this.records.get(rowIndex);
//    }
//}
