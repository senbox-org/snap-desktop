package org.esa.snap.product.library.ui.v2.repository.timeline;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The class computes the product count by month for an year.
 */
public abstract class SingleYearMonthsTimelineHelper extends AbstractTimelineHelper {

    private final JLabel monthNameLabels[];
    private final String monthsOfYear[];
    private final Dimension maximumMonthLabelsSize;

    public SingleYearMonthsTimelineHelper(YearLabel yearLabel, String monthsOfYear[]) {
        this.yearLabels.add(yearLabel);
        this.monthsOfYear = monthsOfYear;

        this.monthNameLabels = new JLabel[monthsOfYear.length];
        for (int i=0; i<monthsOfYear.length; i++) {
            this.monthNameLabels[i] = new JLabel("", JLabel.CENTER);
            addComponentToPanel(this.monthNameLabels[i]);
        }

        for (int i = 0; i<yearLabel.getTimelineBarCount(); i++) {
            TimelineBarComponent monthBarComponent = yearLabel.getTimelineBarAt(i);
            String toolTipText = MultipleYearMonthsTimelineHelper.buildMonthBarTooltipText(monthsOfYear, monthBarComponent);
            monthBarComponent.setToolTipText(toolTipText);
            addComponentToPanel(monthBarComponent);
        }

        this.maximumMonthLabelsSize = computeMaximumMonthLabelsSize(this.monthsOfYear);
    }

    @Override
    public void doLayout(int panelX, int panelY, int panelWidth, int panelHeight) {
        this.yearLabels.get(0).setLocation(panelX, panelY);
        doYearBarsLayout(panelX, panelY, panelWidth, panelHeight, panelWidth, this.maximumMonthLabelsSize.height, this.monthsOfYear.length);
    }

    @Override
    protected void beforeLayoutBar(int panelX, int panelY, int panelWidth, int maximumBarHeight, int monthBarId, int monthBarSegmentIndex, int[] barSegmentsX) {
        layoutMonthLabel(panelX, panelY, panelWidth, maximumBarHeight, monthBarId, monthBarSegmentIndex, barSegmentsX);
    }

    @Override
    protected void afterLayoutBars(int panelX, int panelY, int panelWidth, int maximumBarHeight, int defaultBarCountPerYear, int barCountPerYear,
                                   int[] barSegmentsX, LinkedHashMap<TimelineBarComponent, Integer> monthBarIndecesMap) {

        for (int i=0; i<this.monthsOfYear.length; i++) {
            JLabel monthNameLabel = this.monthNameLabels[i];
            boolean visible = false;
            if (barCountPerYear == defaultBarCountPerYear) {
                visible = true;
                boolean foundMonthBar = false;
                for (Map.Entry<TimelineBarComponent, Integer> entry : monthBarIndecesMap.entrySet()) {
                    TimelineBarComponent monthBarComponent = entry.getKey();
                    if (monthBarComponent.getId() == i) {
                        foundMonthBar = true;
                        break;
                    }
                }
                if (!foundMonthBar) {
                    layoutMonthLabel(panelX, panelY, panelWidth, maximumBarHeight, i, i, barSegmentsX);
                }
            }
            monthNameLabel.setVisible(visible);
        }
    }

    @Override
    protected void beforeMoveLayoutBarToLeft(int panelX, int monthBarId, int fromBarSegmentIndex, int toBarSegmentIndex, int[] barSegmentsX) {
        JLabel monthNameLabel = this.monthNameLabels[monthBarId];
        int monthLabelX = panelX + barSegmentsX[toBarSegmentIndex];
        monthNameLabel.setLocation(monthLabelX, monthNameLabel.getY());
    }

    private void layoutMonthLabel(int panelX, int panelY, int panelWidth, int maximumBarHeight, int monthBarId, int monthBarSegmentIndex, int[] barSegmentsX) {
        int totalNeededLabelsWidth = this.maximumMonthLabelsSize.width * this.monthsOfYear.length;
        JLabel monthNameLabel = this.monthNameLabels[monthBarId];
        String labelText = this.monthsOfYear[monthBarId];
        if (totalNeededLabelsWidth > panelWidth) {
            labelText = labelText.substring(0, 3);
        }
        monthNameLabel.setText(labelText);
        int monthLabelX = panelX + barSegmentsX[monthBarSegmentIndex];
        int monthLabelY = panelY + maximumBarHeight;//(panelHeight - maximumMonthLabelsSize.height);
        int monthLabelWidth = panelWidth / this.monthsOfYear.length;
        monthNameLabel.setBounds(monthLabelX, monthLabelY, monthLabelWidth, this.maximumMonthLabelsSize.height);
        monthNameLabel.setVisible(true);
    }

    private static Dimension computeMaximumMonthLabelsSize(String monthsOfYear[]) {
        JLabel label = new JLabel(monthsOfYear[0]);
        Dimension firstSize = label.getPreferredSize();
        int maximumHeight = firstSize.height;
        int maximumWidth = firstSize.width;
        for (int i=1; i<monthsOfYear.length; i++) {
            label.setText(monthsOfYear[i]);
            Dimension size = label.getPreferredSize();
            if (maximumHeight < size.height) {
                maximumHeight = size.height;
            }
            if (maximumWidth < size.width) {
                maximumWidth = size.width;
            }
        }
        return new Dimension(maximumWidth, maximumHeight);
    }
}
