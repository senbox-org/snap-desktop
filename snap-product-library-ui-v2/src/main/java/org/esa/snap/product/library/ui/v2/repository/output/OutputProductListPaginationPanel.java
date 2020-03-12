package org.esa.snap.product.library.ui.v2.repository.output;

import org.esa.snap.product.library.ui.v2.ComponentDimension;
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

        this.firstPageButton = new JButton("<<");
        this.firstPageButton.setFocusable(false);
        this.firstPageButton.addActionListener(firstPageButtonListener);

        this.previousPageButton = new JButton("<");
        this.previousPageButton.setFocusable(false);
        this.previousPageButton.addActionListener(previousPageButtonListener);

        this.nextPageButton = new JButton(">");
        this.nextPageButton.setFocusable(false);
        this.nextPageButton.addActionListener(nextPageButtonListener);

        this.lastPageButton = new JButton(">>");
        this.lastPageButton.setFocusable(false);
        this.lastPageButton.addActionListener(lastPageButtonListener);

        Dimension buttonPreferredSize = this.lastPageButton.getPreferredSize();
        buttonPreferredSize.height = componentDimension.getTextFieldPreferredHeight();
        setComponentSize(this.firstPageButton, buttonPreferredSize);
        setComponentSize(this.previousPageButton, buttonPreferredSize);
        setComponentSize(this.nextPageButton, buttonPreferredSize);
        setComponentSize(this.lastPageButton, buttonPreferredSize);

        this.totalPagesTextField = new JLabel("                    ", JLabel.CENTER);
        this.totalPagesTextField.setBorder(SwingUtils.LINE_BORDER);
        this.totalPagesTextField.setBackground(componentDimension.getTextFieldBackgroundColor());
        this.totalPagesTextField.setOpaque(true);

        Dimension labelPreferredSize = this.totalPagesTextField.getPreferredSize();
        labelPreferredSize.height = componentDimension.getTextFieldPreferredHeight();
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
