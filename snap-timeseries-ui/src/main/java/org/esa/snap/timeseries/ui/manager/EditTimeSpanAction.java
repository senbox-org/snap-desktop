package org.esa.snap.timeseries.ui.manager;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.TableLayout;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.ui.ModalDialog;
import org.esa.snap.timeseries.core.timeseries.datamodel.AbstractTimeSeries;
import org.esa.snap.timeseries.core.timeseries.datamodel.GridTimeCoding;
import org.esa.snap.timeseries.core.timeseries.datamodel.ProductLocation;
import org.esa.snap.timeseries.core.timeseries.datamodel.TimeCoding;
import org.jdesktop.swingx.JXDatePicker;
import org.openide.util.ImageUtilities;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

class EditTimeSpanAction extends AbstractAction {

    private final AbstractTimeSeries timeSeries;

    EditTimeSpanAction(AbstractTimeSeries timeSeries) {
        this.timeSeries = timeSeries;
        setEnabled(timeSeries != null);
//        putValue(NAME, "[?]"); // todo set name
        ImageIcon editTimeSpanIcon = ImageUtilities.loadImageIcon("org/esa/snap/timeseries/ui/icons/timeseries-rangeedit24.png", false);
        putValue(LARGE_ICON_KEY, editTimeSpanIcon);
        putValue(ACTION_COMMAND_KEY, getClass().getName());
        putValue(SHORT_DESCRIPTION, "Edit time span");
        putValue("componentName", "EditTimeSpan");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final Object source = e.getSource();
        Window window = null;
        if (source instanceof Component) {
            window = SwingUtilities.getWindowAncestor((Component) source);
        }

        final ModalDialog dialog = new EditTimeSpanDialog(window, timeSeries);
        dialog.show();
    }

    private static class EditTimeSpanDialog extends ModalDialog {

        private final DateFormat dateFormat;
        private AbstractTimeSeries timeSeries;
        private JXDatePicker startTimeBox;
        private JXDatePicker endTimeBox;
        private JLabel startTimeLabel;
        private JLabel endTimeLabel;
        private JCheckBox autoAdjustBox;

        private EditTimeSpanDialog(Window window, AbstractTimeSeries timeSeries) {
            super(window, "Edit Time Span", ModalDialog.ID_OK_CANCEL, null);
            dateFormat = ProductData.UTC.createDateFormat(("dd-MMM-yyyy HH:mm:ss"));
            this.timeSeries = timeSeries;
            createUserInterface();
        }

        @Override
        protected void onOK() {
            timeSeries.setAutoAdjustingTimeCoding(autoAdjustBox.isSelected());
            final ProductData.UTC startTime = ProductData.UTC.create(startTimeBox.getDate(), 0);
            final ProductData.UTC endTime = ProductData.UTC.create(endTimeBox.getDate(), 0);
            timeSeries.setTimeCoding(new GridTimeCoding(startTime, endTime));

            super.onOK();
        }

        @Override
        protected boolean verifyUserInput() {
            if (endTimeBox.getDate().compareTo(startTimeBox.getDate()) < 0) {
                showErrorDialog("End time is before start time.");
                return false;
            }
            return true;
        }

        private void createUserInterface() {
            boolean isAutoAdjustingTimeCoding = timeSeries.isAutoAdjustingTimeCoding();
            final TableLayout tableLayout = new TableLayout(2);
            tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
            tableLayout.setTableWeightX(1.0);
            tableLayout.setTableFill(TableLayout.Fill.BOTH);
            tableLayout.setTablePadding(4, 4);
            tableLayout.setCellColspan(0, 0, 2);
            JPanel content = new JPanel(tableLayout);
            autoAdjustBox = createAutoAdjustBox(isAutoAdjustingTimeCoding);
            startTimeLabel = new JLabel("Start time:");
            startTimeBox = createDateComboBox();
            final TimeCoding timeCoding = timeSeries.getTimeCoding();
            startTimeBox.setDate(timeCoding.getStartTime().getAsDate());
            endTimeLabel = new JLabel("End time:");
            endTimeBox = createDateComboBox();
            endTimeBox.setDate(timeCoding.getEndTime().getAsDate());
            content.add(autoAdjustBox);
            content.add(startTimeLabel);
            content.add(startTimeBox);
            content.add(endTimeLabel);
            content.add(endTimeBox);
            setUiEnabled(!isAutoAdjustingTimeCoding);
            setContent(content);
        }

        private JCheckBox createAutoAdjustBox(boolean autoAdjustingTimeCoding) {
            final JCheckBox autoAdjustBox = new JCheckBox("Auto adjust time information", autoAdjustingTimeCoding);
            autoAdjustBox.addActionListener(new AutoAdjustBoxListener(autoAdjustBox));
            return autoAdjustBox;
        }

        private List<Product> getCompatibleProducts() {
            List<Product> result = new ArrayList<>();
            for (ProductLocation productLocation : timeSeries.getProductLocations()) {
                for (Product product : productLocation.getProducts(ProgressMonitor.NULL).values()) {
                    result.addAll(timeSeries.getEoVariables().stream().filter(variable -> timeSeries.isProductCompatible(product, variable)).filter(
                            timeSeries::isEoVariableSelected).map(variable -> product).collect(Collectors.toList()));
                }
            }
            return result;
        }

        private ProductData.UTC getMaxEndTime(final ProductData.UTC endTime1, final ProductData.UTC endTime2) {
            ProductData.UTC endTime;
            if (endTime1.getAsDate().before(endTime2.getAsDate())) {
                endTime = endTime2;
            } else {
                endTime = endTime1;
            }
            return endTime;
        }

        private ProductData.UTC getMinStartTime(final ProductData.UTC startTime1, final ProductData.UTC startTime2) {
            ProductData.UTC startTime;
            if (startTime1.getAsDate().after(startTime2.getAsDate())) {
                startTime = startTime2;
            } else {
                startTime = startTime1;
            }
            return startTime;
        }

        private JXDatePicker createDateComboBox() {
            TimeZone utcZone = TimeZone.getTimeZone("UTC");
            Calendar utc = Calendar.getInstance(utcZone);
            Date date = utc.getTime();
            JXDatePicker datePicker = new JXDatePicker(date);
            datePicker.setTimeZone(utcZone);
            datePicker.setFormats(dateFormat);
            return datePicker;
        }

        private void setUiEnabled(boolean enable) {
            startTimeBox.setEnabled(enable);
            startTimeLabel.setEnabled(enable);
            endTimeBox.setEnabled(enable);
            endTimeLabel.setEnabled(enable);
        }

        private class AutoAdjustBoxListener implements ActionListener {

            private final JCheckBox autoAdjustBox;

            private AutoAdjustBoxListener(JCheckBox autoAdjustBox) {
                this.autoAdjustBox = autoAdjustBox;
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                final boolean selected = autoAdjustBox.isSelected();
                setUiEnabled(!selected);
                if (!selected) {
                    return;
                }
                ProductData.UTC autoStartTime = null;
                ProductData.UTC autoEndTime = null;
                List<Product> compatibleProducts = getCompatibleProducts();
                for (Product product : compatibleProducts) {
                    TimeCoding varTimeCoding = GridTimeCoding.create(product);
                    if (autoStartTime == null) {
                        TimeCoding tsTimeCoding = timeSeries.getTimeCoding();
                        autoStartTime = tsTimeCoding.getStartTime();
                        autoEndTime = tsTimeCoding.getEndTime();
                    }
                    if (varTimeCoding != null) {
                        autoStartTime = getMinStartTime(autoStartTime,
                                varTimeCoding.getStartTime());
                        autoEndTime = getMaxEndTime(autoEndTime, varTimeCoding.getEndTime());
                    }
                }

                if (autoStartTime == null) {
                    try {
                        autoStartTime = ProductData.UTC.parse("1970-01-01", "yyyy-MM-dd");
                        autoEndTime = autoStartTime;
                    } catch (ParseException ignore) {
                    }
                }
                //noinspection ConstantConditions
                startTimeBox.setDate(autoStartTime.getAsDate());
                //noinspection ConstantConditions
                endTimeBox.setDate(autoEndTime.getAsDate());
            }
        }
    }

}
