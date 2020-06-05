package org.esa.snap.product.library.ui.v2.repository.timeline;

import java.util.Calendar;
import java.util.List;

public abstract class MultipleYearDaysTimelineHelper extends AbstractMultipleYearsTimelineHelper {

    public MultipleYearDaysTimelineHelper(List<YearLabel> monthBarsByYear, String monthsOfYear[]) {
        super(monthBarsByYear, monthsOfYear);
    }

    @Override
    protected String buildTooltipText(String monthsOfYear[], TimelineBarComponent barComponent) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, barComponent.getYearLabel().getYear());
        calendar.set(Calendar.DAY_OF_YEAR, barComponent.getId());
        int month = calendar.get(Calendar.MONTH);
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
        return dayOfMonth + " " + monthsOfYear[month] + " " + barComponent.getYearLabel().getYear() + ": " + barComponent.getProductCount();
    }

    @Override
    protected int getDefaultBarCountPerYear() {
        return 366;
    }
}
