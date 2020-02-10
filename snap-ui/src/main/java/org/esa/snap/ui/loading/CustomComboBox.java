package org.esa.snap.ui.loading;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Created by jcoravu on 10/2/2020.
 */
public class CustomComboBox<ItemType> extends JComboBox<ItemType> {

    private final int preferredHeight;

    public CustomComboBox(ItemRenderer<ItemType> itemRenderer, int preferredHeight, boolean isEditable, Color backgroundColor) {
        this(itemRenderer, preferredHeight, isEditable);

        setBackground(backgroundColor);
    }

    public CustomComboBox(ItemRenderer<ItemType> itemRenderer, int preferredHeight, boolean isEditable) {
        super();

        this.preferredHeight = preferredHeight;

        setMaximumRowCount(5); // the maximum number of visible items in the popup
        setEditable(true); // set the combo box as editable
        ComboBoxEditorComponent<ItemType> comboBoxEditorComponent = new ComboBoxEditorComponent<ItemType>(itemRenderer);
        JTextField editorTextField = comboBoxEditorComponent.getEditorComponent();
        editorTextField.setOpaque(false); // set the editor text transparent
        if (!isEditable) {
            comboBoxEditorComponent.getEditorComponent().setEditable(false); // set the editor component read only
        }
        setEditor(comboBoxEditorComponent);
        editorTextField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                if (isEnabled()) {
                    if (isPopupVisible()) {
                        hidePopup();
                    } else {
                        showPopup();
                    }
                }
            }
        });
        int cellItemHeight = getPreferredSize().height;
        setRenderer(new LabelListCellRenderer<ItemType>(cellItemHeight, itemRenderer) {
            @Override
            public JLabel getListCellRendererComponent(JList<? extends ItemType> list, ItemType value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                if (!isSelected) {
                    label.setBackground(CustomComboBox.this.getBackground());
                }
                return label;
            }
        });
    }

    @Override
    protected final void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        graphics.setColor(getBackground());
        graphics.fillRect(0, 0, getWidth(), getHeight());
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        if (getEditor() != null && getEditor().getEditorComponent() != null) {
            getEditor().getEditorComponent().setEnabled(enabled);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();

        size.height = this.preferredHeight;
        return size;
    }

    private static class ComboBoxEditorComponent<ItemType> implements ComboBoxEditor {

        private final ItemRenderer<ItemType> itemRenderer;
        private final JTextField editorTextField;

        private ItemType item;

        private ComboBoxEditorComponent(ItemRenderer<ItemType> itemRenderer) {
            this.itemRenderer = itemRenderer;
            this.editorTextField = new JTextField("", 9);
            this.editorTextField.setBorder(null);
        }

        @Override
        public JTextField getEditorComponent() {
            return this.editorTextField;
        }

        @Override
        public void setItem(Object item) {
            this.item = (ItemType)item;
            String text;
            if (this.item == null) {
                text = "";
            } else {
                text = this.itemRenderer.getItemDisplayText(this.item);
            }
            this.editorTextField.setText(text);
        }

        @Override
        public Object getItem() {
            String newValue = this.editorTextField.getText();
            if (this.item != null && !(this.item instanceof String))  {
                // the item is not a string
                if (newValue.equals(this.itemRenderer.getItemDisplayText(this.item)))  {
                    return this.item;
                }
            }
            return newValue;
        }

        @Override
        public void selectAll() {
            // do nothing
        }

        @Override
        public void addActionListener(ActionListener listener) {
            this.editorTextField.addActionListener(listener);
        }

        @Override
        public void removeActionListener(ActionListener listener) {
            this.editorTextField.removeActionListener(listener);
        }
    }
}
