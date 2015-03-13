package org.esa.snap.rcp.util;

import org.jfree.ui.DateChooserPanel;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.border.BevelBorder;
import java.awt.BorderLayout;
import java.awt.Point;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;

public class DateChooserButton extends JComponent {

    public static final String PROPERTY_NAME_DATE = "date";
    private final SimpleDateFormat dateFormat;
    private Date date;
    private Calendar calendar;
    private JButton datePickerButton;
    private JWindow window;

    public DateChooserButton(SimpleDateFormat dateFormat, Date date) {
        this.dateFormat = dateFormat;
        if (date == null) {
            this.date = Date.from(Instant.now());
        } else {
            this.date = (Date) date.clone();
        }
        calendar = Calendar.getInstance();
        initUI();
    }

    private void initUI() {
        datePickerButton = new JButton();
        datePickerButton.addActionListener(e -> {
            if (!closeWindow()) {
                showWindow();
            }
        });
        this.setLayout(new BorderLayout());
        this.add(datePickerButton, BorderLayout.CENTER);
        updateButtonLabel();
    }

    private void showWindow() {
        DateChooserPanel datePanel = new MyDateChooserPanel();
        datePanel.setDate(date);
        datePanel.addPropertyChangeListener(PROPERTY_NAME_DATE, evt -> {
            DateChooserButton.this.setDate((Date) evt.getNewValue());
            closeWindow();
        });
        window = new JWindow();
        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        contentPane.add(datePanel, BorderLayout.CENTER);
        window.setContentPane(contentPane);
        window.pack();
        Point locationOnScreen = datePickerButton.getLocationOnScreen();
        locationOnScreen.y += datePickerButton.getHeight();
        window.setLocation(locationOnScreen);
        window.setVisible(true);
    }

    private boolean closeWindow() {
        if (window != null && window.isShowing()) {
            window.setVisible(false);
            window = null;
            return true;
        }
        return false;
    }

    public SimpleDateFormat getDateFormat() {
        return dateFormat;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        Date oldDate = this.date;
        if (!oldDate.equals(date)) {
            this.date = date;
            updateButtonLabel();
            DateChooserButton.this.firePropertyChange(PROPERTY_NAME_DATE, oldDate, date);
        }
    }

    public Calendar getCalendar() {
        return calendar;
    }

    public void setCalendar(Calendar calendar) {
        this.calendar = calendar;
    }

    private void updateButtonLabel() {
        datePickerButton.setText(dateFormat.format(date));
    }

    private class MyDateChooserPanel extends DateChooserPanel {

        public MyDateChooserPanel() {
            super(DateChooserButton.this.getCalendar(), true);
        }

        @Override
        public void setDate(Date theDate) {
            Date oldDate = getDate();
            super.setDate(theDate);
            firePropertyChange(PROPERTY_NAME_DATE, oldDate, theDate);
        }
    }
}
