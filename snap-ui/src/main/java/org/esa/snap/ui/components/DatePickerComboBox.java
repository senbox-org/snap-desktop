package org.esa.snap.ui.components;

import org.esa.snap.ui.loading.SwingUtils;
import org.jdesktop.swingx.JXMonthView;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

public class DatePickerComboBox extends JComboBox<Void> {

    private final int preferredHeight;
    private final DateFormat dateFormat;
    private final JXMonthView monthView;
    private final JPopupMenu popup;

    public DatePickerComboBox(int preferredHeight, Color backgroundColor, DateFormat dateFormat) {
        super();

        if (preferredHeight <= 0) {
            throw new IllegalArgumentException("The preferred size " + preferredHeight + " must be > 0.");
        }
        if (backgroundColor == null) {
            throw new NullPointerException("The background color is null.");
        }
        if (dateFormat == null) {
            throw new NullPointerException("The date format is null.");
        }

        this.preferredHeight = preferredHeight;
        this.dateFormat = dateFormat;

        setBackground(backgroundColor);
        setBorder(SwingUtils.LINE_BORDER);
        setEditable(true); // set the combo box as editable

        this.monthView = new JXMonthView();
        this.monthView.setTraversable(true);
        this.monthView.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        newSelectedDate();
                    }
                });
            }
        });

        Date todayDate = new Date(System.currentTimeMillis());
        String dateAsString = this.dateFormat.format(todayDate);

        JLabel todayLabel = new JLabel("Today is " + dateAsString, JLabel.CENTER);
        Border outsideBorder = new MatteBorder(1, 0, 0, 0, SwingUtils.LINE_BORDER.getLineColor());
        Border insideBorder = new EmptyBorder(5, 0, 5, 0);
        todayLabel.setBorder(new CompoundBorder(outsideBorder, insideBorder));

        this.popup = new JPopupMenu();
        this.popup.setLayout(new BorderLayout());
        this.popup.add(this.monthView, BorderLayout.CENTER);
        this.popup.add(todayLabel, BorderLayout.SOUTH);
        this.popup.setBorder(SwingUtils.LINE_BORDER);

        JComponent editorComponent = getEditorComponent();
        editorComponent.setBorder(null);
        editorComponent.setOpaque(false);
        editorComponent.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                popup.setVisible(false);
            }
        });

        replaceArrowButtonMouseListeners();
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
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, this.preferredHeight);
    }

    @Override
    public Dimension getMinimumSize() {
        Dimension size = super.getMinimumSize();

        size.height = this.preferredHeight;
        return size;
    }

    @Override
    public Dimension getMaximumSize() {
        Dimension size = super.getMaximumSize();

        size.height = this.preferredHeight;
        return size;
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();

        size.height = this.preferredHeight;
        return size;
    }

    public void setDate(Date date) {
        this.monthView.setSelectionDate(date);
        String dateAsString = (date == null) ? "" : this.dateFormat.format(date);
        getEditorComponent().setText(dateAsString);
    }

    public String getEnteredDateAsString() {
        return getEditorComponent().getText().trim();
    }

    public Date getDate() {
        String dateAsString = getEnteredDateAsString();
        if (dateAsString.length() > 0) {
            try {
                return this.dateFormat.parse(dateAsString);
            } catch (ParseException e) {
                throw new IllegalStateException("Failed to parse the date '" + dateAsString+"'.", e);
            }
        }
        return null;
    }

    private void newSelectedDate() {
        String dateAsString = this.dateFormat.format(this.monthView.getSelectionDate());
        getEditorComponent().setText(dateAsString);
        this.popup.setVisible(false);
    }

    private JTextComponent getEditorComponent() {
        return (JTextComponent)getEditor().getEditorComponent();
    }

    private void toggleShowPopup() {
        if (this.popup.isVisible()) {
            this.popup.setVisible(false);
        } else {
            String dateAsString = getEnteredDateAsString();
            Date date = null;
            if (dateAsString.length() > 0) {
                try {
                    date = this.dateFormat.parse(dateAsString);
                } catch (ParseException e) {
                    // failed to parse the date
                    getEditorComponent().setText("");
                }
            }
            Date visibleDate = date;
            if (visibleDate == null) {
                visibleDate = new Date(System.currentTimeMillis());
            }
            this.monthView.setSelectionDate(date);
            this.monthView.ensureDateVisible(visibleDate);

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    DatePickerComboBox.this.popup.show(DatePickerComboBox.this, 0, DatePickerComboBox.this.getHeight());
                }
            });
        }
    }

    private void replaceArrowButtonMouseListeners() {
        int count = getComponentCount();
        for (int i=0; i<count; i++) {
            Component component = getComponent(i);
            if (component instanceof JButton) {
                JButton arrowButton = (JButton)component;
                MouseListener[] mouseListeners = arrowButton.getMouseListeners();
                if (mouseListeners.length >= 1) {
                    // remove the second mouse listener to avoid showing the popup containing the list
                    arrowButton.removeMouseListener(mouseListeners[1]);
                }
                arrowButton.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent mouseEvent) {
                        toggleShowPopup();
                    }
                });
            }
        }
    }
}
