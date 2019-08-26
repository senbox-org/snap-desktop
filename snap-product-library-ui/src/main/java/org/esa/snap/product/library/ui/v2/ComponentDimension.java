package org.esa.snap.product.library.ui.v2;

import java.awt.Insets;

/**
 * Created by jcoravu on 26/8/2019.
 */
public interface ComponentDimension {

    public Insets getListItemMargins();

    public int getGapBetweenRows();

    public int getGapBetweenColumns();

    public int getTextFieldPreferredHeight();
}
