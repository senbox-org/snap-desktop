package org.esa.snap.classification.gpf.ui;

import org.esa.snap.classification.gpf.naivebayes.NaiveBayesClassifier;
import org.esa.snap.classification.gpf.naivebayes.NaiveBayesClassifierOp;

/**
 * User interface for NaiveBayesClassifierOp
 */
public class NaiveBayesClassifierOpUI extends BaseClassifierOpUI {

    public NaiveBayesClassifierOpUI() {
        super(NaiveBayesClassifierOp.CLASSIFIER_TYPE, false);
    }

    @Override
    protected String getClassifierFileExtension(){
        return NaiveBayesClassifier.CLASSIFIER_FILE_EXTENSION;
    }

    @Override
    protected void setEnabled(boolean enabled) {  }

}
