package org.esa.snap.product.library.ui.v2.repository.timeline;

import javax.swing.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractTimelineHelper {

    protected final List<YearLabel> yearLabels;

    protected AbstractTimelineHelper() {
        this.yearLabels = new ArrayList<>();
    }

    protected abstract void addComponentToPanel(JComponent componentToAdd);

    public abstract void doLayout(int panelX, int panelY, int panelWidth, int panelHeight);

    protected final void doYearBarsLayout(int panelX, int panelY, int panelWidth, int panelHeight,
                                          int yearLabelsWidth, int yearLabelsHeight, int defaultBarCountPerYear) {

        int maximumProductCountPerBar = computeMaximumProductCountPerBar();
        int maximumBarHeight = panelHeight - yearLabelsHeight;

        int barCountPerYear = defaultBarCountPerYear;
        int minimumGapBetweenBars = 2; // the gap between bars should be an even number
        int barWidth = yearLabelsHeight;

        int maximumBarWidth = (yearLabelsWidth / barCountPerYear) - minimumGapBetweenBars;
        if (maximumBarWidth < barWidth) {
            barCountPerYear = computeMaximumBarCountPerYear();
            if (barCountPerYear > defaultBarCountPerYear) {
                throw new IllegalStateException("Wrong barCountPerYear="+barCountPerYear+".");
            }
            maximumBarWidth = (yearLabelsWidth / barCountPerYear) - minimumGapBetweenBars;
            if (maximumBarWidth < barWidth) {
                barWidth = maximumBarWidth;
            }
        }

        int monthBarSegmentWidth = yearLabelsWidth / barCountPerYear;
        int monthBarSegmentRemainingWidth = yearLabelsWidth % barCountPerYear;
        int barSegmentsX[] = new int[barCountPerYear];
        barSegmentsX[0] = 0;
        for (int i=1; i<barCountPerYear; i++) {
            barSegmentsX[i] = barSegmentsX[i - 1] + monthBarSegmentWidth;
            if (i > 0 && monthBarSegmentRemainingWidth > 0) {
                monthBarSegmentRemainingWidth--;
                barSegmentsX[i]++;
            }
        }
        int monthBarOffsetX = (monthBarSegmentWidth - barWidth) / 2;

        for (int i = 0; i < this.yearLabels.size(); i++) {
            YearLabel yearLabel = this.yearLabels.get(i);
            doYearBarsLayout(panelX, panelY, panelWidth, yearLabel, defaultBarCountPerYear, barCountPerYear, maximumProductCountPerBar,
                             maximumBarHeight, barSegmentsX, barWidth, monthBarOffsetX);
        }
    }

    private void doYearBarsLayout(int panelX, int panelY, int panelWidth, YearLabel yearLabel,
                                    int defaultBarCountPerYear, int barCountPerYear, int maximumProductCountPerBar,
                                    int maximumBarHeight, int[] barSegmentsX, int barWidth, int monthBarOffsetX) {

        if (yearLabel.getTimelineBarCount() > barCountPerYear) {
            throw new IllegalStateException("The month bar count " + yearLabel.getTimelineBarCount() + " is greater than the bar count per year " + barCountPerYear + ".");
        } else {
            LinkedHashMap<TimelineBarComponent, Integer> monthBarIndecesMap = new LinkedHashMap<>();
            for (int k = 0; k < yearLabel.getTimelineBarCount(); k++) {
                TimelineBarComponent monthBarComponent = yearLabel.getTimelineBarAt(k);
                if (monthBarComponent.getId() >= defaultBarCountPerYear) {
                    throw new IllegalStateException("The month bar id " + monthBarComponent.getId() +" is greater or equal than " + defaultBarCountPerYear + ".");
                } else {
                    int monthBarHeight = computeBarHeight(monthBarComponent.getProductCount(), maximumProductCountPerBar, maximumBarHeight);
                    int barY = maximumBarHeight - monthBarHeight;
                    int barX;
                    int segmentBarIndex;
                    if (barCountPerYear == defaultBarCountPerYear) {
                        segmentBarIndex = monthBarComponent.getId();
                        barX = yearLabel.getX() + barSegmentsX[monthBarComponent.getId()] + monthBarOffsetX;
                    } else {
                        segmentBarIndex = (monthBarComponent.getId() * barCountPerYear) / defaultBarCountPerYear;
                        barX = yearLabel.getX() + barSegmentsX[segmentBarIndex] + monthBarOffsetX;
                        if (k > 0) {
                            TimelineBarComponent previousMonthBarComponent = yearLabel.getTimelineBarAt(k - 1);
                            while (segmentBarIndex < barCountPerYear && previousMonthBarComponent.getX() >= barX) {
                                segmentBarIndex++;
                                if (segmentBarIndex < barCountPerYear) {
                                    barX = yearLabel.getX() + barSegmentsX[segmentBarIndex] + monthBarOffsetX;
                                }
                            }
                            if (segmentBarIndex == barCountPerYear) {
                                // move the bars one position to the left
                                for (Map.Entry<TimelineBarComponent, Integer> entry : monthBarIndecesMap.entrySet()) {
                                    TimelineBarComponent addedMonthBarComponent = entry.getKey();
                                    int fromBarSegmentIndex = entry.getValue().intValue();
                                    int toBarSegmentIndex = fromBarSegmentIndex - 1; // decrement by one the index
                                    int addedBarX = yearLabel.getX() + barSegmentsX[toBarSegmentIndex] + monthBarOffsetX;
                                    beforeMoveLayoutBarToLeft(panelX, addedMonthBarComponent.getId(), fromBarSegmentIndex, toBarSegmentIndex, barSegmentsX);
                                    addedMonthBarComponent.setLocation(addedBarX, addedMonthBarComponent.getY());
                                    entry.setValue(toBarSegmentIndex);
                                }
                                segmentBarIndex = barCountPerYear - 1; // the last bar index
                                barX = yearLabel.getX() + barSegmentsX[segmentBarIndex] + monthBarOffsetX;
                            }
                        }
                    }
                    monthBarIndecesMap.put(monthBarComponent, segmentBarIndex);

                    beforeLayoutBar(panelX, panelY, panelWidth, maximumBarHeight, monthBarComponent.getId(), segmentBarIndex, barSegmentsX);
                    monthBarComponent.setBounds(barX, barY, barWidth, monthBarHeight);
                }
            }

            afterLayoutBars(panelX, panelY, panelWidth, maximumBarHeight, defaultBarCountPerYear, barCountPerYear, barSegmentsX, monthBarIndecesMap);
        }
    }

    protected void afterLayoutBars(int panelX, int panelY, int panelWidth, int maximumBarHeight, int defaultBarCountPerYear, int barCountPerYear,
                                   int[] barSegmentsX, LinkedHashMap<TimelineBarComponent, Integer> monthBarIndecesMap) {
    }

    protected void beforeLayoutBar(int panelX, int panelY, int panelWidth, int maximumBarHeight, int monthBarId, int barIndex, int[] barSegmentsX) {
    }

    protected void beforeMoveLayoutBarToLeft(int panelX, int monthBarId, int fromBarSegmentIndex, int toBarSegmentIndex, int[] barSegmentsX) {
    }


    protected int computeMaximumBarCountPerYear() {
        int maximumBarCount = 0;
        for (int i = 0; i < this.yearLabels.size(); i++) {
            YearLabel yearLabel = this.yearLabels.get(i);
            if (yearLabel.getTimelineBarCount() > 0 && maximumBarCount < yearLabel.getTimelineBarCount()) {
                maximumBarCount = yearLabel.getTimelineBarCount();
            }
        }
        return maximumBarCount;
    }

    protected int computeMaximumProductCountPerBar() {
        int maximumProductCountPerBar = this.yearLabels.get(0).computeMaximumProductCount();
        for (int i = 1; i<this.yearLabels.size(); i++) {
            int productCountPerMonth = this.yearLabels.get(i).computeMaximumProductCount();
            if (maximumProductCountPerBar < productCountPerMonth) {
                maximumProductCountPerBar = productCountPerMonth;
            }
        }
        return maximumProductCountPerBar;
    }

    protected static int computeBarHeight(int barProductCount, int maximumProductCountPerBar, int maximumMonthBarHeight) {
        float barHeightPercent = (float) barProductCount / (float) maximumProductCountPerBar;
        return (int) (barHeightPercent * maximumMonthBarHeight);
    }
}
