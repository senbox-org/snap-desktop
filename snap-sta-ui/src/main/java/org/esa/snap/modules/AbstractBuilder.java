package org.esa.snap.modules;

/**
 * Created by kraftek on 11/4/2016.
 */
public abstract class AbstractBuilder {

    public abstract String build();

    protected String safeValue(Object value) {
        return value != null ? value.toString() : "";
    }
}
