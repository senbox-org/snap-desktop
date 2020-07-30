package org.esa.snap.product.library.ui.v2.repository.timeline;

import javax.swing.*;

/**
 * The label to represent a bar from the timeline.
 */
public class TimelineBarComponent extends JLabel {

    private final YearLabel yearLabel;
    private final int id;

    private int productCount;

    public TimelineBarComponent(YearLabel yearLabel, int id) {
        this.yearLabel = yearLabel;
        this.id = id;

        this.productCount = 0;
    }

    public void incrementProductCount() {
        this.productCount++;
    }

    public int getId() {
        return id;
    }

    public int getProductCount() {
        return productCount;
    }

    public YearLabel getYearLabel() {
        return yearLabel;
    }
}
