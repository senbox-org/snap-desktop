package org.esa.snap.rcp.util;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Temporary replacement for the JIDE component with same name.
 */
public class ColorComboBox extends JComboBox<Color> {

    public static final Color TRANSPARENCY = new Color(0, 0, 0, 0);

    public ColorComboBox() {
        super(new DefaultComboBoxModel<Color>(new Color[]{
                Color.RED,
                Color.ORANGE,
                Color.YELLOW,
                TRANSPARENCY,
                null
        }));

        addActionListener(e -> {
            if (getSelectedItem() == null) {
                Color color = JColorChooser.showDialog(ColorComboBox.this, "Colour", Color.WHITE);
                DefaultComboBoxModel<Color> model = (DefaultComboBoxModel<Color>) getModel();
                int i = model.getIndexOf(color);
                if (i < 0) {
                    model.insertElementAt(color, 0);
                }
                model.setSelectedItem(color);
            }
        });

        //setEditable(false);

        MyDefaultListCellRenderer cellRenderer = new MyDefaultListCellRenderer();
        setEditor(cellRenderer);
        setRenderer(cellRenderer);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame(ColorComboBox.class.getSimpleName());
        frame.add(new ColorComboBox());
        frame.pack();
        frame.setVisible(true);
    }

    private class MyDefaultListCellRenderer extends DefaultListCellRenderer implements ComboBoxEditor {
        @Override
        public Component getEditorComponent() {
            update((Color) getModel().getSelectedItem(), false);
            return this;
        }

        @Override
        public void setItem(Object anObject) {
        }

        @Override
        public Object getItem() {
            return getModel().getSelectedItem();
        }

        @Override
        public void selectAll() {
        }

        @Override
        public void addActionListener(ActionListener l) {
        }

        @Override
        public void removeActionListener(ActionListener l) {
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            update((Color) value, isSelected);
            return this;
        }

        private void update(Color color, boolean isSelected) {
            if (color == null) {
                setText("More...");
                setBackground(Color.WHITE);
            } else if (TRANSPARENCY.equals(color)) {
                setText("No Colour");
                setBackground(Color.WHITE);
            } else {
                setText("#" + Integer.toHexString(color.getRGB()));
                setBackground(color);
            }
            if (isSelected) {
                setBorder(new LineBorder(Color.BLUE, 2));
            } else {
                setBorder(new LineBorder(Color.WHITE, 2));
            }
        }
    }
}
