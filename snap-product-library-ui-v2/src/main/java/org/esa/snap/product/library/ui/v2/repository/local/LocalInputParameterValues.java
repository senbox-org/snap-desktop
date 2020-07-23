package org.esa.snap.product.library.ui.v2.repository.local;

import org.esa.snap.product.library.v2.database.model.LocalRepositoryFolder;

import java.util.Map;

/**
 * The data of the search parameters for a local repository.
 *
 * Created by jcoravu on 6/2/2020.
 */
public class LocalInputParameterValues {

    private final Map<String, Object> parameterValues;
    private final String missionName;
    private final LocalRepositoryFolder localRepositoryFolder;

    public LocalInputParameterValues(Map<String, Object> parameterValues, String missionName, LocalRepositoryFolder localRepositoryFolder) {
        this.parameterValues = parameterValues;
        this.missionName = missionName;
        this.localRepositoryFolder = localRepositoryFolder;
    }

    public String getMissionName() {
        return missionName;
    }

    public Object getParameterValue(String parameterName) {
        return this.parameterValues.get(parameterName);
    }

    public LocalRepositoryFolder getLocalRepositoryFolder() {
        return localRepositoryFolder;
    }
}
