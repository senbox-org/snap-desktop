package org.esa.snap.ui.product.spectrum;

import com.jidesoft.swing.TristateCheckBox;

import java.util.ArrayList;
import java.util.List;

class SpectrumSelectionAdmin {

    private final List<List<BandSelectionState>> bandSelectionStates;
    private final List<Integer> numbersOfSelectedBands;
    private final List<Integer> currentStates;

    SpectrumSelectionAdmin() {
        bandSelectionStates = new ArrayList<List<BandSelectionState>>();
        numbersOfSelectedBands = new ArrayList<Integer>();
        currentStates = new ArrayList<Integer>();
    }

    void evaluateSpectrumSelections(DisplayableSpectrum spectrum) {
        List<BandSelectionState> selected = new ArrayList<BandSelectionState>();
        int numberOfSelectedBands = 0;
        for (int i = 0; i < spectrum.getSpectralBands().length; i++) {
            final boolean bandSelected = spectrum.isBandSelected(i);
            final BandSelectionState bandSelectionState = new BandSelectionState();
            bandSelectionState.setSelected(bandSelected);
            selected.add(bandSelectionState);
            if (bandSelected) {
                numberOfSelectedBands++;
            }
        }
        bandSelectionStates.add(selected);
        numbersOfSelectedBands.add(numberOfSelectedBands);

        currentStates.add(-1);
        final int index = bandSelectionStates.size() - 1;
        if (spectrum.isSelected()) {
            currentStates.set(index, TristateCheckBox.STATE_UNSELECTED);
        } else {
            currentStates.set(index, TristateCheckBox.STATE_SELECTED);
        }

        //evaluateState(bandSelectionStates.size() - 1);
    }

    boolean isBandSelected(int row, int i) {
        if (currentStates.get(row) == TristateCheckBox.STATE_MIXED) {
            final List<BandSelectionState> bst = bandSelectionStates.get(row);
            return bst.get(i).isSelected();
        } else return currentStates.get(row) == TristateCheckBox.STATE_SELECTED;
    }

    private void evaluateState(int index) {
        final Integer numberOfBands = numbersOfSelectedBands.get(index);
        if (numberOfBands == 0) {
            currentStates.set(index, TristateCheckBox.STATE_UNSELECTED);
        } else if (numberOfBands == bandSelectionStates.get(index).size()) {
            currentStates.set(index, TristateCheckBox.STATE_SELECTED);
        } else {
            //
            // currentStates.set(index, TristateCheckBox.STATE_MIXED);
            currentStates.set(index, TristateCheckBox.STATE_UNSELECTED);
        }
    }

    int getState(int index) {
        return currentStates.get(index);
    }

    boolean isSpectrumSelected(int row) {
        return currentStates.get(row) != TristateCheckBox.STATE_UNSELECTED;
    }

    void setBandSelected(int row, int bandRow, boolean selected) {
        if (isBandSelected(row, bandRow) != selected) {
            updateBandSelections(row, bandRow, selected);
            updateNumberOfSelectedBands(selected, row);
            //evaluateState(row);
        }
    }

    private void updateBandSelections(int row, int bandRow, boolean selected) {
        bandSelectionStates.get(row).get(bandRow).setSelected(selected);
        if (currentStates.get(row) != TristateCheckBox.STATE_MIXED) {
            for (int i = 0; i < bandSelectionStates.get(row).size(); i++) {
                if (i != bandRow) {
                    bandSelectionStates.get(row).get(i).setSelected(!selected);
                }
            }
        }
    }

    void updateSpectrumSelectionState(int row, int newState) {
        if (newState == TristateCheckBox.STATE_MIXED) {
            if (numbersOfSelectedBands.get(row) == bandSelectionStates.get(row).size() ||
                    numbersOfSelectedBands.get(row) == 0) {
                newState = TristateCheckBox.STATE_UNSELECTED;
            }
        }
        currentStates.set(row, newState);
    }

    private void updateNumberOfSelectedBands(Boolean selected, int row) {
        if (currentStates.get(row) == TristateCheckBox.STATE_MIXED) {
            if (selected) {
                numbersOfSelectedBands.set(row, numbersOfSelectedBands.get(row) + 1);
            } else {
                numbersOfSelectedBands.set(row, numbersOfSelectedBands.get(row) - 1);
            }
        } else {
            if (selected) {
                numbersOfSelectedBands.set(row, 1);
            } else {
                numbersOfSelectedBands.set(row, bandSelectionStates.get(row).size() - 1);
            }
        }
    }

}
