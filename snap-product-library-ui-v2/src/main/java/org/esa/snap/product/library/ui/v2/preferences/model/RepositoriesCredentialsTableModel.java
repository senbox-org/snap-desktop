package org.esa.snap.product.library.ui.v2.preferences.model;

import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.esa.snap.product.library.ui.v2.preferences.RepositoriesCredentialsControllerUI;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * The model of the table containing the user accounts for a remote repository.
 */
public class RepositoriesCredentialsTableModel extends AbstractTableModel {

    /**
     * The column index for remote repository credential password in remote file repository properties table.
     */
    public static final int REPO_CRED_PASS_SEE_COLUMN = 2;
    /**
     * The column index for remote repository credential username in remote file repository properties table.
     */
    private static final int REPO_CRED_USER_COLUMN = 0;
    /**
     * The column index for remote repository credential password in remote file repository properties table.
     */
    private static final int REPO_CRED_PASS_COLUMN = 1;
    private final List<CredentialsTableRow> credentialsTableData;
    private final String[] columnsNames;
    private final Class[] columnsClass;

    public RepositoriesCredentialsTableModel() {
        credentialsTableData = new ArrayList<>();
        columnsNames = new String[]{
                "Username", "Password", ""
        };
        columnsClass = new Class[]{
                JTextField.class, JPasswordField.class, JButton.class
        };
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
        return credentialsTableData.size();
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
        return true;
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
        switch (columnIndex) {
            case REPO_CRED_USER_COLUMN:
                return credentialsTableData.get(rowIndex).getUserCellData();
            case REPO_CRED_PASS_COLUMN:
                return credentialsTableData.get(rowIndex).getPasswordCellData();
            case REPO_CRED_PASS_SEE_COLUMN:
                return credentialsTableData.get(rowIndex).getButton();
            default:
                return null;
        }
    }

    public Credentials get(int row) {
        return credentialsTableData.get(row).getCredentials();
    }

    public boolean add(Credentials credential) {
        boolean exists = false;
        for (CredentialsTableRow credentialData : credentialsTableData) {
            UsernamePasswordCredentials savedCredential = (UsernamePasswordCredentials) credentialData.getCredentials();
            String savedUsername = savedCredential.getUserPrincipal().getName();
            String username = credential.getUserPrincipal().getName();
            String savedPassword = savedCredential.getPassword();
            String password = credential.getPassword();
            if (savedUsername.contentEquals(username) && savedPassword.contentEquals(password)) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            CredentialsTableRow credentialsTableRow = new CredentialsTableRow(credential, credentialsTableData.size());
            credentialsTableData.add(credentialsTableRow);
            int row = credentialsTableData.indexOf(credentialsTableRow);
            fireTableRowsInserted(row, row);
            return true;
        }
        return false;
    }

    public void remove(int row) {
        credentialsTableData.remove(row);
        fireTableRowsDeleted(row, row);
    }

    public void setData(List<Credentials> credentialsList) {
        int rowsDeleted = this.credentialsTableData.size();
        if (rowsDeleted > 0) {
            this.credentialsTableData.clear();
            fireTableRowsDeleted(0, rowsDeleted - 1);
        }
        if (credentialsList != null) {
            for (Credentials credential : credentialsList) {
                CredentialsTableRow credentialsTableRow = new CredentialsTableRow(credential, this.credentialsTableData.size());
                this.credentialsTableData.add(credentialsTableRow);
            }
        }
        fireTableDataChanged();
    }

    public void updateCellData(int row, int column, String data) {
        if (credentialsTableData.size() > row) {
            switch (column) {
                case REPO_CRED_USER_COLUMN:
                    credentialsTableData.get(row).setUserCellData(data);
                    break;
                case REPO_CRED_PASS_COLUMN:
                    credentialsTableData.get(row).setPasswordCellData(data);
                    break;
                default:
                    break;
            }
        }
    }

    public List<Credentials> fetchData() {
        List<Credentials> credentialsList = new ArrayList<>();
        for (CredentialsTableRow credentialsTableRow : this.credentialsTableData) {
            credentialsList.add(credentialsTableRow.getCredentials());
        }
        return credentialsList;
    }

    private class CredentialsTableRow {
        private String userCellData;
        private String passwordCellData;
        private JButton button;
        private boolean hidden = true;

        CredentialsTableRow(Credentials credential, int rowIndex) {
            this.userCellData = credential.getUserPrincipal().getName();
            this.passwordCellData = credential.getPassword();
            this.button = new JButton(RepositoriesCredentialsControllerUI.getPasswordSeeIcon());
            this.button.addMouseListener(new MouseAdapter() {

                @Override
                public void mousePressed(MouseEvent e) {
                    hidden = false;
                    RepositoriesCredentialsTableModel.this.fireTableCellUpdated(rowIndex, REPO_CRED_PASS_COLUMN);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    hidden = true;
                    RepositoriesCredentialsTableModel.this.fireTableCellUpdated(rowIndex, REPO_CRED_PASS_COLUMN);
                }
            });
        }

        String getUserCellData() {
            return userCellData;
        }

        void setUserCellData(String userCellData) {
            this.userCellData = userCellData;
        }

        String getPasswordCellData() {
            if (hidden) {
                return passwordCellData.replaceAll(".+?", "\u25cf");
            }
            return passwordCellData;
        }

        void setPasswordCellData(String passwordCellData) {
            this.passwordCellData = passwordCellData;
        }

        JButton getButton() {
            return button;
        }

        Credentials getCredentials() {
            String username = this.userCellData;
            String password = this.passwordCellData;
            return new UsernamePasswordCredentials(username, password);
        }
    }

}
