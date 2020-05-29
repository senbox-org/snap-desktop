package org.esa.snap.product.library.ui.v2.repository.timeline;

import org.esa.snap.product.library.ui.v2.repository.output.OutputProductResults;

import javax.swing.*;
import java.awt.*;
import java.util.*;

/**
 * Created by jcoravu on 18/3/2020.
 */
public class RepositoryProductsTimelinePanel extends JPanel {

    private final int preferredHeight;
    private final String monthOfYear[];
    private final Color barColor;

    public RepositoryProductsTimelinePanel() {
        super();

        this.preferredHeight = 40;

        this.barColor = new Color(7, 60, 102);

        this.monthOfYear = new String[12];
        this.monthOfYear[0] = "January";
        this.monthOfYear[1] = "February";
        this.monthOfYear[2] = "March";
        this.monthOfYear[3] = "April";
        this.monthOfYear[4] = "May";
        this.monthOfYear[5] = "June";
        this.monthOfYear[6] = "July";
        this.monthOfYear[7] = "August";
        this.monthOfYear[8] = "September";
        this.monthOfYear[9] = "October";
        this.monthOfYear[10] = "November";
        this.monthOfYear[11] = "December";

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

        int componentCount = getComponentCount();
        if (componentCount > 0) {
            int maximumProductCount = ((MonthBarComponent)getComponent(0)).getProductCount();
            for (int i=1; i<componentCount; i++) {
                MonthBarComponent monthBarComponent = (MonthBarComponent)getComponent(i);
                maximumProductCount = Math.max(maximumProductCount, monthBarComponent.getProductCount());
            }
            int maximumMonthBarHeight = getHeight();
            int gapBetweenBars = 10;
            int monthBarWidth = computeMonthBarHeight(componentCount, getWidth(), gapBetweenBars);
            if (monthBarWidth <= gapBetweenBars) {
                gapBetweenBars = gapBetweenBars / 2;
                monthBarWidth = computeMonthBarHeight(componentCount, getWidth(), gapBetweenBars);
            }
            int x = 0;
            for (int i=0; i<componentCount; i++) {
                MonthBarComponent monthBarComponent = (MonthBarComponent)getComponent(i);
                if (i == componentCount - 1) {
                    monthBarWidth = getWidth() - x;
                }
                float percent = (float)monthBarComponent.getProductCount() / (float)maximumProductCount;
                int monthBarHeight = (int)(percent * maximumMonthBarHeight);
                int y = maximumMonthBarHeight - monthBarHeight;
                monthBarComponent.setBounds(x, y, monthBarWidth, monthBarHeight);
                x += monthBarWidth + gapBetweenBars;
            }
        }
    }

    public void refresh(OutputProductResults outputProductResults) {
        java.util.List<MonthBarComponent> monthBars = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        for (int i=0; i<outputProductResults.getAvailableProductCount(); i++) {
            Date acquisitionDate = outputProductResults.getProductAt(i).getAcquisitionDate();
            if (acquisitionDate != null) {
                calendar.setTimeInMillis(acquisitionDate.getTime());
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH);

                MonthBarComponent foundMonthBarComponent = null;
                for (int k=0; k<monthBars.size() && foundMonthBarComponent == null; k++) {
                    MonthBarComponent monthBarComponent = monthBars.get(k);
                    if (monthBarComponent.getYear() == year && monthBarComponent.getMonth() == month) {
                        foundMonthBarComponent = monthBarComponent;
                    }
                }
                if (foundMonthBarComponent == null) {
                    foundMonthBarComponent = new MonthBarComponent(year, month);
                    foundMonthBarComponent.setBackground(this.barColor);
                    foundMonthBarComponent.setOpaque(true);
                    monthBars.add(foundMonthBarComponent);
                }
                foundMonthBarComponent.incrementProductCount();
            }
        }
        if (monthBars.size() > 1) {
            Comparator<MonthBarComponent> comparator = new Comparator<MonthBarComponent>() {
                @Override
                public int compare(MonthBarComponent leftItem, MonthBarComponent rightItem) {
                    int result = Integer.compare(leftItem.getYear(), rightItem.getYear());
                    if (result == 0) {
                        result = Integer.compare(leftItem.getMonth(), rightItem.getMonth());
                    }
                    return result;
                }
            };
            Collections.sort(monthBars, comparator); // sort ascending
        }

        removeAll();
        for (int i=0; i<monthBars.size(); i++) {
            MonthBarComponent monthBarComponent = monthBars.get(i);
            String monthName = this.monthOfYear[monthBarComponent.getMonth()];
            String toolTipText = monthName + " " + Integer.toString(monthBarComponent.getYear()) + ": " + monthBarComponent.getProductCount();
            monthBarComponent.setToolTipText(toolTipText);
            add(monthBarComponent);
        }
        Container parent = getParent();
        if (parent != null) {
            parent.revalidate();
            parent.repaint();
        }
        boolean timelineVisible = (monthBars.size() > 0);
        setVisible(timelineVisible);
    }

    private static class MonthBarComponent extends JLabel {

        private final int year;
        private final int month;

        private int productCount;

        private MonthBarComponent(int year, int month) {
            this.year = year;
            this.month = month;
            this.productCount = 0;
        }

        public void incrementProductCount() {
            this.productCount++;
        }

        public int getYear() {
            return year;
        }

        public int getMonth() {
            return month;
        }

        public int getProductCount() {
            return productCount;
        }
    }

    private static int computeMonthBarHeight(int totalMonthBarCount, int availableWidth, int gapBetweenBars) {
        int totalGapCount = totalMonthBarCount - 1;
        int remainingWidth = availableWidth - (totalGapCount * gapBetweenBars);
        return remainingWidth / totalMonthBarCount;
    }
}
