package org.esa.snap.rcp.imgfilter.model;

import java.io.IOException;

/**
 * @author Norman
 */
public interface FilterSetStore {
    void storeFilterSetModel(FilterSet filterSet) throws IOException;
}
