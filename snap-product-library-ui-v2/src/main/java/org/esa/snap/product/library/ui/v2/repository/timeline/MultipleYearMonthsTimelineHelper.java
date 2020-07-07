package org.esa.snap.product.library.ui.v2.repository.timeline;

import java.util.List;

public abstract class MultipleYearMonthsTimelineHelper extends AbstractMultipleYearsTimelineHelper {

    public MultipleYearMonthsTimelineHelper(List<YearLabel> monthBarsByYear, String monthsOfYear[]) {
        super(monthBarsByYear, monthsOfYear);
    }

    @Override
    protected String buildTooltipText(String monthsOfYear[], TimelineBarComponent barComponent) {
        return buildMonthBarTooltipText(monthsOfYear, barComponent);
    }

    @Override
    protected int getDefaultBarCountPerYear() {
        return 12;
    }

    public static String buildMonthBarTooltipText(String monthsOfYear[], TimelineBarComponent barComponent) {
        return monthsOfYear[barComponent.getId()] + " " + barComponent.getYearLabel().getYear() + ": " + barComponent.getProductCount();
    }
}
