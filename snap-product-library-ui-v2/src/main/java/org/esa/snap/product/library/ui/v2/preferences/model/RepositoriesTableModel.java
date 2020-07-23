package org.esa.snap.product.library.ui.v2.preferences.model;

import org.esa.snap.remote.products.repository.RemoteProductsRepositoryProvider;

import javax.swing.table.AbstractTableModel;
import java.util.List;

/**
 * The model of the table containing the available remote repositories.
 */
public class RepositoriesTableModel extends AbstractTableModel {

    /**
     * The column index for remote repository name in remote repositories table.
     */
    public static final int REPO_NAME_COLUMN = 0;

    private List<RemoteProductsRepositoryProvider> repositoriesNamesList;

    private String[] columnsNames;
    private Class[] columnsClass;

    public RepositoriesTableModel(List<RemoteProductsRepositoryProvider> repositoriesNamesList) {
        columnsNames = new String[]{
                "Name"
        };
        columnsClass = new Class[]{
                String.class
        };
        this.repositoriesNamesList = repositoriesNamesList;
    }

    /**
     * Returns the number of rows in the model. A
     * <code>JTable</code> uses this method to determine how many rows it
     * should display.  This method should be quick, as it
     * is called frequently during rendering.
     *
     * @return the number of rows in the model
     * @see #getColumnCount
     */
    @Override
    public int getRowCount() {
        return repositoriesNamesList.size();
    }

    /**
     * Returns the number of columns in the model. A
     * <code>JTable</code> uses this method to determine how many columns it
     * should create and display by default.
     *
     * @return the number of columns in the model
     * @see #getRowCount
     */
    @Override
    public int getColumnCount() {
        return columnsNames.length;
    }

    /**
     * Returns the name of the column at <code>columnIndex</code>.  This is used
     * to initialize the table's column header name.  Note: this name does
     * not need to be unique; two columns in a table can have the same name.
     *
     * @param columnIndex the index of the column
     * @return the name of the column
     */
    @Override
    public String getColumnName(int columnIndex) {
        return columnsNames[columnIndex];
    }

    /**
     * Returns the most specific superclass for all the cell values
     * in the column.  This is used by the <code>JTable</code> to set up a
     * default renderer and editor for the column.
     *
     * @param columnIndex the index of the column
     * @return the common ancestor class of the object values in the model.
     */
    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnsClass[columnIndex];
    }

    /**
     * Returns true if the cell at <code>rowIndex</code> and
     * <code>columnIndex</code>
     * is editable.  Otherwise, <code>setValueAt</code> on the cell will not
     * change the value of that cell.
     *
     * @param rowIndex    the row whose value to be queried
     * @param columnIndex the column whose value to be queried
     * @return true if the cell is editable
     * @see #setValueAt
     */
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    /**
     * Returns the value for the cell at <code>columnIndex</code> and
     * <code>rowIndex</code>.
     *
     * @param rowIndex    the row whose value is to be queried
     * @param columnIndex the column whose value is to be queried
     * @return the value Object at the specified cell
     */
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex == REPO_NAME_COLUMN) {
            return repositoriesNamesList.get(rowIndex).getRepositoryName();
        }
        return null;
    }

    public RemoteProductsRepositoryProvider get(int row) {
        return repositoriesNamesList.get(row);
    }

    public void add(RemoteProductsRepositoryProvider repositoryName) {
        repositoriesNamesList.add(repositoryName);
        int row = repositoriesNamesList.size() - 1;
        fireTableRowsInserted(row, row);
    }

    public void remove(int row) {
        repositoriesNamesList.remove(row);
        fireTableRowsDeleted(row, row);
    }
}
