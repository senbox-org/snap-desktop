/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.ui;

import org.esa.snap.ui.help.HelpDisplayer;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * The {@code AbstractDialog} is the base class for {@link ModalDialog} and {@link ModelessDialog},
 * two helper classes used to quickly construct modal and modeless dialogs. The dialogs created with this
 * class have a unique border and font and a standard button row for the typical buttons like "OK", "Cancel" etc.
 * <p>
 * <p>Instances of a modal dialog are created with a parent component, a title, the actual dialog content component, and
 * a bit-combination of the standard buttons to be used.
 * <p>
 * <p>A limited way of input validation is provided by the  {@code verifyUserInput} method which can be overridden
 * in order to return {@code false} if a user input is invalid. In this case the {@link #onOK()},
 * {@link #onYes()} and {@link #onNo()} methods are NOT called.
 *
 * @author Norman Fomferra
 */
@NbBundle.Messages({
        "CTL_AbstractDlg_NoHelpThemeAvailable=Sorry, no help theme available.",
        "CTL_AbstractDlg_NoHelpIDShowingStandard=Sorry, help for id '%s' not available.\nShowing standard help."
})
public abstract class AbstractDialog {

    public static final int ID_OK = 0x0001;
    public static final int ID_YES = 0x0002;
    public static final int ID_NO = 0x0004;
    public static final int ID_APPLY = 0x0008;
    public static final int ID_CLOSE = 0x0010;
    public static final int ID_CANCEL = 0x0020;
    public static final int ID_RESET = 0x0040;
    public static final int ID_HELP = 0x0080;
    public static final int ID_OTHER = 0xAAAAAAAA;

    private final JDialog dialog;
    private final Window parent;
    private final int buttonMask;

    private int buttonId;
    private Component content;
    private boolean shown;
    private Map<Integer, AbstractButton> buttonMap;
    private JPanel buttonRow;

    // Java help support
    private String helpId;

    protected AbstractDialog(JDialog dialog, int buttonMask, Object[] otherButtons, String helpID) {
        this.parent = (Window) dialog.getParent();
        this.dialog = dialog;
        this.buttonMask = buttonMask;
        this.buttonMap = new HashMap<>(5);
        setComponentName(dialog);
        setButtonID(0);
        initUI(otherButtons);
        setHelpID(helpID);
    }

    /**
     * Gets the underlying Swing dialog passed to the constructor.
     *
     * @return the underlying Swing dialog.
     */
    public JDialog getJDialog() {
        return dialog;
    }

    /**
     * Gets the owner of the dialog.
     *
     * @return The owner of the dialog.
     */
    public Window getParent() {
        return parent;
    }

    /**
     * @return The dialog's title.
     */
    public String getTitle() {
        return dialog.getTitle();
    }

    /**
     * @param title The dialog's title.
     */
    public void setTitle(String title) {
        dialog.setTitle(title);
    }

    /**
     * Gets the button mask passed to the constructor.
     *
     * @return The button mask.
     */
    public int getButtonMask() {
        return buttonMask;
    }

    /**
     * Gets the identifier for the most recently pressed button.
     *
     * @return The identifier for the most recently pressed button.
     */
    public int getButtonID() {
        return buttonId;
    }


    /**
     * Sets the identifier for the most recently pressed button.
     *
     * @param buttonID The identifier for the most recently pressed button.
     */
    protected void setButtonID(final int buttonID) {
        buttonId = buttonID;
    }

    /**
     * Gets the help identifier for the dialog.
     *
     * @return The help identifier.
     */
    public String getHelpID() {
        return helpId;
    }

    /**
     * Sets the help identifier for the dialog.
     *
     * @param helpID The help identifier.
     */
    public void setHelpID(String helpID) {
        helpId = helpID;
        updateHelpID();
    }

    public JPanel getButtonPanel() {
        return buttonRow;
    }

    /**
     * Gets the dialog's content component.
     *
     * @return The dialog's content component.
     */
    public Component getContent() {
        return content;
    }

    /**
     * Sets the dialog's content component.
     *
     * @param content The dialog's content component.
     */
    public void setContent(Component content) {
        if (this.content != null) {
            dialog.getContentPane().remove(this.content);
        }
        this.content = content;
        dialog.getContentPane().add(this.content, BorderLayout.CENTER);
        dialog.validate();
        updateHelpID();
    }

    /**
     * Sets the dialog's content.
     *
     * @param content The dialog's content.
     */
    public void setContent(Object content) {
        Component component;
        if (content instanceof Component) {
            component = (Component) content;
        } else {
            component = new JLabel(content.toString());
        }
        setContent(component);
    }

    /**
     * Gets the button for the given identifier.
     *
     * @param buttonID The button identifier.
     * @return The button, or {@code null}.
     */
    public AbstractButton getButton(int buttonID) {
        return buttonMap.get(buttonID);
    }

    public void setResizable(boolean resizable) {
        dialog.setResizable(resizable);
    }

    /**
     * Shows the dialog. Overrides shall call {@code super.show()} at the end.
     *
     * @return the identifier of the last button pressed or zero if this is a modeless dialog.
     */
    public int show() {
        setButtonID(0);
        if (!shown) {
            dialog.pack();
            center();
        }
        dialog.setVisible(true);
        shown = true;
        return getButtonID();
    }

    /**
     * Hides the dialog. Overrides shall call {@code super.hide()} at the end. This method does nothing else than hiding the underlying Swing dialog.
     *
     * @see #getJDialog()
     */
    public void hide() {
        dialog.setVisible(false);
    }

    /**
     * This method is called, when the user clicks the close button of the dialog's top window bar.
     * It can also be called directly.
     * Override to implement the dialog's default close behaviour.
     */
    public abstract void close();

    /**
     * Centers the dialog within its parent window.
     */
    public void center() {
        UIUtils.centerComponent(dialog, parent);
    }

    /**
     * Shows an error dialog on top of this dialog.
     *
     * @param errorMessage The message.
     */
    public void showErrorDialog(String errorMessage) {
        showErrorDialog(errorMessage, getJDialog().getTitle());
    }

    public void showErrorDialog(String message, String title) {
        showMessageDialog(getJDialog(), message, title, JOptionPane.ERROR_MESSAGE);
    }

    public static void showErrorDialog(Component component, String message, String dialogTitle) {
        showMessageDialog(component, message, dialogTitle, JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Shows an information dialog on top of this dialog.
     *
     * @param infoMessage The message.
     */
    public void showInformationDialog(String infoMessage) {
        showInformationDialog(infoMessage, getJDialog().getTitle());
    }

    public void showInformationDialog(String infoMessage, String title) {
        showMessageDialog(getJDialog(), infoMessage, title, JOptionPane.INFORMATION_MESSAGE);
    }

    public static void showInformationDialog(Component component, String message, String dialogTitle) {
        showMessageDialog(component, message, dialogTitle, JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Shows a warning dialog on top of this dialog.
     *
     * @param warningMessage The message.
     */
    public void showWarningDialog(String warningMessage) {
        showWarningDialog(warningMessage, getJDialog().getTitle());
    }

    public void showWarningDialog(String warningMessage, String title) {
        showMessageDialog(getJDialog(), warningMessage, title, JOptionPane.WARNING_MESSAGE);
    }

    public static void showWarningDialog(Component component, String message, String dialogTitle) {
        showMessageDialog(component, message, dialogTitle, JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Called if the "OK" button has been clicked.
     * The default implementation calls {@link #hide()}.
     * Clients should override this method to implement meaningful behaviour.
     */
    protected void onOK() {
        hide();
    }

    /**
     * Called if the "Yes" button has been clicked.
     * The default implementation calls {@link #hide()}.
     * Clients should override this method to implement meaningful behaviour.
     */
    protected void onYes() {
        hide();
    }

    /**
     * Called if the "No" button has been clicked.
     * The default implementation calls {@link #hide()}.
     * Clients should override this method to implement meaningful behaviour.
     */
    protected void onNo() {
        hide();
    }

    /**
     * Called if the "Cancel" button has been clicked.
     * The default implementation calls {@link #hide()}.
     * Clients should override this method to implement meaningful behaviour.
     */
    protected void onCancel() {
        hide();
    }

    /**
     * Called if the "Apply" button has been clicked.
     * The default implementation does nothing.
     * Clients should override this method to implement meaningful behaviour.
     */
    protected void onApply() {
    }

    /**
     * Called if the "Close" button has been clicked.
     * The default implementation calls {@link #hide()}.
     * Clients should override this method to implement meaningful behaviour.
     */
    protected void onClose() {
        hide();
    }

    /**
     * Called if the reset button has been clicked.
     * The default implementation does nothing.
     * Clients should override this method to implement meaningful behaviour.
     */
    protected void onReset() {
    }

    /**
     * Called if the help button has been clicked.
     * Clients should override this method to implement a different behaviour.
     */
    protected void onHelp() {
        if (helpId == null) {
            showWarningDialog(String.format(Bundle.CTL_AbstractDlg_NoHelpIDShowingStandard(), helpId));
        }
        HelpDisplayer.show(helpId);
    }

    /**
     * Called if a non-standard button has been clicked.
     * The default implementation calls {@link #hide()}.
     * Clients should override this method to implement meaningful behaviour.
     */
    protected void onOther() {
        hide();
    }

    /**
     * Called in order to perform input validation.
     *
     * @return {@code true} if and only if the validation was successful.
     */
    protected boolean verifyUserInput() {
        return true;
    }

    /**
     * Called by the constructor in order to initialise the user interface.
     * The default implementation does nothing.
     *
     * @param buttons The container into which new buttons shall be collected.
     */
    protected void collectButtons(List<AbstractButton> buttons) {
    }

    private void initUI(Object[] otherItems) {

        buttonRow = new JPanel();
        buttonRow.setLayout(new BoxLayout(buttonRow, BoxLayout.X_AXIS));

        int insetSize = UIDefaults.INSET_SIZE;
        JPanel contentPane = new JPanel(new BorderLayout(0, insetSize + insetSize / 2));
        contentPane.setBorder(UIDefaults.DIALOG_BORDER);
        contentPane.add(buttonRow, BorderLayout.SOUTH);

        dialog.setResizable(true);
        dialog.setContentPane(contentPane);

        ArrayList<AbstractButton> buttons = new ArrayList<>();

        collectButtons(buttons);

        if (otherItems != null) {
            for (Object otherItem : otherItems) {
                if (otherItem instanceof String) {
                    String text = (String) otherItem;
                    JButton otherButton = new JButton(text);
                    otherButton.setName(getQualifiedPropertyName(text));
                    otherButton.addActionListener(e -> {
                        setButtonID(ID_OTHER);
                        if (verifyUserInput()) {
                            onOther();
                        }
                    });
                    buttons.add(otherButton);
                } else if (otherItem instanceof AbstractButton) {
                    AbstractButton otherButton = (AbstractButton) otherItem;
                    otherButton.addActionListener(e -> {
                        setButtonID(ID_OTHER);
                        if (verifyUserInput()) {
                            onOther();
                        }
                    });
                    buttons.add(otherButton);
                }
            }
        }

        if ((buttonMask & ID_OK) != 0) {
            JButton button = new JButton("OK");
            button.setMnemonic('O');
            button.setName(getQualifiedPropertyName("ok"));
            button.addActionListener(e -> {
                setButtonID(ID_OK);
                if (verifyUserInput()) {
                    onOK();
                }
            });
            buttons.add(button);
            button.setDefaultCapable(true);
            getJDialog().getRootPane().setDefaultButton(button);
            registerButton(ID_OK, button);
        }
        if ((buttonMask & ID_YES) != 0) {
            JButton button = new JButton("Yes");
            button.setMnemonic('Y');
            button.setName(getQualifiedPropertyName("yes"));
            button.addActionListener(e -> {
                setButtonID(ID_YES);
                if (verifyUserInput()) {
                    onYes();
                }
            });
            buttons.add(button);
            button.setDefaultCapable(true);
            getJDialog().getRootPane().setDefaultButton(button);
            registerButton(ID_YES, button);
        }
        if ((buttonMask & ID_NO) != 0) {
            JButton button = new JButton("No");
            button.setMnemonic('N');
            button.setName(getQualifiedPropertyName("no"));
            button.addActionListener(e -> {
                setButtonID(ID_NO);
                if (verifyUserInput()) {
                    onNo();
                }
            });
            buttons.add(button);
            registerButton(ID_NO, button);
        }
        if ((buttonMask & ID_CANCEL) != 0) {
            JButton button = new JButton("Cancel");
            button.setMnemonic('C');
            button.setName(getQualifiedPropertyName("cancel"));
            button.addActionListener(e -> close());
            buttons.add(button);
            button.setVerifyInputWhenFocusTarget(false);
            registerButton(ID_CANCEL, button);
        }
        if ((buttonMask & ID_APPLY) != 0) {
            JButton button = new JButton("Apply");
            button.setMnemonic('A');
            button.setName(getQualifiedPropertyName("apply"));
            button.addActionListener(e -> {
                setButtonID(ID_APPLY);
                if (verifyUserInput()) {
                    onApply();
                }
            });
            buttons.add(button);
            button.setDefaultCapable(true);
            getJDialog().getRootPane().setDefaultButton(button);
            registerButton(ID_APPLY, button);
        }
        if ((buttonMask & ID_CLOSE) != 0) {
            JButton button = new JButton("Close");
            button.setMnemonic('C');
            button.setName(getQualifiedPropertyName("close"));
            button.addActionListener(e -> {
                setButtonID(ID_CLOSE);
                onClose();
            });
            button.setToolTipText("Close dialog window");
            buttons.add(button);
            button.setVerifyInputWhenFocusTarget(false);
            registerButton(ID_CLOSE, button);
        }
        if ((buttonMask & ID_RESET) != 0) {
            JButton button = new JButton("Reset");
            button.setName(getQualifiedPropertyName("reset"));
            button.setMnemonic('R');
            button.addActionListener(e -> {
                setButtonID(ID_RESET);
                onReset();
            });
            buttons.add(button);
            registerButton(ID_RESET, button);
        }

        if ((buttonMask & ID_HELP) != 0) {
            JButton button = new JButton("Help");
            button.setName(getQualifiedPropertyName("help"));
            button.setMnemonic('H');
            button.addActionListener(e -> {
                setButtonID(ID_HELP);
                onHelp();
            });
            button.setToolTipText("Show help on this topic.");
            buttons.add(button);
            registerButton(ID_HELP, button);
        }

        buttonRow.add(Box.createHorizontalGlue());
        for (int i = 0; i < buttons.size(); i++) {
            if (i != 0) {
                buttonRow.add(Box.createRigidArea(new Dimension(4, 0)));
            }
            buttonRow.add(buttons.get(i));
        }

        dialog.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                close();
            }
        });
    }

    protected String getQualifiedPropertyName(String name) {
        return getClass().getSimpleName() + "." + name;
    }

    protected void registerButton(int buttonID, AbstractButton button) {
        buttonMap.put(buttonID, button);
    }

    private void updateHelpID() {
        if (helpId == null) {
            return;
        }
        if (getJDialog().getContentPane() != null) {
            Container contentPane = getJDialog().getContentPane();
            if (contentPane instanceof JComponent) {
                HelpCtx.setHelpIDString((JComponent) contentPane, helpId);
            }
        }
    }

    private static void showMessageDialog(Component dialog, String message, String dialogTitle, int messageType) {
        JOptionPane.showMessageDialog(dialog, message, dialogTitle, messageType);
    }

    private void setComponentName(JDialog dialog) {
        if (this.dialog.getName() == null && dialog.getTitle() != null) {
            dialog.setName(dialog.getTitle().toLowerCase().replaceAll(" ", "_"));
        }
    }

}
