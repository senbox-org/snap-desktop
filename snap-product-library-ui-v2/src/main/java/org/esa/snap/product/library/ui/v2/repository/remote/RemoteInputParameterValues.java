package org.esa.snap.product.library.ui.v2.repository.remote;

import java.util.Map;

/**
 * The parameters data for a remote repository.
 *
 * Created by jcoravu on 6/2/2020.
 */
public class RemoteInputParameterValues {

    private final Map<String, Object> parameterValues;
    private final String missionName;

    public RemoteInputParameterValues(Map<String, Object> parameterValues, String missionName) {
        this.parameterValues = parameterValues;
        this.missionName = missionName;
    }

    public String getMissionName() {
        return missionName;
    }

    public Object getParameterValue(String parameterName) {
        return this.parameterValues.get(parameterName);
    }
}
