package org.esa.snap.product.library.ui.v2;

import java.awt.*;

/**
 * The interface containing to retrieve information about a component displayed in a panel.
 *
 * Created by jcoravu on 26/8/2019.
 */
public interface ComponentDimension {

    public int getGapBetweenRows();

    public int getGapBetweenColumns();

    public int getTextFieldPreferredHeight();

    public Color getTextFieldBackgroundColor();
}
