package org.esa.snap.ui.loading;

import org.esa.snap.ui.AbstractDialog;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

/**
 * Created by jcoravu on 18/12/2018.
 */
public abstract class AbstractModalDialog extends AbstractDialog implements IMessageDialog {

    private LoadingIndicatorPanel loadingIndicatorPanel;
    private java.util.List<JComponent> componentsAllwaysEnabled;

    protected AbstractModalDialog(Window parent, String title, boolean isModal, String helpID) {
        super(new JDialogExtended(parent, Dialog.ModalityType.MODELESS), 0, null, helpID);

        JDialogExtended dialog = (JDialogExtended)getJDialog();
        dialog.setTitle(title);
        dialog.setModal(isModal);
        dialog.setLayoutRunnable(new Runnable() {
            @Override
            public void run() {
                centerLoadingIndicatorPanel();
            }
        });
    }

    protected abstract JPanel buildContentPanel(int gapBetweenColumns, int gapBetweenRows);

    protected abstract JPanel buildButtonsPanel(ActionListener cancelActionListener);

    @Override
    public void close() {
        getJDialog().dispose();
    }

    @Override
    public final int show() {
        JDialog dialog = getJDialog();
        if (!dialog.isShowing()) {
            this.componentsAllwaysEnabled = new ArrayList<JComponent>();

            ActionListener cancelActionListener = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    close();
                }
            };
            registerEscapeKey(cancelActionListener);

            IComponentsEnabled componentsEnabled = new IComponentsEnabled() {
                @Override
                public void setComponentsEnabled(boolean enabled) {
                    setEnabledComponentsWhileLoading(enabled);
                }
            };
            this.loadingIndicatorPanel = new LoadingIndicatorPanel(componentsEnabled);
            dialog.getLayeredPane().add(this.loadingIndicatorPanel, JLayeredPane.MODAL_LAYER);

            int gapBetweenColumns = getDefaultGapBetweenColumns();
            int gapBetweenRows = getDefaultGapBetweenRows();

            JPanel contentPanel = buildContentPanel(gapBetweenColumns, gapBetweenRows);
            JPanel buttonsPanel = buildButtonsPanel(cancelActionListener);

            JPanel dialogContentPanel = new JPanel(new BorderLayout(0, getDefaultGapBetweenContentAndButtonPanels()));
            dialogContentPanel.add(contentPanel, BorderLayout.CENTER);
            dialogContentPanel.add(buttonsPanel, BorderLayout.SOUTH);
            Border dialogBorder = buildDialogBorder(getDefaultContentPanelMargins());
            dialogContentPanel.setBorder(dialogBorder);

            dialog.setContentPane(dialogContentPanel);

            dialog.setResizable(true);
            dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            dialog.setUndecorated(false);
            dialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent aEvent) {
                    close();
                }
            });
            dialog.pack();
            dialog.setLocationRelativeTo(dialog.getParent());

            onAboutToShow();

            dialog.setVisible(true);
        }
        return 0;
    }

    protected int getDefaultGapBetweenContentAndButtonPanels() {
        return 7;
    }

    protected int getDefaultContentPanelMargins() {
        return 7;
    }

    protected int getDefaultGapBetweenRows() {
        return 5;
    }

    protected int getDefaultGapBetweenColumns() {
        return 5;
    }

    protected final Insets buildDefaultTextFieldMargins() {
        return new Insets(3, 2, 3, 2);
    }

    protected final Insets buildDefaultListItemMargins() {
        return new Insets(3, 2, 3, 2);
    }

    protected final JPanel buildButtonsPanel(String finishButtonText, ActionListener finishActionListener, String cancelButtonText, ActionListener cancelActionListener) {
        JButton finishButton = buildDialogButton(finishButtonText);
        finishButton.addActionListener(finishActionListener);
        JButton cancelButton = buildDialogButton(cancelButtonText);
        cancelButton.addActionListener(cancelActionListener);

        this.componentsAllwaysEnabled.add(cancelButton);

        JPanel buttonsGridPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        buttonsGridPanel.add(finishButton);
        buttonsGridPanel.add(cancelButton);

        JPanel buttonsPanel = new JPanel(new BorderLayout());
        buttonsPanel.add(buttonsGridPanel, BorderLayout.EAST);
        return buttonsPanel;
    }

    protected void setEnabledComponentsWhileLoading(boolean enabled) {
        JDialog dialog = getJDialog();
        JPanel dialogContentPanel = (JPanel)dialog.getContentPane();

        Stack<JComponent> stack = new Stack<JComponent>();
        stack.push(dialogContentPanel);
        while (!stack.isEmpty()) {
            JComponent component = stack.pop();
            component.setEnabled(enabled);
            int childrenCount = component.getComponentCount();
            for (int i=0; i<childrenCount; i++) {
                Component child = component.getComponent(i);
                if (child instanceof JComponent) {
                    JComponent childComponent = (JComponent) child;
                    boolean found = false;
                    for (int k=0; k<this.componentsAllwaysEnabled.size(); k++) {
                        if (childComponent == this.componentsAllwaysEnabled.get(k)) {
                            found = true;
                        }
                    }
                    if (!found) {
                        // add the component in the stack to be enabled/disabled
                        stack.push(childComponent);
                    }
                }
            }
        }
    }

    protected final ILoadingIndicator getLoadingIndicator() {
        return this.loadingIndicatorPanel;
    }

    protected final int getNewCurrentThreadId() {
        return this.loadingIndicatorPanel.getNewCurrentThreadId();
    }

    protected void onAboutToShow() {
    }

    protected Border buildDialogBorder(int margins) {
        return new EmptyBorder(margins, margins, margins, margins);
    }

    protected void registerEscapeKey(ActionListener cancelActionListener) {
        JRootPane rootPane = getJDialog().getRootPane();
        KeyStroke escapeKey = getEscapeKeyPressed();
        rootPane.registerKeyboardAction(cancelActionListener, escapeKey, JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private KeyStroke getEscapeKeyPressed() {
        int noModifiers = 0;
        return KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, noModifiers, false); // 'false' => when <Escape> key is pressed
    }

    private void centerLoadingIndicatorPanel() {
        JDialog dialog = getJDialog();
        Rectangle layeredPaneBounds = dialog.getLayeredPane().getBounds();
        Dimension size = this.loadingIndicatorPanel.getPreferredSize();
        int x = layeredPaneBounds.width / 2 - size.width / 2;
        int y = layeredPaneBounds.height / 2 - size.height / 2;
        this.loadingIndicatorPanel.setBounds(x, y, size.width, size.height);
    }

    protected final JButton buildDialogButton(String buttonText) {
        JButton button = new JButton(buttonText);
        button.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent event) {
                getJDialog().requestFocusInWindow();
            }
        });
        Dimension size = button.getPreferredSize();
        size.width = 75;
        button.setPreferredSize(size);
        return button;
    }

    private static class JDialogExtended extends JDialog {

        private Runnable layoutRunnable;

        private JDialogExtended(Window owner, ModalityType modalityType) {
            super(owner, modalityType);
        }

        @Override
        protected final JRootPane createRootPane() {
            JRootPane rp = new JRootPane() {
                @Override
                public void doLayout() {
                    super.doLayout();

                    layoutRunnable.run();
                }
            };
            rp.setOpaque(true);
            return rp;
        }

        public final void setLayoutRunnable(Runnable layoutRunnable) {
            this.layoutRunnable = layoutRunnable;
        }
    }
}
