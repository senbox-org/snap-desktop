package org.esa.snap.gui.util;

import com.bc.ceres.swing.CollapsiblePane;
import org.openide.util.ImageUtilities;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by Norman on 24.11.2014.
 */
public class CollapsibleItemsPanel extends JComponent {

    private Item[] items;

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
        setLayout(null);

        ImageIcon colIcon = ImageUtilities.loadImageIcon("org/esa/snap/gui/icons/NodeCollapsed11.png", false);
        ImageIcon expIcon = ImageUtilities.loadImageIcon("org/esa/snap/gui/icons/NodeExpanded11.png", false);
        for (Item item : items) {
            JToggleButton button = new JToggleButton(item.getDisplayName());
            Font font = button.getFont();
            button.setFont(font.deriveFont(Font.BOLD, font.getSize() * 0.8f));

            JPanel panel = new JPanel(new BorderLayout(0, 0));
            panel.add(button, BorderLayout.NORTH);
            panel.add(item.getComponent(), BorderLayout.CENTER);

            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.setBorder(new EmptyBorder(2, 4, 2, 4));
            button.setIcon(expIcon);
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    item.getComponent().setVisible(!button.isSelected());
                    button.setIcon(button.isSelected() ? colIcon : expIcon);
                }
            });

            add(panel);
        }
    }

    public int getItemCount() {
        return items.length;
    }

    public Item getItem(int index) {
        return items[index];
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
