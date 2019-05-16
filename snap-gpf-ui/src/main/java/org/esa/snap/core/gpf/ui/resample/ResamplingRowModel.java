package org.esa.snap.core.gpf.ui.resample;

import org.esa.snap.core.gpf.common.resample.BandResamplingPreset;
import org.netbeans.swing.outline.RowModel;

/**
 * Created by obarrile on 22/04/2019.
 */

public class ResamplingRowModel implements RowModel {

    BandResamplingPreset[] bandResamplingPresets;
    BandsTreeModel myModel;

    public ResamplingRowModel(BandResamplingPreset[] bandResamplingPresets, BandsTreeModel myModel) {
        this.bandResamplingPresets = bandResamplingPresets;
        this.myModel = myModel;
    }

    @Override
    public Class getColumnClass(int column) {
        switch (column) {
            case 0:
                return String.class;
            case 1:
                return String.class;
            default:
                assert false;
        }
        return null;
    }
    @Override
    public int getColumnCount() {
        return 2;
    }
    @Override
    public String getColumnName(int column) {
        return column == 0 ? "Upsampling" : "Downsampling";
    }
    @Override
    public Object getValueFor(Object node, int column) {
        String stringNode = (String) node;
        for(BandResamplingPreset bandResamplingPreset : bandResamplingPresets) {
            if(bandResamplingPreset.getBandName().equals(stringNode)) {
                if(column == 0) {
                    return  bandResamplingPreset.getUpsamplingAlias();
                }
                if(column == 1) {
                    return  bandResamplingPreset.getDownsamplingAlias();
                }
            }
        }
        return null;
    }
    @Override
    public boolean isCellEditable(Object node, int column) {
        return true;
    }
    @Override
    public void setValueFor(Object node, int column, Object value) {
        String stringNode = (String) node;
        for(BandResamplingPreset bandResamplingPreset : bandResamplingPresets) {
            if(bandResamplingPreset.getBandName().equals(stringNode)) {
                if(column == 0) {
                    bandResamplingPreset.setUpsamplingAlias((String) value);
                }
                if(column == 1) {
                    bandResamplingPreset.setDownsamplingAlias((String) value);
                }
            }
        }

        //setValue to Children
        int childCount = myModel.getChildCount(stringNode);
        for ( int i = 0 ; i < childCount ; i++) {
            setValueFor(myModel.getChild(stringNode,i),column, value);
        }
    }
}