package org.esa.snap.rcp.util;

import org.jdesktop.swingx.JXDatePicker;
import org.jdesktop.swingx.calendar.SingleDaySelectionModel;

import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.text.DateFormatter;
import javax.swing.text.DefaultFormatterFactory;
import java.awt.Color;
import java.awt.FlowLayout;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateTimePicker extends JXDatePicker {

    private JSpinner timeSpinner;
    private DateFormat timeFormat;
    private TimeZone timeZone;

    public DateTimePicker(Date d, Locale l, DateFormat dateFormat, DateFormat timeFormat) {
        super(d, l);
        if (!dateFormat.getTimeZone().equals(timeFormat.getTimeZone())) {
            throw new IllegalStateException(String.format("Time zone mismatch: dateFormat is [%s] but timeFormat is [%s]",
                                                          dateFormat, timeFormat));
        }
        timeZone = timeFormat.getTimeZone();
        getMonthView().setSelectionModel(new SingleDaySelectionModel());
        getMonthView().setTimeZone(timeZone);
        setLinkPanel(createTimePanel());
        setFormats(dateFormat);
        setTimeFormat(timeFormat);
        setDateTime(d);
    }


    public void commitEdit() throws ParseException {
        commitTime();
        super.commitEdit();
    }

    public DateFormat getTimeFormat() {
        return timeFormat;
    }

    public void setTimeFormat(DateFormat timeFormat) {
        this.timeFormat = timeFormat;
        updateTextFieldFormat();
    }

    public void setDateTime(Date date) {
        super.setDate(date);
        if (timeSpinner != null) {
            if (date != null) {
                timeSpinner.setValue(date);
            }else {
                Calendar calendar = Calendar.getInstance(timeZone);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                timeSpinner.setValue(calendar.getTime());
            }
        }
    }



    private JPanel createTimePanel() {
        JPanel newPanel = new JPanel();
        newPanel.setLayout(new FlowLayout());
        Date date = getDate();
        if (date == null) {
            Calendar calendar = Calendar.getInstance(timeZone);
            date = calendar.getTime();
        }
        SpinnerDateModel dateModel = new SpinnerDateModel(date, null, null, Calendar.DAY_OF_MONTH);
        timeSpinner = new JSpinner(dateModel);
        if (timeFormat == null) {
            timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
        }
        updateTextFieldFormat();
        newPanel.add(new JLabel("Time:"));
        newPanel.add(timeSpinner);
        newPanel.setBackground(Color.WHITE);
        return newPanel;
    }

    private void updateTextFieldFormat() {
        if (timeSpinner == null) {
            return;
        }
        JFormattedTextField tf = ((JSpinner.DefaultEditor) timeSpinner.getEditor()).getTextField();
        DefaultFormatterFactory factory = (DefaultFormatterFactory) tf.getFormatterFactory();
        DateFormatter formatter = (DateFormatter) factory.getDefaultFormatter();
        // Change the date format to only show the hours
        formatter.setFormat(timeFormat);
    }

    private void commitTime() {
        Date date = getDate();
        if (date != null) {
            Date time = (Date) timeSpinner.getValue();
            Calendar timeCalendar = Calendar.getInstance(timeZone);
            timeCalendar.setTime(time);
            Calendar calendar = Calendar.getInstance(timeZone);
            calendar.setTime(date);
            calendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY));
            calendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE));
            calendar.set(Calendar.SECOND, timeCalendar.get(Calendar.SECOND));
            calendar.set(Calendar.MILLISECOND, timeCalendar.get(Calendar.MILLISECOND));
            Date newDate = calendar.getTime();
            setDate(newDate);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TimeZone utcZone = TimeZone.getTimeZone("UTC");
            Calendar utc = Calendar.getInstance(utcZone);
            Date date = utc.getTime();
            DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);
            dateFormat.setTimeZone(utcZone);
            DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.MEDIUM);
            timeFormat.setTimeZone(utcZone);
            JFrame frame = new JFrame();
            frame.setTitle("Date Time Picker");
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            DateTimePicker dateTimePicker = new DateTimePicker(date, Locale.ENGLISH, dateFormat, timeFormat);
            dateTimePicker.setFormats(dateFormat);
            dateTimePicker.setTimeFormat(timeFormat);
            dateTimePicker.setDateTime(date);
            frame.getContentPane().add(dateTimePicker);
            frame.pack();
            frame.setVisible(true);
        });
    }
}