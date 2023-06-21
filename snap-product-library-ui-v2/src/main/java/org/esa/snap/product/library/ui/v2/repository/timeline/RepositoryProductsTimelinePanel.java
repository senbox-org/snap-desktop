package org.esa.snap.product.library.ui.v2.repository.timeline;

import org.esa.snap.product.library.ui.v2.repository.output.OutputProductResults;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemListener;
import java.time.LocalDateTime;
import java.util.ArrayList;

/**
 * The panel containing the two types to compute the product count from the search list: by day, by month.
 *
 * Created by jcoravu on 18/3/2020.
 */
public class RepositoryProductsTimelinePanel extends JPanel {

    public static final Color BAR_CHART_COLOR = new Color(7, 60, 102);
    private static final byte VERTICAL_GAP = 2;

    private final int preferredHeight;
    private final String monthsOfYear[];
    private final JRadioButton daysTimelineRadioButton;
    private final JRadioButton monthsTimelineRadioButton;

    private AbstractTimelineHelper timelineHelper;

    public RepositoryProductsTimelinePanel() {
        super(null); // 'null' => no layout manager

        this.daysTimelineRadioButton = new JRadioButton("Timeline");
        this.daysTimelineRadioButton.setFocusable(false);

        this.monthsTimelineRadioButton = new JRadioButton("Months");
        this.monthsTimelineRadioButton.setFocusable(false);

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(this.daysTimelineRadioButton);
        buttonGroup.add(this.monthsTimelineRadioButton);

        this.monthsTimelineRadioButton.setSelected(true);

        Dimension daysSize = this.daysTimelineRadioButton.getPreferredSize();
        Dimension monthsSize = this.monthsTimelineRadioButton.getPreferredSize();
        this.preferredHeight = daysSize.height + monthsSize.height + (3 * VERTICAL_GAP);

        this.monthsOfYear = new String[12];
        this.monthsOfYear[0] = "January";
        this.monthsOfYear[1] = "February";
        this.monthsOfYear[2] = "March";
        this.monthsOfYear[3] = "April";
        this.monthsOfYear[4] = "May";
        this.monthsOfYear[5] = "June";
        this.monthsOfYear[6] = "July";
        this.monthsOfYear[7] = "August";
        this.monthsOfYear[8] = "September";
        this.monthsOfYear[9] = "October";
        this.monthsOfYear[10] = "November";
        this.monthsOfYear[11] = "December";

        setVisible(false); // hide the time line by default
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, this.preferredHeight);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension preferredSize = super.getPreferredSize();
        preferredSize.height = this.preferredHeight;
        return preferredSize;
    }

    @Override
    public void doLayout() {
        super.doLayout();

        if (this.timelineHelper != null) {
            int leftPadding = 2;

            Dimension daysSize = this.daysTimelineRadioButton.getPreferredSize();
            int daysY = VERTICAL_GAP;
            this.daysTimelineRadioButton.setBounds(leftPadding, daysY, daysSize.width, daysSize.height);

            Dimension monthsSize = this.monthsTimelineRadioButton.getPreferredSize();
            int monthsY = daysY + daysSize.height + VERTICAL_GAP;
            this.monthsTimelineRadioButton.setBounds(leftPadding, monthsY, monthsSize.width, monthsSize.height);

            int panelX = leftPadding + Math.max(daysSize.width, monthsSize.width);
            int panelY = 0;
            int panelWidth = getWidth() - panelX;
            int panelHeight = getHeight() - panelY;
            this.timelineHelper.doLayout(panelX, panelY, panelWidth, panelHeight);
        }
    }

    public void setItemListener(ItemListener itemListener) {
        this.daysTimelineRadioButton.addItemListener(itemListener);
        this.monthsTimelineRadioButton.addItemListener(itemListener);
    }

    public void refresh(OutputProductResults outputProductResults) {
        // remove all the components
        removeAll();

        // add the radio buttons
        add(this.daysTimelineRadioButton);
        add(this.monthsTimelineRadioButton);

        this.timelineHelper = null; // no products by default
        if (this.daysTimelineRadioButton.isSelected()) {
            java.util.List<YearLabel> dayBarsByYear = computeBarsByDaysOfYear(outputProductResults);
            if (dayBarsByYear.size() > 0) {
                this.timelineHelper = new MultipleYearDaysTimelineHelper(dayBarsByYear, this.monthsOfYear) {
                    @Override
                    protected void addComponentToPanel(JComponent componentToAdd) {
                        add(componentToAdd);
                    }
                };
            }
        } else if (this.monthsTimelineRadioButton.isSelected()) {
            java.util.List<YearLabel> monthBarsByYear = computeBarsByMonthsOfYear(outputProductResults);
            if (monthBarsByYear.size() > 1) {
                this.timelineHelper = new MultipleYearMonthsTimelineHelper(monthBarsByYear, this.monthsOfYear) {
                    @Override
                    protected void addComponentToPanel(JComponent componentToAdd) {
                        add(componentToAdd);
                    }
                };
            } else if (monthBarsByYear.size() == 1) {
                this.timelineHelper = new SingleYearMonthsTimelineHelper(monthBarsByYear.get(0), this.monthsOfYear) {
                    @Override
                    protected void addComponentToPanel(JComponent componentToAdd) {
                        add(componentToAdd);
                    }
                };
            }
        } else {
            throw new IllegalStateException("No timeline selection type.");
        }

        // refresh the panel
        Container parent = getParent();
        if (parent != null) {
            parent.revalidate();
            parent.repaint();
        }

        // show or hide the panel
        setVisible(this.timelineHelper != null);
    }

    private static java.util.List<YearLabel> computeBarsByMonthsOfYear(OutputProductResults outputProductResults) {
        java.util.List<YearLabel> monthBarsByYear = new ArrayList<>();

        for (int i=0; i<outputProductResults.getAvailableProductCount(); i++) {
            LocalDateTime acquisitionDate = outputProductResults.getProductAt(i).getAcquisitionDate();
            if (acquisitionDate != null) {
                int year = acquisitionDate.getYear();
                int month = acquisitionDate.getMonthValue() - 1;

                YearLabel foundPair = null;
                for (int k=0; k<monthBarsByYear.size() && foundPair == null; k++) {
                    YearLabel pair = monthBarsByYear.get(k);
                    if (pair.getYear() == year) {
                        foundPair = pair;
                    }
                }
                if (foundPair == null) {
                    foundPair = new YearLabel(year);
                    monthBarsByYear.add(foundPair);
                }
                TimelineBarComponent foundMonthBarComponent = foundPair.findBarComponentById(month);
                if (foundMonthBarComponent == null) {
                    foundMonthBarComponent = foundPair.addBarComponent(month);
                }
                foundMonthBarComponent.incrementProductCount();
            }
        }
        return monthBarsByYear;
    }

    private static java.util.List<YearLabel> computeBarsByDaysOfYear(OutputProductResults outputProductResults) {
        java.util.List<YearLabel> dayBarsByYear = new ArrayList<>();

        for (int i=0; i<outputProductResults.getAvailableProductCount(); i++) {
            LocalDateTime acquisitionDate = outputProductResults.getProductAt(i).getAcquisitionDate();
            if (acquisitionDate != null) {
                int year = acquisitionDate.getYear();
                int dayOfYear = acquisitionDate.getDayOfYear();

                YearLabel foundPair = null;
                for (int k=0; k<dayBarsByYear.size() && foundPair == null; k++) {
                    YearLabel pair = dayBarsByYear.get(k);
                    if (pair.getYear() == year) {
                        foundPair = pair;
                    }
                }
                if (foundPair == null) {
                    foundPair = new YearLabel(year);
                    dayBarsByYear.add(foundPair);
                }
                TimelineBarComponent foundMonthBarComponent = foundPair.findBarComponentById(dayOfYear);
                if (foundMonthBarComponent == null) {
                    foundMonthBarComponent = foundPair.addBarComponent(dayOfYear);
                }
                foundMonthBarComponent.incrementProductCount();
            }
        }
        return dayBarsByYear;
    }
}
