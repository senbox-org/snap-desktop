package org.esa.snap.ui.product.spectrum;

import com.bc.ceres.core.Assert;
import org.esa.snap.core.datamodel.Band;

import java.awt.Shape;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.List;

public class DisplayableSpectrum implements Spectrum {

    public final static String NO_UNIT = "";
    public final static String MIXED_UNITS = "mixed units";
    public final static String DEFAULT_SPECTRUM_NAME = "Bands";
    public final static String REMAINING_BANDS_NAME = "Other";

    private List<SpectrumBand> bands;
    private String name;
    private Stroke lineStyle;
    private int symbolIndex;
    private int symbolSize;
    private boolean isSelected;
    private String unit;

    public DisplayableSpectrum(String spectrumName, int symbolIndex) {
        this(spectrumName, new SpectrumBand[]{}, symbolIndex);
    }

    public DisplayableSpectrum(String spectrumName, SpectrumBand[] spectralBands, int symbolIndex) {
        this.name = spectrumName;
        bands = new ArrayList<SpectrumBand>(spectralBands.length);
        this.symbolIndex = symbolIndex;
        symbolSize = SpectrumShapeProvider.DEFAULT_SCALE_GRADE;
        unit = NO_UNIT;
        for (SpectrumBand spectralBand : spectralBands) {
            addBand(spectralBand);
        }
        setSelected(true);
    }

    public void addBand(SpectrumBand band) {
        Assert.notNull(band);
        bands.add(band);
        /*
        if(band.isSelected()) {
            setSelected(true);
        }*/
        updateUnit();
    }

    public Shape getScaledShape() {
        return SpectrumShapeProvider.getScaledShape(getSymbolIndex(), getSymbolSize());

    }

    public boolean isDefaultOrRemainingBandsSpectrum() {
        return isRemainingBandsSpectrum() || name.equals(DEFAULT_SPECTRUM_NAME);
    }

    public boolean isRemainingBandsSpectrum() {
        return name.equals(REMAINING_BANDS_NAME);
    }

    public boolean hasBands() {
        return !bands.isEmpty();
    }

    public boolean hasSelectedBands() {
        for (SpectrumBand band : bands) {
            if (band.isSelected()) {
                return true;
            }
        }
        return false;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Band[] getSpectralBands() {
        Band[] spectralBands = new Band[bands.size()];
        for (int i = 0; i < bands.size(); i++) {
            spectralBands[i] = bands.get(i).getOriginalBand();
        }
        return spectralBands;
    }

    public Band[] getSelectedBands() {
        List<Band> selectedBands = new ArrayList<Band>();
        for (SpectrumBand band : bands) {
            if (band.isSelected()) {
                selectedBands.add(band.getOriginalBand());
            }
        }
        return selectedBands.toArray(new Band[selectedBands.size()]);
    }

    public void setBandSelected(int index, boolean selected) {
        bands.get(index).setSelected(selected);
    }

    public boolean isBandSelected(int index) {
        return bands.get(index).isSelected();
    }

    public Stroke getLineStyle() {
        if (isRemainingBandsSpectrum()) {
            return SpectrumStrokeProvider.EMPTY_STROKE;
        }
        return lineStyle;
    }

    public void setLineStyle(Stroke lineStyle) {
        this.lineStyle = lineStyle;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public String getUnit() {
        return unit;
    }

    public int getSymbolSize() {
        return symbolSize;
    }

    public void setSymbolSize(int symbolSize) {
        this.symbolSize = symbolSize;
    }

    public int getSymbolIndex() {
        return symbolIndex;
    }

    public void setSymbolIndex(int symbolIndex) {
        this.symbolIndex = symbolIndex;
    }

    public void updateUnit() {
        if (bands.size() > 0) {
            unit = getUnit(bands.get(0));
        }
        if (bands.size() > 1) {
            for (int i = 1; i < bands.size(); i++) {
                if (!unit.equals(getUnit(bands.get(i)))) {
                    unit = MIXED_UNITS;
                    return;
                }
            }
        }
    }

    private String getUnit(SpectrumBand band) {
        String bandUnit = band.getUnit();
        if(bandUnit == null) {
            bandUnit = "";
        }
        return bandUnit;
    }

    public void remove(int j) {
        bands.remove(j);
    }
}
