package org.esa.snap.opendap.ui;

import com.bc.ceres.binding.ValidationException;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.opendap.datamodel.OpendapLeaf;
import org.esa.snap.opendap.utils.PatternProvider;
import org.esa.snap.rcp.util.DateTimePicker;
import org.esa.snap.util.StringUtils;
import org.esa.snap.util.TimeStampExtractor;
import org.jdesktop.swingx.JXDatePicker;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.units.DateRange;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Logger;

public class TimeRangeFilter implements FilterComponent {

    public static final Logger LOG = Logger.getLogger(TimeRangeFilter.class.getName());
    private JComboBox<String> datePatternComboBox;
    private JComboBox<String> fileNamePatternComboBox;
    private JXDatePicker startDateButton;
    private JXDatePicker stopDateButton;
    private JButton applyButton;
    private JCheckBox filterCheckBox;
    TimeStampExtractor timeStampExtractor;
    List<FilterChangeListener> listeners;

    List<JLabel> labels;
    Date startDate;
    Date endDate;

    public TimeRangeFilter(final JCheckBox filterCheckBox) {
        this.filterCheckBox = filterCheckBox;
        final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        Date date = utc.getTime();
        startDateButton = new JXDatePicker(date);
        startDateButton.setFormats(dateFormat);
        stopDateButton = new JXDatePicker(date);
        stopDateButton.setFormats(dateFormat);

        final int width = 150;
        final Dimension ps = startDateButton.getPreferredSize();
        final Dimension comboBoxDimension = new Dimension(width, ps.height);
        setJComponentSize(comboBoxDimension, startDateButton);
        setJComponentSize(comboBoxDimension, stopDateButton);

        datePatternComboBox = new JComboBox<>();
        setJComponentSize(comboBoxDimension, datePatternComboBox);
        datePatternComboBox.setEditable(true);

        fileNamePatternComboBox = new JComboBox<>();
        setJComponentSize(comboBoxDimension, fileNamePatternComboBox);
        fileNamePatternComboBox.setEditable(true);

        initPatterns();

        final UIUpdater uiUpdater = new UIUpdater();
        filterCheckBox.addActionListener(e -> {
            updateUIState();
            if (timeStampExtractor != null) {
                fireFilterChangedEvent();
            }
        });
        startDateButton.addPropertyChangeListener(DateTimePicker.COMMIT_KEY, evt -> updateUIState());
        startDateButton.addPropertyChangeListener(DateTimePicker.COMMIT_KEY, evt -> validateDateChooser(startDateButton));
        stopDateButton.addPropertyChangeListener(DateTimePicker.COMMIT_KEY, evt -> updateUIState());
        stopDateButton.addPropertyChangeListener(DateTimePicker.COMMIT_KEY, evt -> validateDateChooser(stopDateButton));
        datePatternComboBox.addActionListener(uiUpdater);
        fileNamePatternComboBox.addActionListener(uiUpdater);

        listeners = new ArrayList<>();
        labels = new ArrayList<>();

        applyButton = new JButton("Apply");
        applyButton.addActionListener(e -> {
            if (StringUtils.isNotNullAndNotEmpty(datePatternComboBox.getEditor().getItem().toString())
                && StringUtils.isNotNullAndNotEmpty(fileNamePatternComboBox.getEditor().getItem().toString())) {
                timeStampExtractor = new TimeStampExtractor(datePatternComboBox.getSelectedItem().toString(),
                                                            fileNamePatternComboBox.getSelectedItem().toString());
            } else {
                timeStampExtractor = null;
            }
            startDate = startDateButton.getDate();
            endDate = stopDateButton.getDate();
            updateUIState();
            applyButton.setEnabled(false);
            fireFilterChangedEvent();
        });
    }

    private void updateUIState() {
        final boolean isSelected = filterCheckBox.isSelected();
        datePatternComboBox.setEnabled(isSelected);
        fileNamePatternComboBox.setEnabled(isSelected);
        startDateButton.setEnabled(isSelected);
        stopDateButton.setEnabled(isSelected);
        for (JLabel label : labels) {
            label.setEnabled(isSelected);
        }
        final String datePattern = datePatternComboBox.getSelectedItem().toString();
        final String fileNamePattern = fileNamePatternComboBox.getSelectedItem().toString();
        final boolean hasStartDate = startDateButton.getDate() != null;
        final boolean hasEndDate = stopDateButton.getDate() != null;
        final boolean patternProvided = !("".equals(datePattern) || "".equals(fileNamePattern));
        if (isSelected && (patternProvided || (!hasStartDate && !hasEndDate))) {
            applyButton.setEnabled(true);
        } else {
            applyButton.setEnabled(false);
        }
    }

    private void initPatterns() {
        datePatternComboBox.addItem("");
        fileNamePatternComboBox.addItem("");
        for (String datePattern : PatternProvider.DATE_PATTERNS) {
            datePatternComboBox.addItem(datePattern);
        }
        for (String fileNamePattern : PatternProvider.FILENAME_PATTERNS) {
            fileNamePatternComboBox.addItem(fileNamePattern);
        }
    }

    private void setJComponentSize(Dimension comboBoxDimension, JComponent comboBox) {
        comboBox.setPreferredSize(comboBoxDimension);
        comboBox.setMinimumSize(comboBoxDimension);
    }

    @Override
    public JComponent getUI() {

        final JPanel filterUI = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.insets.bottom = 4;
        gbc.insets.right = 4;
        gbc.anchor = GridBagConstraints.WEST;

        JLabel datePatternLabel = new JLabel("Date pattern:");
        filterUI.add(datePatternLabel, gbc);
        gbc.gridx++;

        filterUI.add(datePatternComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        JLabel filenamePatternLabel = new JLabel("Filename pattern:");
        filterUI.add(filenamePatternLabel, gbc);
        gbc.gridx++;
        filterUI.add(fileNamePatternComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        JLabel startDateLabel = new JLabel("Start date:");
        filterUI.add(startDateLabel, gbc);
        gbc.gridx++;
        filterUI.add(startDateButton, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        JLabel stopDateLabel = new JLabel("Stop date:");
        filterUI.add(stopDateLabel, gbc);

        gbc.gridx++;
        filterUI.add(stopDateButton, gbc);
        gbc.gridy++;

        gbc.weightx = 1;
        gbc.anchor = GridBagConstraints.EAST;
        filterUI.add(applyButton, gbc);

        labels.add(datePatternLabel);
        labels.add(filenamePatternLabel);
        labels.add(startDateLabel);
        labels.add(stopDateLabel);

        updateUIState();

        return filterUI;
    }

    @Override
    public boolean accept(OpendapLeaf leaf) {
        DateRange timeCoverage = leaf.getDataset().getTimeCoverage();
        if (timeCoverage != null) {
            return fitsToServerSpecifiedTimeRange(timeCoverage);
        }
        return timeStampExtractor == null || fitsToUserSpecifiedTimeRange(leaf);
    }

    private boolean fitsToServerSpecifiedTimeRange(DateRange dateRange) {
        if (startDate == null && endDate == null) {
            return true;
        } else if (startDate == null) {
            return endsAtOrBeforeEndDate(dateRange);
        } else if (endDate == null) {
            return startsAtOrAfterStartDate(dateRange);
        }
        return startsAtOrAfterStartDate(dateRange) && endsAtOrBeforeEndDate(dateRange);
    }

    private boolean endsAtOrBeforeEndDate(DateRange dateRange) {
        return dateRange.getEnd().getCalendarDate().equals(CalendarDate.of(endDate)) || dateRange.getEnd().before(endDate);
    }

    private boolean startsAtOrAfterStartDate(DateRange dateRange) {
        return dateRange.getStart().getCalendarDate().equals(CalendarDate.of(startDate)) || dateRange.getStart().after(startDate);
    }

    private boolean fitsToUserSpecifiedTimeRange(OpendapLeaf leaf) {
        try {
            final ProductData.UTC[] timeStamps = timeStampExtractor.extractTimeStamps(leaf.getName());

            final boolean startDateEqualsEndDate =
                    timeStamps[0].getAsDate().getTime() == timeStamps[1].getAsDate().getTime();
            if (startDateEqualsEndDate) {
                timeStamps[1] = null;
            }

            boolean fileHasEndDate = timeStamps[1] != null;
            boolean userHasStartDate = startDate != null;
            boolean userHasEndDate = endDate != null;

            if (userHasStartDate) {
                if (startDate.after(timeStamps[0].getAsDate())) {
                    return false;
                }
            }

            if (userHasEndDate) {
                if (fileHasEndDate) {
                    if (endDate.before(timeStamps[1].getAsDate())) {
                        return false;
                    }
                } else {
                    if (endDate.before(timeStamps[0].getAsDate())) {
                        return false;
                    }
                }
            }
            return true;

        } catch (ValidationException e) {
            return true;
        }
    }

    @Override
    public void addFilterChangeListener(FilterChangeListener listener) {
        listeners.add(listener);
    }

    private void fireFilterChangedEvent() {
        for (FilterChangeListener listener : listeners) {
            listener.filterChanged();
        }
    }

    private class UIUpdater implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            updateUIState();
        }
    }

    private void validateDateChooser(JXDatePicker button) {
        Date startDate = startDateButton.getDate();
        Date endDate = stopDateButton.getDate();
        if (startDate == null || endDate == null) {
            return;
        }
        if (startDate.after(endDate)) {
            if (button.equals(startDateButton)) {
                startDateButton.setDate(endDate);
                LOG.info("Start date after end date: Set start date to end date.");
            } else if (button.equals(stopDateButton)) {
                stopDateButton.setDate(startDate);
                LOG.info("Start date after end date: Set end date to start date.");
            }
        }
    }
}
