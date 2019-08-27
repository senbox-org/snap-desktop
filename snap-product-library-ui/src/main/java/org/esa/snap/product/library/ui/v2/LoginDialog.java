package org.esa.snap.product.library.ui.v2;

import org.esa.snap.ui.loading.AbstractModalDialog;
import org.esa.snap.ui.loading.SwingUtils;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by jcoravu on 27/8/2019.
 */
public class LoginDialog extends AbstractModalDialog {

    private JTextField usernameTextField;
    private JPasswordField passwordTextField;
    private boolean credentialsEntered;

    public LoginDialog(Window parent, String title) {
        super(parent, title, true, null);

        this.credentialsEntered = false;
    }

    @Override
    protected JPanel buildContentPanel(int gapBetweenColumns, int gapBetweenRows) {
        Insets defaultTextFieldMargins = buildDefaultTextFieldMargins();
        Insets defaultListItemMargins = buildDefaultListItemMargins();
        createComponents(defaultTextFieldMargins, defaultListItemMargins);

        JPanel contentPanel = new JPanel(new GridBagLayout());

        GridBagConstraints c = SwingUtils.buildConstraints(0, 0, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, 0, 0);
        contentPanel.add(new JLabel("Username"), c);
        c = SwingUtils.buildConstraints(1, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, 0, gapBetweenColumns);
        contentPanel.add(this.usernameTextField, c);

        c = SwingUtils.buildConstraints(0, 1, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, gapBetweenRows, 0);
        contentPanel.add(new JLabel("Password"), c);
        c = SwingUtils.buildConstraints(1, 1, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, gapBetweenRows, gapBetweenColumns);
        contentPanel.add(this.passwordTextField, c);

        c = SwingUtils.buildConstraints(1, 2, GridBagConstraints.BOTH, GridBagConstraints.WEST, 1, 1, 0, gapBetweenColumns);
        contentPanel.add(new JLabel(), c);

        return contentPanel;
    }

    @Override
    protected JPanel buildButtonsPanel(ActionListener cancelActionListener) {
        ActionListener okActionListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                okButtonPressed();
            }
        };
        return buildButtonsPanel("Ok", okActionListener, "Cancel", cancelActionListener);
    }

    @Override
    protected void onAboutToShow() {
        Dimension size = getJDialog().getPreferredSize();
        getJDialog().setMinimumSize(size);

        //TODO Jean remote
        this.usernameTextField.setText("jcoravu");
        this.passwordTextField.setText("jcoravu@yahoo.com");
    }

    public boolean areCredentialsEntered() {
        return credentialsEntered;
    }

    public String getUsername() {
        return this.usernameTextField.getText();
    }

    public String getPassword() {
        return new String(this.passwordTextField.getPassword());
    }

    private void okButtonPressed() {
        if (getUsername().trim().equals("")) {
            this.usernameTextField.requestFocusInWindow();
        } else if (getPassword().trim().equals("")) {
            this.passwordTextField.requestFocusInWindow();
        } else {
            this.credentialsEntered = true;
            getJDialog().dispose();
        }
    }

    private void createComponents(Insets defaultTextFieldMargins, Insets defaultListItemMargins) {
        this.usernameTextField = new JTextField(30);
        this.usernameTextField.setMargin(defaultTextFieldMargins);

        this.passwordTextField = new JPasswordField(30);
        this.passwordTextField.setMargin(defaultTextFieldMargins);
    }
}
