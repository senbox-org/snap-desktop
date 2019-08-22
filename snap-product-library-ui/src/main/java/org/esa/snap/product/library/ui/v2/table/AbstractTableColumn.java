//package org.esa.snap.product.library.ui.v2.table;
//
///**
// * Created by jcoravu on 8/8/2019.
// */
//public abstract class AbstractTableColumn<RecordType> {
//
//    private final String displayName;
//    private final Class<?> columnClass;
//
//    public AbstractTableColumn(String displayName, Class<?> columnClass) {
//        this.displayName = displayName;
//        this.columnClass = columnClass;
//    }
//
//    public abstract Object getCellValue(RecordType record, int rowIndex, int columnIndex);
//
//    public final String getDisplayName() {
//        return displayName;
//    }
//
//    public Class<?> getColumnClass() {
//        return this.columnClass;
//    }
//}
