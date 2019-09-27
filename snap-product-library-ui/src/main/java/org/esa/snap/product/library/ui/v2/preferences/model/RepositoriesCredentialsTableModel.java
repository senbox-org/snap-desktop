package org.esa.snap.product.library.ui.v2.preferences.model;

import org.apache.http.auth.Credentials;
import org.esa.snap.product.library.ui.v2.preferences.RepositoriesCredentialsControllerUI;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class RepositoriesCredentialsTableModel extends AbstractTableModel {

    /**
     * The column index for remote repository credential username in remote file repository properties table.
     */
    public static final int REPO_CRED_USER_COLUMN = 0;
    /**
     * The column index for remote repository credential password in remote file repository properties table.
     */
    private static final int REPO_CRED_PASS_COLUMN = 1;
    /**
     * The column index for remote repository credential password in remote file repository properties table.
     */
    public static final int REPO_CRED_PASS_SEE_COLUMN = 2;

    private final List<CredentialsTableRow> credentialsTableData;

    private final String[] columnsNames;
    private final Class[] columnsClass;

    public RepositoriesCredentialsTableModel(List<Credentials> credentialsList) {
        credentialsTableData = new ArrayList<>();
        columnsNames = new String[]{
                "Username", "Password", ""
        };
        columnsClass = new Class[]{
                JTextField.class, JPasswordField.class, JButton.class
        };
        setData(credentialsList);
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
                return credentialsTableData.get(rowIndex).getTextField();
            case REPO_CRED_PASS_COLUMN:
                return credentialsTableData.get(rowIndex).getPasswordField();
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
        for (CredentialsTableRow credentialsTableRow : credentialsTableData) {
            Credentials savedCredential = credentialsTableRow.getCredentials();
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
            CredentialsTableRow credentialsTableRow = new CredentialsTableRow(credential);
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
                CredentialsTableRow credentialsTableRow = new CredentialsTableRow(credential);
                this.credentialsTableData.add(credentialsTableRow);
            }
        }
        fireTableDataChanged();
    }

    private class CredentialsTableRow {
        private UserCredential repositoryCredential;
        private JTextField textField;
        private JPasswordField passwordField;
        private JButton button;

        CredentialsTableRow(Credentials credential) {
            this.repositoryCredential = (UserCredential) credential;
            this.textField = new JTextField();
            PlainDocument usernameDocument = new PlainDocument() {
                @Override
                public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
                    super.insertString(offs, str, a);

                    repositoryCredential.setUsername(textField.getText());
                }

                @Override
                public void remove(int offs, int len) throws BadLocationException {
                    super.remove(offs, len);

                    repositoryCredential.setUsername(textField.getText());
                }
            };
            this.textField.setDocument(usernameDocument);
            this.textField.setText(credential.getUserPrincipal().getName());
            this.passwordField = new JPasswordField();
            PlainDocument passwordDocument = new PlainDocument() {
                @Override
                public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
                    super.insertString(offs, str, a);

                    repositoryCredential.setPassword(new String(passwordField.getPassword()));
                }

                @Override
                public void remove(int offs, int len) throws BadLocationException {
                    super.remove(offs, len);

                    repositoryCredential.setPassword(new String(passwordField.getPassword()));
                }
            };
            this.passwordField.setDocument(passwordDocument);
            this.passwordField.setText(credential.getPassword());
            this.button = new JButton(RepositoriesCredentialsControllerUI.getPasswordSeeIcon());
            this.button.addMouseListener(new MouseAdapter() {

                @Override
                public void mousePressed(MouseEvent e) {
                    passwordField.setEchoChar('\u0000');
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    passwordField.setEchoChar('\u25cf');
                }
            });
        }

        JTextField getTextField() {
            return textField;
        }

        JPasswordField getPasswordField() {
            return passwordField;
        }

        JButton getButton() {
            return button;
        }

        Credentials getCredentials() {
            return repositoryCredential;
        }
    }

}
