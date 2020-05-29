package org.esa.snap.product.library.ui.v2.repository.output;

import org.esa.snap.product.library.ui.v2.ComponentDimension;
import org.esa.snap.ui.loading.CustomButton;
import org.esa.snap.ui.loading.SwingUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Created by jcoravu on 29/1/2020.
 */
public class OutputProductListPaginationPanel extends JPanel {

    private final JButton previousPageButton;
    private final JButton nextPageButton;
    private final JButton firstPageButton;
    private final JButton lastPageButton;
    private final JLabel totalPagesTextField;

    public OutputProductListPaginationPanel(ComponentDimension componentDimension, ActionListener firstPageButtonListener, ActionListener previousPageButtonListener,
                                            ActionListener nextPageButtonListener, ActionListener lastPageButtonListener) {
        super();

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        int preferredButtonHeight = componentDimension.getTextFieldPreferredHeight();

        this.firstPageButton = new CustomButton("<<", preferredButtonHeight);
        this.firstPageButton.setFocusable(false);
        this.firstPageButton.addActionListener(firstPageButtonListener);

        this.previousPageButton = new CustomButton("<", preferredButtonHeight);
        this.previousPageButton.setFocusable(false);
        this.previousPageButton.addActionListener(previousPageButtonListener);

        this.nextPageButton = new CustomButton(">", preferredButtonHeight);
        this.nextPageButton.setFocusable(false);
        this.nextPageButton.addActionListener(nextPageButtonListener);

        this.lastPageButton = new CustomButton(">>", preferredButtonHeight);
        this.lastPageButton.setFocusable(false);
        this.lastPageButton.addActionListener(lastPageButtonListener);

        this.totalPagesTextField = new JLabel("                    ", JLabel.CENTER);
        this.totalPagesTextField.setBorder(SwingUtils.LINE_BORDER);
        this.totalPagesTextField.setBackground(componentDimension.getTextFieldBackgroundColor());
        this.totalPagesTextField.setOpaque(true);

        Dimension labelPreferredSize = this.totalPagesTextField.getPreferredSize();
        labelPreferredSize.height = preferredButtonHeight;
        setComponentSize(this.totalPagesTextField, labelPreferredSize);

        add(this.firstPageButton);
        add(Box.createHorizontalStrut(componentDimension.getGapBetweenColumns()));
        add(this.previousPageButton);
        add(Box.createHorizontalStrut(componentDimension.getGapBetweenColumns()));
        add(this.totalPagesTextField);
        add(Box.createHorizontalStrut(componentDimension.getGapBetweenColumns()));
        add(this.nextPageButton);
        add(Box.createHorizontalStrut(componentDimension.getGapBetweenColumns()));
        add(this.lastPageButton);

        refreshPaginationButtons(false, false, "");
    }

    public void refreshPaginationButtons(boolean previousPageEnabled, boolean nextPageEnabled, String text) {
        this.firstPageButton.setEnabled(previousPageEnabled);
        this.previousPageButton.setEnabled(previousPageEnabled);
        this.nextPageButton.setEnabled(nextPageEnabled);
        this.lastPageButton.setEnabled(nextPageEnabled);
        this.totalPagesTextField.setText(text);
    }

    private static void setComponentSize(JComponent component, Dimension preferredSize) {
        component.setPreferredSize(preferredSize);
        component.setMinimumSize(preferredSize);
        component.setMaximumSize(preferredSize);
    }
}
