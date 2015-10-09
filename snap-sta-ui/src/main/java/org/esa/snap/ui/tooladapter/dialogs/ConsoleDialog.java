package org.esa.snap.ui.tooladapter.dialogs;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.awt.event.*;

/**
 * Created by kraftek on 10/8/2015.
 */
public class ConsoleDialog extends JDialog {

    private JTextPane textArea;

    public ConsoleDialog(ToolAdapterExecutionDialog parent) {
        super(parent.getJDialog());
        JDialog owner = parent.getJDialog();
        owner.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });
        owner.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                setLocation(owner.getX() + owner.getWidth(), owner.getY());
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                setLocation(owner.getX() + owner.getWidth(), owner.getY());
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                setVisible(false);
            }
        });
        textArea = new JTextPane();
        textArea.setBackground(Color.BLACK);
        final Container contentPane = super.getContentPane();
        contentPane.setPreferredSize(new Dimension(2 * owner.getWidth(), owner.getHeight()));
        this.setLocation(owner.getX() + owner.getWidth(), owner.getY());
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setWheelScrollingEnabled(true);
        scrollPane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
            BoundedRangeModel brm = scrollPane.getVerticalScrollBar().getModel();
            boolean wasAtBottom = true;
            @Override
            public void adjustmentValueChanged(AdjustmentEvent e) {
                if (!brm.getValueIsAdjusting()) {
                    if (wasAtBottom)
                        brm.setValue(brm.getMaximum());
                } else
                    wasAtBottom = ((brm.getValue() + brm.getExtent()) == brm.getMaximum());

            }
        });
        contentPane.add(scrollPane, BorderLayout.CENTER);
        pack();
    }

    public void append(String text) {
        StyleContext sc = StyleContext.getDefaultStyleContext();
        AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, Color.WHITE);
        aset = sc.addAttribute(aset, StyleConstants.FontFamily, "Lucida Console");
        aset = sc.addAttribute(aset, StyleConstants.Alignment, StyleConstants.ALIGN_LEFT);
        int len = textArea.getDocument().getLength();
        textArea.setCaretPosition(len);
        textArea.setCharacterAttributes(aset, false);
        textArea.replaceSelection("\n" + text);
    }

}
