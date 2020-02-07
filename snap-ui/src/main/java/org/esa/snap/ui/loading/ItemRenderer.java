package org.esa.snap.ui.loading;

/**
 * Created by jcoravu on 7/2/2020.
 */
public interface ItemRenderer<ItemType> {

    public String getItemDisplayText(ItemType item);
}
