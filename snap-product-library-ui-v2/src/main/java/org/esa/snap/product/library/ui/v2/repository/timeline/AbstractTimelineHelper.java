package org.esa.snap.product.library.ui.v2.repository.timeline;

import javax.swing.*;
import java.util.*;

/**
 * The base class to layout the bar components of a timeline.
 */
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
        int barWidth = yearLabelsHeight; // the default bar width value

        int maximumBarWidth = (yearLabelsWidth / barCountPerYear) - minimumGapBetweenBars;
        if (maximumBarWidth < barWidth) {
            // recompute the values
            barCountPerYear = computeMaximumBarCountPerYear(); // recompute the bar count per year
            if (barCountPerYear <= 0 || barCountPerYear > defaultBarCountPerYear) {
                throw new IllegalStateException("Invalid values: barCountPerYear="+barCountPerYear+", defaultBarCountPerYear=" + defaultBarCountPerYear + ".");
            }
            maximumBarWidth = (yearLabelsWidth / barCountPerYear) - minimumGapBetweenBars;
            if (maximumBarWidth < barWidth) {
                barWidth = maximumBarWidth; // update the bar width
            }
        }

        int monthBarSegmentWidth = yearLabelsWidth / barCountPerYear;
        int monthBarSegmentRemainingWidth = yearLabelsWidth % barCountPerYear;
        int barSegmentsX[] = new int[barCountPerYear];
        barSegmentsX[0] = 0; // the first bar has x = 0
        for (int i=1; i<barCountPerYear; i++) {
            barSegmentsX[i] = barSegmentsX[i - 1] + monthBarSegmentWidth;
            if (i > 0 && monthBarSegmentRemainingWidth > 0) {
                monthBarSegmentRemainingWidth--;
                barSegmentsX[i]++; // add one pixel from the remaining width to each bar left offset
            }
        }
        int monthBarOffsetX = (monthBarSegmentWidth - barWidth) / 2;

        // iterate the years
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
            throw new IllegalStateException("The bar count " + yearLabel.getTimelineBarCount() + " is greater than the bar count per year " + barCountPerYear + ".");
        } else {
            Map<TimelineBarComponent, Integer> visibleBarIndicesMap = (yearLabel.getTimelineBarCount() > 0) ? new LinkedHashMap<>() : Collections.emptyMap();
            for (int k = 0; k < yearLabel.getTimelineBarCount(); k++) {
                TimelineBarComponent barComponent = yearLabel.getTimelineBarAt(k);
                if (barComponent.getId() < 0) {
                    throw new IllegalStateException("The bar component id " + barComponent.getId() +" is negative.");
                } else if (barComponent.getId() >= defaultBarCountPerYear) {
                    throw new IllegalStateException("The bar component id " + barComponent.getId() +" is greater or equal than " + defaultBarCountPerYear + ".");
                } else {
                    int monthBarHeight = computeBarHeight(barComponent.getProductCount(), maximumProductCountPerBar, maximumBarHeight);
                    int barY = maximumBarHeight - monthBarHeight;
                    int barX;
                    int segmentBarIndex;
                    if (barCountPerYear == defaultBarCountPerYear) {
                        segmentBarIndex = barComponent.getId();
                        barX = yearLabel.getX() + barSegmentsX[barComponent.getId()] + monthBarOffsetX;
                    } else {
                        segmentBarIndex = (barComponent.getId() * barCountPerYear) / defaultBarCountPerYear;
                        barX = yearLabel.getX() + barSegmentsX[segmentBarIndex] + monthBarOffsetX;
                        if (k > 0) {
                            // recompute the bar position on the X axis
                            TimelineBarComponent previousBarComponent = yearLabel.getTimelineBarAt(k - 1);
                            while (segmentBarIndex < barCountPerYear && previousBarComponent.getX() >= barX) {
                                segmentBarIndex++;
                                if (segmentBarIndex < barCountPerYear) {
                                    barX = yearLabel.getX() + barSegmentsX[segmentBarIndex] + monthBarOffsetX;
                                }
                            }
                            // check if the bar to display is the last last
                            if (segmentBarIndex == barCountPerYear) {
                                // move the bars one position to the left
                                for (Map.Entry<TimelineBarComponent, Integer> entry : visibleBarIndicesMap.entrySet()) {
                                    TimelineBarComponent addedBarComponent = entry.getKey();
                                    int fromBarSegmentIndex = entry.getValue().intValue();
                                    if (fromBarSegmentIndex < 0) {
                                        throw new IllegalStateException("Invalid value: fromBarSegmentIndex="+fromBarSegmentIndex+".");
                                    } else if (fromBarSegmentIndex > 0) {
                                        int toBarSegmentIndex = fromBarSegmentIndex - 1; // decrement by one the index
                                        int addedBarX = yearLabel.getX() + barSegmentsX[toBarSegmentIndex] + monthBarOffsetX;
                                        beforeMoveLayoutBarToLeft(panelX, addedBarComponent.getId(), fromBarSegmentIndex, toBarSegmentIndex, barSegmentsX);
                                        addedBarComponent.setLocation(addedBarX, addedBarComponent.getY());
                                        entry.setValue(toBarSegmentIndex);
                                    }
                                }
                                segmentBarIndex = barCountPerYear - 1; // the last bar index
                                barX = yearLabel.getX() + barSegmentsX[segmentBarIndex] + monthBarOffsetX;
                            }
                        }
                    }

                    visibleBarIndicesMap.put(barComponent, segmentBarIndex);

                    beforeLayoutBar(panelX, panelY, panelWidth, maximumBarHeight, barComponent.getId(), segmentBarIndex, barSegmentsX);
                    barComponent.setBounds(barX, barY, barWidth, monthBarHeight);
                }
            }

            afterLayoutBars(panelX, panelY, panelWidth, maximumBarHeight, defaultBarCountPerYear, barCountPerYear, barSegmentsX, visibleBarIndicesMap);
        }
    }

    protected void afterLayoutBars(int panelX, int panelY, int panelWidth, int maximumBarHeight, int defaultBarCountPerYear, int barCountPerYear,
                                   int[] barSegmentsX, Map<TimelineBarComponent, Integer> visibleBarIndicesMap) {
    }

    protected void beforeLayoutBar(int panelX, int panelY, int panelWidth, int maximumBarHeight, int monthBarId, int barIndex, int[] barSegmentsX) {
    }

    protected void beforeMoveLayoutBarToLeft(int panelX, int monthBarId, int fromBarSegmentIndex, int toBarSegmentIndex, int[] barSegmentsX) {
    }

    private int computeMaximumBarCountPerYear() {
        int maximumBarCount = 0;
        for (int i = 0; i < this.yearLabels.size(); i++) {
            YearLabel yearLabel = this.yearLabels.get(i);
            if (yearLabel.getTimelineBarCount() > 0 && maximumBarCount < yearLabel.getTimelineBarCount()) {
                maximumBarCount = yearLabel.getTimelineBarCount();
            }
        }
        return maximumBarCount;
    }

    private int computeMaximumProductCountPerBar() {
        int maximumProductCountPerBar = 0;
        for (int i = 0; i<this.yearLabels.size(); i++) {
            int productCountPerMonth = this.yearLabels.get(i).computeMaximumProductCount();
            if (maximumProductCountPerBar < productCountPerMonth) {
                maximumProductCountPerBar = productCountPerMonth;
            }
        }
        return maximumProductCountPerBar;
    }

    private static int computeBarHeight(int barProductCount, int maximumProductCountPerBar, int maximumMonthBarHeight) {
        if (barProductCount < 0) {
            throw new IllegalArgumentException("The bar product count is negative: " + barProductCount + ".");
        }
        if (maximumProductCountPerBar < 0) {
            throw new IllegalArgumentException("The maximum product count per bar is negative: " + maximumProductCountPerBar + ".");
        }
        if (maximumMonthBarHeight < 0) {
            throw new IllegalArgumentException("The maximum month bar height is negative: " + maximumMonthBarHeight + ".");
        }
        if (maximumProductCountPerBar > 0) {
            float barHeightPercent = (float) barProductCount / (float) maximumProductCountPerBar;
            return (int) (barHeightPercent * maximumMonthBarHeight);
        }
        return 0;
    }
}
