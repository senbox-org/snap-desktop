package org.esa.snap.binning.operator.ui;

import org.esa.snap.binning.operator.VariableConfig;

class VariableItem {

    VariableConfig variableConfig;

    VariableItem() {
        this.variableConfig = new VariableConfig();
    }

    VariableItem(VariableConfig variableConfig) {
        this.variableConfig = variableConfig;
    }
}
