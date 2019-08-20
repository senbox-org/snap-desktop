package org.esa.snap.product.library.ui.v2;

/**
 * Created by jcoravu on 16/8/2019.
 */
public interface IFilterItems<ItemType, FilterValueType> {

    public boolean matches(ItemType item, FilterValueType filterValue);
}
