package org.esa.snap.ui.product;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.TiePointGrid;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

interface BandChoosingStrategy {

    Band[] getSelectedBands();

    TiePointGrid[] getSelectedTiePointGrids();

    JPanel createCheckersPane();

    void updateCheckBoxStates();

    void setCheckBoxes(JCheckBox selectAllCheckBox, JCheckBox selectNoneCheckBox);

    void selectAll();

    void selectNone();

    boolean atLeastOneBandSelected();

    void selectRasterDataNodes(String[] nodeNames);

}
