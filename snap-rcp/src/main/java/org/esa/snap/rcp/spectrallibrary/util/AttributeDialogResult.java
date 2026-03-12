package org.esa.snap.rcp.spectrallibrary.util;

import org.esa.snap.speclib.model.AttributeType;


public class AttributeDialogResult {

    final String key;
    final AttributeType type;
    final String defaultValueText;


    public AttributeDialogResult(String key, AttributeType type, String defaultValueText) {
        this.key = key;
        this.type = type;
        this.defaultValueText = defaultValueText;
    }
}
