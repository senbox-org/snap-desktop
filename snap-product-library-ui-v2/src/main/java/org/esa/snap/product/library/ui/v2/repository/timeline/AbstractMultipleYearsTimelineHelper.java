package org.esa.snap.product.library.ui.v2.repository.timeline;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * The class computes the product count for several years.
 */
public abstract class AbstractMultipleYearsTimelineHelper extends AbstractTimelineHelper {

    protected static final String SEPARATOR_LABEL_KEY = "SeparatorLabelKey";

    protected AbstractMultipleYearsTimelineHelper(List<YearLabel> monthBarsByYear, String monthsOfYear[]) {
        int miminumYear = monthBarsByYear.get(0).getYear();
        int maximumYear = miminumYear;
        for (int i=0; i<monthBarsByYear.size(); i++) {
            YearLabel yearLabel = monthBarsByYear.get(i);
            this.yearLabels.add(yearLabel);
            if (miminumYear > yearLabel.getYear()) {
                miminumYear = yearLabel.getYear();
            }
            if (maximumYear < yearLabel.getYear()) {
                maximumYear = yearLabel.getYear();
            }
        }

        Comparator<TimelineBarComponent> barsComparator = new Comparator<TimelineBarComponent>() {
            @Override
            public int compare(TimelineBarComponent leftItem, TimelineBarComponent rightItem) {
                return Integer.compare(leftItem.getId(), rightItem.getId());
            }
        };

        for (int year = miminumYear; year<=maximumYear; year++) {
            YearLabel foundYear = null;
            for (int k=0; k<this.yearLabels.size() && foundYear==null; k++) {
                YearLabel existingYearLabel = monthBarsByYear.get(k);
                if (year == existingYearLabel.getYear()) {
                    foundYear = existingYearLabel;
                }
            }
            if (foundYear == null) {
                foundYear = new YearLabel(year);
                this.yearLabels.add(foundYear);
            }
            foundYear.sortBars(barsComparator);
            addComponentToPanel(foundYear);

            if (year > miminumYear) {
                JLabel yearSeparatorLabel = new JLabel();
                yearSeparatorLabel.setBackground(Color.BLACK);
                yearSeparatorLabel.setOpaque(true);
                foundYear.putClientProperty(SEPARATOR_LABEL_KEY, yearSeparatorLabel);
                addComponentToPanel(yearSeparatorLabel);
            }

            for (int i = 0; i<foundYear.getTimelineBarCount(); i++) {
                TimelineBarComponent barComponent = foundYear.getTimelineBarAt(i);
                String toolTipText = buildTooltipText(monthsOfYear, barComponent);
                barComponent.setToolTipText(toolTipText);
                addComponentToPanel(barComponent);
            }
        }
        Comparator<YearLabel> yearsComparator = new Comparator<YearLabel>() {
            @Override
            public int compare(YearLabel leftItem, YearLabel rightItem) {
                return Integer.compare(leftItem.getYear(), rightItem.getYear());
            }
        };
        Collections.sort(this.yearLabels, yearsComparator); // sort ascending by year
    }

    protected abstract String buildTooltipText(String monthsOfYear[], TimelineBarComponent barComponent);

    protected abstract int getDefaultBarCountPerYear();

    @Override
    public final void doLayout(int panelX, int panelY, int panelWidth, int panelHeight) {
        int yearLabelsHeight = computeMaximumYearLabelsHeight();

        int yearCount = this.yearLabels.size();
        int gapBetweenYearLabels = 1;
        int yearLabelsWidth = (panelWidth - (gapBetweenYearLabels * (yearCount - 1))) / yearCount;
        int separatorHeight = panelHeight - yearLabelsHeight;

        int yearLabelX = panelX;
        int yearLabelY = panelY + (panelHeight - yearLabelsHeight);
        for (int i = 0; i < yearCount; i++) {
            YearLabel yearLabel = this.yearLabels.get(i);
            yearLabel.setBounds(yearLabelX, yearLabelY, yearLabelsWidth, yearLabelsHeight);
            yearLabelX += yearLabelsWidth + gapBetweenYearLabels;

            JLabel yearSeparatorLabel = (JLabel) yearLabel.getClientProperty(SEPARATOR_LABEL_KEY);
            if (yearSeparatorLabel != null) {
                yearSeparatorLabel.setBounds(yearLabel.getX() - 1, 0, gapBetweenYearLabels, separatorHeight);
            }
        }

        doYearBarsLayout(panelX, panelY, panelWidth, panelHeight, yearLabelsWidth, yearLabelsHeight, getDefaultBarCountPerYear());
    }

    private int computeMaximumYearLabelsHeight() {
        int maximumHeight = this.yearLabels.get(0).getPreferredSize().height;
        for (int i = 1; i<this.yearLabels.size(); i++) {
            int height = this.yearLabels.get(i).getPreferredSize().height;
            if (maximumHeight < height) {
                maximumHeight = height;
            }
        }
        return maximumHeight;
    }
}
