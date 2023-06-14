/* ========================================================================
 * JCommon : a free general purpose class library for the Java(tm) platform
 * ========================================================================
 *
 * (C) Copyright 2000-2005, by Object Refinery Limited and Contributors.
 * 
 * Project Info:  http://www.jfree.org/jcommon/index.html
 *
 * This library is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published by 
 * the Free Software Foundation; either version 2.1 of the License, or 
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public 
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, 
 * USA.  
 *
 * [Java is a trademark or registered trademark of Sun Microsystems, Inc. 
 * in the United States and other countries.]
 * 
 * -----------------
 * WizardDialog.java
 * -----------------
 * (C) Copyright 2000-2004, by Object Refinery Limited.
 *
 * Original Author:  David Gilbert (for Object Refinery Limited);
 * Contributor(s):   -;
 *
 * $Id: WizardDialog.java,v 1.6 2007/11/02 17:50:36 taqua Exp $
 *
 * Changes (from 26-Oct-2001)
 * --------------------------
 * 26-Oct-2001 : Changed package to com.jrefinery.ui.*;
 * 14-Oct-2002 : Fixed errors reported by Checkstyle (DG);
 *
 */

package org.esa.snap.graphbuilder.rcp.wizards;

import org.esa.snap.ui.help.HelpDisplayer;
import org.jfree.ui.L1R3ButtonPanel;
import org.openide.util.HelpCtx;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * A dialog that presents the user with a sequence of steps for completing a task.  The dialog
 * contains "Next" and "Previous" buttons, allowing the user to navigate through the task.
 * <p/>
 * When the user backs up by one or more steps, the dialog keeps the completed steps so that
 * they can be reused if the user doesn't change anything - this handles the cases where the user
 * backs up a few steps just to review what has been completed.
 * <p/>
 * But if the user changes some options in an earlier step, then the dialog may have to discard
 * the later steps and have them repeated.
 * <p/>
 *
 * @author David Gilbert
 */
public class WizardDialog extends JDialog implements ActionListener {

    /**
     * The end result of the wizard sequence.
     */
    private Object result;

    /**
     * The current step in the wizard process (starting at step zero).
     */
    private int step;

    /**
     * A reference to the current panel.
     */
    private WizardPanel currentPanel;

    private String wizardName = "";

    /**
     * A list of references to the panels the user has already seen - used for navigating through
     * the steps that have already been completed.
     */
    private List<WizardPanel> panels;

    /**
     * A handy reference to the "previous" button.
     */
    private JButton previousButton;

    /**
     * A handy reference to the "next" button.
     */
    private JButton nextButton;

    /**
     * A handy reference to the "finish" button.
     */
    private JButton finishButton;

    /**
     * A handy reference to the "help" button.
     */
    private JButton helpButton;

    // Java help support
    private String helpId;

    /**
     * Standard constructor - builds and returns a new WizardDialog.
     *
     * @param owner      the owner.
     * @param modal      modal?
     * @param title      the title.
     * @param helpID     the help id
     * @param firstPanel the first panel.
     */
    public WizardDialog(final JDialog owner, final boolean modal,
                        final String title, final String helpID, final WizardPanel firstPanel) {

        super(owner, title, modal);
        init(title, helpID, firstPanel);
        setLocation(owner.getSize());
    }

    /**
     * Standard constructor - builds a new WizardDialog owned by the specified JFrame.
     *
     * @param owner      the owner.
     * @param modal      modal?
     * @param title      the title.
     * @param helpID     the help id
     * @param firstPanel the first panel.
     */
    public WizardDialog(final Frame owner, final boolean modal,
                        final String title, final String helpID, final WizardPanel firstPanel) {

        super(owner, title, modal);
        init(title, helpID, firstPanel);
        setLocation(owner.getSize());
    }

    private void init(final String title, final String helpID, final WizardPanel firstPanel) {
        this.wizardName = title;
        this.result = null;
        this.currentPanel = firstPanel;
        this.currentPanel.setOwner(this);
        this.step = 0;
        this.panels = new ArrayList<>(4);
        this.panels.add(firstPanel);
        setContentPane(createContent());
        setTitle(createTitle());
        setHelpID(helpID);

        super.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private void setLocation(final Dimension ownerDim) {
        final int size = 500;
        final int half = size / 2;
        this.setLocation((int) (ownerDim.getWidth() / 2) - half, (int) (ownerDim.getHeight() / 2) - half);
        this.setMinimumSize(new Dimension(size, size));
    }

    public void setIcon(final ImageIcon ico) {
        if (ico == null) return;
        this.setIconImage(ico.getImage());
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

    private void updateHelpID() {
        if (helpId == null) {
            return;
        }
        if (getContentPane() instanceof JComponent) {
            HelpCtx.setHelpIDString((JComponent)getContentPane(), helpId);
        }
        if (helpButton != null) {
            HelpCtx.setHelpIDString(helpButton, helpId);
        }
    }

    /**
     * Returns the result of the wizard sequence.
     *
     * @return the result.
     */
    public Object getResult() {
        return this.result;
    }

    /**
     * Returns the total number of steps in the wizard sequence, if this number is known.  Otherwise
     * this method returns zero.  Subclasses should override this method unless the number of steps
     * is not known.
     *
     * @return the number of steps.
     */
    public int getStepCount() {
        return 0;
    }

    /**
     * Returns true if it is possible to back up to the previous panel, and false otherwise.
     *
     * @return boolean.
     */
    public boolean canDoPreviousPanel() {
        return (this.step > 0);
    }

    /**
     * Returns true if there is a 'next' panel, and false otherwise.
     *
     * @return boolean.
     */
    public boolean canDoNextPanel() {
        return this.currentPanel.hasNextPanel();
    }

    /**
     * Returns true if it is possible to finish the sequence at this point (possibly with defaults
     * for the remaining entries).
     *
     * @return boolean.
     */
    public boolean canFinish() {
        return this.currentPanel.canFinish();
    }

    /**
     * Returns the panel for the specified step (steps are numbered from zero).
     *
     * @param step the current step.
     * @return the panel.
     */
    public WizardPanel getWizardPanel(final int step) {
        if (step < this.panels.size()) {
            return this.panels.get(step);
        } else {
            return null;
        }
    }

    /**
     * Handles events.
     *
     * @param event the event.
     */
    public void actionPerformed(final ActionEvent event) {
        final String command = event.getActionCommand();
        if (command.equals("nextButton")) {
            next();
        } else if (command.equals("previousButton")) {
            previous();
        } else if (command.equals("finishButton")) {
            finish();
        }
    }

    private String createTitle() {
        String stepStr = "";
        if (step != 0)
            stepStr = "Step " + this.step + ' ';
        return wizardName + " : " + stepStr + currentPanel.getPanelTitle();
    }

    /**
     * Handles a click on the "previous" button, by displaying the previous panel in the sequence.
     */
    public void previous() {
        if (this.step > 0) {
            final WizardPanel previousPanel = getWizardPanel(this.step - 1);
            // tell the panel that we are returning
            previousPanel.returnFromLaterStep();
            final Container content = getContentPane();
            content.remove(this.currentPanel);
            content.add(previousPanel);
            this.step = this.step - 1;
            this.currentPanel = previousPanel;
            setTitle(createTitle());
            enableButtons();
            pack();
            repaint();
        }
    }

    /**
     * Displays the next step in the wizard sequence.
     */
    public void next() {

        if (!this.currentPanel.validateInput()) {
            return;
        }
        WizardPanel nextPanel = getWizardPanel(this.step + 1);
        if (nextPanel != null) {
            if (!this.currentPanel.canRedisplayNextPanel()) {
                nextPanel = this.currentPanel.getNextPanel();
            }
        } else {
            nextPanel = this.currentPanel.getNextPanel();
        }

        this.step = this.step + 1;
        if (this.step < this.panels.size()) {
            this.panels.set(this.step, nextPanel);
        } else {
            this.panels.add(nextPanel);
        }

        final Container content = getContentPane();
        content.remove(this.currentPanel);
        content.add(nextPanel);

        this.currentPanel = nextPanel;
        this.currentPanel.setOwner(this);
        setTitle(createTitle());
        enableButtons();
        pack();
        repaint();
    }

    /**
     * Finishes the wizard.
     */
    public void finish() {
        this.currentPanel.finish();
    }

    /**
     * Enables/disables the buttons according to the current step.  A good idea would be to ask the
     * panels to return the status...
     */
    private void enableButtons() {
        this.previousButton.setEnabled(this.step > 0);
        this.nextButton.setEnabled(canDoNextPanel());
        this.finishButton.setEnabled(canFinish());
        this.helpButton.setEnabled(helpId != null);
    }

    public void updateState() {
        enableButtons();
        repaint();
    }

    /**
     * Checks, whether the user cancelled the dialog.
     *
     * @return false.
     */
    public boolean isCancelled() {
        return false;
    }

    /**
     * Creates a panel containing the user interface for the dialog.
     *
     * @return the panel.
     */
    public JPanel createContent() {

        final JPanel content = new JPanel(new BorderLayout());
        content.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        content.add(this.panels.get(0));
        final L1R3ButtonPanel buttons = new L1R3ButtonPanel("Help", "Previous", "Next", "Finish");

        this.helpButton = buttons.getLeftButton();
        this.helpButton.addActionListener(e -> {
            HelpDisplayer.show(helpId);
        });
        this.helpButton.setEnabled(false);

        this.previousButton = buttons.getRightButton1();
        this.previousButton.setActionCommand("previousButton");
        this.previousButton.addActionListener(this);
        this.previousButton.setEnabled(false);

        this.nextButton = buttons.getRightButton2();
        this.nextButton.setActionCommand("nextButton");
        this.nextButton.addActionListener(this);
        this.nextButton.setEnabled(true);

        this.finishButton = buttons.getRightButton3();
        this.finishButton.setActionCommand("finishButton");
        this.finishButton.addActionListener(this);
        this.finishButton.setEnabled(false);

        buttons.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        content.add(buttons, BorderLayout.SOUTH);

        return content;
    }

}
