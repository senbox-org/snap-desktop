package org.esa.snap.ui.product;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.TiePointGrid;

import javax.swing.*;

interface BandChoosingStrategy {

    Band[] getSelectedBands();

    TiePointGrid[] getSelectedTiePointGrids();

    JPanel createCheckersPane();

    void updateCheckBoxStates();

    void setCheckBoxes(JCheckBox selectAllCheckBox, JCheckBox selectNoneCheckBox);

    void setAdvancedCheckBoxes(JCheckBox selectAllCheckBox, JCheckBox selectNoneCheckBox, JCheckBox selectBandCheck);

    void selectAll();

    void selectNone();

    void selectBandGroup(String selectedBandGroupName);

    boolean atLeastOneBandSelected();

    void selectRasterDataNodes(String[] nodeNames);

}
