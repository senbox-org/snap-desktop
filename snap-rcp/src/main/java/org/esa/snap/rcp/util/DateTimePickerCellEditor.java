package org.esa.snap.rcp.util;

import org.esa.snap.core.datamodel.ProductData;
import org.jdesktop.swingx.table.DatePickerCellEditor;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * @author Marco Peters
 */
public class DateTimePickerCellEditor extends DatePickerCellEditor {

    public DateTimePickerCellEditor(DateFormat dateFormat, DateFormat timeFormat) {
        super(null);
        Date asDate = ProductData.UTC.create(new Date(), 0).getAsDate();
        DateTimePicker dateTimePicker = new DateTimePicker(asDate, Locale.getDefault(), dateFormat, timeFormat);
        //---- this duplicates the code in the parent constructor -------------
        dateTimePicker.getEditor().setBorder(BorderFactory.createEmptyBorder(0, 1, 0, 1));
        dateTimePicker.addActionListener(getPickerActionListener());
        datePicker = dateTimePicker;
        //---------------------------------------------------------------------
    }

    private DateTimePicker getEditor() {
        return (DateTimePicker) datePicker;
    }

    public void setTimeFormat(DateFormat timeFormat) {
        getEditor().setTimeFormat(timeFormat);
    }

    public DateFormat getTimeFormat() {
        return getEditor().getTimeFormat();
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        ignoreAction = true;
        Date valueAsDate = getValueAsDate(value);
        getEditor().setDateTime(valueAsDate);
        ignoreAction = false;
        return datePicker;
    }


    public static void main(String[] args) {
        Locale.setDefault(Locale.ENGLISH);

        SwingUtilities.invokeLater(() -> {

            DateFormat dateFormat = ProductData.UTC.createDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            DateFormat timeFormat = ProductData.UTC.createDateFormat("HH:mm:ss");
            Calendar calendar = getCalendar();
            Date[] date1 = {calendar.getTime()};
            calendar.roll(Calendar.DATE, 1);
            Date[] date2 = {(Date) calendar.getTime().clone()};
            calendar.roll(Calendar.DATE, 1);
            calendar.roll(Calendar.HOUR, 3);
            Date[] date3 = {calendar.getTime()};
            Date[] date4 = {null};

            DefaultTableModel tableModel = new DefaultTableModel(0, 1);
            tableModel.addRow(date1);
            tableModel.addRow(date2);
            tableModel.addRow(date3);
            tableModel.addRow(date4);
            JTable table = new JTable(tableModel);
            DateTimePickerCellEditor timePickerCellEditor = new DateTimePickerCellEditor(dateFormat, timeFormat);
            timePickerCellEditor.setClickCountToStart(1);
            table.getColumnModel().getColumn(0).setPreferredWidth(250);
            table.getColumnModel().getColumn(0).setCellEditor(timePickerCellEditor);
            table.getColumnModel().getColumn(0).setCellRenderer(new DateCellRenderer(dateFormat));

            JPanel panel = new JPanel();
            panel.setLayout(new BorderLayout());
            panel.add(table, BorderLayout.CENTER);

            JFrame frame = new JFrame("Test DateTime Picker");
            frame.setContentPane(panel);
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setLocationByPlatform(true);
            frame.pack();
            frame.setVisible(true);
        });
    }

    private static Calendar getCalendar() {
        Calendar calendar = ProductData.UTC.createCalendar();
        calendar.setTimeInMillis(new Date().getTime());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

}
