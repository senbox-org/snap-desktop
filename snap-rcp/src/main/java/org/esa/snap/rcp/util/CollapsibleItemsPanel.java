package org.esa.snap.rcp.util;

import com.bc.ceres.swing.CollapsiblePane;
import org.openide.util.ImageUtilities;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * @author Norman Fomferra
 * @author Tonio Fincke
 */
public class CollapsibleItemsPanel extends JComponent {

    private Item[] items;
    private JToggleButton[] toggleButtons;
    private static ImageIcon COL_ICON = ImageUtilities.loadImageIcon("org/esa/snap/rcp/icons/NodeCollapsed11.png", false);
    private static ImageIcon EXP_ICON = ImageUtilities.loadImageIcon("org/esa/snap/rcp/icons/NodeExpanded11.png", false);

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // e.printStackTrace();
        }

        CollapsibleItemsPanel collapsibleItemsPanel = new CollapsibleItemsPanel(createTableItem("Time", 2, 2),
                                                                                createTableItem("Position", 6, 2),
                                                                                createTableItem("Bands", 18, 3));

        JFrame frame = new JFrame(CollapsiblePane.class.getSimpleName());
        frame.getContentPane().add(new JScrollPane(collapsibleItemsPanel,
                                                   ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                   ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER));
        frame.pack();
        frame.setVisible(true);
    }

    public static Item<JTable> createTableItem(String name, int rows, int columns) {
        JTable table = new JTable(rows, columns);
        table.setCellSelectionEnabled(false);
        table.setColumnSelectionAllowed(false);
        table.setRowSelectionAllowed(false);
        table.setShowGrid(true);
        return new DefaultItem<>(name, table);
    }

    public CollapsibleItemsPanel(Item... items) {
        this.items = items;
        this.toggleButtons = new  JToggleButton[items.length];
        setLayout(null);

        for (int i = 0; i < items.length; i++) {
            Item item = items[i];
            JToggleButton button = new JToggleButton(item.getDisplayName());
            Font font = button.getFont();
            if (font != null) {
                button.setFont(font.deriveFont(Font.PLAIN, font.getSize()));
            } else {
                button.setFont(new Font(Font.DIALOG, Font.PLAIN, 11));
            }

            JPanel panel = new JPanel(new BorderLayout(0, 0));
            panel.add(button, BorderLayout.NORTH);
            panel.add(item.getComponent(), BorderLayout.CENTER);

            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.setBorder(new EmptyBorder(2, 4, 2, 4));
            button.setIcon(EXP_ICON);
            button.addActionListener(e -> {
                item.getComponent().setVisible(!button.isSelected());
                button.setIcon(button.isSelected() ? COL_ICON : EXP_ICON);
            });
            toggleButtons[i] = button;
            add(panel);
        }
    }

    public Item getItem(int index) {
        return items[index];
    }

    public void setCollapsed(int index, boolean collapsed) {
        items[index].getComponent().setVisible(!collapsed);
        toggleButtons[index].setSelected(collapsed);
        toggleButtons[index].setIcon(collapsed ? COL_ICON : EXP_ICON);
    }

    @Override
    public Dimension getPreferredSize() {
        int width = 0;
        int height = 0;
        Component[] components = getComponents();
        for (Component component : components) {
            Dimension preferredSize = component.getPreferredSize();
            width = Math.max(width, preferredSize.width);
            height += preferredSize.height;
        }
        return new Dimension(width, height);
    }

    @Override
    public void doLayout() {
        int y = 0;
        int width = getWidth();
        Component[] components = getComponents();
        for (Component component : components) {
            Dimension preferredSize = component.getPreferredSize();
            component.setBounds(0, y, width, preferredSize.height);
            y += preferredSize.height;
        }
    }

    public boolean isCollapsed(int index) {
        return !items[index].getComponent().isVisible();
    }

    /*
    public static interface Model {
        int getItemCount();

        Item getItem(int index);

        void addItem(int index, Item item);

        void addItem(Item item);

        void removeItem(Item item);

        void addChangeListener(ChangeListener changeListener);

        void removeChangeListener(ChangeListener changeListener);
    }
    */

    public static interface Item<T extends JComponent> {
        String getDisplayName();

        T getComponent();
    }

    public static class DefaultItem<T extends JComponent> implements Item<T> {
        String displayName;
        T component;

        public DefaultItem(String displayName, T component) {
            this.displayName = displayName;
            this.component = component;
        }

        @Override
        public String getDisplayName() {
            return displayName;
        }

        @Override
        public T getComponent() {
            return component;
        }
    }
}
