package org.esa.snap.rcp.util;

import org.jfree.ui.DateChooserPanel;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.border.BevelBorder;
import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
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
        DateChooserPanel datePanel = new DateChooserPanel(getCalendar(), true);
        datePickerButton = new JButton();
        datePickerButton.addActionListener(e -> {
            JWindow frame = new JWindow();
            Point locationOnScreen = datePickerButton.getLocationOnScreen();
            locationOnScreen.move(0, datePickerButton.getHeight());
            frame.setLocation(locationOnScreen);
            JPanel contentPane = new JPanel(new BorderLayout());
            contentPane.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
            datePanel.setDate(date);
            contentPane.add(datePanel, BorderLayout.CENTER);
            contentPane.add(new JButton(new AbstractAction("OK") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    frame.setVisible(false);
                    setDate(datePanel.getDate());
                }
            }), BorderLayout.SOUTH);
            frame.setContentPane(contentPane);
            frame.pack();
            frame.setVisible(true);
        });
        this.setLayout(new BorderLayout());
        this.add(datePickerButton, BorderLayout.CENTER);
        updateButtonLabel();
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

}
