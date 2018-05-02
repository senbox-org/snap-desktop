package org.esa.snap.rcp.statistics;

import com.bc.ceres.swing.TableLayout;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import org.jfree.ui.FontChooserPanel;
import org.jfree.ui.FontDisplayField;
import org.jfree.ui.PaintSample;

class ScatterPlot3DPropertiesPanel extends JPanel {

    private final static String DEFAULT_TITLE = "3D Scatter Plot";

    private JTextField titleTextField;
    private PaintSample titlePaint;
    private JCheckBox showTitleCheckBox;
    private FontDisplayField fontfield;

    private Font font;
    private Color color;

    ScatterPlot3DPropertiesPanel() {
        font = new Font("SansSerif", Font.BOLD, 18);
        color = Color.BLACK;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Title", createTitlePanel());
        add(tabbedPane, BorderLayout.NORTH);
    }

    private JPanel createTitlePanel() {
        titleTextField = new JTextField(DEFAULT_TITLE);
        titlePaint = new PaintSample(color);
        TableLayout tableLayout = new TableLayout(3);
        tableLayout.setTablePadding(2, 2);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setColumnWeightX(0, 0.2);
        tableLayout.setColumnWeightX(1, 1.0);
        tableLayout.setColumnWeightX(2, 0.2);
        JPanel framePanel = new JPanel(tableLayout);
        framePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "General"));
        framePanel.add(new JLabel("Show Title:"));
        showTitleCheckBox = new JCheckBox();
        showTitleCheckBox.setSelected(true);
        framePanel.add(new JPanel());
        framePanel.add(this.showTitleCheckBox);
        JLabel titleLabel = new JLabel("Text:");
        framePanel.add(titleLabel);
        framePanel.add(titleTextField);
        framePanel.add(new JPanel());
        JLabel fontLabel = new JLabel("Font:");
        fontfield = new FontDisplayField(font);
        JButton selectFontButton = new JButton("Select...");
        selectFontButton.addActionListener(e -> attemptFontSelection());
        framePanel.add(fontLabel);
        framePanel.add(this.fontfield);
        framePanel.add(selectFontButton);
        JLabel colorLabel = new JLabel("Color:");
        JButton selectPaintButton = new JButton("Select...");
        selectPaintButton.addActionListener(e -> attemptPaintSelection());
        framePanel.add(colorLabel);
        framePanel.add(titlePaint);
        framePanel.add(selectPaintButton);
        return framePanel;
    }

    boolean showTitle() {
        return showTitleCheckBox.isSelected();
    }

    String getTitle() {
        return titleTextField.getText();
    }

    Font getTitleFont() {
        return font;
    }

    Color getTitleColor() {
        return (Color) titlePaint.getPaint();
    }

    private void attemptFontSelection() {
        FontChooserPanel panel = new FontChooserPanel(font);
        int result = JOptionPane.showConfirmDialog(this, panel, "Font Selection",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == 0) {
            font = panel.getSelectedFont();
            fontfield.setText(font.getFontName() + " " + font.getSize());
        }

    }

    private void attemptPaintSelection() {
        Paint p = titlePaint.getPaint();
        Color defaultColor = p instanceof Color ? (Color) p : Color.blue;
        Color c = JColorChooser.showDialog(this, "Title Color", defaultColor);
        if (c != null) {
            titlePaint.setPaint(c);
        }
    }

}
