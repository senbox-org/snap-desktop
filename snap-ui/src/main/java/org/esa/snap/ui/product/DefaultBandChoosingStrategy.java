package org.esa.snap.ui.product;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.ui.GridBagUtils;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class DefaultBandChoosingStrategy implements BandChoosingStrategy {

    // @todo 3 nf/se - see ProductSubsetDialog for a similar declarations  (code smell!)
    private static final Font SMALL_PLAIN_FONT = new Font("SansSerif", Font.PLAIN, 10);
    private static final Font SMALL_ITALIC_FONT = SMALL_PLAIN_FONT.deriveFont(Font.ITALIC);

    private Band[] allBands;
    private Band[] selectedBands;
    private TiePointGrid[] allTiePointGrids;
    private TiePointGrid[] selectedTiePointGrids;
    private boolean multipleProducts;
    private int numSelected;
    private JCheckBox[] checkBoxes;
    private JCheckBox selectAllCheckBox;
    private JCheckBox selectNoneCheckBox;

    public DefaultBandChoosingStrategy(Band[] allBands, Band[] selectedBands, TiePointGrid[] allTiePointGrids,
                                       TiePointGrid[] selectedTiePointGrids, boolean multipleProducts) {
        this.allBands = allBands;
        this.selectedBands = selectedBands;
        this.allTiePointGrids = allTiePointGrids;
        this.selectedTiePointGrids = selectedTiePointGrids;
        if (this.allBands == null) {
            this.allBands = new Band[0];
        }
        if (this.selectedBands == null) {
            this.selectedBands = new Band[0];
        }
        if (this.allTiePointGrids == null) {
            this.allTiePointGrids = new TiePointGrid[0];
        }
        if (this.selectedTiePointGrids == null) {
            this.selectedTiePointGrids = new TiePointGrid[0];
        }
        BandSorter.sort(allBands);
        this.multipleProducts = multipleProducts;
    }

    @Override
    public Band[] getSelectedBands() {
        checkSelectedBandsAndGrids();
        return selectedBands;
    }

    @Override
    public TiePointGrid[] getSelectedTiePointGrids() {
        checkSelectedBandsAndGrids();
        return selectedTiePointGrids;
    }

    @Override
    public JPanel createCheckersPane() {
        int length = 0;
        if (allBands != null) {
            length += allBands.length;
        }
        if (allTiePointGrids != null) {
            length += allTiePointGrids.length;
        }
        checkBoxes = new JCheckBox[length];
        final JPanel checkersPane = GridBagUtils.createPanel();
        final GridBagConstraints gbc = GridBagUtils.createConstraints("insets.left=4,anchor=NORTHWEST,fill=HORIZONTAL");
        final ActionListener checkListener = createActionListener();
        addBandCheckers(new StringBuffer(), checkersPane, gbc, checkListener);
        addTiePointCheckers(new StringBuffer(), checkersPane, gbc, checkListener);
        GridBagUtils.addVerticalFiller(checkersPane, gbc);
        return checkersPane;
    }

    private void addBandCheckers(final StringBuffer description, final JPanel checkersPane,
                                 final GridBagConstraints gbc, final ActionListener checkListener) {
        for (int i = 0; i < allBands.length; i++) {
            Band band = allBands[i];
            boolean checked = false;
            for (Band selectedBand : selectedBands) {
                if (band == selectedBand) {
                    checked = true;
                    numSelected++;
                    break;
                }
            }

            description.setLength(0);
            description.append(band.getDescription() == null ? "" : band.getDescription());
            if (band.getSpectralWavelength() > 0.0) {
                description.append(" (");
                description.append(band.getSpectralWavelength());
                description.append(" nm)");
            } else if (band.getDate() != null) {
                description.append(" (");
                description.append(band.getDate());
                description.append(")");
            }

            final JCheckBox check = new JCheckBox(getRasterDisplayName(band), checked);
            check.setFont(SMALL_PLAIN_FONT);
            check.addActionListener(checkListener);

            final JLabel label = new JLabel(description.toString());
            label.setFont(SMALL_ITALIC_FONT);

            gbc.gridy++;
            GridBagUtils.addToPanel(checkersPane, check, gbc, "weightx=0,gridx=0");
            GridBagUtils.addToPanel(checkersPane, label, gbc, "weightx=1,gridx=1");
            checkBoxes[i] = check;
        }
    }

    private void addTiePointCheckers(final StringBuffer description, final JPanel checkersPane,
                                     final GridBagConstraints gbc, final ActionListener checkListener) {
        for (int i = 0; i < allTiePointGrids.length; i++) {
            TiePointGrid grid = allTiePointGrids[i];
            boolean checked = false;
            for (TiePointGrid selectedGrid : selectedTiePointGrids) {
                if (grid == selectedGrid) {
                    checked = true;
                    numSelected++;
                    break;
                }
            }

            description.setLength(0);
            description.append(grid.getDescription() == null ? "" : grid.getDescription());

            final JCheckBox check = new JCheckBox(getRasterDisplayName(grid), checked);
            check.setFont(SMALL_PLAIN_FONT);
            check.addActionListener(checkListener);

            final JLabel label = new JLabel(description.toString());
            label.setFont(SMALL_ITALIC_FONT);

            gbc.gridy++;
            GridBagUtils.addToPanel(checkersPane, check, gbc, "weightx=0,gridx=0");
            GridBagUtils.addToPanel(checkersPane, label, gbc, "weightx=1,gridx=1");

            checkBoxes[i + allBands.length] = check;
        }
    }

    private String getRasterDisplayName(RasterDataNode rasterDataNode) {
        return multipleProducts ? rasterDataNode.getDisplayName() : rasterDataNode.getName();
    }

    private ActionListener createActionListener() {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final JCheckBox check = (JCheckBox) e.getSource();
                if (check.isSelected()) {
                    numSelected++;
                } else {
                    numSelected--;
                }
                updateCheckBoxStates();
            }
        };
    }

    public void updateCheckBoxStates() {
        selectAllCheckBox.setSelected(numSelected == checkBoxes.length);
        selectAllCheckBox.setEnabled(numSelected < checkBoxes.length);
        selectAllCheckBox.updateUI();
        selectNoneCheckBox.setSelected(numSelected == 0);
        selectNoneCheckBox.setEnabled(numSelected > 0);
        selectNoneCheckBox.updateUI();
    }

    @Override
    public void setCheckBoxes(JCheckBox selectAllCheckBox, JCheckBox selectNoneCheckBox) {
        this.selectAllCheckBox = selectAllCheckBox;
        this.selectNoneCheckBox = selectNoneCheckBox;
        updateCheckBoxStates();
    }


    @Override
    public void selectAll() {
        select(true);
    }

    @Override
    public void selectNone() {
        select(false);
    }

    @Override
    public boolean atLeastOneBandSelected() {
        checkSelectedBandsAndGrids();
        return selectedBands.length > 0;
    }

    @Override
    public void selectRasterDataNodes(String[] nodeNames) {
        for (int i = 0; i < allBands.length; i++) {
            Band band = allBands[i];
            for (String nodeName : nodeNames) {
                if (nodeName.equals(band.getName())) {
                    checkBoxes[i].setSelected(true);
                    numSelected++;
                    break;
                }
            }
        }
        for (int i = 0; i < allTiePointGrids.length; i++) {
            TiePointGrid grid = allTiePointGrids[i];
            for (String nodeName : nodeNames) {
                if (nodeName.equals(grid.getName())) {
                    checkBoxes[allBands.length + i].setSelected(true);
                    numSelected++;
                    break;
                }
            }
        }
        updateCheckBoxStates();
    }

    private void checkSelectedBandsAndGrids() {
        final List<Band> bands = new ArrayList<>();
        final List<TiePointGrid> grids = new ArrayList<>();
        for (int i = 0; i < checkBoxes.length; i++) {
            JCheckBox checkBox = checkBoxes[i];
            if (checkBox.isSelected()) {
                if (allBands.length > i) {
                    bands.add(allBands[i]);
                } else {
                    grids.add(allTiePointGrids[i - allBands.length]);
                }
            }
        }
        selectedBands = bands.toArray(new Band[bands.size()]);
        selectedTiePointGrids = grids.toArray(new TiePointGrid[grids.size()]);
    }

    private void select(boolean b) {
        for (JCheckBox checkBox : checkBoxes) {
            if (b && !checkBox.isSelected()) {
                numSelected++;
            }
            if (!b && checkBox.isSelected()) {
                numSelected--;
            }
            checkBox.setSelected(b);
        }
        updateCheckBoxStates();
    }

}
