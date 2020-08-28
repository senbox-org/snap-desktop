package org.esa.snap.ui.color;

import org.esa.snap.core.util.NamingConvention;

import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Vector;

/**
 * A color chooser panel.
 *
 * @author Norman Fomferra
 * @author Marco Peters
 * @since SNAP 2.0
 */
public class ColorChooserPanel extends JPanel {
    public static final String SELECTED_COLOR_PROPERTY = "selectedColor";
    static final Color TRANSPARENCY = new Color(0, 0, 0, 0);
    private static final int GAP = 2;

    private Color selectedColor;

    public ColorChooserPanel() {
        this(Color.WHITE);
    }

    public ColorChooserPanel(Color selectedColor) {
        super(new BorderLayout(GAP, GAP));
        setBorder(new EmptyBorder(GAP, GAP, GAP, GAP));

        setSelectedColor(selectedColor);

        JButton noneButton = new JButton("None");
        noneButton.addActionListener(e -> {
            setSelectedColor(TRANSPARENCY);
        });

        JButton moreButton = new JButton("More...");
        moreButton.addActionListener(e -> {
            Color color = showMoreColorsDialog();
            if (color != null) {
                setSelectedColor(color);
            }
        });

        add(noneButton, BorderLayout.NORTH);
        add(createColorPicker(), BorderLayout.CENTER);
        add(moreButton, BorderLayout.SOUTH);
        // todo - use colors from popup menu LAF
        setBackground(Color.WHITE);
    }

    public Color getSelectedColor() {
        return selectedColor;
    }

    public void setSelectedColor(Color selectedColor) {
        Color oldValue = this.selectedColor;
        this.selectedColor = selectedColor;
        firePropertyChange(SELECTED_COLOR_PROPERTY, oldValue, this.selectedColor);
    }

    protected JComponent createColorPicker() {

        Color[] colors = {Color.BLACK,
                Color.DARK_GRAY,
                Color.GRAY,
                Color.LIGHT_GRAY,
                Color.WHITE,
                Color.CYAN,
                Color.BLUE,
                Color.MAGENTA,
                Color.YELLOW,
                Color.ORANGE,
                Color.RED,
                Color.PINK,
                Color.GREEN};


        JPanel colorsPanel = new JPanel(new GridLayout(-1, 6, 4, 4));
        colorsPanel.setOpaque(false);
        for (Color color : colors) {
            ColorLabel colorLabel = new ColorLabel(color);
            colorLabel.setDisplayName(ColorCodes.getName(color));
            colorLabel.setHoverEnabled(true);
            colorLabel.setMaximumSize(colorLabel.getPreferredSize());
            colorLabel.setMinimumSize(colorLabel.getPreferredSize());
            colorLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    setSelectedColor(colorLabel.getColor());
                }
            });
            colorsPanel.add(colorLabel);
        }
        return colorsPanel;
    }

    protected Color showMoreColorsDialog() {
        JColorChooser colorChooser = new JColorChooser(getSelectedColor());
        AbstractColorChooserPanel[] oldChooserPanels = colorChooser.getChooserPanels();
        AbstractColorChooserPanel[] newChooserPanels = new AbstractColorChooserPanel[oldChooserPanels.length + 1];
        System.arraycopy(oldChooserPanels, 0, newChooserPanels, 1, oldChooserPanels.length);
        newChooserPanels[0] = new MyAbstractColorChooserPanel();
        colorChooser.setChooserPanels(newChooserPanels);
        ColorTracker colorTracker = new ColorTracker(colorChooser);
        JDialog dialog = JColorChooser.createDialog(this, "Select " + NamingConvention.COLOR_MIXED_CASE, true, colorChooser, colorTracker, null);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setVisible(true);
        return colorTracker.getColor();
    }

    private static class MyAbstractColorChooserPanel extends AbstractColorChooserPanel implements ListSelectionListener {

        private JList<String> colorList;

        public MyAbstractColorChooserPanel() {
        }


        @Override
        public void updateChooser() {
            Color selectedColor = getColorSelectionModel().getSelectedColor();
            if (selectedColor != null) {
                int i = ColorCodes.indexOf(selectedColor);
                if (i >= 0) {
                    colorList.setSelectedIndex(i);
                }
            }
        }

        @Override
        protected void buildChooser() {
            colorList = new JList<>(new Vector<>(ColorCodes.getNames()));
            DefaultListCellRenderer cellRenderer = new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    setOpaque(true);
                    Color color = ColorCodes.getColor(value.toString());
                    int max = Math.max(color.getRed(), Math.max(color.getGreen(), color.getBlue()));
                    setForeground(max < 160 ? Color.WHITE : Color.BLACK);
                    setBackground(color);
                    setBorder(new EmptyBorder(5, 5, 5, 5));
                    setFont(getFont().deriveFont(14f));
                    return this;
                }
            };
            colorList.setCellRenderer(cellRenderer);
            colorList.addListSelectionListener(this);
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(5, 5, 5, 5));
            add(new JScrollPane(colorList), BorderLayout.CENTER);
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            int selectedIndex = colorList.getSelectedIndex();
            Color color = ColorCodes.getColor(selectedIndex);
            getColorSelectionModel().setSelectedColor(color);
        }

        @Override
        public String getDisplayName() {
            return "HTML Color Codes";
        }

        @Override
        public Icon getSmallDisplayIcon() {
            return null;
        }

        @Override
        public Icon getLargeDisplayIcon() {
            return null;
        }
    }

    private static class ColorTracker implements ActionListener {
        private JColorChooser colorChooser;
        private Color color;

        public ColorTracker(JColorChooser colorChooser) {
            this.colorChooser = colorChooser;
        }

        public Color getColor() {
            return color;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            color = colorChooser.getColor();
        }
    }
}
